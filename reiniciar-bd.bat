@echo off
chcp 65001 > nul
echo ===========================================
echo   Reiniciar Base de Datos - Sistema Ferretería
echo ===========================================
echo.

echo IMPORTANTE: Cierra cualquier aplicación Java que esté en ejecución
echo.
pause

echo.
echo Eliminando base de datos...
del "%USERPROFILE%\.ferreteria-java-data\ferreteria.db" 2>nul
if exist "%USERPROFILE%\.ferreteria-java-data\ferreteria.db" (
    echo.
    echo ✗ No se pudo eliminar la base de datos
    echo   Por favor cierra manualmente cualquier aplicación que la esté usando
    echo   y ejecuta este script nuevamente.
    pause
    exit /b 1
)

echo ✓ Base de datos eliminada
echo.
echo Compilando proyecto...
call mvn compile -q

echo.
echo Creando nueva base de datos y generando datos de prueba...
call mvn exec:java -Dexec.mainClass="com.ferreteria.utils.ResetDatabase" -q

echo.
echo ===========================================
echo ✓ Proceso completado
echo ===========================================
echo.
echo Ahora puedes iniciar la aplicación y probar:
echo 1. Ir a Reportes
echo 2. Seleccionar mes actual o anterior
echo 3. Generar reporte
echo 4. Exportar a PDF o Excel
echo.
pause
