param([string]$DataDirectory, [string]$LegacyDirectory)
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$config = Join-Path $DataDirectory 'desktop-settings.dpapi'
if (-not (Test-Path -LiteralPath $config)) { $config = Join-Path $LegacyDirectory 'desktop-settings.dpapi' }
if (-not (Test-Path -LiteralPath $config)) { Write-Output '{}'; exit 0 }
$secure = Get-Content -LiteralPath $config -Raw | ConvertTo-SecureString
$plain = [Net.NetworkCredential]::new('', $secure).Password
$values = $plain | ConvertFrom-Json
$allowed = @('DAYFLOW_GMAIL_CLIENT_ID','DAYFLOW_GMAIL_CLIENT_SECRET','DAYFLOW_MAIL_HOST','DAYFLOW_MAIL_PORT','DAYFLOW_MAIL_USERNAME','DAYFLOW_MAIL_PASSWORD','DAYFLOW_MAIL_FROM','DAYFLOW_MAIL_SSL')
$result = @{}
foreach ($property in $values.PSObject.Properties) {
    if ($property.Name -in $allowed) { $result[$property.Name] = [string]$property.Value }
}
$result | ConvertTo-Json -Compress
