#Requires -Version 7.4
#Requires -RunAsAdministrator
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/local.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
$ErrorActionPreference = 'Stop'
$config = Read-AtfSetupConfig $ConfigPath
$output = New-AtfSetupOutput $OutputDirectory
try { $existingPodman = Resolve-AtfPodman } catch { $existingPodman = $null }
if ($existingPodman) {
    $existingVersion = Invoke-AtfSetupProcess $existingPodman @('--version') -OutputDirectory $output
    if ($existingVersion.Stdout.Trim() -ne "podman version $($config.podman.version)") { throw 'Existing Podman version differs; review the upgrade explicitly.' }
    Write-Output "ATF_PODMAN_PATH=$existingPodman"
    Write-Output "ATF_SETUP_RECEIPT=$output"
    return
}
$winget = (Get-Command winget.exe -ErrorAction Stop).Source
$installed = Invoke-AtfSetupProcess $winget @('install', '--id', $config.podman.packageId, '--exact', '--version', $config.podman.version,
    '--source', 'winget', '--silent', '--accept-source-agreements', '--accept-package-agreements', '--disable-interactivity',
    '--log', (Join-Path $output 'installer.log')) -OutputDirectory $output -TimeoutSeconds 600 -AllowFailure
$podman = Resolve-AtfPodman
$version = Invoke-AtfSetupProcess $podman @('--version') -OutputDirectory $output
if ($version.Stdout.Trim() -ne "podman version $($config.podman.version)") { throw "Unexpected installed version: $($version.Stdout)" }
if ($installed.ExitCode -ne 0) { throw "Installer exit $($installed.ExitCode); review $output" }
Write-Output "ATF_PODMAN_PATH=$podman"
Write-Output "ATF_SETUP_RECEIPT=$output"
