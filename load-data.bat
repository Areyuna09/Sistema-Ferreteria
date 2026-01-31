@echo off
echo Cargando datos SQL en la base de datos...
cd /d "c:\wamp64\www\Sistema-Ferreteria"
mvn compile
if %errorlevel% neq 0 (
    echo Error en compilacion
    pause
    exit /b 1
)
mvn exec:java -Dexec.mainClass="com.ferreteria.utils.SQLDataLoader"
if %errorlevel% neq 0 (
    echo Error ejecutando cargador de datos
    pause
    exit /b 1
)
echo.
echo Datos cargados exitosamente!
pause
