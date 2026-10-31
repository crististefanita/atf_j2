#Requires -Version 7.4
[CmdletBinding()]
param([string]$OutputDirectory)
Import-Module "$PSScriptRoot/support/AtfSetup.psm1" -Force
$ErrorActionPreference='Stop'
$output=New-AtfSetupOutput $OutputDirectory
$moduleRoot=[IO.Path]::GetFullPath("$PSScriptRoot/..")
$paths=@('prerequisites/Configure-KafkaHostAccess.ps1','scripts/support/AtfSetup.psm1','scripts/support/Kafka.psm1',
    'scripts/support/RemotePodman.psm1','config/local.json','config/remote-podman.json','config/remote-kafka.json')
$memory=[IO.MemoryStream]::new()
$manifest=@()
try {
    $archive=[IO.Compression.ZipArchive]::new($memory,[IO.Compression.ZipArchiveMode]::Create,$true)
    try {
        foreach($path in $paths) {
            $source=Join-Path $moduleRoot $path
            $entry=$archive.CreateEntry("atf-setup/$path")
            $stream=$entry.Open()
            try { $bytes=[IO.File]::ReadAllBytes($source); $stream.Write($bytes) } finally { $stream.Dispose() }
            $manifest+=@{path="atf-setup/$path";sha256=(Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash}
        }
    } finally { $archive.Dispose() }
    $zip=$memory.ToArray()
} finally { $memory.Dispose() }
$hash=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($zip))
$template=@'
#Requires -Version 7.4
#Requires -RunAsAdministrator
[CmdletBinding()]
param([string]$OutputDirectory)
$ErrorActionPreference='Stop'
$bytes=[Convert]::FromBase64String('__PAYLOAD__')
if([Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($bytes)) -cne '__SHA256__'){throw 'Host package integrity differs.'}
$staging=Join-Path ([IO.Path]::GetTempPath()) ('atf-kafka-host-' + [guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($staging)|Out-Null
$archive=Join-Path $staging 'sources.zip'
[IO.File]::WriteAllBytes($archive,$bytes)
[IO.Compression.ZipFile]::ExtractToDirectory($archive,$staging)
Write-Output "ATF_HOST_SOURCES=$staging"
& (Join-Path $staging 'atf-setup/prerequisites/Configure-KafkaHostAccess.ps1') -OutputDirectory $OutputDirectory
'@
$launcher=$template.Replace('__PAYLOAD__',[Convert]::ToBase64String($zip)).Replace('__SHA256__',$hash)
$launcherPath=Join-Path $output 'Run-KafkaHostAccess.ps1'
[IO.File]::WriteAllText($launcherPath,$launcher)
$tokens=$null;$errors=$null
[Management.Automation.Language.Parser]::ParseFile($launcherPath,[ref]$tokens,[ref]$errors)|Out-Null
if($errors.Count){throw 'Generated host launcher syntax differs.'}
[IO.File]::WriteAllBytes((Join-Path $output 'ATF-Kafka-HostAccess.zip'),$zip)
@{source='HOST_SETUP_PACKAGE';status='PASS';hostExecuted=$false;archiveSha256=$hash;files=$manifest
    launcherSha256=(Get-FileHash -LiteralPath $launcherPath -Algorithm SHA256).Hash} |
    ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $output 'package.json') -Encoding utf8
Write-Output "ATF_HOST_LAUNCHER=$launcherPath"
Write-Output "ATF_SETUP_RECEIPT=$output"
