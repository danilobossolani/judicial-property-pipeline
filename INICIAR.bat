@echo off
setlocal
cd /d "%~dp0"
title Judicial Pipeline
color 0A

echo.
echo ==============================================
echo              JUDICIAL PIPELINE
echo ==============================================
echo.

where docker >nul 2>nul
if errorlevel 1 (
  echo.
  echo O componente necessario nao foi encontrado.
  echo Execute novamente o instalador do Judicial Pipeline.
  echo.
  pause
  exit /b 1
)

docker info >nul 2>nul
if errorlevel 1 (
  echo.
  echo Preparando o sistema. Nao feche esta janela...
  if exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
    start "" /min "%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
  )
  powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ok=$false; for($i=0; $i -lt 180 -and -not $ok; $i++){ try { docker info *> $null; if($LASTEXITCODE -eq 0){ $ok=$true } } catch {}; if(-not $ok){ Start-Sleep -Seconds 2 } }; if(-not $ok){ exit 1 }"
  if errorlevel 1 (
    echo.
    echo O Windows ainda esta terminando a preparacao.
    echo Reinicie o computador e clique novamente no atalho Judicial Pipeline.
    echo.
    pause
    exit /b 1
  )
)

if not exist ".env" (
  copy /y ".env.example" ".env" >nul
  echo.
  echo O arquivo de configuracao foi criado.
  echo Preencha os campos solicitados no Bloco de Notas, salve e execute INICIAR.bat novamente.
  echo.
  start "" notepad.exe "%CD%\.env"
  pause
  exit /b 1
)

findstr /c:"troque-por" ".env" >nul
if not errorlevel 1 (
  echo.
  echo A configuracao ainda contem valores de exemplo.
  echo Substitua todos os textos iniciados por "troque-por", salve e tente novamente.
  echo.
  start "" notepad.exe "%CD%\.env"
  pause
  exit /b 1
)

echo.
echo Iniciando o Judicial Pipeline...
echo Na primeira vez, esta etapa pode demorar conforme a internet.
echo Nao feche esta janela.
docker compose up --build -d > inicio.log 2>&1
if errorlevel 1 (
  echo.
  echo Nao foi possivel iniciar o sistema.
  echo Execute VER-STATUS.bat e envie a tela ao responsavel tecnico.
  pause
  exit /b 1
)

echo.
echo Aguardando o sistema ficar pronto...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ok=$false; for($i=0; $i -lt 90 -and -not $ok; $i++){ try { $r=Invoke-WebRequest -Uri 'http://127.0.0.1:8080/actuator/health' -UseBasicParsing -TimeoutSec 5; if($r.StatusCode -eq 200){ $ok=$true } } catch {}; if(-not $ok){ Start-Sleep -Seconds 2 } }; if(-not $ok){ exit 1 }"
if errorlevel 1 (
  echo.
  echo O sistema ainda nao respondeu. Execute VER-STATUS.bat para ver os detalhes.
  pause
  exit /b 1
)

echo.
echo Judicial Pipeline iniciado com sucesso.
echo Abrindo http://localhost:8080
start "" "http://localhost:8080"
echo.
pause
