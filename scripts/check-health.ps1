$ErrorActionPreference = 'Stop'
$rootDir = Split-Path $PSScriptRoot -Parent
$failed = $false
$checks = @(
  @{Name='Local Java backend';Url='http://localhost:8088/analyze/health';Health=$true},
  @{Name='Cloudflare Pages';Url='https://astrodc.top';Health=$false},
  @{Name='Cloudflare Tunnel / Java backend';Url='https://api.astrodc.top/analyze/health';Health=$true}
)
foreach ($check in $checks) {
  try {
    $response = Invoke-WebRequest -Uri $check.Url -UseBasicParsing -TimeoutSec 20
    if ($response.StatusCode -ne 200) { throw "HTTP $($response.StatusCode)" }
    if ($check.Health) {
      $health = $response.Content | ConvertFrom-Json
      if ($health.status -ne 'healthy') { throw 'Backend health response is not healthy.' }
    } elseif ($response.Content -notmatch 'id=["'']app["'']') {
      throw 'Pages did not return the website application.'
    }
    Write-Output ("PASS " + $check.Name + " " + $check.Url)
  } catch {
    $failed = $true
    Write-Output ("FAIL " + $check.Name + ": " + $_.Exception.Message)
  }
}
$backendDir = Join-Path $rootDir 'backend'
$listener = Get-NetTCPConnection -LocalPort 8088 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
$owned = $false
if ($listener) {
  $process = Get-CimInstance Win32_Process -Filter ("ProcessId=" + $listener.OwningProcess)
  $command = [string]$process.CommandLine
  if ($command.Contains($backendDir)) { $owned = $true }
  if ($command -match '@([^\s"]+\.argfile)' -and (Test-Path -LiteralPath $Matches[1])) {
    $classpath = [System.IO.File]::ReadAllText($Matches[1]).Replace('\\', '\')
    $owned = $classpath.Contains((Join-Path $backendDir 'target'))
  }
}
if ($owned) { Write-Output ("PASS Backend runs from " + $backendDir) }
else { $failed = $true; Write-Output 'FAIL Backend process does not use this astrodc checkout.' }
if ($failed) { exit 1 }
