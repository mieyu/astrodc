param([ValidateSet('test','package')][string]$Task = 'test')
$ErrorActionPreference = 'Stop'
$backendDir = Join-Path (Split-Path $PSScriptRoot -Parent) 'backend'
Push-Location $backendDir
try {
  & (Join-Path $backendDir 'mvnw.cmd') $Task '-DskipTests=false'
  exit $LASTEXITCODE
} finally { Pop-Location }
