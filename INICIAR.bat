@echo off
setlocal
cd /d "%~dp0"
title Judicial Pipeline
color 0A

set "MODO_AUTOMATICO=0"
if /i "%~1"=="--automatico" set "MODO_AUTOMATICO=1"

if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" set "PATH=%ProgramFiles%\Docker\Docker\resources\bin;%PATH%"
if exist "%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe" set "PATH=%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin;%PATH%"
set "LOG_DIR=%LOCALAPPDATA%\JudicialPipeline"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" >nul 2>nul

echo.
echo ==============================================
echo              JUDICIAL PIPELINE
echo ==============================================
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Preparar-Windows.ps1" -Silencioso
set "PREPARACAO_WINDOWS=%ERRORLEVEL%"
if "%PREPARACAO_WINDOWS%"=="10" (
  echo O Windows terminou a primeira etapa da preparacao.
  echo Reinicie o computador e clique novamente no atalho Judicial Pipeline.
  echo.
  if "%MODO_AUTOMATICO%"=="0" pause
  exit /b 10
)
if not "%PREPARACAO_WINDOWS%"=="0" (
  echo O Windows nao conseguiu atualizar o componente necessario.
  echo Execute novamente o instalador do Judicial Pipeline.
  echo.
  if "%MODO_AUTOMATICO%"=="0" pause
  exit /b 1
)

where docker >nul 2>nul
if errorlevel 1 (
  echo.
  echo O componente necessario nao foi encontrado.
  echo Execute novamente o instalador do Judicial Pipeline.
  echo.
  if "%MODO_AUTOMATICO%"=="0" pause
  exit /b 1
)

docker info >nul 2>nul
if errorlevel 1 (
  echo.
  echo Preparando o sistema. Nao feche esta janela...
  if exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
    start "" /min "%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
  ) else if exist "%LOCALAPPDATA%\Programs\DockerDesktop\Docker Desktop.exe" (
    start "" /min "%LOCALAPPDATA%\Programs\DockerDesktop\Docker Desktop.exe"
  )
  powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ok=$false; for($i=0; $i -lt 180 -and -not $ok; $i++){ try { docker info *> $null; if($LASTEXITCODE -eq 0){ $ok=$true } } catch {}; if(-not $ok){ Start-Sleep -Seconds 2 } }; if(-not $ok){ exit 1 }"
  if errorlevel 1 (
    echo.
    echo O Windows ainda esta terminando a preparacao.
    echo Reinicie o computador e clique novamente no atalho Judicial Pipeline.
    echo.
    if "%MODO_AUTOMATICO%"=="0" pause
    exit /b 1
  )
)

if not exist ".env" (
  echo.
  echo A instalacao precisa ser reparada.
  echo Execute novamente o instalador do Judicial Pipeline.
  echo.
  if "%MODO_AUTOMATICO%"=="0" pause
  exit /b 1
)

findstr /c:"troque-por" ".env" >nul
if not errorlevel 1 (
  echo.
  echo A configuracao ainda contem valores de exemplo.
  echo Execute novamente o instalador do Judicial Pipeline.
  echo.
  if "%MODO_AUTOMATICO%"=="0" pause
  exit /b 1
)

set "APP_PORT=8080"
for /f "usebackq tokens=1,* delims==" %%A in (".env") do if /i "%%A"=="APP_PORT" set "APP_PORT=%%B"

echo.
echo Iniciando o Judicial Pipeline...
echo Na primeira vez, esta etapa pode demorar conforme a internet.
echo Nao feche esta janela.
docker compose up -d > "%LOG_DIR%\inicio.log" 2>&1
if errorlevel 1 (
  echo.
  echo Nao foi possivel iniciar o sistema.
  echo Execute VER-STATUS.bat e envie a tela ao responsavel tecnico.
  if "%MODO_AUTOMATICO%"=="0" pause
  exit /b 1
)

echo.
echo Aguardando o sistema ficar pronto...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ok=$false; for($i=0; $i -lt 90 -and -not $ok; $i++){ try { $r=Invoke-WebRequest -Uri 'http://127.0.0.1:%APP_PORT%/actuator/health' -UseBasicParsing -TimeoutSec 5; if($r.StatusCode -eq 200){ $ok=$true } } catch {}; if(-not $ok){ Start-Sleep -Seconds 2 } }; if(-not $ok){ exit 1 }"
if errorlevel 1 (
  echo.
  echo O sistema ainda nao respondeu. Execute VER-STATUS.bat para ver os detalhes.
  if "%MODO_AUTOMATICO%"=="0" pause
  exit /b 1
)

echo.
echo Judicial Pipeline iniciado com sucesso.
echo Abrindo http://localhost:%APP_PORT%
if not "%JUDICIAL_PIPELINE_NO_BROWSER%"=="1" start "" "http://localhost:%APP_PORT%"
echo.
exit /b 0
