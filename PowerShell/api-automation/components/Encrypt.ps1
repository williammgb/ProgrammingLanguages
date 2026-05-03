function Protect-PasswordSimple {
    <#
    .SYNOPSIS
    Encrypts password in a very simple way (convert to bytes and then to Base64 string).
    #>
    param(
        [Parameter(Mandatory = $true)]
        [string]$NormalPassword
    )

    # convert password to bytes
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($NormalPassword)
    # convert bytes to Base64 string
    return [System.Convert]::ToBase64String($bytes)
}