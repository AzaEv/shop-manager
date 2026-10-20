@echo off
chcp 65001 >nul
cd /d "%~dp0"
title Shop Manager (PostgreSQL)
echo Запуск Shop Manager с PostgreSQL...
echo Нужны: служба PostgreSQL и БД shop_manager ^(пользователь shop / пароль shop^).
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run.ps1" -Postgres
set "EXIT_CODE=%ERRORLEVEL%"
echo.
pause
exit /b %EXIT_CODE%
