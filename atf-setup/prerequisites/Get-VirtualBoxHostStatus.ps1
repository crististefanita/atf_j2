#Requires -Version 7.4
[CmdletBinding()]
param([string]$Vm, [string]$OutputDirectory)
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
$ErrorActionPreference = 'Stop'
$output = New-AtfSetupOutput $OutputDirectory
$receipt = [ordered]@{ source = 'VIRTUALBOX_HOST_DIAGNOSTIC'; observedAtUtc = [DateTime]::UtcNow.ToString('O')
    vm = $Vm; status = 'RUNNING'; guestReadiness = 'NOT_TESTED' }
try {
    if ([string]::IsNullOrWhiteSpace($Vm)) { throw 'Specify -Vm with the actual VM name or UUID; interactive prompts are disabled by design.' }
    $command = Get-Command VBoxManage.exe -ErrorAction SilentlyContinue
    $executable = if ($command) { $command.Source } else { Join-Path $env:ProgramFiles 'Oracle/VirtualBox/VBoxManage.exe' }
    if (-not (Test-Path -LiteralPath $executable -PathType Leaf)) { throw 'Run on the physical VirtualBox host with VBoxManage installed; Guest Additions are insufficient.' }
    $version = Invoke-AtfSetupProcess $executable @('--version') -OutputDirectory $output
    $info = Invoke-AtfSetupProcess $executable @('showvminfo', $Vm, '--machinereadable') -OutputDirectory $output
    $log = Invoke-AtfSetupProcess $executable @('showvminfo', $Vm, '--log=0') -OutputDirectory $output
    $receipt.version = $version.Stdout.Trim()
    $receipt.configuration = @($info.Stdout -split '\r?\n' | Where-Object {
        $_ -match '^(VMState|nested-hw-virt|hwvirtex|paravirtprovider|effparavirtprovider|cpus)=' })
    $receipt.hypervisorLog = @($log.Stdout -split '\r?\n' | Where-Object { $_ -match '(?i)\bHM:|\bNEM:|VMX|VT-x|nested' })
    $receipt.status = 'COLLECTED'
} catch { $receipt.status = 'FAIL'; $receipt.failure = $_.Exception.Message; throw }
finally {
    $receipt | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $output 'virtualbox-host.json') -Encoding utf8
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
