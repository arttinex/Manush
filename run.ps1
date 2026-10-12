# =========================================================
# MANUSH Platform Runner (PowerShell)
# Loads .env and starts Spring Boot
# =========================================================

# Load .env if it exists
if (Test-Path ".env") {
    Write-Host "[MANUSH] Loading .env variables..." -ForegroundColor Cyan
    Get-Content ".env" | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $name = $parts[0].Trim()
            $val = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($name, $val, "Process")
        }
    }
}

Write-Host "[MANUSH] Starting Manush Application on http://localhost:8080 ..." -ForegroundColor Magenta
.\mvnw.cmd spring-boot:run
