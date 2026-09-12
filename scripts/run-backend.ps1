param([string]$NodePath = "node")
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not (Test-Path (Join-Path $projectRoot '.env'))) { throw 'Create .env in the project root using backend/.env.example. Never commit API keys.' }
adb reverse tcp:8787 tcp:8787
Push-Location (Join-Path $projectRoot 'backend')
try { & $NodePath --env-file=../.env server.mjs } finally { Pop-Location }
