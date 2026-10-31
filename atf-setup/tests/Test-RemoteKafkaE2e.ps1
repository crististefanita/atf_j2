#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/remote-kafka.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/Kafka.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/SetupAcceptance.psm1" -Force
$ErrorActionPreference = 'Stop'
$output = New-AtfSetupOutput $OutputDirectory
$receipt = [ordered]@{ source='REAL_REMOTE_KAFKA_E2E'; observedAtUtc=[DateTime]::UtcNow.ToString('O'); status='RUNNING'
    rounds=@(foreach ($number in 1..2) { [ordered]@{ name="round-$number"; status='NOT_RUN' } })
    acceptance='NOT_RUN'; limitation='Infrastructure only; no Eventuate saga, business deduplication or application E2E claim.' }
$evidence = @()
try {
    $config = Read-AtfKafkaSetupConfig $ConfigPath
    foreach ($round in $receipt.rounds) {
        $round.status = 'RUNNING'
        try {
            $directory = Join-Path $output $round.name
            & "$PSScriptRoot/../Setup-RemoteKafka.ps1" -ConfigPath $ConfigPath -OutputDirectory $directory *> (Join-Path $output "$($round.name).log")
            $evidence += @{ setup=Get-Content -LiteralPath (Join-Path $directory 'setup.json') -Raw | ConvertFrom-Json -AsHashtable
                ready=Get-Content -LiteralPath (Join-Path $directory 'Start-Kafka/kafka-ready.json') -Raw | ConvertFrom-Json -AsHashtable
                proof=Get-Content -LiteralPath (Join-Path $directory 'Test-Kafka/kafka-proof.json') -Raw | ConvertFrom-Json -AsHashtable
                external=Get-Content -LiteralPath (Join-Path $directory 'Test-KafkaExternal/kafka-external.json') -Raw | ConvertFrom-Json -AsHashtable }
            $round.status = 'PASS'
        } catch { $round.status='FAIL'; throw }
    }
    $receipt.acceptance='RUNNING'
    Assert-AtfKafkaReuseEvidence $config $evidence
    $receipt.containerId=$evidence[0].ready.containerId
    $receipt.bootstrapServers=Get-AtfKafkaBootstrap $config
    $receipt.internalProbeIds=@($evidence | ForEach-Object { $_.proof.probeId })
    $receipt.externalProbeIds=@($evidence | ForEach-Object { $_.external.probeId })
    $receipt.acceptance='PASS'; $receipt.status='PASS'
} catch {
    $receipt.status='FAIL'; $receipt.failure=$_.Exception.Message
    if ($receipt.acceptance -eq 'RUNNING') { $receipt.acceptance='FAIL' }
    throw
} finally {
    $receipt | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $output 'remote-kafka-e2e.json') -Encoding utf8
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
