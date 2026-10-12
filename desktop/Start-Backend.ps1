$ErrorActionPreference = 'Stop'
$launcher = Join-Path (Split-Path $PSScriptRoot -Parent) 'DayFlowJ-Ac.ps1'
& $launcher
