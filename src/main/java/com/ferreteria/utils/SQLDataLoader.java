package com.ferreteria.utils;

import com.ferreteria.models.dao.DatabaseConfig;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Utilidad para cargar datos SQL desde archivos en la base de datos
 */
public class SQLDataLoader {
    private static final Logger LOGGER = Logger.getLogger(SQLDataLoader.class.getName());

    public static void main(String[] args) {
        try {
            System.out.println("🔄 Cargando datos SQL en la base de datos...");
            
            // Cargar categorías
            loadSQLFile("src/main/resources/sql/categories_init.sql");
            System.out.println("✅ Categorías cargadas");
            
            // Cargar productos
            loadSQLFile("src/main/resources/sql/products_part1.sql");
            System.out.println("✅ Productos cargados");
            
            // Cargar variantes
            loadSQLFile("src/main/resources/sql/variants_part1.sql");
            System.out.println("✅ Variantes cargadas");
            
            // Verificar datos
            verifyData();
            
            System.out.println("🎉 ¡Todos los datos SQL cargados exitosamente!");
            
        } catch (Exception e) {
            System.err.println("❌ Error cargando datos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void loadSQLFile(String filePath) throws Exception {
        Connection conn = DatabaseConfig.getInstance().getConnection();
        Statement stmt = conn.createStatement();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                // Ignorar comentarios y líneas vacías
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                
                sqlBuilder.append(line).append("\n");
                
                // Si encontramos un punto y coma, ejecutamos el SQL acumulado
                if (line.endsWith(";")) {
                    String sql = sqlBuilder.toString().trim();
                    if (!sql.isEmpty()) {
                        try {
                            stmt.execute(sql);
                        } catch (Exception e) {
                            System.err.println("Error ejecutando SQL: " + sql);
                            System.err.println("Error: " + e.getMessage());
                        }
                    }
                    sqlBuilder.setLength(0);
                }
            }
            
            // Ejecutar cualquier SQL restante
            String remainingSQL = sqlBuilder.toString().trim();
            if (!remainingSQL.isEmpty()) {
                try {
                    stmt.execute(remainingSQL);
                } catch (Exception e) {
                    System.err.println("Error ejecutando SQL final: " + remainingSQL);
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }
        
        stmt.close();
        LOGGER.info("Archivo SQL cargado: " + filePath);
    }
    
    private static void verifyData() throws Exception {
        Connection conn = DatabaseConfig.getInstance().getConnection();
        Statement stmt = conn.createStatement();
        
        System.out.println("\n📊 === VERIFICACIÓN DE DATOS ===");
        
        String[] tables = {"users", "categories", "products", "product_variants", "sales", "sale_items", "sale_payments"};
        String[] names = {"Usuarios", "Categorías", "Productos", "Variantes", "Ventas", "Items de venta", "Pagos"};
        
        for (int i = 0; i < tables.length; i++) {
            try {
                var rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tables[i]);
                if (rs.next()) {
                    System.out.println(names[i] + ": " + rs.getInt(1) + " registros");
                }
                rs.close();
            } catch (Exception e) {
                System.out.println(names[i] + ": Error - " + e.getMessage());
            }
        }
        
        // Mostrar algunos datos de ejemplo
        System.out.println("\n📋 === EJEMPLOS DE DATOS ===");
        
        try {
            var rs = stmt.executeQuery("SELECT id, name FROM categories ORDER BY id LIMIT 5");
            System.out.println("Categorías:");
            while (rs.next()) {
                System.out.println("  " + rs.getInt(1) + ". " + rs.getString(2));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("Error mostrando categorías: " + e.getMessage());
        }
        
        try {
            var rs = stmt.executeQuery("SELECT id, code, name FROM products ORDER BY id LIMIT 5");
            System.out.println("\nProductos:");
            while (rs.next()) {
                System.out.println("  " + rs.getInt(1) + ". " + rs.getString(2) + " - " + rs.getString(3));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("Error mostrando productos: " + e.getMessage());
        }
        
        stmt.close();
    }
}
