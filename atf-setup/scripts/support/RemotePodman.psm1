#Requires -Version 7.4
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Import-Module "$PSScriptRoot/AtfSetup.psm1"

function Read-AtfRemotePodmanConfig {
    param([Parameter(Mandatory)][string]$Path)
    $config = Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json -AsHashtable
    if ($config.schemaVersion -ne 1 -or $config.owner -ne 'atf-j2' -or
            $config.podman.version -ne '5.8.3' -or $config.podman.connection -notmatch '^atf-j2-[a-z0-9-]+$') {
        throw 'Unsupported remote Podman recipe or connection owner.'
    }
    $endpoint = [uri]$config.podman.url
    if ($config.podman.transport -ne 'VIRTUALBOX_HOST_ONLY_TCP' -or $endpoint.Scheme -ne 'tcp' -or
            $endpoint.Host -notmatch '^192\.168\.56\.(?:[1-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-4])$' -or
            $endpoint.Port -lt 1024 -or $endpoint.Port -gt 65535 -or $endpoint.UserInfo -or
            $endpoint.AbsolutePath -notin @('', '/') -or $endpoint.Query -or $endpoint.Fragment) {
        throw 'Remote TCP is restricted to the explicitly reviewed VirtualBox host-only subnet 192.168.56.0/24.'
    }
    if ($config.podman.serverVersion -notmatch '^5\.[0-9]+\.[0-9]+$' -or
            $config.probe.image -notmatch '^docker\.io/library/hello-world@sha256:[a-f0-9]{64}$' -or
            $config.probe.platform -ne 'linux/amd64' -or $config.probe.expectedOutput -ne 'Hello from Docker!') {
        throw 'Remote server version and immutable Linux amd64 smoke recipe must be explicit.'
    }
    return $config
}

function Assert-AtfRemoteConnection {
    param([Parameter(Mandatory)]$Config, [Parameter(Mandatory)]$Connection)
    if ($Connection.Name -cne $Config.podman.connection -or $Connection.URI -cne $Config.podman.url -or
            ($Connection.Contains('Identity') -and $Connection.Identity)) {
        throw 'Existing remote connection differs; refusing overwrite or adoption.'
    }
}

function Get-AtfRemoteConnection {
    param([Parameter(Mandatory)]$Config, [Parameter(Mandatory)][string]$OutputDirectory)
    $result = Invoke-AtfSetupProcess (Resolve-AtfPodman) @('system', 'connection', 'list', '--format', 'json') -OutputDirectory $OutputDirectory
    $connections = @($result.Stdout | ConvertFrom-Json -AsHashtable)
    $matches = @($connections | Where-Object { $_.Name -eq $Config.podman.connection })
    if ($matches.Count -eq 0) { return $null }
    if ($matches.Count -ne 1) { throw 'Remote connection name is ambiguous.' }
    Assert-AtfRemoteConnection $Config $matches[0]
    return $matches[0]
}

function Invoke-AtfRemotePodman {
    param([Parameter(Mandatory)]$Config, [Parameter(Mandatory)][string[]]$Arguments,
        [Parameter(Mandatory)][string]$OutputDirectory, [int]$TimeoutSeconds = 30, [switch]$AllowFailure)
    # Explicit URL pins each invocation even if another process edits the named connection.
    Invoke-AtfSetupProcess (Resolve-AtfPodman) (@('--url', $Config.podman.url) + $Arguments) `
        -OutputDirectory $OutputDirectory -TimeoutSeconds $TimeoutSeconds -AllowFailure:$AllowFailure
}

function Assert-AtfRemoteEngine {
    param([Parameter(Mandatory)]$Config, [Parameter(Mandatory)]$Info)
    if ($Info.Client.Version -cne $Config.podman.version -or $Info.version.Version -cne $Config.podman.serverVersion -or
            $Info.host.os -cne 'linux' -or $Info.host.arch -cne 'amd64' -or $Info.host.security.rootless -ne $true -or
            $Info.host.serviceIsRemote -ne $true) {
        throw 'Remote engine differs from the reviewed client/server, Linux amd64 or rootless contract.'
    }
}

Export-ModuleMember -Function Read-AtfRemotePodmanConfig, Assert-AtfRemoteConnection, Get-AtfRemoteConnection,
    Invoke-AtfRemotePodman, Assert-AtfRemoteEngine
