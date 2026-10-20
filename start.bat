@echo off
chcp 65001 >nul
cd /d "%~dp0"
title Shop Manager
echo Запуск Shop Manager...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run.ps1"
set "EXIT_CODE=%ERRORLEVEL%"
echo.
if not "%EXIT_CODE%"=="0" (
  echo Если окно сразу закрылось с ошибкой — пришлите текст выше.
)
pause
exit /b %EXIT_CODE%
