#Requires -Version 7.4
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module "$PSScriptRoot/AtfSetup.psm1"
Import-Module "$PSScriptRoot/Kafka.psm1"

function Assert-AtfKafkaReuseEvidence {
    param([Parameter(Mandatory)]$Config, [Parameter(Mandatory)][array]$Rounds)
    if ($Rounds.Count -ne 2) { throw 'Exactly two successful Setup rounds are required.' }
    $spec = Get-AtfKafkaSpecification $Config
    $remote = $Config.podman.provider -eq 'remote'
    $stages = if ($remote) { 'Initialize-RemotePodman,Start-Kafka,Test-Kafka,Test-KafkaExternal' }
        else { 'Initialize-Podman,Start-Kafka,Test-Kafka' }
    foreach ($round in $Rounds) {
        $setup = $round.setup; $ready = $round.ready; $proof = $round.proof
        if ($setup.source -ne 'ATF_SETUP' -or $setup.status -ne 'PASS' -or $setup.stages.Count -ne ($stages -split ',').Count -or
                ($setup.stages.name -join ',') -ne $stages -or
                @($setup.stages | Where-Object { $_.status -ne 'PASS' }).Count -ne 0) { throw 'Incomplete or failed Setup cannot certify reuse.' }
        if ($ready.source -ne 'REAL_KAFKA' -or $ready.status -ne 'READY' -or $ready.specification -ne $spec -or
                $ready.image -ne $Config.kafka.image -or $ready.bootstrapServers -ne (Get-AtfKafkaBootstrap $Config) -or
                $ready.containerId -notmatch '^[a-f0-9]{64}$' -or $ready.imageId -notmatch '^[a-f0-9]{64}$') {
            throw 'Ready evidence differs from the immutable local recipe.'
        }
        if ($proof.source -ne 'REAL_KAFKA' -or $proof.status -ne 'PASS' -or $proof.hostTcpReachable -ne $true -or
                $proof.brokerPath -ne 'container INTERNAL listener' -or $proof.containerId -ne $ready.containerId -or
                $proof.image -ne $ready.image -or $proof.probeId -notmatch '^[a-f0-9]{32}$' -or
                $proof.topic -ne "atf-j2-probe-$($proof.probeId)" -or $proof.group -ne "atf-j2-probe-$($proof.probeId)" -or
                $proof.partition -ne 'Partition:0' -or $proof.offset -notmatch '^Offset:\d+$' -or
                $proof.payload.probeId -ne $proof.probeId -or $proof.payload.source -ne 'REAL_KAFKA' -or
                $proof.payload.purpose -ne 'infrastructure-roundtrip') { throw 'Broker delivery is missing, stale or uncorrelated.' }
        if ($remote) {
            $external = $round.external; $client = $external.client
            try { $payload=$client.payload | ConvertFrom-Json -AsHashtable }
            catch { throw 'External Windows client payload is missing or invalid.' }
            if($payload -isnot [Collections.IDictionary] -or -not $payload.Contains('probeId') -or -not $payload.Contains('source')) {
                throw 'External Windows client payload is missing or invalid.'
            }
            if ($ready.engineTarget -cne $Config.podman.machine -or $proof.engineTarget -cne $Config.podman.machine -or
                    $external.source -cne 'REAL_KAFKA_EXTERNAL' -or $external.status -cne 'PASS' -or $external.clientLocation -cne 'Windows caller' -or
                    $external.bootstrapServers -cne (Get-AtfKafkaBootstrap $Config) -or $external.containerId -cne $ready.containerId -or
                    $external.imageId -cne $ready.imageId -or $external.image -cne $ready.image -or
                    $external.engineTarget -cne $Config.podman.machine -or $external.probeId -notmatch '^[a-f0-9]{32}$' -or
                    $client.source -cne 'REAL_KAFKA_EXTERNAL' -or $client.status -cne 'PASS' -or $client.probeId -cne $external.probeId -or
                    $client.bootstrapServers -cne (Get-AtfKafkaBootstrap $Config) -or
                    $client.advertisedHost -cne (Get-AtfKafkaExternalHost $Config) -or $client.advertisedPort -ne $Config.kafka.hostPort -or
                    $client.topic -cne "atf-j2-external-$($client.probeId)" -or $client.group -cne $client.topic -or
                    $client.topicCreated -ne $true -or $client.producerAcknowledged -ne $true -or $client.consumerMatched -ne $true -or
                    $payload.probeId -cne $client.probeId -or $payload.source -cne 'REAL_KAFKA_EXTERNAL' -or
                    $client.partition -ne 0 -or ($client.offset -isnot [long] -and $client.offset -isnot [int]) -or $client.offset -lt 0) {
                throw 'External Windows client evidence is missing, stale or inconsistent with the selected remote route.'
            }
        }
    }
    foreach ($key in @('containerId', 'imageId', 'image', 'specification')) {
        if ($Rounds[0].ready[$key] -ne $Rounds[1].ready[$key]) { throw "Kafka resource changed between Setup rounds: $key" }
    }
    if ($Rounds[0].proof.probeId -eq $Rounds[1].proof.probeId) { throw 'Replayed proof cannot certify the second roundtrip.' }
    if ($remote -and $Rounds[0].external.probeId -ceq $Rounds[1].external.probeId) { throw 'Replayed external client proof cannot certify reuse.' }
    if ($remote -and @($Rounds[0].proof.probeId,$Rounds[1].proof.probeId,$Rounds[0].external.probeId,$Rounds[1].external.probeId |
            Select-Object -Unique).Count -ne 4) { throw 'Probe identity reused across internal and external routes.' }
}

Export-ModuleMember -Function Assert-AtfKafkaReuseEvidence
