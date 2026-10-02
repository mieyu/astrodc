$ErrorActionPreference = 'Stop'
$rootDir = Split-Path $PSScriptRoot -Parent
& (Join-Path $PSScriptRoot 'start-backend.ps1')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$runtimeDir = Join-Path $rootDir '.runtime'
if (-not (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)) {
  $process = Start-Process -FilePath 'npm.cmd' -ArgumentList 'run serve -- --host localhost --port 8080' -WorkingDirectory (Join-Path $rootDir 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeDir 'frontend.out.log') -RedirectStandardError (Join-Path $runtimeDir 'frontend.err.log')
  Set-Content -LiteralPath (Join-Path $runtimeDir 'frontend-launcher.pid') -Value $process.Id
}
Write-Output 'Frontend: http://localhost:8080'
Write-Output 'Backend: http://localhost:8088'
