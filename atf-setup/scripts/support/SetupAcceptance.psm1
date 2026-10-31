#Requires -Version 7.4
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module "$PSScriptRoot/AtfSetup.psm1"

function Assert-AtfKafkaReuseEvidence {
    param([Parameter(Mandatory)]$Config, [Parameter(Mandatory)][array]$Rounds)
    if ($Rounds.Count -ne 2) { throw 'Exactly two successful Setup rounds are required.' }
    $spec = Get-AtfKafkaSpecification $Config
    foreach ($round in $Rounds) {
        $setup = $round.setup; $ready = $round.ready; $proof = $round.proof
        if ($setup.source -ne 'ATF_SETUP' -or $setup.status -ne 'PASS' -or $setup.stages.Count -ne 3 -or
                ($setup.stages.name -join ',') -ne 'Initialize-Podman,Start-Kafka,Test-Kafka' -or
                @($setup.stages | Where-Object { $_.status -ne 'PASS' }).Count -ne 0) { throw 'Incomplete or failed Setup cannot certify reuse.' }
        if ($ready.source -ne 'REAL_KAFKA' -or $ready.status -ne 'READY' -or $ready.specification -ne $spec -or
                $ready.image -ne $Config.kafka.image -or $ready.bootstrapServers -ne "127.0.0.1:$($Config.kafka.hostPort)" -or
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
    }
    foreach ($key in @('containerId', 'imageId', 'image', 'specification')) {
        if ($Rounds[0].ready[$key] -ne $Rounds[1].ready[$key]) { throw "Kafka resource changed between Setup rounds: $key" }
    }
    if ($Rounds[0].proof.probeId -eq $Rounds[1].proof.probeId) { throw 'Replayed proof cannot certify the second roundtrip.' }
}

Export-ModuleMember -Function Assert-AtfKafkaReuseEvidence
