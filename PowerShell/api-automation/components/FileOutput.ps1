function Save-JsonToFile {
    <#
    .SYNOPSIS
    Saves object array to JSON file.
    #>
    param(
        [Parameter(Mandatory = $true)]
        [object]$Data,

        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $dir = Split-Path $Path -Parent # get parent directory of the path
    # check if directory exists, if not create it (Out-Null hides output)
    if (-not (Test-Path $dir)) {
        New-Item -Path $dir -ItemType Directory | Out-Null
    }

    # convert object array to JSON and save to file
    $Data | ConvertTo-Json -Depth 6 | Out-File -FilePath $Path -Encoding UTF8
}