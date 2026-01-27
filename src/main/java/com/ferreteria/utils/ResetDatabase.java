package com.ferreteria.utils;

import com.ferreteria.models.dao.DatabaseConfig;
import com.ferreteria.models.dao.DatabaseInitializer;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Reinicia la base de datos eliminando el archivo y recreándolo.
 */
public class ResetDatabase {

    public static void main(String[] args) {
        DatabaseConfig dbConfig = DatabaseConfig.getInstance();
        String dbPath = dbConfig.getDbPath();

        System.out.println("===========================================");
        System.out.println("  Reiniciar Base de Datos");
        System.out.println("===========================================\n");

        try {
            // Cerrar todas las conexiones
            dbConfig.close();

            // Esperar un momento
            Thread.sleep(1000);

            // Eliminar archivo de base de datos
            File dbFile = new File(dbPath);
            if (dbFile.exists()) {
                System.out.println("Eliminando base de datos existente...");
                if (dbFile.delete()) {
                    System.out.println("✓ Base de datos eliminada");
                } else {
                    System.err.println("✗ No se pudo eliminar la base de datos");
                    System.err.println("  Por favor cierra cualquier aplicación que esté usando la BD");
                    System.exit(1);
                }
            }

            // Esperar un momento más
            Thread.sleep(500);

            // Inicializar nueva base de datos
            System.out.println("\nCreando nueva base de datos...");
            DatabaseInitializer initializer = new DatabaseInitializer(dbConfig);
            initializer.initialize();
            System.out.println("✓ Base de datos inicializada");

            // Generar datos de prueba
            System.out.println("\nGenerando datos de prueba...");
            TestDataGenerator generator = new TestDataGenerator();
            generator.generateAllTestData();

            System.out.println("\n===========================================");
            System.out.println("✓ Base de datos reiniciada exitosamente");
            System.out.println("===========================================");
            System.out.println("\nCredenciales de acceso:");
            System.out.println("- admin / admin123 (Administrador)");
            System.out.println("- vendedor1 / vendedor123 (Vendedor)");
            System.out.println("- vendedor2 / vendedor123 (Vendedor)");
            System.out.println("- supervisor / supervisor123 (Administrador)");

        } catch (Exception e) {
            System.err.println("\n✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
