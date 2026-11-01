#Requires -Version 7.4
[CmdletBinding()]
param([Parameter(Mandatory)][string]$RuntimeDirectory,[string]$ConfigPath="$PSScriptRoot/config/eventuate-live.json",[string]$OutputDirectory)
Import-Module "$PSScriptRoot/scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/scripts/support/EventuateLab.psm1" -Force
$ErrorActionPreference='Stop'
$output=New-AtfSetupOutput $OutputDirectory
$primary=$null
$receipt=[ordered]@{source='REAL_INFRASTRUCTURE_SYNTHETIC_EVENT';applicationAcceptance='NOT_RUN';status='RUNNING';borrowedKafka='PRESERVE'}
try {
    $c=Read-AtfEventuateConfig $ConfigPath
    $receipt.infrastructure=Assert-AtfEventuateInfrastructure $c $output -RequiredAvailableMiB 512
    $db=Get-AtfEventuateContainer $c 'atf-j2-eventuate-db' $output
    if(-not $db -or -not $db.State.Running){throw 'Owned MySQL must be running; no fallback database is selected.'}
    $runtime=[IO.Path]::GetFullPath($RuntimeDirectory)
    $manifest=Get-Content -LiteralPath (Join-Path $runtime 'runtime-manifest.json') -Raw | ConvertFrom-Json -AsHashtable
    foreach($inputFile in $manifest.inputs) {
        if(($inputFile.path -notmatch '^(classes/\d+/|lib/)[A-Za-z0-9_./$-]+$' -and $inputFile.path -cne 'config/atf.yaml') -or $inputFile.path.Contains('..')){throw 'Unsafe runtime manifest path.'}
        $file=Join-Path $runtime $inputFile.path
        if((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant() -cne $inputFile.sha256){throw 'Runtime changed after export.'}
    }
    $actual=@(Get-ChildItem -LiteralPath $runtime -File -Recurse | ForEach-Object {[IO.Path]::GetRelativePath($runtime,$_.FullName).Replace('\','/')} |
        Where-Object {$_ -notin @('runtime-manifest.json','classpath.txt')} | Sort-Object)
    $expected=@($manifest.inputs | ForEach-Object {$_.path} | Sort-Object)
    if(Compare-Object $actual $expected){throw 'Runtime has missing or unrecorded files.'}
    if((Get-FileHash -LiteralPath (Join-Path $runtime 'classpath.txt') -Algorithm SHA256).Hash.ToLowerInvariant() -cne $manifest.classpathSha256){throw 'Runtime classpath changed after export.'}
    $classpath=[IO.File]::ReadAllText((Join-Path $runtime 'classpath.txt'))
    if($classpath.Split(':') | Where-Object {-not $_.StartsWith('/tmp/atf-runtime/') -or $_.Contains('..')}){throw 'Runtime classpath must stay inside the owned container directory.'}
    $name='atf-j2-eventuate-probe-'+[guid]::NewGuid().ToString('N').Substring(0,10)
    $null=Invoke-AtfEventuatePodman $c @('create','--name',$name,'--network',$c.network,
        '--label',"io.atf.owner=$($c.owner)",'--label',"io.atf.spec=$($c.spec)",'--memory','384m','--cpus','1',
        $c.javaImage,'java','-Xms32m','-Xmx160m','-XX:MaxMetaspaceSize=128m','-XX:ReservedCodeCacheSize=32m','-Xss512k',
        '-Datf.eventuate.live.enabled=true','-cp',$classpath,'atf.eventuate.tests.support.acceptance.live.InfrastructureAcceptance',
        '/tmp/atf-runtime/results','/tmp/atf-runtime/config/atf.yaml') $output
    $null=Invoke-AtfEventuatePodman $c @('cp',$runtime,"${name}:/tmp/atf-runtime") $output -TimeoutSeconds 90
    $execution=Invoke-AtfEventuatePodman $c @('start','--attach',$name) $output -TimeoutSeconds 120 -AllowFailure
    $null=Invoke-AtfEventuatePodman $c @('logs',$name) $output -TimeoutSeconds 15 -AllowFailure
    $container=Get-AtfEventuateContainer $c $name $output
    $receipt.container=$name;$receipt.exitCode=$container.State.ExitCode;$receipt.clientExitCode=$execution.ExitCode
    $copy=Invoke-AtfEventuatePodman $c @('cp',"${name}:/tmp/atf-runtime/results",(Join-Path $output 'results')) $output -TimeoutSeconds 60 -AllowFailure
    if($container.State.Running -or $container.State.ExitCode -ne 0 -or $execution.ExitCode -ne 0 -or $copy.ExitCode -ne 0){throw 'Infrastructure probe failed; inspect owned process and copied evidence. Engine and broker are preserved.'}
    $probe=Get-Content -LiteralPath (Join-Path $output 'results/receipt.json') -Raw | ConvertFrom-Json -AsHashtable
    if($probe.status -cne 'PASS' -or $probe.applicationAcceptance -cne 'NOT_RUN'){throw 'Infrastructure receipt contract differs.'}
    $receipt.status='PASS'
} catch {$primary=$_.Exception;$receipt.status='FAIL';$receipt.failure=$primary.Message;throw}
finally {
    Write-AtfEventuateReceipt $receipt (Join-Path $output 'eventuate-probe.json') $primary
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
