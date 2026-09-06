@echo off
setlocal
cd /d "%~dp0"
title Encerrar Judicial Pipeline
color 0A

set "MODO_AUTOMATICO=0"
if /i "%~1"=="--automatico" set "MODO_AUTOMATICO=1"

if not "%JUDICIAL_PIPELINE_SKIP_DOCKER_PATH%"=="1" (
  if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" set "PATH=%ProgramFiles%\Docker\Docker\resources\bin;%PATH%"
  if exist "%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe" set "PATH=%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin;%PATH%"
)

set "LOG_DIR=%JUDICIAL_PIPELINE_LOG_DIR%"
if not defined LOG_DIR set "LOG_DIR=%LOCALAPPDATA%\JudicialPipeline"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" >nul 2>nul
set "LOG_FILE=%LOG_DIR%\encerramento.log"

echo.>> "%LOG_FILE%"
echo [%DATE% %TIME%] Encerramento solicitado.>> "%LOG_FILE%"
echo.
echo Encerrando o Judicial Pipeline e liberando a memoria...

where docker >nul 2>nul
if errorlevel 1 goto ENCERRAR_WSL

call docker info >nul 2>nul
if errorlevel 1 goto PARAR_DOCKER

call docker compose stop --timeout 30 >> "%LOG_FILE%" 2>&1
if errorlevel 1 (
  echo Nao foi possivel parar os componentes do Judicial Pipeline.>> "%LOG_FILE%"
  if "%MODO_AUTOMATICO%"=="0" pause
  exit /b 1
)

:PARAR_DOCKER
if "%JUDICIAL_PIPELINE_KEEP_ENGINE%"=="1" goto SUCESSO

call docker desktop stop --timeout 30 >> "%LOG_FILE%" 2>&1
if not errorlevel 1 goto ENCERRAR_WSL

echo O comando de encerramento do Docker nao respondeu; aplicando alternativa segura.>> "%LOG_FILE%"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$names=@('Docker Desktop','com.docker.backend','com.docker.build'); $running=Get-Process -ErrorAction SilentlyContinue | Where-Object { $names -contains $_.ProcessName }; foreach($process in $running){ try { [void]$process.CloseMainWindow() } catch {} }; Start-Sleep -Seconds 5; Get-Process -ErrorAction SilentlyContinue | Where-Object { $names -contains $_.ProcessName } | Stop-Process -Force -ErrorAction SilentlyContinue" >> "%LOG_FILE%" 2>&1

:ENCERRAR_WSL
if "%JUDICIAL_PIPELINE_KEEP_ENGINE%"=="1" goto SUCESSO
where wsl.exe >nul 2>nul
if errorlevel 1 goto SUCESSO

wsl.exe --terminate docker-desktop >> "%LOG_FILE%" 2>&1

:SUCESSO
echo [%DATE% %TIME%] Encerramento concluido.>> "%LOG_FILE%"
echo.
echo Judicial Pipeline encerrado. Os dados foram preservados.
echo Para voltar a usar, clique no atalho Judicial Pipeline.
echo.
if "%MODO_AUTOMATICO%"=="0" pause
exit /b 0
