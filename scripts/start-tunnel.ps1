$ErrorActionPreference = 'Stop'
$rootDir = Split-Path $PSScriptRoot -Parent
$configFile = Join-Path $env:USERPROFILE '.cloudflared/config.yml'
if (-not (Test-Path -LiteralPath $configFile)) { throw "Tunnel configuration missing: $configFile" }
$running = @(Get-CimInstance Win32_Process -Filter "Name='cloudflared.exe'" | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($configFile) })
if ($running.Count) { Write-Output 'astrodc tunnel is already running.'; exit 0 }
$command = Get-Command cloudflared.exe -ErrorAction SilentlyContinue
$executable = if ($command) { $command.Source } else {
  Get-ChildItem -Path (Join-Path $env:LOCALAPPDATA 'Microsoft/WinGet/Packages/Cloudflare.cloudflared_*/cloudflared.exe') -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $executable) { throw 'cloudflared.exe was not found.' }
$runtimeDir = Join-Path $rootDir '.runtime'
New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
$process = Start-Process -FilePath $executable -ArgumentList ('tunnel --config "' + $configFile + '" run') -WindowStyle Hidden -PassThru -WorkingDirectory $rootDir -RedirectStandardOutput (Join-Path $runtimeDir 'tunnel.out.log') -RedirectStandardError (Join-Path $runtimeDir 'tunnel.err.log')
Set-Content -LiteralPath (Join-Path $runtimeDir 'tunnel.pid') -Value $process.Id
Write-Output ('Started astrodc tunnel PID ' + $process.Id)
