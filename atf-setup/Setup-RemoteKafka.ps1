#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/config/remote-kafka.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/scripts/support/Kafka.psm1" -Force
$ErrorActionPreference = 'Stop'
$output = New-AtfSetupOutput $OutputDirectory
$steps = @('prerequisites/Initialize-RemotePodman.ps1', 'scripts/Start-Kafka.ps1', 'scripts/Test-Kafka.ps1', 'scripts/Test-KafkaExternal.ps1')
$receipt = [ordered]@{ source='ATF_SETUP'; profile='REMOTE_KAFKA'; observedAtUtc=[DateTime]::UtcNow.ToString('O'); status='RUNNING'
    stages=@(foreach ($step in $steps) { [ordered]@{ name=[IO.Path]::GetFileNameWithoutExtension($step); status='NOT_RUN'; script=$step } }) }
try {
    $config = Read-AtfKafkaSetupConfig $ConfigPath
    if ($config.podman.provider -cne 'remote') { throw 'Explicit remote Kafka configuration is required.' }
    foreach ($stage in $receipt.stages) {
        $stage.status = 'RUNNING'
        $stage.log = "$($stage.name).log"
        try {
            $inputPath = if ($stage.name -eq 'Initialize-RemotePodman') { $config.remotePodmanPath } else { $ConfigPath }
            & (Join-Path $PSScriptRoot $stage.script) -ConfigPath $inputPath -OutputDirectory (Join-Path $output $stage.name) *> (Join-Path $output $stage.log)
            $stage.status = 'PASS'
        } catch { $stage.status='FAIL'; throw }
    }
    $receipt.status = 'PASS'
} catch { $receipt.status='FAIL'; $receipt.failure=$_.Exception.Message; throw }
finally {
    $receipt | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $output 'setup.json') -Encoding utf8
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
