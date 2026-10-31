#Requires -Version 7.4
[CmdletBinding()]
param([string]$OutputDirectory)
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/Kafka.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/SetupAcceptance.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/KafkaClient.psm1" -Force
$ErrorActionPreference='Stop'
$output=New-AtfSetupOutput $OutputDirectory
$checks=[Collections.Generic.List[object]]::new()
$receipt=[ordered]@{ source='MOCK_REMOTE_KAFKA_COMPONENT'; status='RUNNING'; checks=$checks; brokerExecuted=$false }
function Equal($Expected,$Actual,[string]$Name) {
    $passed=$Expected -ceq $Actual
    $checks.Add([ordered]@{name=$Name;expected=$Expected;actual=$Actual;passed=$passed})
    if(-not $passed){throw "Component assertion failed: $Name"}
}
function Refuses([scriptblock]$Action,[string]$Cause,[string]$Name) {
    $failure=$null
    try { & $Action | Out-Null } catch { $failure=$_.Exception.Message }
    $passed=$null -ne $failure -and $failure.Contains($Cause)
    $checks.Add([ordered]@{name=$Name;expected=$Cause;actual=$failure;passed=$passed})
    if(-not $passed){throw "Component refusal failed: $Name"}
}
try {
    $config=Read-AtfKafkaSetupConfig "$PSScriptRoot/../config/remote-kafka.json"
    $local=Read-AtfKafkaSetupConfig "$PSScriptRoot/../config/local.json"
    Equal '192.168.56.1:19092' (Get-AtfKafkaBootstrap $config) 'Remote metadata endpoint is the physical host, not guest loopback'
    Equal '127.0.0.1:19092' (Get-AtfKafkaBootstrap $local) 'Local route remains loopback'
    Equal $local.kafka.image $config.kafka.image 'Both profiles share the immutable broker recipe'
    $arguments=New-AtfKafkaRunArguments $config
    Equal $true ($arguments -contains '127.0.0.1:19092:19092') 'Remote engine port remains bound to engine loopback'
    Equal $true ($arguments -contains 'KAFKA_ADVERTISED_LISTENERS=EXTERNAL://192.168.56.1:19092,INTERNAL://atf-j2-kafka:29092') 'External and internal metadata endpoints are distinct'
    Equal $false ($arguments -contains '--replace' -or $arguments -contains '--privileged') 'Remote route does not relax container policy'
    $literal='literal $(not-a-command); "quoted" & symbols'
    $producer=New-AtfKafkaProducerArguments $config 'atf-j2-probe-component' $literal
    Equal $false ($producer -contains '-i') 'Producer avoids the remote stdin lifecycle race'
    Equal $literal $producer[-3] 'Payload remains one literal argument, outside the shell program'
    Equal $true ($producer -contains 'pipefail') 'Producer failure propagates through the fixed pipeline'
    $fixture=Join-Path $output 'config'
    [IO.Directory]::CreateDirectory($fixture) | Out-Null
    foreach($name in @('local.json','remote-podman.json')) { Copy-Item -LiteralPath "$PSScriptRoot/../config/$name" -Destination $fixture }
    $mutantPath=Join-Path $fixture 'remote-kafka.json'
    foreach($mutation in @('profile','owner','base','remote','public','guest','port','target')) {
        $profile=Get-Content -LiteralPath "$PSScriptRoot/../config/remote-kafka.json" -Raw | ConvertFrom-Json -AsHashtable
        $cause=switch($mutation) {
            'profile' {$profile.profile='AUTO';'Unsupported'}
            'owner' {$profile.owner='foreign';'Unsupported'}
            'base' {$profile.baseRecipe='../local.json';'configuration directory'}
            'remote' {$profile.podmanRecipe='C:/outside.json';'configuration directory'}
            'public' {$profile.externalHost='8.8.8.8';'host-only route'}
            'guest' {$profile.hostAccess.guestAddress='192.168.56.1';'host-only route'}
            'port' {$profile.hostAccess.port=19093;'host-only route'}
            'target' {$profile.hostAccess.connectAddress='0.0.0.0';'host-only route'}
        }
        $profile | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $mutantPath -Encoding utf8
        Refuses {Read-AtfKafkaSetupConfig $mutantPath} $cause "Rejects remote profile: $mutation"
    }
    function New-Rounds {
        return @(foreach($id in @(('a'*32),('b'*32))) {
            $externalId=if($id.StartsWith('a')){'c'*32}else{'d'*32}
            @{setup=@{source='ATF_SETUP';status='PASS';stages=@(foreach($name in @('Initialize-RemotePodman','Start-Kafka','Test-Kafka','Test-KafkaExternal')){@{name=$name;status='PASS'}})}
                ready=@{source='REAL_KAFKA';status='READY';specification=Get-AtfKafkaSpecification $config;image=$config.kafka.image
                    imageId='e'*64;containerId='f'*64;bootstrapServers='192.168.56.1:19092';engineTarget='atf-j2-host'}
                proof=@{source='REAL_KAFKA';status='PASS';hostTcpReachable=$true;brokerPath='container INTERNAL listener';containerId='f'*64;image=$config.kafka.image
                    probeId=$id;topic="atf-j2-probe-$id";group="atf-j2-probe-$id";partition='Partition:0';offset='Offset:0';engineTarget='atf-j2-host'
                    payload=@{probeId=$id;source='REAL_KAFKA';purpose='infrastructure-roundtrip'}}
                external=@{source='REAL_KAFKA_EXTERNAL';status='PASS';clientLocation='Windows caller';bootstrapServers='192.168.56.1:19092';containerId='f'*64
                    imageId='e'*64;image=$config.kafka.image;engineTarget='atf-j2-host';probeId=$externalId
                    client=@{source='REAL_KAFKA_EXTERNAL';status='PASS';probeId=$externalId;bootstrapServers='192.168.56.1:19092'
                        advertisedHost='192.168.56.1';advertisedPort=19092;topic="atf-j2-external-$externalId";group="atf-j2-external-$externalId"
                        topicCreated=$true;producerAcknowledged=$true;consumerMatched=$true;partition=0;offset=0
                        payload=(@{probeId=$externalId;source='REAL_KAFKA_EXTERNAL'} | ConvertTo-Json -Compress)}}
            }
        })
    }
    $accepted=$false
    Assert-AtfKafkaReuseEvidence $config (New-Rounds)
    $accepted=$true
    Equal $true $accepted 'Two internal and external roundtrips with a stable broker accepted'
    foreach($mutation in @('internal-only','external-fail','wrong-listener','wrong-port','no-ack','no-delivery','wrong-probe','offset-type','offset-negative','wrong-image','wrong-client-location','payload','replay','incomplete')) {
        $rounds=New-Rounds
        $cause=switch($mutation) {
            'internal-only' {$rounds[1].proof.hostTcpReachable=$false;'missing, stale or uncorrelated'}
            'external-fail' {$rounds[1].external.status='FAIL';'External Windows client'}
            'wrong-listener' {$rounds[1].external.client.advertisedHost='127.0.0.1';'External Windows client'}
            'wrong-port' {$rounds[1].external.client.advertisedPort=9092;'External Windows client'}
            'no-ack' {$rounds[1].external.client.producerAcknowledged=$false;'External Windows client'}
            'no-delivery' {$rounds[1].external.client.consumerMatched=$false;'External Windows client'}
            'wrong-probe' {$rounds[1].external.client.probeId='0'*32;'External Windows client'}
            'offset-type' {$rounds[1].external.client.offset='0';'External Windows client'}
            'offset-negative' {$rounds[1].external.client.offset=-1;'External Windows client'}
            'wrong-image' {$rounds[1].external.imageId='0'*64;'External Windows client'}
            'wrong-client-location' {$rounds[1].external.clientLocation='container';'External Windows client'}
            'payload' {$rounds[1].external.client.payload='{}';'External Windows client payload'}
            'replay' {$rounds[1].external=$rounds[0].external;'Replayed external'}
            'incomplete' {$rounds[1].setup.stages[3].status='NOT_RUN';'Incomplete or failed'}
        }
        Refuses {Assert-AtfKafkaReuseEvidence $config $rounds} $cause "Rejects evidence: $mutation"
    }
    $clientRecipe=Read-AtfKafkaClientRecipe "$PSScriptRoot/../config/kafka-client.json" '4.1.2'
    Equal 2 $clientRecipe.libraries.Count 'Client recipe declares the two required immutable libraries'
    Refuses {Read-AtfKafkaClientRecipe "$PSScriptRoot/../config/kafka-client.json" '4.1.3'} 'broker version' 'Different broker client recipe refused'
    $corrupted=Join-Path $output 'corrupted.jar'
    [IO.File]::WriteAllBytes($corrupted,[byte[]]@(1,2,3))
    Refuses {Assert-AtfKafkaClientLibrary $corrupted $clientRecipe.libraries[0].sha256} 'pinned image manifest' 'Corrupted client refused before Java execution'
    $packageOutput=Join-Path $output 'host-package'
    & "$PSScriptRoot/../scripts/Export-KafkaHostAccess.ps1" -OutputDirectory $packageOutput | Out-Null
    $package=Get-Content -LiteralPath (Join-Path $packageOutput 'package.json') -Raw | ConvertFrom-Json -AsHashtable
    $extracted=Join-Path $output 'extracted-host'
    [IO.Compression.ZipFile]::ExtractToDirectory((Join-Path $packageOutput 'ATF-Kafka-HostAccess.zip'),$extracted)
    Equal 7 $package.files.Count 'Host package includes all dependencies without a checkout on the physical host'
    foreach($file in $package.files) {
        Equal $file.sha256 (Get-FileHash -LiteralPath (Join-Path $extracted $file.path) -Algorithm SHA256).Hash "Host source readback: $($file.path)"
    }
    $launcher=Get-Content -LiteralPath (Join-Path $packageOutput 'Run-KafkaHostAccess.ps1') -Raw
    $encoded=[regex]::Match($launcher,"FromBase64String\('([^']+)'\)").Groups[1].Value
    Equal $package.archiveSha256 ([Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Convert]::FromBase64String($encoded)))) 'Single-file launcher embeds the exact reviewed archive'
    $receipt.status='PASS'
} catch {$receipt.status='FAIL';$receipt.failure=$_.Exception.Message;throw}
finally {
    $receipt | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $output 'remote-kafka-components.json') -Encoding utf8
    Write-Output "ATF_SETUP_COMPONENTS=$($checks.Count)"
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
