package com.ferreteria;

import com.ferreteria.models.dao.DatabaseConfig;
import com.ferreteria.models.dao.DatabaseInitializer;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * Punto de entrada principal de la aplicación JavaFX.
 */
public class Main extends Application {

    private static Stage primaryStage;
    private static Stage debugStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // Aplicar tema AtlantaFX PrimerLight
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        try {
            initializeDatabase();
            showLoginScreen();
        } catch (Exception e) {
            System.err.println("Error iniciando aplicación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeDatabase() {
        System.out.println("Inicializando base de datos...");
        DatabaseInitializer initializer = new DatabaseInitializer(DatabaseConfig.getInstance());
        initializer.initialize();
    }

    private void showLoginScreen() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));

        Scene scene = new Scene(root, 1100, 650);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        // Agregar atajo F12 para Debug Panel
        setupDebugShortcut(scene);

        primaryStage.setTitle("Ferreteria - Sistema de Gestion");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(950);
        primaryStage.setMinHeight(600);
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void navigateTo(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(Main.class.getResource(fxmlPath));

            // Cambiar solo el root de la escena actual para mantener el estado de la ventana
            Scene currentScene = primaryStage.getScene();
            currentScene.setRoot(root);

            // Agregar atajo F12 para Debug Panel (excepto si ya estamos en debug)
            if (!fxmlPath.contains("Debug")) {
                setupDebugShortcut(currentScene);
            }

            primaryStage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Configura el atajo F12 para abrir el panel de debug.
     */
    private static void setupDebugShortcut(Scene scene) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F12) {
                openDebugPanel();
                event.consume();
            }
        });
    }

    /**
     * Abre el panel de debug en ventana separada.
     */
    public static void openDebugPanel() {
        if (debugStage != null && debugStage.isShowing()) {
            debugStage.toFront();
            return;
        }

        try {
            Parent root = FXMLLoader.load(Main.class.getResource("/views/Debug.fxml"));
            Scene scene = new Scene(root, 1000, 600);
            scene.getStylesheets().add(Main.class.getResource("/styles/main.css").toExternalForm());

            debugStage = new Stage();
            debugStage.setTitle("DEBUG PANEL - Logs & Testing");
            debugStage.setScene(scene);
            debugStage.setMinWidth(800);
            debugStage.setMinHeight(500);
            debugStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeDebugPanel() {
        if (debugStage != null) {
            debugStage.close();
            debugStage = null;
        }
    }

    @Override
    public void stop() {
        DatabaseConfig.getInstance().close();
        System.out.println("Aplicación cerrada");
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
