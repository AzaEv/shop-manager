@echo off
chcp 65001 >nul
cd /d "%~dp0"
title Установка Shop Manager

net session >nul 2>&1
if errorlevel 1 (
  echo Нужны права администратора.
  echo Запустите этот файл правой кнопкой: "Запуск от имени администратора".
  pause
  exit /b 1
)

echo Установка JDK 17 и PostgreSQL 16 через winget...
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install-windows.ps1"
set "EXIT_CODE=%ERRORLEVEL%"
echo.
if not "%EXIT_CODE%"=="0" (
  echo Установка завершилась с ошибкой.
) else (
  echo Можно запускать проект: start.bat или Запустить-PostgreSQL.bat
)
pause
exit /b %EXIT_CODE%
