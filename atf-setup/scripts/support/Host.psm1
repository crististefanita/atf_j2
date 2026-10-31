#Requires -Version 7.4
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-AtfBootEvidence {
    param([Parameter(Mandatory)][datetime]$FallbackBootTime,
        [scriptblock]$ReadSystemEvents = { param([hashtable]$Request) Get-WinEvent @Request })
    $result = [ordered]@{ bootedAtUtc = $FallbackBootTime.ToUniversalTime().ToString('O'); bootCorrelation = 'CIM_TIME_FALLBACK' }
    $bootRecordId = $null
    try {
        $marker = & $ReadSystemEvents @{ FilterHashtable = @{ LogName = 'System';
            ProviderName = 'Microsoft-Windows-Kernel-General'; Id = 12 }; MaxEvents = 1; ErrorAction = 'Stop' }
        if ($null -eq $marker -or $marker.RecordId -le 0) { throw 'No usable System boot marker.' }
        $bootRecordId = $marker.RecordId
        $result.bootMarker = [ordered]@{ recordId = $bootRecordId; observedAtUtc = $marker.TimeCreated.ToUniversalTime().ToString('O') }
        $result.bootCorrelation = 'SYSTEM_RECORD_ID'
    } catch { $result.bootMarkerDiagnostic = $_.Exception.Message }
    try {
        $request = if ($null -ne $bootRecordId) {
            @{ LogName = 'System'; FilterXPath = "*[System[Provider[@Name='Microsoft-Windows-Hyper-V-Hypervisor'] and (EventRecordID >= $bootRecordId)]]";
                MaxEvents = 8; ErrorAction = 'Stop' }
        } else {
            @{ FilterHashtable = @{ LogName = 'System'; ProviderName = 'Microsoft-Windows-Hyper-V-Hypervisor'; StartTime = $FallbackBootTime };
                MaxEvents = 8; ErrorAction = 'Stop' }
        }
        $result.hypervisorBootEvents = @(& $ReadSystemEvents $request |
            Select-Object TimeCreated, Id, RecordId, LevelDisplayName, Message)
    } catch { $result.hypervisorEventDiagnostic = $_.Exception.Message }
    return $result
}

Export-ModuleMember -Function Get-AtfBootEvidence
