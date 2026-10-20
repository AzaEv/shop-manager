#Requires -Version 5.1
# Устанавливает JDK 17 (Temurin) и PostgreSQL 16 для Shop Manager.
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Root = Split-Path -Parent $PSScriptRoot

function Assert-Admin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $p = New-Object Security.Principal.WindowsPrincipal($id)
    if (-not $p.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw 'Запустите этот файл правой кнопкой: «Запуск от имени администратора».'
    }
}

function Ensure-Winget {
    if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
        throw 'Не найден winget. Обновите «Установщик приложений» из Microsoft Store и повторите.'
    }
}

Assert-Admin
Ensure-Winget

Write-Host '=== 1/3 Temurin JDK 17 ==='
winget install --id EclipseAdoptium.Temurin.17.JDK -e --accept-package-agreements --accept-source-agreements

Write-Host ''
Write-Host '=== 2/3 PostgreSQL 16 ==='
winget install --id PostgreSQL.PostgreSQL.16 -e --accept-package-agreements --accept-source-agreements

Write-Host ''
Write-Host '=== 3/3 База shop_manager ==='
$psql = @(
    'C:\Program Files\PostgreSQL\16\bin\psql.exe',
    'C:\Program Files\PostgreSQL\17\bin\psql.exe',
    'C:\Program Files\PostgreSQL\15\bin\psql.exe'
) | Where-Object { Test-Path $_ } | Select-Object -First 1

if (-not $psql) {
    Write-Host 'psql.exe не найден. Перезагрузите ПК после установки PostgreSQL и снова запустите Установить.bat,'
    Write-Host 'либо создайте БД вручную: пользователь shop / пароль shop, база shop_manager.'
    exit 0
}

$setupSql = Join-Path $Root 'scripts\setup-db.sql'
$superPasswords = @('postgres', 'admin', 'root', '1', '123', '123456')
$ok = $false
foreach ($pwd in $superPasswords) {
    $env:PGPASSWORD = $pwd
    & $psql -U postgres -h localhost -d postgres -c 'SELECT 1' 1>$null 2>$null
    if ($LASTEXITCODE -eq 0) {
        $ok = $true
        break
    }
}

if (-not $ok) {
    Write-Host 'PostgreSQL установлен, но пароль пользователя postgres неизвестен.'
    Write-Host 'Откройте SQL Shell (psql) и выполните команды из scripts\setup-db.sql'
    Write-Host 'либо введите пароль суперпользователя, который задали при установке PostgreSQL.'
    exit 0
}

& $psql -U postgres -h localhost -d postgres -v ON_ERROR_STOP=1 -f $setupSql
if ($LASTEXITCODE -ne 0) {
    throw 'Не удалось создать базу shop_manager. Проверьте пароль postgres и что служба PostgreSQL запущена.'
}

Write-Host ''
Write-Host 'Готово: JDK 17 + PostgreSQL 16 + база shop_manager.'
Write-Host 'Дальше в папке проекта: Запустить-PostgreSQL.bat'
Write-Host 'Быстрый демо-режим без PostgreSQL: start.bat'
