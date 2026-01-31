# 🏪 Sistema de Gestión de Ferretería

## 📋 Información General

**Nombre del Proyecto:** Sistema Ferretería  
**Versión:** 1.0.0  
**Estado:** ✅ Completamente Funcional  
**Branch Principal:** `seba_branch`  
**Tecnología:** Java 17 + JavaFX 21 + SQLite  

---

## 🚀 Ejecución del Sistema

### Requisitos Mínimos
- Java 17 o superior
- Maven 3.6+
- Sistema Operativo: Windows/Linux/macOS

### Inicio Rápido
```bash
# 1. Clonar el repositorio
git clone <repository-url>
cd Sistema-Ferreteria

# 2. Compilar el proyecto
mvn clean compile

# 3. Ejecutar la aplicación
mvn javafx:run
```

### Credenciales de Acceso por Defecto
- **Usuario:** `admin`
- **Contraseña:** `admin123`

---

## 🗂️ Estructura del Proyecto

```
Sistema-Ferreteria/
├── src/main/java/com/ferreteria/
│   ├── Main.java                    # Punto de entrada principal
│   ├── controllers/                 # Controladores JavaFX
│   │   ├── DashboardController.java
│   │   ├── ProductsController.java
│   │   ├── CategoriesController.java
│   │   ├── SalesController.java
│   │   ├── ReportsController.java
│   │   ├── UsersController.java
│   │   └── NavbarController.java
│   ├── models/                      # Modelos de datos
│   │   ├── dao/                     # Acceso a datos
│   │   └── entities/                # Entidades del sistema
│   └── util/                        # Utilidades varias
├── src/main/resources/
│   ├── views/                       # Archivos FXML (interfaces)
│   ├── styles/                      # CSS y estilos
│   └── images/                      # Imágenes y recursos
├── ferreteria.db                    # Base de datos SQLite
└── pom.xml                         # Configuración Maven
```

---

## 🎛️ Módulos del Sistema

### ✅ Dashboard
- **Estado:** Funcional
- **Características:**
  - Estadísticas generales del sistema
  - Cards con información clave
  - Navegación rápida a todos los módulos

### ✅ Gestión de Productos
- **Estado:** Mejorado con scroll optimizado
- **Características:**
  - CRUD completo (Crear, Leer, Actualizar, Eliminar)
  - Búsqueda y filtrado avanzado
  - Vista detallada con scroll optimizado
  - Gestión de categorías
  - Control de stock
  - Precios y ubicación

### ✅ Gestión de Categorías
- **Estado:** Completamente funcional
- **Características:**
  - CRUD completo
  - Sistema de tabs para mejor organización
  - Búsqueda en tiempo real
  - Validación de datos

### ✅ Gestión de Ventas
- **Estado:** Completamente funcional
- **Características:**
  - Historial completo de ventas
  - Estadísticas de ventas (hoy, mes, promedio)
  - Filtros por fecha y estado
  - Búsqueda de productos
  - Paginación de resultados
  - Vista detallada de ventas

### ✅ Reportes
- **Estado:** Completamente funcional
- **Características:**
  - Generación de reportes por rango de fechas
  - Estadísticas detalladas
  - Búsqueda avanzada de ventas
  - Exportación a PDF y Excel
  - Gráficos de ventas
  - Resumen por métodos de pago
  - Análisis de productos vendidos

### ✅ Configuración
- **Estado:** Funcional
- **Características:**
  - Configuración general del sistema
  - Parámetros personalizables

### ✅ Gestión de Usuarios
- **Estado:** Funcional
- **Características:**
  - CRUD de usuarios
  - Gestión de roles y permisos
  - Control de acceso

---

## 🎨 Mejoras de UI/UX Implementadas

### Botones Optimizados
- **Antes:** Botones azules con baja visibilidad
- **Ahora:** Botones grises oscuros (#374151) con texto blanco
- **Mejora:** Mayor contraste y legibilidad

### Scroll Optimizado
- **Módulo Productos:** Scroll mejorado para evitar contenido "elongado"
- **Diálogos:** Scroll optimizado en vistas detalladas
- **Experiencia:** Navegación fluida sin contenido truncado

### Estadísticas Visuales
- **Cards informativos** con iconos descriptivos
- **Colores diferenciados** por tipo de dato
- **Layout responsivo** y moderno

---

## 🔧 Configuración Técnica

### Base de Datos
- **Tipo:** SQLite
- **Ubicación:** `ferreteria.db` (raíz del proyecto)
- **Inicialización:** Automática al primer inicio
- **Generación de datos:** `generar-datos.bat`

### Dependencias Principales
```xml
<dependencies>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21.0.1</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>21.0.1</version>
    </dependency>
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.45.1.0</version>
    </dependency>
</dependencies>
```

### Configuración Maven
- **Java Version:** 17
- **JavaFX Version:** 21.0.1
- **Plugin:** javafx-maven-plugin 0.0.8

---

## 🐛 Solución de Problemas Comunes

### Error: Base de Datos Bloqueada
**Síntoma:** `database is locked`
**Solución:**
```bash
# Opción 1: Usar PowerShell
Get-Process java* | Stop-Process -Force

# Opción 2: Usar CMD
taskkill /F /IM java.exe
taskkill /F /IM javaw.exe

# Opción 3: Reiniciar el sistema
```

### Error: LoadException en Ventas/Reportes
**Síntoma:** `javafx.fxml.LoadException: Invalid path`
**Causa:** Caracteres especiales en FXML
**Solución:** Ya solucionado - se eliminaron emojis y caracteres especiales

### Error: Compilación Fallida
**Síntoma:** `cannot find symbol`
**Solución:**
```bash
mvn clean compile
# Si persiste, verificar imports en controllers
```

---

## 📝 Historial de Actualizaciones Importantes

### ✅ v1.0.0 - Versión Final Estable
- **Fecha:** 31 de Enero de 2026
- **Cambios:**
  - ✅ Ventas y Reportes completamente funcionales
  - ✅ Botones optimizados para mayor visibilidad
  - ✅ Scroll mejorado en Products y diálogos
  - ✅ Sistema estable sin errores de carga
  - ✅ Todos los módulos navegables

### 🔧 Arreglos Críticos Aplicados
1. **Emojis eliminados** de FXML (causaban LoadException)
2. **Caracteres especiales eliminados** ($, #, %)
3. **Tipos de componentes corregidos** (HBox/VBox mismatch)
4. **Botones optimizados** (gris oscuro con texto blanco)
5. **Scroll optimizado** en módulos Products y diálogos

---

## 🔄 Flujo de Trabajo de Git

### Branch Actual
```bash
# Branch de trabajo
seba_branch
```

### Comandos Útiles
```bash
# Ver estado actual
git status

# Ver historial de cambios
git log --oneline -10

# Cambiar al branch principal
git checkout seba_branch

# Sincronizar cambios
git pull origin seba_branch
```

### Estructura de Commits
- **🔧 ARREGLO:** Para correcciones de bugs
- **✅ MEJORA:** Para nuevas funcionalidades
- **🎨 UI/UX:** Para cambios visuales
- **📝 DOC:** Para documentación

---

## 📊 Estadísticas del Sistema

### Módulos Funcionales: 7/7 ✅
- Dashboard ✅
- Products ✅ (Mejorado)
- Categories ✅ (Completo)
- Sales ✅ (Funcional)
- Reports ✅ (Funcional)
- Configuration ✅
- Users ✅

### Componentes Técnicos
- **Controllers:** 7
- **Views (FXML):** 7
- **DAO Classes:** 6
- **Entities:** 6
- **CSS Styles:** 1 archivo principal

---

## 🚀 Próximos Mejoras (Futuras)

### Planeadas
- [ ] Sistema de inventario avanzado
- [ ] Reportes personalizados
- [ ] Integración con APIs externas
- [ ] Módulo de proveedores
- [ ] Sistema de facturación

### Opcionales
- [ ] Tema oscuro/claro
- [ ] Exportación a más formatos
- [ ] Backup automático
- [ ] Sistema de notificaciones

---

## 📞 Soporte y Contacto

### Para Soporte Técnico
1. **Revisar este README** para soluciones comunes
2. **Verificar logs** en la consola de ejecución
3. **Reiniciar base de datos** si es necesario
4. **Contactar al desarrollador** para problemas complejos

### Información de Depuración
- **Logs:** Consola de aplicación
- **Base de datos:** `ferreteria.db`
- **Configuración:** `pom.xml`
- **Estilos:** `src/main/resources/styles/main.css`

---

## 📜 Licencia

**Propiedad:** Desarrollado para gestión de ferretería  
**Uso:** Interno  
**Restricciones:** Sin redistribución sin permiso

---

**Última Actualización:** 31 de Enero de 2026  
**Versión:** 1.0.0 Estable  
**Estado:** ✅ Producción Listo
