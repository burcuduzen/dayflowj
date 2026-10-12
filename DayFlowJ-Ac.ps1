Add-Type -AssemblyName System.Windows.Forms
$exe = Join-Path $PSScriptRoot 'desktop\runtime\DayFlowJ.exe'
if (Test-Path -LiteralPath $exe) {
    Start-Process -FilePath $exe -WorkingDirectory $PSScriptRoot
} else {
    [Windows.Forms.MessageBox]::Show('Masaüstü paketi bulunamadı. desktop klasörünü kontrol et.', 'DayFlowJ') | Out-Null
}

