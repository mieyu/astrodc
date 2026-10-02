param([switch]$Foreground)
$ErrorActionPreference = 'Stop'
$rootDir = Split-Path $PSScriptRoot -Parent
$backendDir = Join-Path $rootDir 'backend'
$runtimeDir = Join-Path $rootDir '.runtime'
New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
if ($Foreground) {
  Push-Location $backendDir
  try { & (Join-Path $backendDir 'mvnw.cmd') 'spring-boot:run'; exit $LASTEXITCODE } finally { Pop-Location }
}
$guard = New-Object System.Threading.Mutex($false, 'Local\AstronomyBackendStartup')
$acquired = $false
try {
  try { $acquired = $guard.WaitOne(0) } catch [System.Threading.AbandonedMutexException] { $acquired = $true }
  if (-not $acquired) { exit 0 }
  if (Get-NetTCPConnection -LocalPort 8088 -State Listen -ErrorAction SilentlyContinue) { exit 0 }
  $process = Start-Process -FilePath (Join-Path $backendDir 'mvnw.cmd') -ArgumentList 'spring-boot:run' -WorkingDirectory $backendDir -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeDir 'backend.out.log') -RedirectStandardError (Join-Path $runtimeDir 'backend.err.log')
  Set-Content -LiteralPath (Join-Path $runtimeDir 'backend-launcher.pid') -Value $process.Id
  for ($attempt = 0; $attempt -lt 30; $attempt++) {
    Start-Sleep -Seconds 2
    if (Get-NetTCPConnection -LocalPort 8088 -State Listen -ErrorAction SilentlyContinue) { exit 0 }
    $process.Refresh()
    if ($process.HasExited) { throw 'Backend launcher exited before port 8088 became available.' }
  }
  throw 'Backend did not start listening within 60 seconds.'
} catch {
  Add-Content -LiteralPath (Join-Path $runtimeDir 'backend.recovery.log') -Value ((Get-Date -Format o) + ' ' + $_.Exception.Message)
  throw
} finally {
  if ($acquired) { $guard.ReleaseMutex() }
  $guard.Dispose()
}
