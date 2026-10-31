#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/local.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/support/Kafka.psm1" -Force
$ErrorActionPreference = 'Stop'
$config = Read-AtfKafkaSetupConfig $ConfigPath
$output = New-AtfSetupOutput $OutputDirectory
$container = Get-AtfKafkaContainer $config $output
if ($container.State.Running) { Invoke-AtfPodman $config @('stop', '--time', '20', $config.kafka.container) -OutputDirectory $output | Out-Null }
Write-Output "ATF_SETUP_RECEIPT=$output"
Write-Output 'Owned Kafka container stopped. Container, volume, network and machine retained.'
