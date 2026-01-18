# Sistema Ferretería - JavaFX

Sistema de gestión para ferretería desarrollado con Java 17, JavaFX y SQLite.

## Arquitectura

El proyecto sigue **Clean Architecture** con principios **SOLID**:

```
src/main/java/com/ferreteria/
├── domain/                    # Capa de dominio
│   ├── entities/              # Entidades de negocio
│   ├── repositories/          # Interfaces (puertos)
│   └── exceptions/            # Excepciones de dominio
├── application/               # Capa de aplicación
│   └── usecases/              # Casos de uso
├── infrastructure/            # Capa de infraestructura
│   ├── persistence/           # Repositorios SQLite
│   └── ui/                    # Controladores JavaFX
├── Main.java                  # Entrada de aplicación
└── Launcher.java              # Launcher para JAR
```

## Requisitos

- **Java 17** o superior
- **Maven 3.8+**

## Instalación

```bash
# Clonar
git clone <url-repositorio>
cd ferreteria-java

# Compilar
mvn clean compile

# Ejecutar
mvn javafx:run
```

## Credenciales por defecto

```
Usuario: admin
Contraseña: admin123
```

## Generar ejecutable

```bash
# Crear JAR con dependencias
mvn clean package

# Ejecutar JAR
java -jar target/ferreteria-app-1.0.0.jar
```

## Tecnologías

- **Java 17** - Lenguaje
- **JavaFX 21** - Interfaz gráfica
- **SQLite** - Base de datos embebida
- **BCrypt** - Hash de contraseñas
- **Maven** - Gestión de dependencias

## Principios aplicados

### SOLID
- **S**ingle Responsibility: Cada clase tiene una responsabilidad
- **O**pen/Closed: Extensible mediante interfaces
- **L**iskov Substitution: Repositorios intercambiables
- **I**nterface Segregation: Interfaces pequeñas y específicas
- **D**ependency Inversion: Dependemos de abstracciones

### Clean Architecture
- **Domain**: Entidades y reglas de negocio (sin dependencias externas)
- **Application**: Casos de uso (orquestación)
- **Infrastructure**: Implementaciones (UI, DB)

## Estructura de la base de datos

```sql
-- Usuarios
users (id, username, password, role, full_name, active, created_at)

-- Productos
products (id, code, name, description, category, price, cost, stock, min_stock, location, active)

-- Ventas
sales (id, user_id, total, payment_method, created_at)
```

## Ventajas sobre Electron

| Característica | Electron | JavaFX |
|---------------|----------|--------|
| Tamaño ejecutable | ~150-200MB | ~40-50MB |
| RAM en uso | ~200-400MB | ~50-100MB |
| Tiempo de inicio | 3-5 seg | 1-2 seg |
| Dependencias runtime | Node.js + Chromium | JVM |

---

**Versión:** 1.0.0
**Java:** 17+
