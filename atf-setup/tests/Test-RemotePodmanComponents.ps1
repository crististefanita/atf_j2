#Requires -Version 7.4
[CmdletBinding()]
param([string]$OutputDirectory)
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/RemotePodman.psm1" -Force
$ErrorActionPreference = 'Stop'
$output = New-AtfSetupOutput $OutputDirectory
$checks = [Collections.Generic.List[object]]::new()
$receipt = [ordered]@{ source = 'MOCK_REMOTE_PODMAN_COMPONENT'; status = 'RUNNING'; checks = $checks; engineExecuted = $false }
function Equal($Expected, $Actual, [string]$Name) {
    $passed = $Expected -ceq $Actual
    $checks.Add([ordered]@{ name = $Name; expected = $Expected; actual = $Actual; passed = $passed })
    if (-not $passed) { throw "Component assertion failed: $Name" }
}
function Refuses([scriptblock]$Action, [string]$Cause, [string]$Name) {
    $failure = $null
    try { & $Action | Out-Null } catch { $failure = $_.Exception.Message }
    $passed = $null -ne $failure -and $failure.Contains($Cause)
    $checks.Add([ordered]@{ name = $Name; expected = $Cause; actual = $failure; passed = $passed })
    if (-not $passed) { throw "Component refusal failed: $Name" }
}
try {
    $configPath = "$PSScriptRoot/../config/remote-podman.json"
    $config = Read-AtfRemotePodmanConfig $configPath
    Equal 'tcp://192.168.56.1:8888' $config.podman.url 'Reviewed host-only endpoint accepted'
    $mutatedPath = Join-Path $output 'invalid.json'
    foreach ($mutation in @('public', 'wildcard', 'credentials', 'path', 'query', 'port', 'scheme', 'owner', 'version', 'server', 'image', 'platform')) {
        $mutant = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json -AsHashtable
        $cause = switch ($mutation) {
            'public' { $mutant.podman.url = 'tcp://8.8.8.8:8888'; 'host-only subnet' }
            'wildcard' { $mutant.podman.url = 'tcp://0.0.0.0:8888'; 'host-only subnet' }
            'credentials' { $mutant.podman.url = 'tcp://user:pass@192.168.56.1:8888'; 'host-only subnet' }
            'path' { $mutant.podman.url = 'tcp://192.168.56.1:8888/api'; 'host-only subnet' }
            'query' { $mutant.podman.url = 'tcp://192.168.56.1:8888?other=endpoint'; 'host-only subnet' }
            'port' { $mutant.podman.url = 'tcp://192.168.56.1:80'; 'host-only subnet' }
            'scheme' { $mutant.podman.url = 'http://192.168.56.1:8888'; 'host-only subnet' }
            'owner' { $mutant.podman.connection = 'another-project'; 'connection owner' }
            'version' { $mutant.podman.version = 'latest'; 'connection owner' }
            'server' { $mutant.podman.serverVersion = 'latest'; 'server version' }
            'image' { $mutant.probe.image = 'docker.io/library/hello-world:latest'; 'immutable' }
            'platform' { $mutant.probe.platform = 'linux/arm64'; 'immutable' }
        }
        $mutant | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $mutatedPath -Encoding utf8
        Refuses { Read-AtfRemotePodmanConfig $mutatedPath } $cause "Rejects remote config: $mutation"
    }
    Refuses { Read-AtfRemotePodmanConfig "$PSScriptRoot/../config/local.json" } 'property' 'Local WSL profile cannot silently become remote'
    $connection = @{ Name = $config.podman.connection; URI = $config.podman.url }
    Assert-AtfRemoteConnection $config $connection
    Equal $false $connection.Contains('Identity') 'Actual TCP JSON without optional Identity is accepted'
    $connection.Identity = ''
    Assert-AtfRemoteConnection $config $connection
    Equal '' $connection.Identity 'Empty optional identity accepted'
    foreach ($field in @('Name', 'URI', 'Identity')) {
        $mutant = $connection.Clone()
        $mutant[$field] = 'another-destination'
        Refuses { Assert-AtfRemoteConnection $config $mutant } 'refusing overwrite' "Foreign connection $field refused"
    }
    function New-Engine {
        return @{ Client = @{ Version = $config.podman.version }; version = @{ Version = $config.podman.serverVersion }
            host = @{ os = 'linux'; arch = 'amd64'; security = @{ rootless = $true }; serviceIsRemote = $true } }
    }
    $reviewedEngine = New-Engine
    Assert-AtfRemoteEngine $config $reviewedEngine
    Equal '5.7.0' $reviewedEngine.version.Version 'Reviewed server accepted without rewriting its version'
    foreach ($mutation in @('client', 'server', 'os', 'arch', 'rootful', 'local')) {
        $engine = New-Engine
        switch ($mutation) {
            'client' { $engine.Client.Version = '5.8.2' }
            'server' { $engine.version.Version = '5.7.1' }
            'os' { $engine.host.os = 'windows' }
            'arch' { $engine.host.arch = 'arm64' }
            'rootful' { $engine.host.security.rootless = $false }
            'local' { $engine.host.serviceIsRemote = $false }
        }
        Refuses { Assert-AtfRemoteEngine $config $engine } 'rootless contract' "Rejects engine mismatch: $mutation"
    }
    $invalidOutput = Join-Path $output 'invalid-setup'
    $pwsh = (Get-Process -Id $PID).Path
    $run = Invoke-AtfSetupProcess $pwsh @('-NoProfile', '-NonInteractive', '-File', "$PSScriptRoot/../Setup-RemotePodman.ps1",
        '-ConfigPath', $mutatedPath, '-OutputDirectory', $invalidOutput) -OutputDirectory $output -AllowFailure
    $failed = Get-Content -LiteralPath (Join-Path $invalidOutput 'remote-setup.json') -Raw | ConvertFrom-Json -AsHashtable
    Equal 1 $run.ExitCode 'Invalid setup fails without interaction or engine access'
    Equal 'FAIL' $failed.status 'Parent preserves FAIL'
    Equal 1 $failed.stages.Count 'Smoke is not run after initialization failure'
    Equal 'FAIL' $failed.stages[0].status 'Original failed initialization is explicit'
    $receipt.status = 'PASS'
} catch { $receipt.status = 'FAIL'; $receipt.failure = $_.Exception.Message; throw }
finally {
    $receipt | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $output 'remote-components.json') -Encoding utf8
    Write-Output "ATF_SETUP_COMPONENTS=$($checks.Count)"
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
