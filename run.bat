@echo off
setlocal ENABLEDELAYEDEXPANSION

REM Script de lanzamiento para Windows (CMD)
REM 1) Intenta ejecutar el uber-jar si existe
REM 2) Si no, construye con Maven y vuelve a intentar
REM 3) Si sigue sin estar el jar, usa fallback por classpath (copia de dependencias)

set DIR=%~dp0
set JAR=%DIR%target\gestor-correo-1.0-SNAPSHOT.jar

if exist "%JAR%" goto RUN_JAR

echo [run.bat] JAR no encontrado, construyendo con Maven (package)...
call mvn -q -DskipTests package
if exist "%JAR%" goto RUN_JAR

echo [run.bat] No se encontro el JAR tras compilar. Usando fallback por classpath...
call mvn -q -DskipTests compile
call mvn -q -DskipTests dependency:copy-dependencies -DoutputDirectory=target\dependency

set CP=%DIR%target\classes;%DIR%target\dependency\*

echo [run.bat] Ejecutando con classpath: %CP%
java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -cp "%CP%" com.gestorcorreo.Main
goto END

:RUN_JAR
echo [run.bat] Ejecutando JAR: %JAR%
java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -jar "%JAR%"

:END
endlocal
