function Test-UserValidationResponse {
    <#
    .SYNOPSIS
    Validates properties of API user validation response.
    #>
    param(
        [Parameter(Mandatory = $true)]
        [object]$ApiResponse
    )

    # $null -eq checks if the object is null
    if ($null -eq $ApiResponse) { return $false }
    if ($null -eq $ApiResponse.valid) { return $false }

    return [bool]$ApiResponse.valid
}