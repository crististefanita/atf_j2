#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/local.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/support/Kafka.psm1" -Force
$ErrorActionPreference = 'Stop'
$config = Read-AtfSetupConfig $ConfigPath
$output = New-AtfSetupOutput $OutputDirectory
Invoke-AtfPodman $config @('info', '--format', 'json') -OutputDirectory $output | Out-Null
$spec = Get-AtfKafkaSpecification $config
foreach ($kind in @('network', 'volume')) {
    $name = $config.kafka[$kind]
    $exists = Invoke-AtfPodman $config @($kind, 'exists', $name) -OutputDirectory $output -AllowFailure
    if ($exists.ExitCode -eq 1) {
        Invoke-AtfPodman $config @($kind, 'create', '--label', "io.atf.owner=$($config.owner)", '--label', "io.atf.spec=$spec", $name) `
            -OutputDirectory $output | Out-Null
    } elseif ($exists.ExitCode -ne 0) { throw "Cannot inspect $kind ($($exists.ExitCode)); refusing creation." }
    $inspect = Invoke-AtfPodman $config @($kind, 'inspect', $name) -OutputDirectory $output
    Assert-AtfResourceOwner (@($inspect.Stdout | ConvertFrom-Json -AsHashtable)[0]) $config.owner $spec
}
$container = Get-AtfKafkaContainer $config $output -AllowMissing
if ($null -eq $container) {
    Invoke-AtfPodman $config @('pull', '--platform', $config.kafka.platform, $config.kafka.image) `
        -OutputDirectory $output -TimeoutSeconds 600 | Out-Null
    Invoke-AtfPodman $config (New-AtfKafkaRunArguments $config) -OutputDirectory $output -TimeoutSeconds 60 | Out-Null
} elseif (-not $container.State.Running) {
    Invoke-AtfPodman $config @('start', $config.kafka.container) -OutputDirectory $output | Out-Null
}
Wait-AtfKafkaReady $config $output
$container = Get-AtfKafkaContainer $config $output
[ordered]@{ source = 'REAL_KAFKA'; status = 'READY'; observedAtUtc = [DateTime]::UtcNow.ToString('O')
    bootstrapServers = "127.0.0.1:$($config.kafka.hostPort)"; containerId = $container.Id; imageId = $container.Image
    image = $config.kafka.image; specification = $spec } |
    ConvertTo-Json | Set-Content -LiteralPath (Join-Path $output 'kafka-ready.json') -Encoding utf8
Write-Output "ATF_SETUP_RECEIPT=$output"
