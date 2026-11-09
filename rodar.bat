@echo off
REM Sobe o projeto inteiro no Windows: Servidor Socket -> Ponte HTTP -> Frontend
REM Uso:  rodar.bat            (normal)
REM       rodar.bat --popular  (popula o banco com dados de exemplo antes)
REM
REM Precisa de: Java 11+, Node 18+ e MongoDB rodando na porta 27017
REM (instalado como servico do Windows ou via "docker compose up -d").
REM O Maven NAO precisa estar instalado: o script usa o mvnw.cmd do backend.

setlocal
set RAIZ=%~dp0
set MVN=%RAIZ%backend\mvnw.cmd

where java >nul 2>nul || (echo Java nao encontrado. Instale o Java 11 ou superior. & pause & exit /b 1)
where npm  >nul 2>nul || (echo Node/npm nao encontrado. Instale o Node 18 ou superior. & pause & exit /b 1)

where docker >nul 2>nul && (pushd "%RAIZ%" & docker compose up -d >nul 2>nul & popd)

cd /d "%RAIZ%backend"
echo Compilando backend (na primeira vez baixa as dependencias, pode demorar)...
call "%MVN%" -q compile || (echo Erro na compilacao & pause & exit /b 1)

if "%1"=="--popular" (
  echo Populando banco com dados de exemplo...
  call "%MVN%" -q exec:java@popular
)

echo Abrindo Servidor Socket e Ponte HTTP em janelas separadas...
start "Servidor Socket" cmd /k ""%MVN%" -q exec:java@servidor"
timeout /t 8 /nobreak >nul
start "Ponte HTTP" cmd /k ""%MVN%" -q exec:java@ponte"
timeout /t 5 /nobreak >nul

echo Frontend - abra http://localhost:5173  (feche as janelas para parar tudo)
cd /d "%RAIZ%frontend"
if not exist node_modules call npm install
call npm run dev
