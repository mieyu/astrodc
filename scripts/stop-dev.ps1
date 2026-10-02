$ErrorActionPreference = 'Stop'
$rootDir = Split-Path $PSScriptRoot -Parent
$pattern = [regex]::Escape($rootDir)
$targets = @(Get-CimInstance Win32_Process | Where-Object {
  if ($_.Name -notin @('java.exe','node.exe') -or -not $_.CommandLine) { return $false }
  $command = $_.CommandLine.Replace('/', '\')
  if ($command -match $pattern) { return $true }
  if ($_.Name -eq 'java.exe' -and $command -match '@([^\s"]+\.argfile)') {
    $argumentFile = $Matches[1]
    if (Test-Path -LiteralPath $argumentFile) {
      return [System.IO.File]::ReadAllText($argumentFile).Replace('\\', '\').Replace('/', '\') -match $pattern
    }
  }
  return $false
})
foreach ($process in $targets) { Stop-Process -Id $process.ProcessId -ErrorAction SilentlyContinue }
Write-Output ('Stopped ' + $targets.Count + ' astrodc processes. The backend recovery task may restart the backend.')