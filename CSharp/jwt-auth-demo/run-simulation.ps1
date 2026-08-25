<#
Starts Postgres + the API, waits for the API to be healthy, seeds test data,
runs the console simulation, then stops the API again. One command, one run.

Usage: ./run-simulation.ps1  (or: pwsh ./run-simulation.ps1)
#>

# stop the script on the first unhandled error instead of continuing
$ErrorActionPreference = "Stop"
$repoRoot = $PSScriptRoot
$apiUrl = "http://localhost:5163"
$apiOutLog = Join-Path $env:TEMP "myapp-api-out.log"
$apiErrLog = Join-Path $env:TEMP "myapp-api-err.log"

Write-Host "Starting postgres-db container..."
docker start postgres-db | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Could not start the 'postgres-db' container. Make sure Docker Desktop is running and the container exists (see README.md)." -ForegroundColor Red
    exit 1
}

Write-Host "Starting the API..."
# -NoNewWindow + redirected streams: runs in this same console session, output goes to log files
# instead of stdout, so it doesn't show with the simulation's output below
$apiProcess = Start-Process -FilePath "dotnet" `
    -ArgumentList "run", "--project", (Join-Path $repoRoot "MyApp.Api"), "--urls", $apiUrl `
    -RedirectStandardOutput $apiOutLog -RedirectStandardError $apiErrLog `
    -NoNewWindow -PassThru

# everything after this point can fail mid-way (API never comes up, simulation errors, etc.);
# the try/finally guarantees the API process gets stopped either way, so it never leaks
try {
    Write-Host "Waiting for the API to come up..."
    $ready = $false
    # poll for up to ~30s: dotnet run needs a moment to build + start Kestrel
    for ($i = 0; $i -lt 30; $i++) {
        if ($apiProcess.HasExited) {
            # e.g. its own startup DB check failed and it exited itself - no point polling further
            Write-Host "API process exited early (likely can't reach the database). Log:" -ForegroundColor Red
            Get-Content $apiOutLog, $apiErrLog -ErrorAction SilentlyContinue | Write-Host
            exit 1
        }
        try {
            # /api/test is a public endpoint, just used here as a "did it start" ping
            Invoke-WebRequest -Uri "$apiUrl/api/test" -UseBasicParsing -TimeoutSec 2 | Out-Null
            $ready = $true
            break
        } catch {
            Start-Sleep -Seconds 1
        }
    }
    if (-not $ready) {
        Write-Host "API did not come up in time. Log:" -ForegroundColor Red
        Get-Content $apiOutLog, $apiErrLog -ErrorAction SilentlyContinue | Write-Host
        exit 1
    }
    Write-Host "API is up."

    Write-Host "Seeding test data..."
    # idempotent on the server side, safe to call even if already seeded from a previous run
    Invoke-WebRequest -Uri "$apiUrl/api/seed" -Method Post -UseBasicParsing | Out-Null

    Write-Host "Running the simulation..."
    Write-Host ""
    # runs in the foreground so its timestamped log lines print live in this terminal
    dotnet run --project (Join-Path $repoRoot "MyApp.Console")
}
finally {
    # always runs, whether the simulation finished, failed, or the script errored out above
    Write-Host ""
    Write-Host "Stopping the API..."
    if (-not $apiProcess.HasExited) {
        Stop-Process -Id $apiProcess.Id -Force
    }
}
