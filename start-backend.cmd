@echo off
rem Arranca el backend AegisOPS localmente (fuera de Docker) en el puerto 8082.
rem Requiere: JDK 21 y los contenedores de Postgres/Qdrant/n8n en marcha.
setlocal
cd /d "%~dp0Backend"

rem Cargar variables desde ..\.env (ignora comentarios y lineas vacias)
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%~dp0.env") do set "%%a=%%b"

set "SERVER_PORT=8082"
call mvnw.cmd spring-boot:run
endlocal
