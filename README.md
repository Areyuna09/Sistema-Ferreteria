# Sistema de Gestión para Ferretería

Sistema de punto de venta para ferretería con interfaz gráfica moderna. Incluye autenticación de usuarios, dashboard de estadísticas, gestión de productos e inventario.

## Stack Tecnológico

| Tecnología | Versión | Descripción |
|------------|---------|-------------|
| Java | 17+ | Lenguaje principal |
| JavaFX | 21.0.1 | Framework de interfaz gráfica |
| AtlantaFX | 2.0.1 | Tema moderno para JavaFX |
| SQLite | 3.45.1 | Base de datos embebida |
| Maven | 3.9+ | Gestión de dependencias y build |
| BCrypt | 0.4 | Hash seguro de contraseñas |

## Requisitos del Sistema

### Para desarrollo
- JDK 17 o superior
- Maven 3.9 o superior
- IDE con soporte para JavaFX (IntelliJ IDEA, Eclipse, VS Code)

### Para generar instaladores
- **Windows:** WiX Toolset 3.14 (`winget install WiXToolset.WiXToolset`)
- **Linux:** dpkg-deb (incluido en la mayoría de distribuciones)

## Instalación y Ejecución

### Clonar el repositorio
```bash
git clone https://github.com/Areyuna09/Sistema-Ferreteria.git
cd Sistema-Ferreteria
```

### Ejecutar en modo desarrollo
```bash
mvn javafx:run
```

### Compilar JAR
```bash
mvn clean package -DskipTests
```

## Generar Instaladores

### Windows (.exe)
```bash
# Ejecutar el script
build-exe.bat
```
El instalador se genera en: `target/dist/Ferreteria-1.0.0.exe`

### Linux (.deb)
```bash
# Dar permisos y ejecutar
chmod +x build-linux.sh
./build-linux.sh
```
El paquete se genera en: `target/dist/ferreteria_1.0.0_amd64.deb`

Instalar con:
```bash
sudo dpkg -i target/dist/ferreteria_1.0.0_amd64.deb
```

## Estructura del Proyecto

```
src/main/java/com/ferreteria/
├── Main.java                    # Punto de entrada JavaFX
├── Launcher.java                # Launcher para JAR ejecutable
├── application/
│   └── usecases/                # Casos de uso de la aplicación
├── domain/
│   ├── entities/                # Entidades del dominio
│   ├── exceptions/              # Excepciones personalizadas
│   └── repositories/            # Interfaces de repositorios
└── infrastructure/
    ├── persistence/             # Implementación SQLite
    └── ui/                      # Controladores JavaFX

src/main/resources/
├── views/                       # Archivos FXML
├── styles/                      # CSS personalizado
└── icons/                       # Iconos de la aplicación
```

## Credenciales por Defecto

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| admin | admin123 | Administrador |

## Arquitectura

El proyecto sigue los principios de **Clean Architecture**:

- **Domain:** Entidades y reglas de negocio independientes del framework
- **Application:** Casos de uso que orquestan la lógica de negocio
- **Infrastructure:** Implementaciones concretas (UI, persistencia)

## Licencia

MIT License
