#Requires -Version 7.4
[CmdletBinding()]
param([string]$OutputDirectory)
$ErrorActionPreference = 'Stop'
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/Kafka.psm1" -Force
$output = New-AtfSetupOutput $OutputDirectory
$checks = [Collections.Generic.List[string]]::new()
function Check([bool]$Condition, [string]$Name) {
    if (-not $Condition) { throw "Setup contract failed: $Name" }
    $checks.Add($Name)
}
function Rejects([scriptblock]$Action, [string]$Name) {
    try { & $Action | Out-Null } catch { $checks.Add($Name); return }
    throw "Setup counterexample escaped: $Name"
}
$root = [IO.Path]::GetFullPath("$PSScriptRoot/..")
foreach ($file in Get-ChildItem -LiteralPath $root -Recurse -File | Where-Object { $_.Extension -in @('.ps1', '.psm1') }) {
    $parseErrors = $null; $tokens = $null
    [Management.Automation.Language.Parser]::ParseFile($file.FullName, [ref]$tokens, [ref]$parseErrors) | Out-Null
    Check ($parseErrors.Count -eq 0) "PowerShell syntax: $($file.Name)"
}
$config = Read-AtfSetupConfig "$root/config/local.json"
$spec = Get-AtfKafkaSpecification $config
$resource = @{ Config = @{ Labels = @{ 'io.atf.owner' = 'atf-j2'; 'io.atf.spec' = $spec } } }
Assert-AtfResourceOwner $resource 'atf-j2' $spec
$checks.Add('Matching owned container accepted')
Assert-AtfResourceOwner @{ labels = @{ 'io.atf.owner' = 'atf-j2'; 'io.atf.spec' = $spec } } 'atf-j2' $spec
$checks.Add('Network labels accepted with actual lowercase JSON property')
Rejects { Assert-AtfResourceOwner $resource 'another-workspace' $spec } 'Foreign resource refused'
Rejects { Assert-AtfResourceOwner $resource 'atf-j2' 'different-recipe' } 'Stale recipe refused'
Rejects { Assert-AtfResourceOwner @{ Labels = @{} } 'atf-j2' $spec } 'Unlabelled resource refused'
$mutatedPath = Join-Path $output 'invalid.json'
foreach ($change in @('digest', 'port', 'heap', 'owner')) {
    $mutant = Get-Content -LiteralPath "$root/config/local.json" -Raw | ConvertFrom-Json -AsHashtable
    switch ($change) {
        'digest' { $mutant.kafka.image = 'docker.io/apache/kafka:latest' }
        'port' { $mutant.kafka.hostPort = 80 }
        'heap' { $mutant.kafka.heapMiB = $mutant.kafka.memoryMiB }
        'owner' { $mutant.owner = 'foreign' }
    }
    $mutant | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $mutatedPath -Encoding utf8
    Rejects { Read-AtfSetupConfig $mutatedPath } "Invalid config refused: $change"
}
$arguments = New-AtfKafkaRunArguments $config
Check ($arguments -contains '127.0.0.1:19092:19092') 'Broker port bound to loopback only'
Check ($arguments[-1] -match '@sha256:[a-f0-9]{64}$') 'Immutable image retained in process arguments'
Check ($arguments -notcontains '--replace' -and $arguments -notcontains '--privileged') 'No replacement or privileged launch'
Check (($arguments | Where-Object { $_ -match 'KAFKA_ADVERTISED_LISTENERS=' }) -eq
    'KAFKA_ADVERTISED_LISTENERS=EXTERNAL://127.0.0.1:19092,INTERNAL://atf-j2-kafka:29092') 'Host and container listeners remain distinct'
$pwsh = (Get-Process -Id $PID).Path
$special = 'literal $(not-a-command); spaces & symbols'
$echoFile = Join-Path $output 'echo.ps1'
'param([string]$Value); [Console]::Out.Write($Value); [Console]::Error.Write("evidence"); exit 7' |
    Set-Content -LiteralPath $echoFile -Encoding utf8
$result = Invoke-AtfSetupProcess $pwsh @('-NoProfile', '-File', $echoFile, $special) -OutputDirectory $output -AllowFailure
Check ($result.ExitCode -eq 7 -and $result.Stdout -eq $special -and $result.Stderr -eq 'evidence') 'Literal arguments and original exit code retained'
Rejects { Invoke-AtfSetupProcess $pwsh @('-NoProfile', '-File', $echoFile, $special) -OutputDirectory $output } 'Nonzero command cannot become PASS'
Rejects { New-AtfSetupOutput $output } 'Existing evidence directory never overwritten'
$stdinFile = Join-Path $output 'stdin.ps1'
'[Console]::Out.Write([Console]::In.ReadToEnd())' | Set-Content -LiteralPath $stdinFile -Encoding utf8
$inputResult = Invoke-AtfSetupProcess $pwsh @('-NoProfile', '-File', $stdinFile) -InputText $special -OutputDirectory $output
Check ($inputResult.Stdout -eq $special) 'Producer stdin is literal and reaches EOF'
$slowFile = Join-Path $output 'slow.ps1'
'Start-Sleep -Seconds 30' | Set-Content -LiteralPath $slowFile -Encoding utf8
$timeoutResult = Invoke-AtfSetupProcess $pwsh @('-NoProfile', '-File', $slowFile) -TimeoutSeconds 1 -AllowFailure -OutputDirectory $output
Check ($timeoutResult.ExitCode -eq 124 -and $timeoutResult.ElapsedMs -lt 5000) 'Expired owned client is bounded and cannot return PASS'
$environmentFile = Join-Path $output 'environment.ps1'
'[Console]::Out.Write([Environment]::GetEnvironmentVariable("ATF_SETUP_CONTRACT_TOKEN"))' | Set-Content -LiteralPath $environmentFile -Encoding utf8
$originalEnvironment = [Environment]::GetEnvironmentVariable('ATF_SETUP_CONTRACT_TOKEN')
$environmentResult = Invoke-AtfSetupProcess $pwsh @('-NoProfile', '-File', $environmentFile) `
    -Environment @{ATF_SETUP_CONTRACT_TOKEN = 'literal-test-value'} -OutputDirectory $output
Check ($environmentResult.Stdout -eq 'literal-test-value' -and
    [Environment]::GetEnvironmentVariable('ATF_SETUP_CONTRACT_TOKEN') -eq $originalEnvironment) 'Provider environment is scoped to the child process'
[ordered]@{ mode = 'OFFLINE_SCRIPT_CONTRACT'; status = 'PASS'; checks = $checks; brokerExecuted = $false } |
    ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $output 'setup-contracts.json') -Encoding utf8
Write-Output "ATF_SETUP_CONTRACTS=$($checks.Count)"
Write-Output "ATF_SETUP_RECEIPT=$output"
