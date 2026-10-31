#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/local.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
$ErrorActionPreference = 'Stop'
$config = Read-AtfSetupConfig $ConfigPath
$output = New-AtfSetupOutput $OutputDirectory
$podman = Resolve-AtfPodman
if (Test-Path 'HKLM:/SOFTWARE/Microsoft/Windows/CurrentVersion/Component Based Servicing/RebootPending') {
    throw "Windows has a pending component restart; restart manually before machine initialization. Evidence: $output"
}
if (-not (Get-Service vmcompute -ErrorAction SilentlyContinue)) {
    throw 'VirtualMachinePlatform is not active. Run Enable-Wsl.ps1 in an elevated session; no automatic installer dialog.'
}
$hostSystem = Get-CimInstance Win32_ComputerSystem
$hostCpu = Get-CimInstance Win32_Processor
[ordered]@{ source = 'HOST_PREFLIGHT'; observedAtUtc = [DateTime]::UtcNow.ToString('O')
    model = $hostSystem.Model; hypervisorPresent = $hostSystem.HypervisorPresent
    virtualizationFirmwareEnabled = $hostCpu.VirtualizationFirmwareEnabled
    secondLevelAddressTranslationExtensions = $hostCpu.SecondLevelAddressTranslationExtensions
    limitation = 'CPU indicators are diagnostic; successful Podman info is required for engine readiness.' } |
    ConvertTo-Json | Set-Content -LiteralPath (Join-Path $output 'host-preflight.json') -Encoding utf8
if ($hostSystem.Model -match 'VirtualBox' -and -not $hostCpu.VirtualizationFirmwareEnabled -and -not $hostSystem.HypervisorPresent) {
    throw 'VirtualBox does not expose nested virtualization to this guest. Enable it on the host before machine initialization.'
}
$wsl = Invoke-AtfSetupProcess (Join-Path $env:WINDIR 'System32/wsl.exe') @('--status') -OutputDirectory $output -AllowFailure
if ($wsl.ExitCode -ne 0) { throw "WSL is not ready. Run Enable-Wsl.ps1 elevated; no automatic restart. Evidence: $output" }
$list = Invoke-AtfSetupProcess $podman @('machine', 'list', '--format', 'json') -OutputDirectory $output
$machines = @($list.Stdout | ConvertFrom-Json -AsHashtable)
$machine = @($machines | Where-Object { $_.Name -eq $config.podman.machine })
if ($machine.Count -eq 0) {
    Invoke-AtfSetupProcess $podman @('machine', 'init', '--rootful=false', '--cpus', "$($config.podman.cpus)",
        '--memory', "$($config.podman.memoryMiB)", '--disk-size', "$($config.podman.diskGiB)", $config.podman.machine) `
        -Environment @{CONTAINERS_MACHINE_PROVIDER = $config.podman.provider} -OutputDirectory $output -TimeoutSeconds 900 | Out-Null
}
$inspect = Invoke-AtfSetupProcess $podman @('machine', 'inspect', $config.podman.machine) -OutputDirectory $output
$actual = @($inspect.Stdout | ConvertFrom-Json -AsHashtable)[0]
if ($actual.Name -ne $config.podman.machine -or $actual.Rootful -eq $true) { throw 'Machine name/rootless policy differs.' }
if ($actual.State -ne 'running') {
    Invoke-AtfSetupProcess $podman @('machine', 'start', $config.podman.machine) `
        -OutputDirectory $output -TimeoutSeconds 180 | Out-Null
}
Invoke-AtfPodman $config @('info', '--format', 'json') -OutputDirectory $output | Out-Null
Write-Output "ATF_SETUP_RECEIPT=$output"
