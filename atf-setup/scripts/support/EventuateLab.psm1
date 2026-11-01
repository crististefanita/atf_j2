#Requires -Version 7.4
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
Import-Module "$PSScriptRoot/AtfSetup.psm1"

function Read-AtfEventuateConfig {
    param([Parameter(Mandatory)][string]$Path)
    $c=Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json -AsHashtable
    if($c.schemaVersion -ne 1 -or $c.owner -cne 'atf-j2-eventuate' -or
       $c.connection -cne 'atf-j2-host' -or $c.borrowedKafka -cne 'atf-j2-kafka' -or
       $c.network -cne 'atf-j2-kafka-net' -or $c.bootstrap -cne 'atf-j2-kafka:29092' -or
       $c.upstreamCommit -notmatch '^[a-f0-9]{40}$') {throw 'Unreviewed Eventuate laboratory identity.'}
    foreach($field in @('builderImage','javaImage','databaseImage','cdcImage')) {
        if($c[$field] -notmatch '^docker\.io/[a-z0-9/-]+:[a-zA-Z0-9._-]+@sha256:[a-f0-9]{64}$') {throw 'Immutable Eventuate image required.'}
    }
    foreach($field in @('minimumAvailableMiB','startupSeconds')) {
        if($c[$field] -isnot [int] -and $c[$field] -isnot [long]){throw 'Integer laboratory limit required.'}
    }
    if($c.minimumAvailableMiB -lt 2048 -or $c.startupSeconds -lt 1 -or $c.startupSeconds -gt 180){throw 'Invalid laboratory limits.'}
    $c.spec=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes(($c | ConvertTo-Json -Depth 8 -Compress)))).ToLowerInvariant()
    return $c
}

function Invoke-AtfEventuatePodman {
    param([Parameter(Mandatory)]$Config,[Parameter(Mandatory)][string[]]$Arguments,
          [Parameter(Mandatory)][string]$OutputDirectory,[int]$TimeoutSeconds=30,[switch]$AllowFailure)
    Invoke-AtfSetupProcess -File (Resolve-AtfPodman) -Arguments (@('--connection',$Config.connection)+$Arguments) `
        -OutputDirectory $OutputDirectory -TimeoutSeconds $TimeoutSeconds -AllowFailure:$AllowFailure
}

function Get-AtfEventuateContainer {
    param([Parameter(Mandatory)]$Config,[Parameter(Mandatory)][string]$Name,[Parameter(Mandatory)][string]$OutputDirectory)
    if($Name -notmatch '^atf-j2-eventuate-[a-z0-9-]+$'){throw 'Only Eventuate-owned container names are permitted.'}
    $exists=Invoke-AtfEventuatePodman $Config @('container','exists',$Name) $OutputDirectory -AllowFailure
    if($exists.ExitCode -eq 1){return $null}
    if($exists.ExitCode -ne 0){throw 'Cannot establish container existence.'}
    $result=Invoke-AtfEventuatePodman $Config @('inspect',$Name) $OutputDirectory
    $container=@($result.Stdout | ConvertFrom-Json -AsHashtable)[0]
    Assert-AtfResourceOwner $container $Config.owner $Config.spec
    return $container
}

function Assert-AtfEventuateInfrastructure {
    param([Parameter(Mandatory)]$Config,[Parameter(Mandatory)][string]$OutputDirectory,[switch]$DatabaseOnly,[int]$RequiredAvailableMiB=0)
    $r=Invoke-AtfEventuatePodman $Config @('inspect',$Config.borrowedKafka) $OutputDirectory
    $broker=@($r.Stdout | ConvertFrom-Json -AsHashtable)[0]
    if(-not $broker.State.Running -or -not $broker.NetworkSettings.Networks.Contains($Config.network)) {throw 'Borrowed Kafka is not ready on the reviewed network.'}
    $memory=Invoke-AtfEventuatePodman $Config @('exec',$Config.borrowedKafka,'cat','/proc/meminfo') $OutputDirectory
    $match=[regex]::Match($memory.Stdout,'(?m)^MemAvailable:\s+(\d+)\s+kB')
    if(-not $match.Success){throw 'Cannot establish engine memory budget.'}
    $available=[long]$match.Groups[1].Value/1024
    $minimum=if($RequiredAvailableMiB -gt 0){$RequiredAvailableMiB}elseif($DatabaseOnly){768}else{$Config.minimumAvailableMiB}
    if($available -lt $minimum){throw "Eventuate requires $minimum MiB available; observed $([math]::Floor($available)) MiB. Existing containers and engine are preserved."}
    return @{brokerId=$broker.Id;brokerStartedAt=$broker.State.StartedAt;availableMiB=[math]::Floor($available)}
}

function Wait-AtfEventuateCommand {
    param([Parameter(Mandatory)]$Config,[Parameter(Mandatory)][string[]]$Arguments,
          [Parameter(Mandatory)][string]$OutputDirectory)
    $clock=[Diagnostics.Stopwatch]::StartNew()
    do {
        $result=Invoke-AtfEventuatePodman $Config $Arguments $OutputDirectory -TimeoutSeconds 10 -AllowFailure
        if($result.ExitCode -eq 0){return}
        Start-Sleep -Milliseconds 500
    } while($clock.Elapsed.TotalSeconds -lt $Config.startupSeconds)
    throw "Eventuate readiness exceeded $($Config.startupSeconds) seconds; existing services are preserved."
}

function Write-AtfEventuateReceipt {
    param([Parameter(Mandatory)]$Receipt,[Parameter(Mandatory)][string]$Path,[AllowNull()][Exception]$Primary)
    try { [IO.File]::WriteAllText($Path,($Receipt | ConvertTo-Json -Depth 12),[Text.UTF8Encoding]::new($false)) }
    catch {
        if($null -eq $Primary){throw}
        $Primary.Data['ATF_SETUP_RECEIPT_FAILURE']=$_.Exception.GetBaseException().Message
        [Console]::Error.WriteLine("ATF_SETUP_SECONDARY_RECEIPT_FAILURE=$($_.Exception.GetBaseException().Message)")
    }
}

Export-ModuleMember -Function Read-AtfEventuateConfig,Invoke-AtfEventuatePodman,Get-AtfEventuateContainer,Assert-AtfEventuateInfrastructure,Wait-AtfEventuateCommand,Write-AtfEventuateReceipt
