#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/local.json", [string]$OutputDirectory, [switch]$InternalOnly)
Import-Module "$PSScriptRoot/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/support/Kafka.psm1" -Force
$ErrorActionPreference = 'Stop'
$config = Read-AtfKafkaSetupConfig $ConfigPath
$output = New-AtfSetupOutput $OutputDirectory
$container = Get-AtfKafkaContainer $config $output
if (-not $container.State.Running) { throw 'Kafka container is stopped; run Start-Kafka.ps1.' }
Wait-AtfKafkaReady $config $output
if (-not $InternalOnly) {
    $socket = [Net.Sockets.TcpClient]::new()
    try { $socket.ConnectAsync((Get-AtfKafkaExternalHost $config), $config.kafka.hostPort).WaitAsync([TimeSpan]::FromSeconds(5)).GetAwaiter().GetResult() | Out-Null }
    finally { $socket.Dispose() }
}
$id = [guid]::NewGuid().ToString('N')
$topic = "atf-j2-probe-$id"
$group = "atf-j2-probe-$id"
$bootstrap = "$($config.kafka.container):29092"
$prefix = @('exec', $config.kafka.container, 'timeout', '--signal=TERM', '--kill-after=1s', '15s')
Invoke-AtfPodman $config ($prefix + @('/opt/kafka/bin/kafka-topics.sh', '--bootstrap-server', $bootstrap, '--create',
    '--topic', $topic, '--partitions', '1', '--replication-factor', '1', '--config', 'retention.ms=3600000')) -OutputDirectory $output | Out-Null
$message = [ordered]@{ probeId = $id; source = 'REAL_KAFKA'; purpose = 'infrastructure-roundtrip' } | ConvertTo-Json -Compress
Invoke-AtfPodman $config (New-AtfKafkaProducerArguments $config $topic $message) -OutputDirectory $output -TimeoutSeconds 20 | Out-Null
$consumed = Invoke-AtfPodman $config ($prefix + @('/opt/kafka/bin/kafka-console-consumer.sh', '--bootstrap-server', $bootstrap,
    '--topic', $topic, '--group', $group, '--from-beginning', '--max-messages', '1', '--timeout-ms', '10000',
    '--property', 'print.partition=true', '--property', 'print.offset=true')) -OutputDirectory $output -TimeoutSeconds 20
$lines = @($consumed.Stdout -split '\r?\n' | Where-Object { $_.Contains($id) })
if ($lines.Count -ne 1) { throw 'Expected one uniquely correlated broker delivery.' }
$columns = $lines[0] -split "`t"
$actual = $columns[-1] | ConvertFrom-Json -AsHashtable
if ($actual.probeId -ne $id -or $actual.source -ne 'REAL_KAFKA' -or $columns.Count -ne 3 -or
        $columns[0] -notmatch '^Partition:0$' -or $columns[1] -notmatch '^Offset:\d+$') { throw 'Broker delivery identity or position differs.' }
[ordered]@{ source = 'REAL_KAFKA'; status = 'PASS'; observedAtUtc = [DateTime]::UtcNow.ToString('O')
    topic = $topic; group = $group; probeId = $id; partition = $columns[0]; offset = $columns[1]
    payload = $actual; hostTcpReachable = -not $InternalOnly; brokerPath = 'container INTERNAL listener'
    bootstrapServers = Get-AtfKafkaBootstrap $config; engineTarget = $config.podman.machine
    containerId = $container.Id; image = $config.kafka.image
    limitation = 'Internal CLI roundtrip; an InternalOnly run does not certify guest TCP or external Kafka metadata. No Eventuate or business idempotency claim.' } |
    ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $output 'kafka-proof.json') -Encoding utf8
Write-Output "ATF_SETUP_RECEIPT=$output"
