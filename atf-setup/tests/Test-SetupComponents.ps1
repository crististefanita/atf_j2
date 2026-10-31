#Requires -Version 7.4
[CmdletBinding()]
param([string]$OutputDirectory)
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/Host.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/SetupAcceptance.psm1" -Force
$ErrorActionPreference = 'Stop'
$output = New-AtfSetupOutput $OutputDirectory
$checks = [Collections.Generic.List[object]]::new()
$receipt = [ordered]@{ source = 'MOCK_SETUP_COMPONENT'; status = 'RUNNING'; checks = $checks; brokerExecuted = $false }
function Equal($Expected, $Actual, [string]$Name) {
    $passed = $Expected -ceq $Actual
    $checks.Add([ordered]@{ name = $Name; expected = $Expected; actual = $Actual; passed = $passed })
    if (-not $passed) { throw "Component assertion failed: $Name" }
}
function Refuses([scriptblock]$Action, [string]$Cause, [string]$Name) {
    $failure = $null
    try { & $Action } catch { $failure = $_.Exception.Message }
    $passed = $null -ne $failure -and $failure.Contains($Cause)
    $checks.Add([ordered]@{ name = $Name; expected = $Cause; actual = $failure; passed = $passed })
    if (-not $passed) { throw "Component refusal failed: $Name" }
}
try {
    $marker = [pscustomobject]@{ RecordId = 100; TimeCreated = [datetime]'2026-10-31T10:00:00Z' }
    $events = @(
        [pscustomobject]@{ RecordId = 90; Id = 41; TimeCreated = [datetime]'2026-10-31T11:00:00Z'; LevelDisplayName = 'Error'; Message = 'Previous boot' },
        [pscustomobject]@{ RecordId = 104; Id = 41; TimeCreated = [datetime]'2026-10-31T10:00:01Z'; LevelDisplayName = 'Error'; Message = 'Current boot VMX failure' })
    foreach ($shift in @(-130, 130)) {
        $queries = [Collections.Generic.List[hashtable]]::new()
        $reader = {
            param([hashtable]$Request)
            $queries.Add($Request)
            if ($Request.ContainsKey('FilterHashtable') -and $Request.FilterHashtable.Id -eq 12) { return $marker }
            if ($Request.FilterXPath -notmatch 'EventRecordID >= (\d+)') { throw 'Missing record boundary.' }
            $boundary = [long]$Matches[1]
            return @($events | Where-Object { $_.RecordId -ge $boundary })
        }.GetNewClosure()
        $result = Get-AtfBootEvidence $marker.TimeCreated.AddSeconds($shift) $reader
        Equal 'SYSTEM_RECORD_ID' $result.bootCorrelation "Clock shift $shift uses System ordering"
        Equal 104 $result.hypervisorBootEvents[0].RecordId "Clock shift $shift retains current Event 41"
        Equal 1 $result.hypervisorBootEvents.Count "Clock shift $shift excludes previous boot despite its later timestamp"
        Equal $false $queries[1].ContainsKey('FilterHashtable') "Clock shift $shift avoids CIM timestamp filtering"
        Equal 2 $queries.Count "Clock shift $shift reads bounded marker and event queries"
    }
    $queries = [Collections.Generic.List[hashtable]]::new()
    $fallback = [datetime]'2026-10-31T10:02:10Z'
    $reader = {
        param([hashtable]$Request)
        $queries.Add($Request)
        if ($Request.FilterHashtable.Id -eq 12) { throw 'Boot marker access denied' }
        return @()
    }.GetNewClosure()
    $result = Get-AtfBootEvidence $fallback $reader
    Equal 'CIM_TIME_FALLBACK' $result.bootCorrelation 'Unavailable marker discloses reduced correlation'
    Equal 'Boot marker access denied' $result.bootMarkerDiagnostic 'Original boot query diagnostic retained'
    Equal $fallback $queries[1].FilterHashtable.StartTime 'Fallback uses the supplied CIM boundary'
    Equal 0 $result.hypervisorBootEvents.Count 'No events is explicit and does not imply readiness'
    $reader = { param([hashtable]$Request) throw 'System log unavailable' }
    $result = Get-AtfBootEvidence $fallback $reader
    Equal 'System log unavailable' $result.hypervisorEventDiagnostic 'Event query failure retained independently'

    $missingVm = Join-Path $output 'missing-vm'
    $pwsh = (Get-Process -Id $PID).Path
    $process = Invoke-AtfSetupProcess $pwsh @('-NoProfile', '-NonInteractive', '-File',
        "$PSScriptRoot/../prerequisites/Get-VirtualBoxHostStatus.ps1", '-OutputDirectory', $missingVm) `
        -OutputDirectory $output -TimeoutSeconds 5 -AllowFailure
    $hostReceipt = Get-Content -LiteralPath (Join-Path $missingVm 'virtualbox-host.json') -Raw | ConvertFrom-Json -AsHashtable
    Equal 1 $process.ExitCode 'Missing VM fails silently without an interactive prompt'
    Equal 'FAIL' $hostReceipt.status 'Missing VM preserves a failed host receipt'
    Equal 'NOT_TESTED' $hostReceipt.guestReadiness 'Host diagnostic cannot certify guest readiness'
    Equal $true $hostReceipt.failure.Contains('Specify -Vm') 'Missing input retains its exact diagnostic'

    $config = Read-AtfSetupConfig "$PSScriptRoot/../config/local.json"
    function New-Rounds {
        $rounds = foreach ($id in @(('a' * 32), ('b' * 32))) {
            @{ setup = @{ source = 'ATF_SETUP'; status = 'PASS'; stages = @(
                        @{ name = 'Initialize-Podman'; status = 'PASS' }, @{ name = 'Start-Kafka'; status = 'PASS' }, @{ name = 'Test-Kafka'; status = 'PASS' }) }
                ready = @{ source = 'REAL_KAFKA'; status = 'READY'; specification = Get-AtfKafkaSpecification $config
                    image = $config.kafka.image; imageId = 'c' * 64; containerId = 'd' * 64; bootstrapServers = '127.0.0.1:19092' }
                proof = @{ source = 'REAL_KAFKA'; status = 'PASS'; containerId = 'd' * 64; image = $config.kafka.image
                    hostTcpReachable = $true; brokerPath = 'container INTERNAL listener'; probeId = $id
                    topic = "atf-j2-probe-$id"; group = "atf-j2-probe-$id"; partition = 'Partition:0'; offset = 'Offset:0'
                    payload = @{ probeId = $id; source = 'REAL_KAFKA'; purpose = 'infrastructure-roundtrip' } } }
        }
        return $rounds
    }
    $accepted = $false
    try { Assert-AtfKafkaReuseEvidence $config (New-Rounds); $accepted = $true } catch { }
    Equal $true $accepted 'Two independent roundtrips with a stable resource accepted'
    foreach ($mutation in @('setup', 'stage', 'missing', 'recipe', 'image', 'resource', 'replay', 'payload', 'offset', 'host', 'source')) {
        $rounds = @(New-Rounds)
        $cause = switch ($mutation) {
            'setup' { $rounds[1].setup.status = 'FAIL'; 'Incomplete or failed Setup' }
            'stage' { $rounds[1].setup.stages[2].status = 'FAIL'; 'Incomplete or failed Setup' }
            'missing' { $rounds = @($rounds[0]); 'Exactly two' }
            'recipe' { $rounds[1].ready.specification = 'stale'; 'immutable local recipe' }
            'image' { $rounds[1].ready.image = 'docker.io/apache/kafka:latest'; 'immutable local recipe' }
            'resource' { $rounds[1].ready.containerId = 'e' * 64; $rounds[1].proof.containerId = 'e' * 64; 'resource changed' }
            'replay' { $rounds[1].proof = $rounds[0].proof; 'Replayed proof' }
            'payload' { $rounds[1].proof.payload.probeId = 'stale'; 'uncorrelated' }
            'offset' { $rounds[1].proof.offset = 'unknown'; 'uncorrelated' }
            'host' { $rounds[1].proof.hostTcpReachable = $false; 'uncorrelated' }
            'source' { $rounds[1].proof.source = 'MOCK'; 'uncorrelated' }
        }
        Refuses { Assert-AtfKafkaReuseEvidence $config $rounds } $cause "Rejects $mutation counterexample"
    }
    $receipt.status = 'PASS'
} catch { $receipt.status = 'FAIL'; $receipt.failure = $_.Exception.Message; throw }
finally {
    $receipt | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $output 'setup-components.json') -Encoding utf8
    Write-Output "ATF_SETUP_COMPONENTS=$($checks.Count)"
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
