#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

function Find-SystemJdk17 {
    $dirs = @()
    foreach ($pattern in @(
            'C:\Program Files\Eclipse Adoptium\jdk-17*',
            'C:\Program Files\Microsoft\jdk-17*',
            'C:\Program Files\Java\jdk-17*',
            'C:\Program Files\Amazon Corretto\jdk17*',
            'C:\Program Files\Temurin\jdk-17*'
        )) {
        $dirs += Get-ChildItem -Path $pattern -Directory -ErrorAction SilentlyContinue
    }
    foreach ($dir in $dirs) {
        if (Test-Path (Join-Path $dir.FullName 'bin\java.exe')) {
            return $dir.FullName
        }
    }
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        $ver = & (Join-Path $env:JAVA_HOME 'bin\java.exe') -version 2>&1 | Out-String
        if ($ver -match 'version "17\.') {
            return $env:JAVA_HOME
        }
    }
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $ver = & java -version 2>&1 | Out-String
        if ($ver -match 'version "17\.') {
            $bin = Split-Path -Parent $javaCmd.Source
            return (Split-Path -Parent $bin)
        }
    }
    return $null
}

function Ensure-PortableJdk17 {
    $tools = Join-Path $Root '.tools'
    $jdkHome = Join-Path $tools 'jdk-17'
    $javaExe = Join-Path $jdkHome 'bin\java.exe'
    if (Test-Path $javaExe) {
        return $jdkHome
    }

    New-Item -ItemType Directory -Force -Path $tools | Out-Null
    $zip = Join-Path $tools 'jdk17.zip'
    $extract = Join-Path $tools 'jdk-extract'
    $url = 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk'

    Write-Host ''
    Write-Host 'JDK 17 не найден. Скачиваю портативный Temurin JDK 17 (~180 МБ, один раз)...'
    Write-Host $url
    Write-Host ''

    if (Test-Path $zip) { Remove-Item -Force $zip }
    if (Test-Path $extract) { Remove-Item -Recurse -Force $extract }

    try {
        Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    }
    catch {
        throw @"
Не удалось скачать JDK 17.
Проверьте интернет и повторите запуск.
Ошибка: $($_.Exception.Message)
"@
    }

    Expand-Archive -Path $zip -DestinationPath $extract -Force
    $jdkDir = Get-ChildItem -Path $extract -Directory | Select-Object -First 1
    if (-not $jdkDir) {
        throw 'Архив JDK скачан, но папка JDK внутри не найдена.'
    }
    if (Test-Path $jdkHome) { Remove-Item -Recurse -Force $jdkHome }
    Move-Item -Path $jdkDir.FullName -Destination $jdkHome
    Remove-Item -Force $zip -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force $extract -ErrorAction SilentlyContinue

    if (-not (Test-Path $javaExe)) {
        throw "После распаковки нет файла: $javaExe"
    }
    return $jdkHome
}

$mode = 'demo'
if ($args -contains '-Postgres') { $mode = 'postgres' }

$jdk = Find-SystemJdk17
if (-not $jdk) {
    $jdk = Ensure-PortableJdk17
}

$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;" + $env:PATH
Write-Host "Используется Java: $jdk"
& java -version

$springArgs = @()
if ($mode -eq 'demo') {
    Write-Host ''
    Write-Host 'Режим: demo (встроенная база H2, PostgreSQL не нужен).'
    $springArgs = @('--args=--spring.profiles.active=demo')
}
else {
    Write-Host ''
    Write-Host 'Режим: PostgreSQL (jdbc:postgresql://localhost:5432/shop_manager, shop/shop).'
}

Write-Host ''
Write-Host 'Запуск Shop Manager...'
Write-Host 'Когда сервер поднимется, откроется браузер: http://localhost:8080'
Write-Host ''
Write-Host 'Учётки:'
Write-Host '  Покупатель:     customer@shop.local / customer123'
Write-Host '  Менеджер:       manager@shop.local / manager123'
Write-Host '  Кладовщик:      warehouse@shop.local / warehouse123'
Write-Host '  Администратор:  admin@shop.local / admin123'
Write-Host ''
Write-Host 'Остановка: закройте это окно или Ctrl+C.'
Write-Host ''

$openBrowser = Start-Job -ScriptBlock {
    for ($i = 0; $i -lt 120; $i++) {
        try {
            $r = Invoke-WebRequest -Uri 'http://localhost:8080/actuator/health' -UseBasicParsing -TimeoutSec 2
            if ($r.StatusCode -eq 200) {
                Start-Process 'http://localhost:8080/login'
                return
            }
        }
        catch {
            # ещё не готов
        }
        Start-Sleep -Seconds 2
    }
}

$gradle = Join-Path $Root 'gradlew.bat'
if (-not (Test-Path $gradle)) {
    throw "Не найден gradlew.bat в $Root"
}

# Освобождаем порт 8080, если там завис старый процесс
try {
    $owners = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($procId in $owners) {
        Write-Host "Порт 8080 занят процессом $procId — останавливаю..."
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
}
catch {
    # Get-NetTCPConnection может быть недоступен — игнорируем
}

$argList = @('/c', 'gradlew.bat', 'bootRun') + $springArgs
$p = Start-Process -FilePath 'cmd.exe' -ArgumentList $argList -WorkingDirectory $Root -PassThru -NoNewWindow -Wait
$code = 0
if ($null -ne $p.ExitCode) { $code = $p.ExitCode }

Stop-Job $openBrowser -ErrorAction SilentlyContinue | Out-Null
Remove-Job $openBrowser -Force -ErrorAction SilentlyContinue | Out-Null

if ($code -ne 0) {
    Write-Host ''
    Write-Host "Приложение завершилось с ошибкой (код $code)."
    Write-Host 'Частые причины: нет интернета для первой загрузки Gradle, антивирус блокирует Java.'
    exit $code
}

Write-Host ''
Write-Host 'Приложение остановлено.'
