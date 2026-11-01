#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath="$PSScriptRoot/config/eventuate-live.json",[string]$OutputDirectory)
Import-Module "$PSScriptRoot/scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/scripts/support/EventuateLab.psm1" -Force
$ErrorActionPreference='Stop'
$output=New-AtfSetupOutput $OutputDirectory
$primary=$null
$receipt=[ordered]@{source='PINNED_UPSTREAM_BUILD';status='RUNNING';jars=@();borrowedKafka='PRESERVE'}
try {
    $c=Read-AtfEventuateConfig $ConfigPath
    $receipt.infrastructure=Assert-AtfEventuateInfrastructure $c $output -RequiredAvailableMiB 1800
    $git=(Get-Command git -ErrorAction Stop).Source
    $source=Join-Path $output 'source.git'
    $null=Invoke-AtfSetupProcess $git @('clone','--bare','https://github.com/eventuate-tram/eventuate-tram-examples-customers-and-orders.git',$source) $output -TimeoutSeconds 120
    $revision=Invoke-AtfSetupProcess $git @('-C',$source,'rev-parse','--verify',$c.upstreamCommit) $output
    if($revision.Stdout.Trim() -cne $c.upstreamCommit){throw 'Upstream source identity differs.'}
    $archive=Join-Path $output 'upstream.tar'
    $null=Invoke-AtfSetupProcess $git @('-C',$source,'archive','--format=tar',"--output=$archive",$c.upstreamCommit) $output
    $name='atf-j2-eventuate-build-'+[guid]::NewGuid().ToString('N').Substring(0,10)
    $program='mkdir -p /workspace && tar -xf /tmp/source.tar -C /workspace && cd /workspace && exec gradle --no-daemon --max-workers=2 -Dorg.gradle.jvmargs=-Xmx768m -Dorg.gradle.internal.http.connectionTimeout=15000 -Dorg.gradle.internal.http.socketTimeout=30000 -PeventuateMavenRepoUrl=https://repo.maven.apache.org/maven2 :customer-service:customer-service-main:bootJar :order-service:order-service-main:bootJar'
    $null=Invoke-AtfEventuatePodman $c @('create','--name',$name,'--label',"io.atf.owner=$($c.owner)",'--label',"io.atf.spec=$($c.spec)",
        '--memory','1536m','--cpus','2','--user','root','--workdir','/tmp',$c.builderImage,'sh','-c',$program) $output -TimeoutSeconds 120
    $null=Invoke-AtfEventuatePodman $c @('cp',$archive,"${name}:/tmp/source.tar") $output -TimeoutSeconds 60
    $null=Invoke-AtfEventuatePodman $c @('start','--attach',$name) $output -TimeoutSeconds 900
    foreach($service in @('customer','order')) {
        $jar=Join-Path $output "$service.jar"
        $null=Invoke-AtfEventuatePodman $c @('cp',"${name}:/workspace/${service}-service/${service}-service-main/build/libs/${service}-service-main-0.1.0-SNAPSHOT.jar",$jar) $output -TimeoutSeconds 60
        $receipt.jars+=@{service=$service;file="$service.jar";sha256=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()}
    }
    $receipt.status='PASS';$receipt.container=$name;$receipt.upstreamCommit=$c.upstreamCommit;$receipt.builderImage=$c.builderImage
} catch {$primary=$_.Exception;$receipt.status='FAIL';$receipt.failure=$primary.Message;throw}
finally {
    Write-AtfEventuateReceipt $receipt (Join-Path $output 'eventuate-build.json') $primary
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
