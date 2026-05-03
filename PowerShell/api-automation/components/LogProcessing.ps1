function Get-FilteredLogs {
    <#
    .SYNOPSIS
    Filters log objects by property/value.
    #>
    param(
        [Parameter(Mandatory = $true)]
        [array]$Logs,

        [Parameter(Mandatory = $true)]
        [string]$PropertyName,

        [Parameter(Mandatory = $true)]
        [string]$ExpectedValue
    )

    if ($null -eq $Logs) {
        return @()
    }

    # safely convert to array and filter logs
    $logItems = @($Logs)
    return @($logItems | Where-Object { $_.$PropertyName -eq $ExpectedValue })
}