@echo off
echo ========================================
echo  FERRETERIA - Generador de Ejecutable
echo ========================================
echo.

cd /d "%~dp0"

echo [1/4] Limpiando builds anteriores...
if exist "target\dist" rmdir /s /q "target\dist"
if exist "target\installer-input" rmdir /s /q "target\installer-input"

echo [2/4] Compilando proyecto con Maven...
call mvn clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Fallo la compilacion con Maven
    pause
    exit /b 1
)
echo      Compilacion exitosa!

echo [3/4] Preparando archivos para el instalador...
mkdir "target\installer-input"
copy "target\ferreteria-app-1.0.0.jar" "target\installer-input\"
echo      Archivos preparados!

echo [4/4] Generando instalador .exe con jpackage...
call jpackage ^
    --input target\installer-input ^
    --name Ferreteria ^
    --main-jar ferreteria-app-1.0.0.jar ^
    --main-class com.ferreteria.Launcher ^
    --dest target\dist ^
    --type exe ^
    --win-menu ^
    --win-shortcut ^
    --win-dir-chooser ^
    --app-version 1.0.0 ^
    --vendor "Ferreteria" ^
    --description "Sistema de Gestion para Ferreteria" ^
    --icon src\main\resources\icons\ferreteria.ico ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --verbose
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Fallo jpackage
    pause
    exit /b 1
)

echo.
echo ========================================
echo  BUILD EXITOSO!
echo ========================================
echo.
echo El instalador se genero en:
echo   target\dist\Ferreteria-1.0.0.exe
echo.
echo Tamano del instalador:
for %%A in (target\dist\Ferreteria-1.0.0.exe) do echo   %%~zA bytes
echo.
echo Puedes distribuir este archivo .exe
echo ========================================
pause
