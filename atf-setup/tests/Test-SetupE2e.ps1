#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/local.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/SetupAcceptance.psm1" -Force
$ErrorActionPreference = 'Stop'
$config = Read-AtfSetupConfig $ConfigPath
$output = New-AtfSetupOutput $OutputDirectory
$receipt = [ordered]@{ source = 'REAL_SETUP_E2E'; observedAtUtc = [DateTime]::UtcNow.ToString('O'); status = 'RUNNING'
    rounds = @(foreach ($number in 1..2) { [ordered]@{ name = "round-$number"; status = 'NOT_RUN'; log = "round-$number.log" } })
    acceptance = 'NOT_RUN'; limitation = 'Infrastructure only; no Java host-client metadata, Eventuate saga or business idempotency claim.' }
$evidence = @()
try {
    foreach ($round in $receipt.rounds) {
        $name = $round.name
        $roundOutput = Join-Path $output $name
        $round.status = 'RUNNING'
        try {
            & "$PSScriptRoot/../Setup.ps1" -ConfigPath $ConfigPath -OutputDirectory $roundOutput *> (Join-Path $output "$name.log")
            $evidence += @{ setup = Get-Content -LiteralPath (Join-Path $roundOutput 'setup.json') -Raw | ConvertFrom-Json -AsHashtable
                ready = Get-Content -LiteralPath (Join-Path $roundOutput 'Start-Kafka/kafka-ready.json') -Raw | ConvertFrom-Json -AsHashtable
                proof = Get-Content -LiteralPath (Join-Path $roundOutput 'Test-Kafka/kafka-proof.json') -Raw | ConvertFrom-Json -AsHashtable }
            $round.status = 'PASS'
        } catch { $round.status = 'FAIL'; throw }
    }
    Assert-AtfKafkaReuseEvidence $config $evidence
    $receipt.acceptance = 'PASS'
    $receipt.containerId = $evidence[0].ready.containerId
    $receipt.probeIds = @($evidence | ForEach-Object { $_.proof.probeId })
    $receipt.status = 'PASS'
} catch {
    $receipt.status = 'FAIL'
    $receipt.failure = $_.Exception.Message
    if ($evidence.Count -eq 2) { $receipt.acceptance = 'FAIL' }
    throw
} finally {
    $receipt | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $output 'setup-e2e.json') -Encoding utf8
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
