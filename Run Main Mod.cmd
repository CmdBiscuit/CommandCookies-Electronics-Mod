@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Launch-Mod.ps1" -Mode main
echo.
pause
