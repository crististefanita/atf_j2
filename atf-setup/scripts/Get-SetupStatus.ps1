#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/local.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/support/Host.psm1" -Force
$ErrorActionPreference = 'Stop'
$config = Read-AtfSetupConfig $ConfigPath
$output = New-AtfSetupOutput $OutputDirectory
$status = [ordered]@{ observedAtUtc = [DateTime]::UtcNow.ToString('O'); source = 'HOST_PREFLIGHT'; engineReady = $false; status = 'BLOCKED' }
$status.host = Get-CimInstance Win32_ComputerSystem | Select-Object Manufacturer, Model, HypervisorPresent, TotalPhysicalMemory
$status.cpu = Get-CimInstance Win32_Processor | Select-Object Name, VirtualizationFirmwareEnabled, SecondLevelAddressTranslationExtensions, VMMonitorModeExtensions
$boot = (Get-CimInstance Win32_OperatingSystem).LastBootUpTime
$bootEvidence = Get-AtfBootEvidence -FallbackBootTime $boot
foreach ($key in $bootEvidence.Keys) { $status[$key] = $bootEvidence[$key] }
$status.componentRestartPending = Test-Path 'HKLM:/SOFTWARE/Microsoft/Windows/CurrentVersion/Component Based Servicing/RebootPending'
$status.windowsFeatures = @(Get-CimInstance Win32_OptionalFeature |
    Where-Object { $_.Name -in @('VirtualMachinePlatform', 'Microsoft-Windows-Subsystem-Linux', 'Microsoft-Hyper-V-All', 'HypervisorPlatform') } |
    Select-Object Name, InstallState)
$wsl = Join-Path $env:WINDIR 'System32/wsl.exe'
$status.wsl = [ordered]@{}
foreach ($check in @(@{ name = 'version'; arguments = @('--version') }, @{ name = 'status'; arguments = @('--status') },
        @{ name = 'distributions'; arguments = @('--list', '--verbose') })) {
    try {
        $result = Invoke-AtfSetupProcess $wsl $check.arguments -OutputDirectory $output -AllowFailure
        $status.wsl[$check.name] = [ordered]@{ exitCode = $result.ExitCode; stdout = $result.Stdout; stderr = $result.Stderr }
    } catch { $status.wsl[$check.name] = [ordered]@{ failure = $_.Exception.Message } }
}
try {
    $podman = Resolve-AtfPodman
    $status.podmanPath = $podman
    $version = Invoke-AtfSetupProcess $podman @('--version') -OutputDirectory $output
    $status.podmanVersion = $version.Stdout.Trim()
    foreach ($check in @(@{ name = 'machines'; arguments = @('machine', 'list', '--format', 'json') },
            @{ name = 'connections'; arguments = @('system', 'connection', 'list', '--format', 'json') })) {
        $result = Invoke-AtfSetupProcess $podman $check.arguments -OutputDirectory $output -AllowFailure
        $status[$check.name] = [ordered]@{ exitCode = $result.ExitCode; stdout = $result.Stdout; stderr = $result.Stderr }
    }
    $engine = Invoke-AtfPodman $config @('info', '--format', 'json') -OutputDirectory $output -AllowFailure
    $status.engineReady = $engine.ExitCode -eq 0
    if ($status.engineReady) { $status.engine = $engine.Stdout | ConvertFrom-Json -AsHashtable; $status.status = 'READY' }
    else { $status.blocker = $engine.Stderr }
} catch { $status.blocker = $_.Exception.Message }
$path = Join-Path $output 'status.json'
$status | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $path -Encoding utf8
Write-Output "ATF_SETUP_RECEIPT=$path"
