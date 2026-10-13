@echo off
chcp 65001 >nul
cd /d "%~dp0"
title Shop Manager

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Не найден JDK 17:
  echo %JAVA_HOME%
  echo Приложению нужна именно Java 17.
  pause
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Проверка PostgreSQL...
sc query postgresql-x64-16 | find "RUNNING" >nul
if errorlevel 1 (
  echo Запуск службы PostgreSQL...
  net start postgresql-x64-16
  if errorlevel 1 (
    echo Не удалось запустить PostgreSQL.
    echo Запустите этот файл правой кнопкой: "Запуск от имени администратора".
    pause
    exit /b 1
  )
)

echo.
echo Запуск Shop Manager.
echo Когда в окне появится строка Started ShopManagerApplication, откройте:
echo http://localhost:8080
echo.
echo Покупатель: customer@shop.local / customer123
echo Менеджер: manager@shop.local / manager123
echo Кладовщик: warehouse@shop.local / warehouse123
echo Администратор: admin@shop.local / admin123
echo.
echo Чтобы остановить сервер, закройте это окно или нажмите Ctrl+C.
echo.

call "%~dp0gradlew.bat" bootRun
echo.
echo Приложение остановлено.
pause