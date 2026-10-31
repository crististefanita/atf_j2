#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/remote-kafka.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/support/Kafka.psm1" -Force
Import-Module "$PSScriptRoot/support/KafkaClient.psm1" -Force
$ErrorActionPreference = 'Stop'
$output = New-AtfSetupOutput $OutputDirectory
$receipt = [ordered]@{ source='REAL_KAFKA_EXTERNAL'; observedAtUtc=[DateTime]::UtcNow.ToString('O'); status='RUNNING'
    clientLocation='Windows caller'; dependencies=@(); limitation='Infrastructure metadata and correlated delivery; no Eventuate saga or business idempotency claim.' }
try {
    $config = Read-AtfKafkaSetupConfig $ConfigPath
    $container = Get-AtfKafkaContainer $config $output
    if (-not $container.State.Running) { throw 'Kafka container is not running.' }
    $java = (Get-Command java.exe -ErrorAction Stop).Source
    $javac = (Get-Command javac.exe -ErrorAction Stop).Source
    $version = Invoke-AtfSetupProcess $java @('-version') -OutputDirectory $output
    if (($version.Stdout + $version.Stderr) -notmatch 'version "21\.') { throw 'The infrastructure client requires the reviewed Java 21 toolchain.' }
    $libraries = Join-Path $output 'client'
    $classes = Join-Path $output 'classes'
    [IO.Directory]::CreateDirectory($libraries) | Out-Null
    [IO.Directory]::CreateDirectory($classes) | Out-Null
    $imageVersion = [regex]::Match($config.kafka.image, '^docker\.io/apache/kafka:([0-9.]+)@').Groups[1].Value
    $clientRecipe = Read-AtfKafkaClientRecipe "$PSScriptRoot/../config/kafka-client.json" $imageVersion
    foreach ($library in $clientRecipe.libraries) {
        $name = $library.name
        $destination = Join-Path $libraries $name
        Invoke-AtfPodman $config @('cp', "$($config.kafka.container):/opt/kafka/libs/$name", $destination) -OutputDirectory $output | Out-Null
        $actualHash = Assert-AtfKafkaClientLibrary $destination $library.sha256
        $receipt.dependencies += @{ name=$name; sha256=$actualHash }
    }
    $source = [IO.Path]::GetFullPath("$PSScriptRoot/../probes/java/KafkaExternalProbe.java")
    $classpath = Join-Path $libraries '*'
    Invoke-AtfSetupProcess $javac @('-proc:none', '--release', '21', '-cp', $classpath, '-d', $classes, $source) -OutputDirectory $output | Out-Null
    $receipt.probeSourceSha256 = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
    $receipt.probeId = [guid]::NewGuid().ToString('N')
    $nativeReceipt = Join-Path $output 'client-proof.json'
    $run = Invoke-AtfSetupProcess $java @('-cp', "$classes;$classpath", 'KafkaExternalProbe', (Get-AtfKafkaBootstrap $config),
        (Get-AtfKafkaExternalHost $config), "$($config.kafka.hostPort)", $receipt.probeId, $nativeReceipt) -OutputDirectory $output -TimeoutSeconds 60 -AllowFailure
    if (Test-Path -LiteralPath $nativeReceipt -PathType Leaf) { $receipt.client = Get-Content -LiteralPath $nativeReceipt -Raw | ConvertFrom-Json -AsHashtable }
    if ($run.ExitCode -ne 0) { throw "External Kafka client failed ($($run.ExitCode)); native receipt/logs: $output" }
    $proof = $receipt.client
    if ($proof.status -cne 'PASS' -or $proof.source -cne 'REAL_KAFKA_EXTERNAL' -or $proof.probeId -cne $receipt.probeId -or
            $proof.advertisedHost -cne (Get-AtfKafkaExternalHost $config) -or $proof.advertisedPort -ne $config.kafka.hostPort -or
            $proof.producerAcknowledged -ne $true -or $proof.consumerMatched -ne $true -or $proof.partition -ne 0 -or $proof.offset -lt 0) {
        throw 'External Kafka metadata or delivery evidence is incomplete.'
    }
    $receipt.containerId = $container.Id
    $receipt.image = $config.kafka.image
    $receipt.imageId = $container.Image
    $receipt.bootstrapServers = Get-AtfKafkaBootstrap $config
    $receipt.engineTarget = $config.podman.machine
    $receipt.status = 'PASS'
} catch { $receipt.status='FAIL'; $receipt.failure=$_.Exception.Message; throw }
finally {
    $receipt | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $output 'kafka-external.json') -Encoding utf8
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
