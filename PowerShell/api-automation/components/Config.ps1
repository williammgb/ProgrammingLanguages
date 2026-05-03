function Get-AppConfig {
    <#
    .SYNOPSIS
    Loads application configuration from config.json.
    #>

    # argument is the path to the config file
    param(
        [Parameter(Mandatory = $true)] 
        [string]$ConfigPath
    )

    # check if config file exists
    if (-not (Test-Path $ConfigPath)) {
        throw "Config file not found at path: $ConfigPath"
    }

    # read config file and convert to PowerShell object
    return (Get-Content $ConfigPath -Raw | ConvertFrom-Json)
}