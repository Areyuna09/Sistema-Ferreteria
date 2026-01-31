package com.ferreteria.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Utilidad para navegación global entre vistas
 */
public class NavigationManager {
    
    private static Stage currentStage;
    
    public static void setCurrentStage(Stage stage) {
        currentStage = stage;
    }
    
    public static void navigateTo(String fxmlPath, String title) {
        try {
            System.out.println("Navegando a: " + fxmlPath);
            Parent root = FXMLLoader.load(NavigationManager.class.getResource(fxmlPath));
            
            if (currentStage != null) {
                // Mantener el tamaño actual de la ventana
                Scene currentScene = currentStage.getScene();
                double currentWidth = currentScene.getWidth();
                double currentHeight = currentScene.getHeight();
                
                // Crear nueva escena con el mismo tamaño
                Scene newScene = new Scene(root, currentWidth, currentHeight);
                newScene.getStylesheets().add(NavigationManager.class.getResource("/styles/main.css").toExternalForm());
                
                // Reemplazar la escena de la ventana actual
                currentStage.setTitle(title);
                currentStage.setScene(newScene);
            } else {
                System.err.println("Error: No hay una ventana actual establecida");
            }
            
        } catch (Exception e) {
            System.err.println("Error navegando a " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
