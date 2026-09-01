@echo off
setlocal
cd /d "%~dp0"

where docker >nul 2>nul
if errorlevel 1 (
  echo.
  echo Docker Desktop nao foi encontrado.
  echo Instale e abra o Docker Desktop antes de continuar.
  echo https://www.docker.com/products/docker-desktop/
  echo.
  pause
  exit /b 1
)

docker info >nul 2>nul
if errorlevel 1 (
  echo.
  echo O Docker Desktop esta instalado, mas ainda nao esta pronto.
  echo Abra o Docker Desktop, aguarde aparecer "Engine running" e tente novamente.
  echo.
  pause
  exit /b 1
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
echo Iniciando o Judicial Pipeline. A primeira execucao pode demorar alguns minutos...
docker compose up --build -d
if errorlevel 1 (
  echo.
  echo Nao foi possivel iniciar. Consulte os detalhes acima.
  pause
  exit /b 1
)

echo.
echo Aguardando o sistema ficar pronto...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ok=$false; for($i=0; $i -lt 90 -and -not $ok; $i++){ try { $r=Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health' -TimeoutSec 2; if($r.status -eq 'UP'){ $ok=$true } } catch {}; if(-not $ok){ Start-Sleep -Seconds 2 } }; if(-not $ok){ exit 1 }"
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
