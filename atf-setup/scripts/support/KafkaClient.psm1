#Requires -Version 7.4
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'

function Read-AtfKafkaClientRecipe {
    param([Parameter(Mandatory)][string]$Path,[Parameter(Mandatory)][string]$BrokerVersion)
    $recipe=Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json -AsHashtable
    if($recipe.schemaVersion -ne 1 -or $recipe.brokerVersion -cne $BrokerVersion -or $recipe.libraries.Count -ne 2 -or
            (($recipe.libraries.name | Sort-Object) -join ',') -cne "kafka-clients-$BrokerVersion.jar,slf4j-api-1.7.36.jar") {
        throw 'The infrastructure client recipe differs from the reviewed broker version or libraries.'
    }
    foreach($library in $recipe.libraries) {
        if($library.sha256 -notmatch '^[a-f0-9]{64}$'){throw 'Invalid client library digest.'}
    }
    return $recipe
}

function Assert-AtfKafkaClientLibrary {
    param([Parameter(Mandatory)][string]$Path,[Parameter(Mandatory)][string]$ExpectedSha256)
    if($ExpectedSha256 -notmatch '^[a-f0-9]{64}$'){throw 'Invalid expected client library digest.'}
    $actual=(Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
    if($actual -ine $ExpectedSha256){throw "Client library differs from the pinned image manifest: $([IO.Path]::GetFileName($Path))"}
    return $actual
}

Export-ModuleMember -Function Read-AtfKafkaClientRecipe,Assert-AtfKafkaClientLibrary
