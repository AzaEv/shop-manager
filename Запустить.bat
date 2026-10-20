@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title Shop Manager

rem --- Java 17 ---
set "JAVA_HOME="
if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe" (
  set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
)
if not defined JAVA_HOME (
  for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-17*") do (
    if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"
  )
)
if not defined JAVA_HOME (
  for /d %%D in ("C:\Program Files\Microsoft\jdk-17*") do (
    if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"
  )
)
if not defined JAVA_HOME (
  for /d %%D in ("C:\Program Files\Java\jdk-17*") do (
    if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"
  )
)
if not defined JAVA_HOME (
  for /d %%D in ("C:\Program Files\Amazon Corretto\jdk17*") do (
    if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D"
  )
)
if not defined JAVA_HOME if defined JAVA_HOME_17 if exist "%JAVA_HOME_17%\bin\java.exe" (
  set "JAVA_HOME=%JAVA_HOME_17%"
)

if not defined JAVA_HOME (
  echo Не найден JDK 17.
  echo.
  echo Установите Temurin JDK 17 и снова запустите этот файл:
  echo https://adoptium.net/temurin/releases/?version=17
  echo.
  echo В установщике отметьте "Set JAVA_HOME" и добавление в PATH.
  pause
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Используется Java: %JAVA_HOME%

rem --- PostgreSQL или демо-режим на H2 ---
set "SPRING_ARGS="
set "PG_SERVICE="
for %%S in (postgresql-x64-16 postgresql-x64-17 postgresql-x64-15) do (
  if not defined PG_SERVICE (
    sc query %%S >nul 2>&1
    if not errorlevel 1 set "PG_SERVICE=%%S"
  )
)

if defined PG_SERVICE (
  echo Проверка PostgreSQL ^(%PG_SERVICE%^)...
  sc query %PG_SERVICE% | find "RUNNING" >nul
  if errorlevel 1 (
    echo Запуск службы PostgreSQL...
    net start %PG_SERVICE%
    if errorlevel 1 (
      echo Не удалось запустить PostgreSQL. Переключаюсь на демо-режим ^(H2^).
      set "SPRING_ARGS=--args=--spring.profiles.active=demo"
    )
  )
) else (
  echo PostgreSQL не установлен. Запуск в демо-режиме ^(встроенная база H2^).
  echo Для полноценного режима установите PostgreSQL 16 и создайте БД shop_manager
  echo ^(пользователь shop / пароль shop^).
  echo.
  set "SPRING_ARGS=--args=--spring.profiles.active=demo"
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

call "%~dp0gradlew.bat" bootRun %SPRING_ARGS%
set "EXIT_CODE=!ERRORLEVEL!"
echo.
if not "!EXIT_CODE!"=="0" (
  echo Приложение завершилось с ошибкой ^(код !EXIT_CODE!^).
  echo Частые причины: нет JDK 17, занят порт 8080, нет доступа в интернет для первой загрузки Gradle.
) else (
  echo Приложение остановлено.
)
pause
exit /b !EXIT_CODE!
