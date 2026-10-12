$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$backend = Join-Path $root 'backend'
$java = Join-Path $root 'desktop\jre\bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) {
    $java = Join-Path $env:USERPROFILE 'Downloads\OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1\jdk-17.0.20.1+1\bin\java.exe'
}
if (-not (Test-Path -LiteralPath $java) -and $env:JAVA_HOME) {
    $java = Join-Path $env:JAVA_HOME 'bin\java.exe'
}
if (-not (Test-Path -LiteralPath $java)) { $java = (Get-Command java -ErrorAction Stop).Source }
$config = Join-Path $backend 'data\desktop-settings.dpapi'
if (Test-Path -LiteralPath $config) {
    $secure = Get-Content -LiteralPath $config -Raw | ConvertTo-SecureString
    $plain = [Net.NetworkCredential]::new('', $secure).Password
    $values = $plain | ConvertFrom-Json
    foreach ($property in $values.PSObject.Properties) {
        if ($property.Name -in @('DAYFLOW_GMAIL_CLIENT_ID','DAYFLOW_GMAIL_CLIENT_SECRET','DAYFLOW_MAIL_HOST','DAYFLOW_MAIL_PORT','DAYFLOW_MAIL_USERNAME','DAYFLOW_MAIL_PASSWORD','DAYFLOW_MAIL_FROM','DAYFLOW_MAIL_SSL')) {
            [Environment]::SetEnvironmentVariable($property.Name, [string]$property.Value, 'Process')
        }
    }
    $plain = $null
    $values = $null
}
$data = Join-Path $backend 'data'
New-Item -ItemType Directory -Path $data -Force | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
Set-Location -LiteralPath $backend
$javaProcess = Start-Process -FilePath $java -ArgumentList @('-jar', ('"' + (Join-Path $backend 'target\dayflowj-0.0.1-SNAPSHOT.jar') + '"')) -WorkingDirectory $backend -WindowStyle Hidden -RedirectStandardOutput (Join-Path $data "desktop-$stamp.log") -RedirectStandardError (Join-Path $data "desktop-$stamp.err.log") -Wait -PassThru
exit $javaProcess.ExitCode

