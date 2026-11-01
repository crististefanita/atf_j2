#Requires -Version 7.4
[CmdletBinding()]
param([string]$OutputDirectory)
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/EventuateLab.psm1" -Force
$ErrorActionPreference='Stop'
$output=New-AtfSetupOutput $OutputDirectory
$checks=[Collections.Generic.List[object]]::new()
function Equal($Expected,$Actual,[string]$Name) {
    $passed=$Expected -ceq $Actual
    $checks.Add([ordered]@{name=$Name;expected=$Expected;actual=$Actual;passed=$passed})
    if(-not $passed){throw "Component assertion failed: $Name"}
}
function Refuses([scriptblock]$Action,[string]$Cause,[string]$Name) {
    $message=$null
    try { & $Action | Out-Null } catch {$message=$_.Exception.Message}
    Equal $true ($null -ne $message -and $message.Contains($Cause)) $Name
}
$receipt=[ordered]@{source='MOCK_EVENTUATE_SETUP_COMPONENT';status='RUNNING';brokerExecuted=$false;checks=$checks}
try {
    $config=Read-AtfEventuateConfig "$PSScriptRoot/../config/eventuate-live.json"
    Equal 'atf-j2-kafka:29092' $config.bootstrap 'Container bootstrap stays on the existing internal network'
    Equal 2560 $config.minimumAvailableMiB 'Full application memory gate stays explicit'
    foreach($field in @('owner','connection','network','upstreamCommit','javaImage','minimumAvailableMiB')) {
        $mutant=Get-Content -LiteralPath "$PSScriptRoot/../config/eventuate-live.json" -Raw | ConvertFrom-Json -AsHashtable
        $mutant[$field]=switch($field){'minimumAvailableMiB'{512} 'javaImage'{'docker.io/library/java:latest'} default{'foreign'}}
        $path=Join-Path $output "mutant-$field.json"
        [IO.File]::WriteAllText($path,($mutant | ConvertTo-Json -Depth 8))
        Refuses {Read-AtfEventuateConfig $path} $(switch($field){'javaImage'{'Immutable'} 'minimumAvailableMiB'{'Invalid'} default{'identity'}}) "Refuses changed $field"
    }
    $module=Get-Module EventuateLab
    & $module {
        function script:Invoke-AtfEventuatePodman {
            param($Config,$Arguments,$OutputDirectory,$TimeoutSeconds,$AllowFailure)
            if($Arguments[0] -eq 'inspect') {
                return @{Stdout='[{"Id":"borrowed-id","State":{"Running":true,"StartedAt":"unchanged"},"NetworkSettings":{"Networks":{"atf-j2-kafka-net":{}}}}]';ExitCode=0}
            }
            if($Arguments[0] -eq 'exec'){return @{Stdout="MemAvailable: 1331200 kB`n";ExitCode=0}}
            throw 'Offline component refused an unexpected Podman operation'
        }
    }
    $facts=Assert-AtfEventuateInfrastructure $config $output -RequiredAvailableMiB 512
    Equal 'borrowed-id' $facts.brokerId 'Read-only infrastructure inventory preserves borrowed identity'
    Equal 1300 $facts.availableMiB 'Memory gate uses MemAvailable'
    Refuses {Assert-AtfEventuateInfrastructure $config $output} 'requires 2560 MiB' 'Low memory refuses full application without resource eviction'
    $blocked=Join-Path $output 'blocked';[IO.File]::WriteAllText($blocked,'file, not directory')
    $primary=[InvalidOperationException]::new('original execution failure')
    Write-AtfEventuateReceipt @{status='FAIL'} (Join-Path $blocked 'receipt.json') $primary
    Equal 'original execution failure' $primary.Message 'Receipt failure preserves the original exception'
    Equal $true $primary.Data.Contains('ATF_SETUP_RECEIPT_FAILURE') 'Receipt failure remains a secondary diagnostic'
    Refuses {Write-AtfEventuateReceipt @{status='PASS'} (Join-Path $blocked 'receipt.json') $null} 'Could not find' 'Receipt-only failure remains a failure'
    $errors=$null;$tokens=$null
    foreach($name in @('Build-EventuateApplication.ps1','Start-EventuateLive.ps1','Run-EventuateAcceptance.ps1','scripts/Export-EventuateRuntime.ps1')) {
        $null=[Management.Automation.Language.Parser]::ParseFile((Join-Path "$PSScriptRoot/.." $name),[ref]$tokens,[ref]$errors)
        Equal 0 $errors.Count "Parser accepts $name"
    }
    $receipt.status='PASS'
} catch {$receipt.status='FAIL';$receipt.failure=$_.Exception.Message;throw}
finally {
    [IO.File]::WriteAllText((Join-Path $output 'components.json'),($receipt | ConvertTo-Json -Depth 10))
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
