@echo off
chcp 65001 > nul
cls
echo ===========================================
echo   Generar Datos de Prueba
echo   Sistema Ferretería
echo ===========================================
echo.
echo IMPORTANTE:
echo Este script generará datos de prueba en la base de datos existente.
echo.
echo Si tienes la aplicación abierta, por favor CIÉRRALA antes de continuar.
echo.
pause

echo.
echo Compilando proyecto...
call mvn compile -q

echo.
echo Generando datos de prueba...
echo (Esto puede tardar unos segundos)
echo.

mvn exec:java -Dexec.mainClass="com.ferreteria.utils.TestDataGenerator"

echo.
echo.
echo Verificando datos insertados...
echo.
mvn exec:java -Dexec.mainClass="com.ferreteria.utils.CheckDatabase" -q

echo.
echo ===========================================
echo   Proceso completado
echo ===========================================
echo.
echo Si ves errores arriba, significa que la base de datos
echo está siendo usada por otro proceso.
echo.
echo Solución:
echo 1. Cierra TODAS las aplicaciones Java
echo 2. Verifica el Administrador de Tareas (no debe haber java.exe)
echo 3. Ejecuta este script nuevamente
echo.
pause
