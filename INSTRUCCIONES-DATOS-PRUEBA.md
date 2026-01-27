# Instrucciones para Generar Datos de Prueba

## Problema Actual
La base de datos está siendo utilizada por otro proceso y no puede ser eliminada para recrearla con datos de prueba.

## Solución Manual

### Paso 1: Cerrar Aplicaciones Java
Asegúrate de que **NO haya ninguna aplicación Java en ejecución**:
- Cierra el IDE si estás ejecutando la aplicación desde allí
- Cierra cualquier ventana de la aplicación de Ferretería
- Verifica en el Administrador de Tareas que no haya procesos Java activos

### Paso 2: Ejecutar el Script de Reinicio
Hay dos opciones:

#### Opción A: Script Batch (Recomendado para Windows)
```bash
reiniciar-bd.bat
```

#### Opción B: Comando Maven Directo
```bash
# 1. Eliminar base de datos manualmente
del "%USERPROFILE%\.ferreteria-java-data\ferreteria.db"

# 2. Compilar y ejecutar
mvn compile
mvn exec:java -Dexec.mainClass="com.ferreteria.utils.ResetDatabase"
```

### Paso 3: Verificar Datos
Después de generar los datos, puedes verificarlos con:
```bash
mvn exec:java -Dexec.mainClass="com.ferreteria.utils.VerifyTestData"
```

## Datos Generados

El script genera automáticamente:

### Configuración del Negocio
- Nombre: Ferretería El Tornillo Feliz
- Dirección: Av. San Martin 1234, Ciudad de Buenos Aires
- Teléfono: +54 11 4567-8900
- CUIT: 20-12345678-9

### Usuarios
| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| admin | admin123 | Administrador |
| vendedor1 | vendedor123 | Vendedor |
| vendedor2 | vendedor123 | Vendedor |
| supervisor | supervisor123 | Administrador |

### Categorías (8)
- Herramientas Manuales
- Herramientas Eléctricas
- Tornillería
- Pinturería
- Electricidad
- Plomería
- Construcción
- Jardín

### Productos y Variantes
- 16 productos diferentes
- 39 variantes de productos con precios y stock
- Ejemplos: Martillos, Destornilladores, Taladros, Pinturas, etc.

### Ventas
- Entre 15-25 ventas del mes anterior
- Entre 20-35 ventas del mes actual
- Distribución aleatoria de días
- Diferentes métodos de pago (efectivo, tarjeta débito/crédito, transferencia)
- Ventas con múltiples productos
- Algunos pagos combinados (20%)

## Probar Exportaciones PDF/Excel

Una vez generados los datos:

1. **Iniciar la aplicación**
   ```bash
   mvn javafx:run
   ```

2. **Iniciar sesión**
   - Usuario: `admin`
   - Contraseña: `admin123`

3. **Ir a Reportes**
   - Navega a la sección de "Reportes" en el menú

4. **Generar Reporte**
   - Selecciona el mes actual o el mes anterior
   - Haz clic en "Generar Reporte"

5. **Exportar**
   - Haz clic en "Exportar PDF" o "Exportar Excel"
   - Selecciona ubicación y nombre del archivo
   - El archivo se abrirá automáticamente

## Ubicación de la Base de Datos

La base de datos se encuentra en:
```
Windows: C:\Users\[TuUsuario]\.ferreteria-java-data\ferreteria.db
Linux/Mac: ~/.ferreteria-java-data/ferreteria.db
```

## Troubleshooting

### La base de datos sigue bloqueada
1. Abre el Administrador de Tareas (Ctrl+Shift+Esc)
2. Busca procesos `java.exe` o `javaw.exe`
3. Finaliza todos los procesos Java
4. Intenta ejecutar el script nuevamente

### No se generan datos
1. Verifica que Maven esté instalado: `mvn -version`
2. Compila el proyecto: `mvn clean compile`
3. Revisa los logs en la consola

### Los reportes no muestran datos
1. Verifica que seleccionaste el mes correcto (mes actual o anterior)
2. Ejecuta el verificador de datos:
   ```bash
   mvn exec:java -Dexec.mainClass="com.ferreteria.utils.VerifyTestData"
   ```

## Resumen de Archivos Creados

- `src/main/java/com/ferreteria/utils/TestDataGenerator.java` - Generador de datos
- `src/main/java/com/ferreteria/utils/ResetDatabase.java` - Script de reinicio
- `src/main/java/com/ferreteria/utils/VerifyTestData.java` - Verificador de datos
- `src/main/java/com/ferreteria/utils/PDFExporter.java` - Exportador PDF
- `src/main/java/com/ferreteria/utils/ExcelExporter.java` - Exportador Excel
- `reiniciar-bd.bat` - Script batch para Windows
