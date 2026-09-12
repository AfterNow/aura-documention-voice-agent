param([int]$Port = 8787, [switch]$Once)
$ErrorActionPreference = 'Continue'
do {
 if (Get-Command adb -ErrorAction SilentlyContinue) {
  $devices = & adb devices 2>$null
  foreach ($deviceLine in $devices) {
   if ($deviceLine -match '^(\S+)\s+device\s*$') {
    $deviceSerial = $Matches[1]
    $targetPort = "tcp:$Port"
    $rules = & adb -s $deviceSerial reverse --list 2>$null
    $expectedRule = '\s' + [regex]::Escape($targetPort) + '\s+' + [regex]::Escape($targetPort) + '\s*$'
    if (-not ($rules | Where-Object { $_ -match $expectedRule })) {
     & adb -s $deviceSerial reverse $targetPort $targetPort 2>$null | Out-Null
     if ($LASTEXITCODE -eq 0) { Write-Output "USB forwarding restored for $deviceSerial on port $Port" }
    }
   }
  }
 }
 if (-not $Once) { Start-Sleep -Seconds 3 }
} while (-not $Once)
