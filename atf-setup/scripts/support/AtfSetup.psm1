#Requires -Version 7.4
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Read-AtfSetupConfig {
    param([Parameter(Mandatory)][string]$Path)
    $config = Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json -AsHashtable
    if ($config.schemaVersion -ne 1 -or $config.owner -ne 'atf-j2') { throw 'Unsupported setup schema or workspace owner.' }
    foreach ($name in @($config.podman.machine, $config.kafka.container, $config.kafka.network, $config.kafka.volume)) {
        if ($name -notmatch '^atf-j2(?:-[a-z0-9-]+)?$') { throw "Invalid ATF resource name: $name" }
    }
    if ($config.podman.provider -ne 'wsl' -or $config.podman.packageId -ne 'RedHat.Podman' -or $config.podman.version -ne '5.8.3') {
        throw 'This reviewed recipe requires Podman 5.8.3 with the WSL provider.'
    }
    if ($config.kafka.image -notmatch '^docker\.io/apache/kafka:[0-9.]+@sha256:[a-f0-9]{64}$' -or $config.kafka.platform -ne 'linux/amd64') {
        throw 'Kafka must use the reviewed Linux amd64 image with an immutable digest.'
    }
    foreach ($value in @($config.kafka.hostPort, $config.kafka.memoryMiB, $config.kafka.heapMiB, $config.kafka.readinessSeconds,
            $config.podman.memoryMiB, $config.podman.cpus, $config.podman.diskGiB)) {
        if ($value -isnot [long] -and $value -isnot [int]) { throw 'Setup limits must be JSON integers.' }
        if ($value -le 0) { throw 'Setup limits must be positive.' }
    }
    if ($config.kafka.hostPort -lt 1024 -or $config.kafka.hostPort -gt 65535 -or
            $config.kafka.heapMiB -ge $config.kafka.memoryMiB -or $config.kafka.readinessSeconds -gt 300) {
        throw 'Invalid broker port, heap or readiness budget.'
    }
    if ($config.kafka.clusterId -notmatch '^[A-Za-z0-9_-]{22}$') { throw 'Invalid Kafka cluster ID.' }
    return $config
}

function New-AtfSetupOutput {
    param([string]$Directory)
    if (-not $Directory) { $Directory = Join-Path ([IO.Path]::GetTempPath()) ('atf-setup-' + [guid]::NewGuid().ToString('N')) }
    $absolute = [IO.Path]::GetFullPath($Directory)
    if (Test-Path -LiteralPath $absolute) { throw "Output must be a fresh directory: $absolute" }
    [IO.Directory]::CreateDirectory($absolute) | Out-Null
    return $absolute
}

function Invoke-AtfSetupProcess {
    param([Parameter(Mandatory)][string]$File, [string[]]$Arguments = @(),
        [Parameter(Mandatory)][string]$OutputDirectory, [int]$TimeoutSeconds = 30,
        [AllowNull()][string]$InputText, [switch]$AllowFailure, [hashtable]$Environment = @{})
    $id = [guid]::NewGuid().ToString('N')
    $start = [Diagnostics.ProcessStartInfo]::new($File)
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.RedirectStandardInput = $true
    if ([IO.Path]::GetFileName($File) -ieq 'wsl.exe') {
        $start.StandardOutputEncoding = [Text.Encoding]::Unicode
        $start.StandardErrorEncoding = [Text.Encoding]::Unicode
    }
    foreach ($argument in $Arguments) { $start.ArgumentList.Add($argument) }
    foreach ($key in $Environment.Keys) { $start.Environment[$key] = $Environment[$key] }
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $start
    $clock = [Diagnostics.Stopwatch]::StartNew()
    try {
        if (-not $process.Start()) { throw "Cannot start $File" }
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        if ($null -ne $InputText) { $process.StandardInput.Write($InputText) }
        $process.StandardInput.Close()
        $expired = -not $process.WaitForExit($TimeoutSeconds * 1000)
        if ($expired) { $process.Kill($true); $process.WaitForExit() }
        $stdout = $stdoutTask.GetAwaiter().GetResult()
        $stderr = $stderrTask.GetAwaiter().GetResult()
        $code = if ($expired) { 124 } else { $process.ExitCode }
    } finally { $clock.Stop(); $process.Dispose() }
    [IO.File]::WriteAllText((Join-Path $OutputDirectory "$id.stdout.log"), $stdout)
    [IO.File]::WriteAllText((Join-Path $OutputDirectory "$id.stderr.log"), $stderr)
    $receipt = [ordered]@{ file = $File; arguments = $Arguments; exitCode = $code; elapsedMs = $clock.ElapsedMilliseconds
        stdoutLog = "$id.stdout.log"; stderrLog = "$id.stderr.log" }
    $receipt | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $OutputDirectory "$id.command.json") -Encoding utf8
    if ($code -ne 0 -and -not $AllowFailure) { throw "Command failed ($code): $File; evidence: $OutputDirectory; $stderr" }
    return [pscustomobject]@{ ExitCode = $code; Stdout = $stdout; Stderr = $stderr; ElapsedMs = $clock.ElapsedMilliseconds }
}

function Resolve-AtfPodman {
    $command = Get-Command podman.exe -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $installed = Join-Path $env:ProgramFiles 'RedHat/Podman/podman.exe'
    if (Test-Path -LiteralPath $installed -PathType Leaf) { return $installed }
    throw 'Podman is absent. Run Install-Podman.ps1 first.'
}

function Assert-AtfResourceOwner {
    param([Parameter(Mandatory)]$Resource, [Parameter(Mandatory)][string]$Owner,
        [Parameter(Mandatory)][string]$Specification)
    $labels = if ($Resource.Contains('Config')) { $Resource.Config.Labels }
        elseif ($Resource.Contains('Labels')) { $Resource['Labels'] } else { $Resource['labels'] }
    if ($null -eq $labels -or $labels['io.atf.owner'] -ne $Owner -or $labels['io.atf.spec'] -ne $Specification) {
        throw 'Existing resource ownership or specification differs; refusing adoption, replacement or deletion.'
    }
}

function Get-AtfKafkaSpecification {
    param([Parameter(Mandatory)]$Config)
    $bytes = [Text.Encoding]::UTF8.GetBytes(($Config.kafka | ConvertTo-Json -Depth 8 -Compress))
    return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes)).ToLowerInvariant()
}

function Invoke-AtfPodman {
    param([Parameter(Mandatory)]$Config, [Parameter(Mandatory)][string[]]$Arguments,
        [Parameter(Mandatory)][string]$OutputDirectory, [int]$TimeoutSeconds = 30,
        [AllowNull()][string]$InputText, [switch]$AllowFailure)
    $target = if ($Config.podman.provider -eq 'remote') { @('--url', $Config.podman.url) }
        else { @('--connection', $Config.podman.machine) }
    Invoke-AtfSetupProcess -File (Resolve-AtfPodman) -Arguments ($target + $Arguments) `
        -OutputDirectory $OutputDirectory -TimeoutSeconds $TimeoutSeconds -InputText $InputText -AllowFailure:$AllowFailure
}

Export-ModuleMember -Function Read-AtfSetupConfig, New-AtfSetupOutput, Invoke-AtfSetupProcess, Resolve-AtfPodman,
    Assert-AtfResourceOwner, Get-AtfKafkaSpecification, Invoke-AtfPodman
