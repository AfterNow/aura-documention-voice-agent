param([string]$JavaHome = $env:JAVA_HOME, [string]$Serial = "")
$ErrorActionPreference = "Stop"
if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
 & .\gradlew.bat assembleDebug --no-daemon --project-cache-dir work/gradle-cache --console plain
 if ($LASTEXITCODE -ne 0) { throw 'Android build failed' }
 $adbArguments = @(); if ($Serial) { $adbArguments = @('-s', $Serial) }
 & adb @adbArguments reverse tcp:8787 tcp:8787
 & adb @adbArguments install -r app/build/outputs/apk/debug/app-debug.apk
 if ($LASTEXITCODE -ne 0) { throw 'APK installation failed' }
 & adb @adbArguments shell am force-stop com.afternow.aura.manuals
 & adb @adbArguments shell am start -n com.afternow.aura.manuals/.MainActivity
} finally { Pop-Location }
