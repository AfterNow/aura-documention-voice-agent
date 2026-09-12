param([string]$NodePath = "node", [switch]$Lan)
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not (Test-Path (Join-Path $projectRoot '.env'))) { throw 'Create .env in the project root using backend/.env.example. Never commit API keys.' }
$previousBackendHost = $env:HOST
$previousBackendPort = $env:PORT
$usbWatcher = $null
# Explicit mode overrides .env; starting without -Lan always stays on loopback.
$env:HOST = if ($Lan) { '0.0.0.0' } else { '127.0.0.1' }
$env:PORT = '8787'
try {
 $usbWatcher = Start-ThreadJob -FilePath (Join-Path $PSScriptRoot 'watch-adb.ps1')
 if (-not $Lan) {
  & adb reverse tcp:8787 tcp:8787
  if ($LASTEXITCODE -ne 0) { throw 'ADB forwarding failed. Connect the device over USB, or use -Lan for Wi-Fi.' }
 }
 Push-Location (Join-Path $projectRoot 'backend')
 try { & $NodePath --env-file=../.env server.mjs } finally { Pop-Location }
} finally {
 $env:HOST = $previousBackendHost
 $env:PORT = $previousBackendPort
 if ($usbWatcher) { Stop-Job $usbWatcher; Remove-Job $usbWatcher }
}
