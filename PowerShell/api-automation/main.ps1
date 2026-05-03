param(
    # If set, password is encrypted.
    [switch]$EncryptPassword,

    # Optional output path override.
    [string]$OutputFile
)

# Import all scripts
. "$PSScriptRoot\components\Config.ps1"
. "$PSScriptRoot\components\Encrypt.ps1"
. "$PSScriptRoot\components\Input.ps1"
. "$PSScriptRoot\components\ApiClient.ps1"
. "$PSScriptRoot\components\Validation.ps1"
. "$PSScriptRoot\components\LogProcessing.ps1"
. "$PSScriptRoot\components\FileOutput.ps1"

# Stops the API server if it is still running from a previous session
function Stop-ApiServerOnPort {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl
    )

    try {
        $baseUri = [uri]$BaseUrl
        # Detect all active connections on the port
        $listeners = Get-NetTCPConnection -LocalPort $baseUri.Port -State Listen -ErrorAction SilentlyContinue
        if ($null -ne $listeners) {
            foreach ($listener in @($listeners)) {
                try {
                    Stop-Process -Id $listener.OwningProcess -ErrorAction Stop
                    Write-Host "Stopped active API server on port $($baseUri.Port)." -ForegroundColor DarkGray
                }
                catch {
                    Write-Host "Warning: Could not stop active API server process $($listener.OwningProcess)." -ForegroundColor Yellow
                }
            }
        }
    }
    catch {
        Write-Host "Warning: Could not parse BaseUrl '$BaseUrl' to detect existing API process." -ForegroundColor Yellow
    }
}

# Start FastAPI server in background if not already reachable.
function Start-ApiServerIfNeeded {
    param(
        [Parameter(Mandatory = $true)]
        [string]$BaseUrl,
        [Parameter(Mandatory = $true)]
        [string]$ApiScript
    )

    # If the API is already running, stop it 
    try {
        Invoke-RestMethod -Uri "$BaseUrl/docs" -Method Get -TimeoutSec 2 | Out-Null
        Write-Host "API already running at $BaseUrl. Restarting..." -ForegroundColor DarkGray
        Stop-ApiServerOnPort -BaseUrl $BaseUrl
    }
    catch {
        Write-Host "API not running. Starting FastAPI server..." -ForegroundColor Yellow
    }
    
    # Store the name of the API module (api.py)
    $apiModuleName = [System.IO.Path]::GetFileNameWithoutExtension($ApiScript)

    # Start the FastAPI server
    $apiProcess = Start-Process -FilePath "python" `
        -ArgumentList "-m uvicorn $apiModuleName`:app --host 127.0.0.1 --port 5000" `
        -WorkingDirectory $PSScriptRoot `
        -WindowStyle Hidden `
        -PassThru

    # Wait up to 10 seconds for server startup, checks every 0.5 seconds.
    for ($i = 1; $i -le 20; $i++) {
        Start-Sleep -Milliseconds 500
        try {
            Invoke-RestMethod -Uri "$BaseUrl/docs" -Method Get -TimeoutSec 2 | Out-Null
            Write-Host "FastAPI server started successfully." -ForegroundColor Green
            return $apiProcess
        }
        catch {
            # keep waiting
        }
    }

    throw "FastAPI server did not start in time. Ensure dependencies are installed: fastapi, uvicorn"
}

$startedApiProcess = $null

try {
    # Load configuration.
    $config = Get-AppConfig -ConfigPath ".\config.json"

    $baseUrl = $config.Api.BaseUrl
    $apiScriptPath = $config.Api.ScriptPath
    $validateUrl = "$baseUrl$($config.Api.Endpoints.ValidateUser)"
    $logsUrl = "$baseUrl$($config.Api.Endpoints.GetLogs)"

    # Start API server first.
    $startedApiProcess = Start-ApiServerIfNeeded -BaseUrl $baseUrl -ApiScript $apiScriptPath

    # Check if the output file is set, if not use the default output file.
    if (-not $OutputFile) {
        $OutputFile = $config.Defaults.OutputFile
    }

    # Prompt user for input.
    $user = Get-UserInput

    # Convert SecureString password to plaintext
    # Turns SS into pointer in memory, then converts to string, then frees the pointer (frees allocated memory)
    if ($user.password -is [securestring]) {
        $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($user.password)
        try {
            $user.password = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        }
        finally {
            if ($bstr -ne [IntPtr]::Zero) {
                [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
            }
        }
    }

    # Optionally encrypt password and set strict password mode.
    $passwordMode = "plaintext"
    if ($EncryptPassword) {
        $user.password = Protect-PasswordSimple -NormalPassword $user.password
        $passwordMode = "base64"
    }
    $user.password_mode = $passwordMode

    # Validate user via API.
    $validationResponse = Invoke-ApiPost -Url $validateUrl -Body $user
    $isValid = Test-UserValidationResponse -ApiResponse $validationResponse

    if (-not $isValid) {
        Write-Host "User validation FAILED. Exiting." -ForegroundColor Red
        exit 1
    }

    Write-Host "User validation SUCCESS." -ForegroundColor Green
    if ($validationResponse.mode) {
        Write-Host "Validation mode: $($validationResponse.mode)" -ForegroundColor DarkGray
    }

    # Pull logs and filter them.
    $encodedUser = [System.Uri]::EscapeDataString([string]$user.name) # makes sure the string is safe to use in a URL
    $allLogs = Invoke-ApiGet -Url "${logsUrl}?user=$encodedUser"
    $filterProperty = Read-Host "Filter property (default: $($config.Defaults.LogFilterProperty))"
    $filterValue = Read-Host "Filter value (default: $($config.Defaults.LogFilterValue))"

    if (-not $filterProperty) { $filterProperty = $config.Defaults.LogFilterProperty }
    if (-not $filterValue) { $filterValue = $config.Defaults.LogFilterValue }

    $filtered = Get-FilteredLogs -Logs $allLogs -PropertyName $filterProperty -ExpectedValue $filterValue

    # Save filtered logs to file.
    Save-JsonToFile -Data $filtered -Path $OutputFile

    # Final user output.
    Write-Host "Filtered logs saved to: $OutputFile" -ForegroundColor Cyan
    Write-Host "Total logs received: $($allLogs.Count)"
    Write-Host "Filtered logs count: $($filtered.Count)"
}
catch {
    Write-Host "Unexpected error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
finally { # after script completion,stop the FastAPI server if it is still running
    if ($null -ne $startedApiProcess) {
        try {
            if (-not $startedApiProcess.HasExited) {
                Stop-Process -Id $startedApiProcess.Id -ErrorAction Stop
                Write-Host "FastAPI server stopped." -ForegroundColor DarkGray
            }
        }
        catch {
            Write-Host "Warning: Could not stop FastAPI server process $($startedApiProcess.Id)." -ForegroundColor Yellow
        }
    }
}