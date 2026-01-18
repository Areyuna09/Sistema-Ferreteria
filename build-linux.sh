#!/bin/bash
echo "========================================"
echo " FERRETERIA - Generador de Paquete DEB"
echo "========================================"
echo

cd "$(dirname "$0")"

echo "[1/4] Limpiando builds anteriores..."
rm -rf target/dist target/installer-input

echo "[2/4] Compilando proyecto con Maven..."
mvn clean package -DskipTests -q
if [ $? -ne 0 ]; then
    echo "ERROR: Fallo la compilacion con Maven"
    exit 1
fi
echo "     Compilacion exitosa!"

echo "[3/4] Preparando archivos para el instalador..."
mkdir -p target/installer-input
cp target/ferreteria-app-1.0.0.jar target/installer-input/
echo "     Archivos preparados!"

echo "[4/4] Generando paquete .deb con jpackage..."
jpackage \
    --input target/installer-input \
    --name ferreteria \
    --main-jar ferreteria-app-1.0.0.jar \
    --main-class com.ferreteria.Launcher \
    --dest target/dist \
    --type deb \
    --linux-shortcut \
    --linux-menu-group "Office" \
    --app-version 1.0.0 \
    --vendor "Ferreteria" \
    --description "Sistema de Gestion para Ferreteria" \
    --icon src/main/resources/icons/ferreteria.png \
    --java-options "-Dfile.encoding=UTF-8" \
    --verbose
if [ $? -ne 0 ]; then
    echo "ERROR: Fallo jpackage"
    exit 1
fi

echo
echo "========================================"
echo " BUILD EXITOSO!"
echo "========================================"
echo
echo "El paquete se genero en:"
ls -lh target/dist/*.deb
echo
echo "Instalar con: sudo dpkg -i target/dist/ferreteria_1.0.0_amd64.deb"
echo "========================================"
