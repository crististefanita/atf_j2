#Requires -Version 7.4
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/config/local.json", [string]$OutputDirectory, [switch]$InstallPrerequisites)
Import-Module "$PSScriptRoot/scripts/support/AtfSetup.psm1" -Force
$ErrorActionPreference = 'Stop'
if ($InstallPrerequisites) {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw 'Open one elevated PowerShell session first. Scripts never open their own UAC dialog.'
    }
}
$output = New-AtfSetupOutput $OutputDirectory
$receipt = [ordered]@{ source = 'ATF_SETUP'; observedAtUtc = [DateTime]::UtcNow.ToString('O'); status = 'RUNNING'; stages = @() }
try {
    $steps = if ($InstallPrerequisites) { @('prerequisites/Install-Podman.ps1', 'prerequisites/Enable-Wsl.ps1') } else { @() }
    $steps += @('prerequisites/Initialize-Podman.ps1', 'scripts/Start-Kafka.ps1', 'scripts/Test-Kafka.ps1')
    foreach ($step in $steps) {
        $name = [IO.Path]::GetFileNameWithoutExtension($step)
        $stage = [ordered]@{ name = $name; status = 'RUNNING'; log = "$name.log" }
        $receipt.stages += $stage
        try {
            & (Join-Path $PSScriptRoot $step) -ConfigPath $ConfigPath -OutputDirectory (Join-Path $output $name) *> (Join-Path $output "$name.log")
            $stage.status = 'PASS'
        } catch {
            $stage.status = 'FAIL'
            throw
        }
    }
    $receipt.status = 'PASS'
} catch {
    $receipt.status = 'FAIL'
    $receipt.failure = $_.Exception.Message
    throw
} finally {
    $receipt | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $output 'setup.json') -Encoding utf8
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
