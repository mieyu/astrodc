[CmdletBinding()]
param(
  [ValidateSet('on', 'off', 'exit')][string]$Command,
  [switch]$DryRun
)
$ErrorActionPreference = 'Stop'
$rootDir = Split-Path $PSScriptRoot -Parent
function Invoke-AstroControl {
  param([string]$Action)
  if ($Action -eq 'exit') { return $false }
  if ($Action -eq 'on') {
    if ($DryRun) {
      Write-Host ("Would start backend in " + (Join-Path $rootDir 'backend'))
      Write-Host ("Would start frontend in " + (Join-Path $rootDir 'frontend'))
      return $true
    }
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'start-backend.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Backend startup failed.' }
    if (-not (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)) {
      $runtimeDir = Join-Path $rootDir '.runtime'
      New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
      $process = Start-Process -FilePath 'npm.cmd' -ArgumentList 'run serve -- --host localhost --port 8080' -WorkingDirectory (Join-Path $rootDir 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeDir 'frontend.out.log') -RedirectStandardError (Join-Path $runtimeDir 'frontend.err.log')
      Set-Content -LiteralPath (Join-Path $runtimeDir 'frontend-launcher.pid') -Value $process.Id
    }
    Write-Host 'Frontend: http://localhost:8080'
    Write-Host 'Backend: http://localhost:8088'
    return $true
  }
  $pattern = [regex]::Escape($rootDir)
  $targets = @(Get-CimInstance Win32_Process | Where-Object {
    if ($_.Name -notin @('java.exe','node.exe') -or -not $_.CommandLine) { return $false }
    $processCommand = $_.CommandLine.Replace('/', '\')
    if ($processCommand -match $pattern) { return $true }
    if ($_.Name -eq 'java.exe' -and $processCommand -match '@([^\s"]+\.argfile)') {
      $argumentFile = $Matches[1]
      if (Test-Path -LiteralPath $argumentFile) {
        return [IO.File]::ReadAllText($argumentFile).Replace('\\', '\').Replace('/', '\') -match $pattern
      }
    }
    return $false
  })
  foreach ($process in $targets) {
    if ($DryRun) { Write-Host ("Would stop astrodc PID " + $process.ProcessId) }
    else { Stop-Process -Id $process.ProcessId -ErrorAction SilentlyContinue }
  }
  if (-not $DryRun) { Write-Host ('Stopped ' + $targets.Count + ' astrodc processes. The backend recovery task may restart the backend.') }
  return $true
}
if ($Command) { Invoke-AstroControl $Command | Out-Null; return }
while ($true) {
  $action = Read-Host 'on/off/exit'
  if ($action -notin @('on','off','exit')) { continue }
  if (-not (Invoke-AstroControl $action)) { break }
}
