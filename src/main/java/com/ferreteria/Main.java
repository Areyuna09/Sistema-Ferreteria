package com.ferreteria;

import com.ferreteria.infrastructure.persistence.DatabaseConfig;
import com.ferreteria.infrastructure.persistence.DatabaseInitializer;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de entrada principal de la aplicación JavaFX.
 */
public class Main extends Application {

    private static Stage primaryStage;

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

        primaryStage.setTitle("Ferreteria - Sistema de Gestion");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(950);
        primaryStage.setMinHeight(600);
        primaryStage.centerOnScreen();
        primaryStage.show();
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
