#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath="$PSScriptRoot/config/eventuate-live.json",[string]$ApplicationJarsDirectory,
      [string]$OutputDirectory,[switch]$DatabaseOnly)
Import-Module "$PSScriptRoot/scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/scripts/support/EventuateLab.psm1" -Force
$ErrorActionPreference='Stop'
$output=New-AtfSetupOutput $OutputDirectory
$primary=$null
$receipt=[ordered]@{source='REAL_EVENTUATE_SETUP';status='RUNNING';services=@();borrowedKafka='PRESERVE';scope=if($DatabaseOnly){'DATABASE_ONLY'}else{'APPLICATION'}}
try {
    $c=Read-AtfEventuateConfig $ConfigPath
    $receipt.infrastructure=Assert-AtfEventuateInfrastructure $c $output -DatabaseOnly:$DatabaseOnly
    $db='atf-j2-eventuate-db'
    $container=Get-AtfEventuateContainer $c $db $output
    if(-not $container) {
        $null=Invoke-AtfEventuatePodman $c @('run','--detach','--name',$db,'--network',$c.network,
            '--label',"io.atf.owner=$($c.owner)",'--label',"io.atf.spec=$($c.spec)",'--memory','384m','--cpus','1',
            '--env','MYSQL_ROOT_PASSWORD=atf-lab-admin','--env','MYSQL_DATABASE=eventuate',
            '--env','MYSQL_USER=atf_app','--env','MYSQL_PASSWORD=atf-lab-app',
            $c.databaseImage,'--innodb-buffer-pool-size=64M','--performance-schema=OFF','--max-connections=35') $output -TimeoutSeconds 120
    } elseif(-not $container.State.Running) {throw 'Owned database is stopped; explicit operator recovery required, no automatic replacement.'}
    Wait-AtfEventuateCommand $c @('exec',$db,'mysqladmin','--user=root','--password=atf-lab-admin','ping','--silent') $output
    $reader=Join-Path (Split-Path -Parent $ConfigPath) 'eventuate-reader.sql'
    $null=Invoke-AtfEventuatePodman $c @('cp',$reader,"${db}:/tmp/atf-reader.sql") $output
    $null=Invoke-AtfEventuatePodman $c @('exec',$db,'mysql','--user=root','--password=atf-lab-admin','--execute=source /tmp/atf-reader.sql') $output
    $receipt.services+=@{name=$db;status='READY'}
    if(-not $DatabaseOnly) {
        foreach($service in @('customers','orders')) {
            $jar=Join-Path $ApplicationJarsDirectory $(if($service -eq 'customers'){'customer.jar'}else{'order.jar'})
            if(-not (Test-Path -LiteralPath $jar -PathType Leaf)){throw "Built upstream JAR required: $jar"}
            $jarHash=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()
            $name="atf-j2-eventuate-$service"
            $container=Get-AtfEventuateContainer $c $name $output
            if(-not $container) {
                $null=Invoke-AtfEventuatePodman $c @('create','--name',$name,'--network',$c.network,
                    '--label',"io.atf.owner=$($c.owner)",'--label',"io.atf.spec=$($c.spec)",'--label',"io.atf.jar=$jarHash",'--memory','320m','--cpus','1',
                    '--env','SPRING_DATASOURCE_URL=jdbc:mysql://atf-j2-eventuate-db:3306/eventuate?useSSL=false&allowPublicKeyRetrieval=true',
                    '--env','SPRING_DATASOURCE_USERNAME=atf_app','--env','SPRING_DATASOURCE_PASSWORD=atf-lab-app',
                    '--env','SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=4','--env','SPRING_DATASOURCE_HIKARI_MINIMUMIDLE=1',
                    '--env',"EVENTUATELOCAL_KAFKA_BOOTSTRAP_SERVERS=$($c.bootstrap)",'--env','MANAGEMENT_TRACING_EXPORT_ENABLED=false',
                    $c.javaImage,'java','-Xms32m','-Xmx112m','-XX:MaxMetaspaceSize=128m','-XX:ReservedCodeCacheSize=32m','-Xss512k','-jar','/tmp/service.jar') $output
                $null=Invoke-AtfEventuatePodman $c @('cp',$jar,"${name}:/tmp/service.jar") $output -TimeoutSeconds 60
                $null=Invoke-AtfEventuatePodman $c @('start',$name) $output
            } elseif(-not $container.State.Running -or $container.Config.Labels['io.atf.jar'] -cne $jarHash) {throw 'Existing application state or JAR differs; replacement refused.'}
            $receipt.services+=@{name=$name;status='STARTED';jarSha256=$jarHash}
        }
        $name='atf-j2-eventuate-cdc'
        $container=Get-AtfEventuateContainer $c $name $output
        if(-not $container) {
            $null=Invoke-AtfEventuatePodman $c @('run','--detach','--name',$name,'--network',$c.network,
                '--label',"io.atf.owner=$($c.owner)",'--label',"io.atf.spec=$($c.spec)",'--memory','384m','--cpus','1',
                '--env','JAVA_OPTS=-Xms32m -Xmx128m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=32m',
                '--env','SPRING_PROFILES_ACTIVE=KafkaLeadership','--env',"EVENTUATELOCAL_KAFKA_BOOTSTRAP_SERVERS=$($c.bootstrap)",
                '--env','EVENTUATE_CDC_READER_READER1_TYPE=polling',
                '--env','EVENTUATE_CDC_READER_READER1_DATASOURCEURL=jdbc:mysql://atf-j2-eventuate-db:3306/eventuate?useSSL=false&allowPublicKeyRetrieval=true',
                '--env','EVENTUATE_CDC_READER_READER1_DATASOURCEUSERNAME=atf_app','--env','EVENTUATE_CDC_READER_READER1_DATASOURCEPASSWORD=atf-lab-app',
                '--env','EVENTUATE_CDC_READER_READER1_DATASOURCEDRIVERCLASSNAME=com.mysql.cj.jdbc.Driver',
                '--env','EVENTUATE_CDC_READER_READER1_LEADERSHIPLOCKPATH=/atf-j2/eventuate/cdc/reader1',
                '--env','EVENTUATE_CDC_READER_READER1_OUTBOXID=1','--env','EVENTUATE_CDC_READER_READER1_POLLINGINTERVALINMILLISECONDS=100',
                '--env','EVENTUATE_CDC_PIPELINE_PIPELINE1_TYPE=eventuate-tram','--env','EVENTUATE_CDC_PIPELINE_PIPELINE1_READER=reader1',
                '--env','EVENTUATE_CDC_PIPELINE_PIPELINE1_EVENTUATEDATABASESCHEMA=eventuate',$c.cdcImage) $output -TimeoutSeconds 120
        } elseif(-not $container.State.Running) {throw 'Owned CDC is stopped; replacement refused.'}
        $receipt.services+=@{name=$name;status='STARTED';reader='REAL_OUTBOX_POLLING'}
        $receipt.status='STARTED_REQUIRES_READINESS'
    } else {$receipt.status='DATABASE_READY_APPLICATION_NOT_RUN'}
} catch {$primary=$_.Exception;$receipt.status='FAIL';$receipt.failure=$primary.Message;throw}
finally {
    Write-AtfEventuateReceipt $receipt (Join-Path $output 'eventuate-setup.json') $primary
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
