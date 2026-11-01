#Requires -Version 7.4
[CmdletBinding()]
param([Parameter(Mandatory)][string]$SurefireXml,[string]$OutputDirectory,
      [string]$ReportConfigPath="$PSScriptRoot/../config/eventuate-report.yaml")
Import-Module "$PSScriptRoot/support/AtfSetup.psm1" -Force
$ErrorActionPreference='Stop'
$output=New-AtfSetupOutput $OutputDirectory
$settings=[Xml.XmlReaderSettings]::new();$settings.DtdProcessing=[Xml.DtdProcessing]::Prohibit
$reader=[Xml.XmlReader]::Create([IO.Path]::GetFullPath($SurefireXml),$settings)
try {$xml=[Xml.XmlDocument]::new();$xml.Load($reader)}finally{$reader.Dispose()}
$properties=@($xml.testsuite.properties.property)
$classpath=($properties | Where-Object {$_.name -eq 'surefire.test.class.path'} | Select-Object -First 1).value
if(-not $classpath){$classpath=($properties | Where-Object {$_.name -eq 'java.class.path'} | Select-Object -First 1).value}
if(-not $classpath){throw 'Full Surefire test classpath is required.'}
$entries=@($classpath.Split([IO.Path]::PathSeparator) | Where-Object {$_} | Select-Object -Unique)
$linux=[Collections.Generic.List[string]]::new();$inputs=[Collections.Generic.List[object]]::new()
New-Item -ItemType Directory -Path (Join-Path $output 'classes'),(Join-Path $output 'lib'),(Join-Path $output 'config') | Out-Null
$reportConfig=Join-Path $output 'config/atf.yaml'
Copy-Item -LiteralPath $ReportConfigPath -Destination $reportConfig
$inputs.Add(@{path='config/atf.yaml';sha256=(Get-FileHash -LiteralPath $reportConfig -Algorithm SHA256).Hash.ToLowerInvariant()})
$index=0
foreach($entry in $entries) {
    if(Test-Path -LiteralPath $entry -PathType Container) {
        $relative="classes/$index";$destination=Join-Path $output $relative
        Copy-Item -LiteralPath $entry -Destination $destination -Recurse
        $linux.Add("/tmp/atf-runtime/$relative");$index++
        foreach($file in Get-ChildItem -LiteralPath $destination -File -Recurse) {
            $inputs.Add(@{path=[IO.Path]::GetRelativePath($output,$file.FullName).Replace('\','/');sha256=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()})
        }
    } elseif(Test-Path -LiteralPath $entry -PathType Leaf) {
        if([IO.Path]::GetExtension($entry) -ne '.jar'){throw 'Unexpected classpath artifact.'}
        $relative='lib/'+[IO.Path]::GetFileName($entry);$destination=Join-Path $output $relative
        $hash=(Get-FileHash -LiteralPath $entry -Algorithm SHA256).Hash.ToLowerInvariant()
        if(Test-Path -LiteralPath $destination) {
            if((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant() -ne $hash){throw 'Conflicting runtime artifact names.'}
        } else {Copy-Item -LiteralPath $entry -Destination $destination;$inputs.Add(@{path=$relative;sha256=$hash})}
        $linux.Add("/tmp/atf-runtime/$relative")
    } else {throw "Missing classpath input: $entry"}
}
if(-not ($inputs | Where-Object {$_.path -match '^lib/atf-core-.+\.jar$'}) -or -not ($inputs | Where-Object {$_.path -match '^lib/atf-reporting-.+\.jar$'})) {
    throw 'The runtime must contain the actual Core and Reporting JARs.'
}
[IO.File]::WriteAllText((Join-Path $output 'classpath.txt'),(($linux | Select-Object -Unique) -join ':'),[Text.UTF8Encoding]::new($false))
@{schemaVersion=1;sourceSurefireXml=[IO.Path]::GetFullPath($SurefireXml);inputs=$inputs.ToArray();
  classpathSha256=(Get-FileHash -LiteralPath (Join-Path $output 'classpath.txt') -Algorithm SHA256).Hash.ToLowerInvariant()} |
    ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $output 'runtime-manifest.json') -Encoding utf8
Write-Output "ATF_RUNTIME_BUNDLE=$output"
