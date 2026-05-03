function Invoke-ApiPost {
    <#
    .SYNOPSIS
    Sends POST request with JSON body and returns JSON response.
    #>
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,

        [Parameter(Mandatory = $true)]
        [object]$Body
    )
    # send POST request with JSON body (depth 5 is the max nested depth of the object) and return JSON response
    return Invoke-RestMethod -Uri $Url -Method Post -Body ($Body | ConvertTo-Json -Depth 5) -ContentType "application/json"
}

function Invoke-ApiGet {
    <#
    .SYNOPSIS
    Sends GET request and returns JSON response.
    #>
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url
    )

    return Invoke-RestMethod -Uri $Url -Method Get
}