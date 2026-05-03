function Get-UserInput {
    <#
    .SYNOPSIS
    Prompts user for credentials and role.
    #>
    
    $name = Read-Host "Enter username"
    $password = Read-Host "Enter password" -AsSecureString
    $role = Read-Host "Enter role (admin/user)"

    return [PSCustomObject]@{
        name     = $name
        password = $password
        role     = $role
        password_mode = "plaintext"
    }
}