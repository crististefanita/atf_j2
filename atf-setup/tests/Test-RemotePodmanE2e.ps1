#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/remote-podman.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/RemotePodman.psm1" -Force
$ErrorActionPreference = 'Stop'
$output = New-AtfSetupOutput $OutputDirectory
$receipt = [ordered]@{ source = 'REAL_REMOTE_PODMAN_E2E'; observedAtUtc = [DateTime]::UtcNow.ToString('O'); status = 'RUNNING'
    rounds = @(foreach ($number in 1..2) { [ordered]@{ name = "round-$number"; status = 'NOT_RUN' } })
    acceptance = 'NOT_RUN'; limitation = 'Remote Podman provisioning only; no guest WSL2, Kafka or Eventuate claim.' }
$proofs = @()
try {
    $config = Read-AtfRemotePodmanConfig $ConfigPath
    foreach ($round in $receipt.rounds) {
        $round.status = 'RUNNING'
        try {
            $directory = Join-Path $output $round.name
            & "$PSScriptRoot/../Setup-RemotePodman.ps1" -ConfigPath $ConfigPath -OutputDirectory $directory *> (Join-Path $output "$($round.name).log")
            $setup = Get-Content -LiteralPath (Join-Path $directory 'remote-setup.json') -Raw | ConvertFrom-Json -AsHashtable
            $engine = Get-Content -LiteralPath (Join-Path $directory 'Initialize-RemotePodman/remote-podman.json') -Raw | ConvertFrom-Json -AsHashtable
            $probe = Get-Content -LiteralPath (Join-Path $directory 'Test-RemotePodman/podman-smoke.json') -Raw | ConvertFrom-Json -AsHashtable
            if ($setup.status -cne 'PASS' -or $engine.status -cne 'PASS' -or $probe.status -cne 'PASS' -or
                    $engine.url -cne $config.podman.url -or $probe.url -cne $config.podman.url -or
                    $probe.image -cne $config.probe.image -or $probe.containerExitCode -ne 0 -or $probe.containerRemoved -ne $true -or
                    $probe.probeId -notmatch '^[a-f0-9]{32}$' -or $probe.imageId -notmatch '^[a-f0-9]{64}$') {
                throw 'Remote setup did not produce complete native engine/container evidence.'
            }
            $proofs += $probe
            if ($round.name -eq 'round-2' -and $engine.connectionCreated -ne $false) { throw 'Second setup did not reuse the existing named connection.' }
            $round.status = 'PASS'
        } catch { $round.status = 'FAIL'; throw }
    }
    $receipt.acceptance = 'RUNNING'
    if ($proofs[0].probeId -ceq $proofs[1].probeId -or $proofs[0].imageId -cne $proofs[1].imageId) {
        throw 'Repeated setup reused a smoke proof or changed the immutable image.'
    }
    $receipt.probeIds = @($proofs.probeId)
    $receipt.imageId = $proofs[0].imageId
    $receipt.url = $config.podman.url
    $receipt.acceptance = 'PASS'
    $receipt.status = 'PASS'
} catch {
    $receipt.status = 'FAIL'; $receipt.failure = $_.Exception.Message
    if ($receipt.acceptance -eq 'RUNNING') { $receipt.acceptance = 'FAIL' }
    throw
} finally {
    $receipt | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $output 'remote-e2e.json') -Encoding utf8
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
