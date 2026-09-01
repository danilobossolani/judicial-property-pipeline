@echo off
setlocal
cd /d "%~dp0"

where docker >nul 2>nul
if errorlevel 1 (
  echo Docker Desktop nao foi encontrado.
  pause
  exit /b 1
)

echo.
docker compose ps
echo.
echo Ultimas mensagens da aplicacao:
docker compose logs --tail 80 app
echo.
pause

