@echo off
setlocal
cd /d "%~dp0"

where docker >nul 2>nul
if errorlevel 1 (
  echo Docker Desktop nao foi encontrado.
  pause
  exit /b 1
)

docker compose stop
if errorlevel 1 (
  echo.
  echo Nao foi possivel parar o sistema. Confirme se o Docker Desktop esta aberto.
  pause
  exit /b 1
)

echo.
echo Judicial Pipeline parado. Os dados foram preservados.
echo Para voltar a usar, execute INICIAR.bat.
echo.
pause

