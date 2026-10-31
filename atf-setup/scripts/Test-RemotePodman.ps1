#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/remote-podman.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/support/RemotePodman.psm1" -Force
$ErrorActionPreference = 'Stop'
$output = New-AtfSetupOutput $OutputDirectory
$receipt = [ordered]@{ source = 'REAL_REMOTE_PODMAN_CONTAINER'; observedAtUtc = [DateTime]::UtcNow.ToString('O'); status = 'RUNNING'
    guestWslReady = 'NOT_TESTED'; kafkaReady = 'NOT_TESTED'; applicationE2e = 'NOT_TESTED' }
try {
    $config = Read-AtfRemotePodmanConfig $ConfigPath
    $receipt.url = $config.podman.url
    $connection = Get-AtfRemoteConnection $config $output
    if ($null -eq $connection) { throw 'Initialize the reviewed remote connection first.' }
    $info = Invoke-AtfRemotePodman $config @('info', '--format', 'json') -OutputDirectory $output
    Assert-AtfRemoteEngine $config ($info.Stdout | ConvertFrom-Json -AsHashtable)
    Invoke-AtfRemotePodman $config @('pull', '--platform', $config.probe.platform, $config.probe.image) `
        -OutputDirectory $output -TimeoutSeconds 180 | Out-Null
    $image = Invoke-AtfRemotePodman $config @('image', 'inspect', $config.probe.image) -OutputDirectory $output
    $actual = @($image.Stdout | ConvertFrom-Json -AsHashtable)[0]
    if ($actual.Os -cne 'linux' -or $actual.Architecture -cne 'amd64' -or $actual.RepoDigests -cnotcontains $config.probe.image) {
        throw 'Smoke image identity or platform differs from the immutable recipe.'
    }
    $receipt.probeId = [guid]::NewGuid().ToString('N')
    $receipt.containerName = "atf-j2-podman-probe-$($receipt.probeId)"
    $receipt.image = $config.probe.image
    $receipt.imageId = $actual.Id
    $run = Invoke-AtfRemotePodman $config @('run', '--rm', '--pull=never', '--network=none', '--name', $receipt.containerName,
        '--label', "io.atf.owner=$($config.owner)", '--label', "io.atf.probe=$($receipt.probeId)",
        '--platform', $config.probe.platform, $config.probe.image) -OutputDirectory $output
    if (-not $run.Stdout.Contains($config.probe.expectedOutput)) { throw 'Container output did not confirm the reviewed hello-world execution.' }
    $absent = Invoke-AtfRemotePodman $config @('container', 'exists', $receipt.containerName) -OutputDirectory $output -AllowFailure
    if ($absent.ExitCode -ne 1) { throw 'The uniquely owned smoke container was not removed, or its absence could not be verified.' }
    $receipt.containerExitCode = $run.ExitCode
    $receipt.containerRemoved = $true
    $receipt.expectedOutput = $config.probe.expectedOutput
    $receipt.stdout = $run.Stdout
    $receipt.status = 'PASS'
} catch { $receipt.status = 'FAIL'; $receipt.failure = $_.Exception.Message; throw }
finally {
    $receipt | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $output 'podman-smoke.json') -Encoding utf8
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
