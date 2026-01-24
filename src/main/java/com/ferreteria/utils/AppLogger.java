package com.ferreteria.utils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

/**
 * Sistema de logging centralizado para la aplicación.
 * Registra eventos en archivo y consola con formato personalizado.
 */
public class AppLogger {

    private static final String LOG_FILE = "logs/sistema.log";
    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final int MAX_FILE_COUNT = 5;

    private static Logger logger;
    private static boolean initialized = false;
    private static javafx.scene.control.TextArea uiOutput;

    private AppLogger() {}

    /**
     * Obtiene la instancia del logger, inicializándolo si es necesario.
     */
    public static synchronized Logger getLogger() {
        if (!initialized) {
            initialize();
        }
        return logger;
    }

    /**
     * Configura un TextArea para mostrar logs en la UI.
     */
    public static void setUiOutput(javafx.scene.control.TextArea textArea) {
        uiOutput = textArea;
    }

    private static void initialize() {
        try {
            // Crear directorio de logs si no existe
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            logger = Logger.getLogger("Ferreteria");
            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false);

            // Handler para archivo con rotación
            FileHandler fileHandler = new FileHandler(LOG_FILE, MAX_FILE_SIZE, MAX_FILE_COUNT, true);
            fileHandler.setFormatter(new CustomFormatter());
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);

            // Handler para consola
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new CustomFormatter());
            consoleHandler.setLevel(Level.INFO);
            logger.addHandler(consoleHandler);

            initialized = true;
            logger.info("Sistema de logging inicializado");

        } catch (IOException e) {
            System.err.println("Error inicializando logger: " + e.getMessage());
            // Fallback a logger básico
            logger = Logger.getLogger("Ferreteria");
        }
    }

    /**
     * Registra operación INFO con usuario actual.
     */
    public static void info(String message) {
        String formatted = formatWithUser(message);
        getLogger().info(formatted);
        appendToUi("INFO", formatted);
    }

    /**
     * Registra operación INFO para un módulo específico.
     */
    public static void info(String module, String message) {
        String formatted = formatWithModule(module, message);
        getLogger().info(formatted);
        appendToUi("INFO", formatted);
    }

    /**
     * Registra advertencia WARNING.
     */
    public static void warning(String message) {
        String formatted = formatWithUser(message);
        getLogger().warning(formatted);
        appendToUi("WARNING", formatted);
    }

    /**
     * Registra advertencia WARNING para un módulo específico.
     */
    public static void warning(String module, String message) {
        String formatted = formatWithModule(module, message);
        getLogger().warning(formatted);
        appendToUi("WARNING", formatted);
    }

    /**
     * Registra error ERROR.
     */
    public static void error(String message) {
        String formatted = formatWithUser(message);
        getLogger().severe(formatted);
        appendToUi("ERROR", formatted);
    }

    /**
     * Registra error ERROR con excepción.
     */
    public static void error(String message, Throwable throwable) {
        String formatted = formatWithUser(message);
        getLogger().log(Level.SEVERE, formatted, throwable);
        appendToUi("ERROR", formatted + " - " + throwable.getMessage());
    }

    /**
     * Registra error ERROR para un módulo específico.
     */
    public static void error(String module, String message, Throwable throwable) {
        String formatted = formatWithModule(module, message);
        getLogger().log(Level.SEVERE, formatted, throwable);
        appendToUi("ERROR", formatted + " - " + throwable.getMessage());
    }

    private static void appendToUi(String level, String message) {
        if (uiOutput != null) {
            javafx.application.Platform.runLater(() -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                uiOutput.appendText("[" + timestamp + "] [" + level + "] " + message + "\n");
            });
        }
    }

    // === Métodos específicos para operaciones CRUD ===

    /**
     * Registra creación de entidad.
     */
    public static void logCreate(String entity, int id) {
        info("CRUD", "CREATE " + entity + " ID=" + id);
    }

    /**
     * Registra lectura de entidad.
     */
    public static void logRead(String entity, int id) {
        getLogger().fine(formatWithModule("CRUD", "READ " + entity + " ID=" + id));
    }

    /**
     * Registra actualización de entidad.
     */
    public static void logUpdate(String entity, int id) {
        info("CRUD", "UPDATE " + entity + " ID=" + id);
    }

    /**
     * Registra eliminación de entidad.
     */
    public static void logDelete(String entity, int id) {
        info("CRUD", "DELETE " + entity + " ID=" + id);
    }

    /**
     * Registra inicio de sesión.
     */
    public static void logLogin(String username) {
        info("AUTH", "Login exitoso: " + username);
    }

    /**
     * Registra intento de login fallido.
     */
    public static void logLoginFailed(String username, String reason) {
        warning("AUTH", "Login fallido para '" + username + "': " + reason);
    }

    /**
     * Registra cierre de sesión.
     */
    public static void logLogout(String username) {
        info("AUTH", "Logout: " + username);
    }

    /**
     * Registra venta completada.
     */
    public static void logSale(int saleId, String total) {
        info("VENTAS", "Venta #" + saleId + " completada - Total: $" + total);
    }

    /**
     * Registra anulación de venta.
     */
    public static void logSaleCancelled(int saleId) {
        warning("VENTAS", "Venta #" + saleId + " ANULADA");
    }

    private static String formatWithUser(String message) {
        String username = getCurrentUsername();
        return "[" + username + "] " + message;
    }

    private static String formatWithModule(String module, String message) {
        String username = getCurrentUsername();
        return "[" + username + "] [" + module + "] " + message;
    }

    private static String getCurrentUsername() {
        try {
            if (SessionManager.getInstance().isLoggedIn()) {
                return SessionManager.getInstance().getCurrentUser().getUsername();
            }
        } catch (Exception ignored) {}
        return "SYSTEM";
    }

    /**
     * Formatter personalizado para los logs.
     * Formato: [FECHA HORA] [NIVEL] - Mensaje
     */
    private static class CustomFormatter extends Formatter {

        private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();

            // Fecha y hora
            sb.append("[");
            sb.append(LocalDateTime.now().format(DATE_FORMAT));
            sb.append("] ");

            // Nivel
            sb.append("[");
            sb.append(String.format("%-7s", record.getLevel().getName()));
            sb.append("] ");

            // Mensaje
            sb.append(formatMessage(record));
            sb.append(System.lineSeparator());

            // Stack trace si hay excepción
            if (record.getThrown() != null) {
                sb.append("         Exception: ");
                sb.append(record.getThrown().toString());
                sb.append(System.lineSeparator());
                for (StackTraceElement element : record.getThrown().getStackTrace()) {
                    sb.append("            at ");
                    sb.append(element.toString());
                    sb.append(System.lineSeparator());
                }
            }

            return sb.toString();
        }
    }
}
