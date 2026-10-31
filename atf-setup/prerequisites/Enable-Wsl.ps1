#Requires -Version 7.4
#Requires -RunAsAdministrator
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/local.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
$ErrorActionPreference = 'Stop'
$config = Read-AtfSetupConfig $ConfigPath
$output = New-AtfSetupOutput $OutputDirectory
$package = Get-AppxPackage -Name MicrosoftCorporationII.WindowsSubsystemForLinux
if (-not $package -or $package.Version -ne "$($config.podman.wslVersion).0") {
    Invoke-AtfSetupProcess (Get-Command winget.exe -ErrorAction Stop).Source @('install', '--id', 'Microsoft.WSL', '--exact',
        '--version', $config.podman.wslVersion, '--source', 'winget', '--silent', '--accept-source-agreements',
        '--accept-package-agreements', '--disable-interactivity', '--log', (Join-Path $output 'wsl-package.log')) `
        -OutputDirectory $output -TimeoutSeconds 600 | Out-Null
}
$feature = Invoke-AtfSetupProcess (Join-Path $env:WINDIR 'System32/dism.exe') @('/Online', '/Enable-Feature',
    '/FeatureName:VirtualMachinePlatform', '/All', '/NoRestart', '/Quiet', "/LogPath:$(Join-Path $output 'dism.log')") `
    -OutputDirectory $output -TimeoutSeconds 600 -AllowFailure
if ($feature.ExitCode -notin @(0, 3010)) { throw "VirtualMachinePlatform activation failed: $($feature.ExitCode); evidence: $output" }
[ordered]@{ package = 'Microsoft.WSL'; version = $config.podman.wslVersion; virtualMachinePlatformRequested = $true
    restartRequired = $feature.ExitCode -eq 3010; restartPerformed = $false } | ConvertTo-Json |
    Set-Content -LiteralPath (Join-Path $output 'wsl-prerequisite.json') -Encoding utf8
if ($feature.ExitCode -eq 3010) { throw "Windows requires a manual restart before machine initialization. No restart performed; evidence: $output" }
Write-Output "ATF_SETUP_RECEIPT=$output"
Write-Output 'WSL package and VirtualMachinePlatform configured without restart. Machine readiness remains a separate check.'
