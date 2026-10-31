#Requires -Version 7.4
#Requires -RunAsAdministrator
[CmdletBinding()]
param([string]$ConfigPath = "$PSScriptRoot/../config/remote-kafka.json", [string]$OutputDirectory)
Import-Module "$PSScriptRoot/../scripts/support/AtfSetup.psm1" -Force
Import-Module "$PSScriptRoot/../scripts/support/Kafka.psm1" -Force
$ErrorActionPreference = 'Stop'
$output = New-AtfSetupOutput $OutputDirectory
$receipt = [ordered]@{ source = 'REAL_WINDOWS_HOST_ROUTE'; observedAtUtc = [DateTime]::UtcNow.ToString('O'); status = 'RUNNING'
    routeCreated = $false; firewallCreated = $false; guestReadiness = 'NOT_TESTED' }
try {
    $config = Read-AtfKafkaSetupConfig $ConfigPath
    if ($config.podman.provider -cne 'remote') { throw 'This script requires the explicit remote Kafka profile.' }
    $access = $config.hostAccess
    $receipt.route = $access
    $system = Get-CimInstance Win32_ComputerSystem
    if ($system.Model -match 'VirtualBox' -or -not (Get-NetIPAddress -AddressFamily IPv4 -IPAddress $access.listenAddress -ErrorAction SilentlyContinue)) {
        throw 'Run on the physical Windows host owning the configured host-only IP, not inside the guest.'
    }
    $netsh = Join-Path $env:WINDIR 'System32/netsh.exe'
    $read = Invoke-AtfSetupProcess $netsh @('interface', 'portproxy', 'show', 'v4tov4') -OutputDirectory $output
    $routes = @(foreach ($line in $read.Stdout -split '\r?\n') {
        if ($line -match '^\s*(\d+\.\d+\.\d+\.\d+)\s+(\d+)\s+(\d+\.\d+\.\d+\.\d+)\s+(\d+)\s*$') {
            @{ listenAddress=$Matches[1]; listenPort=[int]$Matches[2]; connectAddress=$Matches[3]; connectPort=[int]$Matches[4] }
        }
    })
    $collision = @($routes | Where-Object { $_.listenPort -eq $access.port -and $_.listenAddress -in @($access.listenAddress, '0.0.0.0') })
    $rules = @(Get-NetFirewallRule -Name $access.firewallRule -ErrorAction SilentlyContinue)
    $description = "io.atf.owner=atf-j2; route=$($access.listenAddress):$($access.port)=>$($access.connectAddress):$($access.port); guest=$($access.guestAddress)"
    if ($rules.Count -gt 1 -or $collision.Count -gt 1) { throw 'Ambiguous existing host route or firewall rule.' }
    if ($collision.Count -eq 1 -and ($rules.Count -ne 1 -or $collision[0].listenAddress -cne $access.listenAddress -or
            $collision[0].connectAddress -cne $access.connectAddress -or $collision[0].connectPort -ne $access.port)) {
        throw 'Existing portproxy is foreign or differs; refusing adoption or replacement.'
    }
    if ($rules.Count -eq 1) {
        $rule = $rules[0]
        $address = $rule | Get-NetFirewallAddressFilter
        $port = $rule | Get-NetFirewallPortFilter
        if ($rule.Description -cne $description -or $rule.Direction -ne 'Inbound' -or $rule.Action -ne 'Allow' -or
                $rule.Enabled -ne 'True' -or $rule.Profile -ne 'Any' -or ($address.LocalAddress -join ',') -cne $access.listenAddress -or
                ($address.RemoteAddress -join ',') -cne $access.guestAddress -or ($port.LocalPort -join ',') -ne "$($access.port)" -or
                "$($port.Protocol)" -notin @('TCP','6')) { throw 'Existing firewall rule differs; refusing overwrite.' }
    }
    if ($collision.Count -eq 0 -and (Get-NetTCPConnection -State Listen -LocalPort $access.port -ErrorAction SilentlyContinue |
            Where-Object { $_.LocalAddress -in @($access.listenAddress, '0.0.0.0', '::') })) {
        throw 'Another listener owns the requested host port; refusing replacement.'
    }
    $socket = [Net.Sockets.TcpClient]::new()
    try { $socket.ConnectAsync($access.connectAddress, $access.port).WaitAsync([TimeSpan]::FromSeconds(5)).GetAwaiter().GetResult() | Out-Null }
    finally { $socket.Dispose() }
    if ($rules.Count -eq 0) {
        $firewall = @{ Name=$access.firewallRule; DisplayName=$access.firewallRule; Description=$description; Group='ATF J2'
            Direction='Inbound'; Action='Allow'; Enabled='True'; Profile='Any'; Protocol='TCP'; LocalAddress=$access.listenAddress
            RemoteAddress=$access.guestAddress; LocalPort=$access.port }
        New-NetFirewallRule @firewall | Out-Null
        $receipt.firewallCreated = $true
    }
    if ($collision.Count -eq 0) {
        Invoke-AtfSetupProcess $netsh @('interface', 'portproxy', 'add', 'v4tov4', "listenaddress=$($access.listenAddress)",
            "listenport=$($access.port)", "connectaddress=$($access.connectAddress)", "connectport=$($access.port)", 'protocol=tcp') -OutputDirectory $output | Out-Null
        $receipt.routeCreated = $true
    }
    Invoke-AtfSetupProcess $netsh @('interface', 'portproxy', 'show', 'v4tov4') -OutputDirectory $output | Out-Null
    $socket = [Net.Sockets.TcpClient]::new()
    try { $socket.ConnectAsync($access.listenAddress, $access.port).WaitAsync([TimeSpan]::FromSeconds(5)).GetAwaiter().GetResult() | Out-Null }
    finally { $socket.Dispose() }
    $receipt.status = 'PASS'
} catch { $receipt.status = 'FAIL'; $receipt.failure = $_.Exception.Message; throw }
finally {
    $receipt | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath (Join-Path $output 'host-kafka-access.json') -Encoding utf8
    Write-Output "ATF_SETUP_RECEIPT=$output"
}
