[CmdletBinding()]
param(
  [ValidateSet('on', 'off', 'exit')][string]$Command,
  [switch]$DryRun
)
$ErrorActionPreference = 'Stop'
function Invoke-AstroControl {
  param([string]$Action)
  if ($Action -eq 'exit') { return $false }
  $entry = if ($Action -eq 'on') { 'dev.ps1' } else { 'stop-dev.ps1' }
  if ($DryRun) { Write-Host ("Would invoke " + (Join-Path $PSScriptRoot $entry)) }
  else { & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot $entry) }
  return $true
}
if ($Command) { Invoke-AstroControl $Command | Out-Null; return }
while ($true) {
  $action = Read-Host 'on/off/exit'
  if ($action -notin @('on','off','exit')) { continue }
  if (-not (Invoke-AstroControl $action)) { break }
}
