#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/config/remote-podman.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/scripts/support/AtfSetup.psm1" -Force
$ErrorActionPreference = 'Stop'
$output = New-AtfSetupOutput $OutputDirectory
$receipt = [ordered]@{ source = 'REAL_REMOTE_PODMAN_SETUP'; observedAtUtc = [DateTime]::UtcNow.ToString('O'); status = 'RUNNING'
    stages = @(); limitation = 'Remote engine and container only; no guest WSL2, Kafka listener or Eventuate acceptance.' }
try {
    foreach ($step in @('prerequisites/Initialize-RemotePodman.ps1', 'scripts/Test-RemotePodman.ps1')) {
        $name = [IO.Path]::GetFileNameWithoutExtension($step)
        $stage = [ordered]@{ name = $name; status = 'RUNNING'; log = "$name.log" }
        $receipt.stages += $stage
        try {
            & (Join-Path $PSScriptRoot $step) -ConfigPath $ConfigPath -OutputDirectory (Join-Path $output $name) *> (Join-Path $output "$name.log")
            $stage.status = 'PASS'
        } catch { $stage.status = 'FAIL'; throw }
    }
    $receipt.status = 'PASS'
} catch { $receipt.status = 'FAIL'; $receipt.failure = $_.Exception.Message; throw }
finally {
    $receipt | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $output 'remote-setup.json') -Encoding utf8
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
