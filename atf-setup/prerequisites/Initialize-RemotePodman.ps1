#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/remote-podman.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/RemotePodman.psm1" -Force
$ErrorActionPreference = 'Stop'
$output = New-AtfSetupOutput $OutputDirectory
$receipt = [ordered]@{ source = 'REAL_REMOTE_PODMAN'; observedAtUtc = [DateTime]::UtcNow.ToString('O'); status = 'RUNNING'
    guestWslReady = 'NOT_TESTED'; kafkaReady = 'NOT_TESTED'; connectionCreated = $false }
try {
    $config = Read-AtfRemotePodmanConfig $ConfigPath
    $receipt.connection = $config.podman.connection
    $receipt.url = $config.podman.url
    $receipt.transport = $config.podman.transport
    $podman = Resolve-AtfPodman
    $version = Invoke-AtfSetupProcess $podman @('--version') -OutputDirectory $output
    if ($version.Stdout.Trim() -cne "podman version $($config.podman.version)") { throw 'Installed Podman client version differs.' }
    $info = Invoke-AtfRemotePodman $config @('info', '--format', 'json') -OutputDirectory $output
    $engine = $info.Stdout | ConvertFrom-Json -AsHashtable
    Assert-AtfRemoteEngine $config $engine
    $connection = Get-AtfRemoteConnection $config $output
    if ($null -eq $connection) {
        Invoke-AtfSetupProcess $podman @('system', 'connection', 'add', $config.podman.connection, $config.podman.url) -OutputDirectory $output | Out-Null
        $receipt.connectionCreated = $true
        $connection = Get-AtfRemoteConnection $config $output
        if ($null -eq $connection) { throw 'Named connection was not persisted.' }
    }
    $named = Invoke-AtfSetupProcess $podman @('--connection', $config.podman.connection, 'info', '--format', 'json') -OutputDirectory $output
    Assert-AtfRemoteEngine $config ($named.Stdout | ConvertFrom-Json -AsHashtable)
    $receipt.clientVersion = $engine.Client.Version
    $receipt.serverVersion = $engine.version.Version
    $receipt.rootless = $engine.host.security.rootless
    $receipt.serverHostname = $engine.host.hostname
    $receipt.serverKernel = $engine.host.kernel
    $receipt.status = 'PASS'
} catch { $receipt.status = 'FAIL'; $receipt.failure = $_.Exception.Message; throw }
finally {
    $receipt | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $output 'remote-podman.json') -Encoding utf8
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
