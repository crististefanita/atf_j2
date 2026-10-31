#Requires -Version 7.4
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module "$PSScriptRoot/AtfSetup.psm1"

function New-AtfKafkaRunArguments {
    param([Parameter(Mandatory)]$Config)
    $kafka = $Config.kafka
    $spec = Get-AtfKafkaSpecification $Config
    $environment = [ordered]@{
        KAFKA_NODE_ID = '1'; KAFKA_PROCESS_ROLES = 'broker,controller'
        KAFKA_LISTENERS = "EXTERNAL://:$($kafka.hostPort),INTERNAL://:29092,CONTROLLER://:9093"
        KAFKA_ADVERTISED_LISTENERS = "EXTERNAL://127.0.0.1:$($kafka.hostPort),INTERNAL://$($kafka.container):29092"
        KAFKA_LISTENER_SECURITY_PROTOCOL_MAP = 'EXTERNAL:PLAINTEXT,INTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT'
        KAFKA_INTER_BROKER_LISTENER_NAME = 'INTERNAL'; KAFKA_CONTROLLER_LISTENER_NAMES = 'CONTROLLER'
        KAFKA_CONTROLLER_QUORUM_VOTERS = "1@$($kafka.container):9093"; CLUSTER_ID = $kafka.clusterId
        KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR = '1'; KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR = '1'
        KAFKA_TRANSACTION_STATE_LOG_MIN_ISR = '1'; KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS = '0'
        KAFKA_SHARE_COORDINATOR_STATE_TOPIC_REPLICATION_FACTOR = '1'; KAFKA_SHARE_COORDINATOR_STATE_TOPIC_MIN_ISR = '1'
        KAFKA_LOG_DIRS = '/var/lib/kafka/data'; KAFKA_HEAP_OPTS = "-Xms128m -Xmx$($kafka.heapMiB)m"
    }
    $arguments = [Collections.Generic.List[string]]::new()
    foreach ($value in @('run', '--detach', '--name', $kafka.container, '--hostname', $kafka.container,
            '--network', $kafka.network, '--platform', $kafka.platform,
            '--label', "io.atf.owner=$($Config.owner)", '--label', "io.atf.spec=$spec",
            '--publish', "127.0.0.1:$($kafka.hostPort):$($kafka.hostPort)",
            '--memory', "$($kafka.memoryMiB)m", '--cpus', '1', '--volume', "$($kafka.volume):/var/lib/kafka/data")) {
        $arguments.Add($value)
    }
    foreach ($key in $environment.Keys) { $arguments.Add('--env'); $arguments.Add("$key=$($environment[$key])") }
    $arguments.Add($kafka.image)
    return $arguments.ToArray()
}

function Get-AtfKafkaContainer {
    param([Parameter(Mandatory)]$Config, [Parameter(Mandatory)][string]$OutputDirectory, [switch]$AllowMissing)
    $exists = Invoke-AtfPodman $Config @('container', 'exists', $Config.kafka.container) -OutputDirectory $OutputDirectory -AllowFailure
    if ($exists.ExitCode -eq 1 -and $AllowMissing) { return $null }
    if ($exists.ExitCode -ne 0) { throw "Kafka container is absent or engine unavailable ($($exists.ExitCode))." }
    $inspect = Invoke-AtfPodman $Config @('inspect', $Config.kafka.container) -OutputDirectory $OutputDirectory
    $container = @($inspect.Stdout | ConvertFrom-Json -AsHashtable)[0]
    Assert-AtfResourceOwner $container $Config.owner (Get-AtfKafkaSpecification $Config)
    $image = Invoke-AtfPodman $Config @('image', 'inspect', $Config.kafka.image) -OutputDirectory $OutputDirectory
    $expected = @($image.Stdout | ConvertFrom-Json -AsHashtable)[0]
    if ($container.Image -ne $expected.Id) { throw 'Existing Kafka image ID differs from the immutable recipe.' }
    return $container
}

function Wait-AtfKafkaReady {
    param([Parameter(Mandatory)]$Config, [Parameter(Mandatory)][string]$OutputDirectory)
    $clock = [Diagnostics.Stopwatch]::StartNew()
    $budget = $Config.kafka.readinessSeconds
    do {
        $remaining = $budget - $clock.Elapsed.TotalSeconds
        if ($remaining -lt 1) { break }
        $remoteLimit = "$([Math]::Min(4, [Math]::Floor($remaining)))s"
        $probe = Invoke-AtfPodman $Config @('exec', $Config.kafka.container, 'timeout', '--signal=TERM', '--kill-after=1s',
            $remoteLimit, '/opt/kafka/bin/kafka-broker-api-versions.sh',
            '--bootstrap-server', "$($Config.kafka.container):29092") -OutputDirectory $OutputDirectory `
            -TimeoutSeconds ([Math]::Min(5, [Math]::Floor($remaining))) -AllowFailure
        if ($probe.ExitCode -eq 0 -and $clock.Elapsed.TotalSeconds -le $budget) { return }
        $remaining = $budget - $clock.Elapsed.TotalSeconds
        if ($remaining -gt 0) { Start-Sleep -Milliseconds ([Math]::Min(500, [Math]::Floor($remaining * 1000))) }
    } while ($clock.Elapsed.TotalSeconds -lt $budget)
    throw "Kafka did not become ready within the single $budget-second budget. Container retained; evidence: $OutputDirectory"
}

Export-ModuleMember -Function New-AtfKafkaRunArguments, Get-AtfKafkaContainer, Wait-AtfKafkaReady
