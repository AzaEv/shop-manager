@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title Shop Manager — защита

echo ============================================
echo   Shop Manager — запуск для показа
echo ============================================
echo.
echo После старта откройте:  http://localhost:8080
echo Вход (покупатель):     customer@shop.local
echo Пароль:                 customer123
echo.
echo Админ: admin@shop.local / admin123
echo Менеджер: manager@shop.local / manager123
echo Кладовщик: warehouse@shop.local / warehouse123
echo.
echo Окно нельзя закрывать, пока идёт показ.
echo Остановка: Ctrl+C или закрыть это окно.
echo ============================================
echo.

set "JAVA_HOME="
if exist "%~dp0.tools\jdk-17\bin\java.exe" set "JAVA_HOME=%~dp0.tools\jdk-17"
if not defined JAVA_HOME if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
if not defined JAVA_HOME for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"
if not defined JAVA_HOME for /d %%D in ("C:\Program Files\Microsoft\jdk-17*") do if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"
if not defined JAVA_HOME for /d %%D in ("C:\Program Files\Java\jdk-17*") do if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"
if not defined JAVA_HOME for /d %%D in ("C:\Program Files\Amazon Corretto\jdk17*") do if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"
if not defined JAVA_HOME for /d %%D in ("C:\Program Files\Temurin\jdk-17*") do if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"

if not defined JAVA_HOME (
  echo JDK 17 не найден. Скачиваю портативный Temurin JDK 17...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; $root='%~dp0'; $tools=Join-Path $root '.tools'; $jdk=Join-Path $tools 'jdk-17'; New-Item -ItemType Directory -Force -Path $tools | Out-Null; $zip=Join-Path $tools 'jdk17.zip'; $extract=Join-Path $tools 'jdk-extract'; if (Test-Path $zip) { Remove-Item -Force $zip }; if (Test-Path $extract) { Remove-Item -Recurse -Force $extract }; $url='https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk'; Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing; Expand-Archive -Path $zip -DestinationPath $extract -Force; $dir=Get-ChildItem -Path $extract -Directory | Select-Object -First 1; if (-not $dir) { throw 'JDK folder not found' }; if (Test-Path $jdk) { Remove-Item -Recurse -Force $jdk }; Move-Item $dir.FullName $jdk; Remove-Item -Force $zip -ErrorAction SilentlyContinue; Remove-Item -Recurse -Force $extract -ErrorAction SilentlyContinue"
  if errorlevel 1 (
    echo Не удалось скачать JDK 17. Установите: https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b 1
  )
  set "JAVA_HOME=%~dp0.tools\jdk-17"
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Не найден java.exe: %JAVA_HOME%
  pause
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Java: %JAVA_HOME%
java -version
echo.
echo Режим: demo (встроенная база H2 — PostgreSQL не нужен)
echo.

start "" cmd /c "for /l %%i in (1,1,90) do (curl -sf http://localhost:8080/actuator/health >nul 2>&1 && start http://localhost:8080/login && exit /b 0 & timeout /t 2 /nobreak >nul)"

call "%~dp0gradlew.bat" bootRun --args=--spring.profiles.active=demo
set "EXIT_CODE=!ERRORLEVEL!"
echo.
if not "!EXIT_CODE!"=="0" (
  echo Ошибка запуска (код !EXIT_CODE!).
) else (
  echo Приложение остановлено.
)
pause
exit /b !EXIT_CODE!