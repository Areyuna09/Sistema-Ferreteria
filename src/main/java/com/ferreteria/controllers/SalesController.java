package com.ferreteria.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Controlador para la vista de Ventas.
 * Actualmente en desarrollo.
 */
public class SalesController {

    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize() {
        if (welcomeLabel != null) {
            welcomeLabel.setText("Módulo de Ventas - En Desarrollo");
        }
        System.out.println("SalesController inicializado - Módulo en desarrollo");
    }

    @FXML
    public void handleDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle("Sistema Ferreteria - Dashboard");
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("Error al navegar al Dashboard: " + e.getMessage());
        }
    }

    @FXML
    public void handleProducts() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Products.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle("Sistema Ferreteria - Productos");
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("Error al navegar a Productos: " + e.getMessage());
        }
    }

    @FXML
    public void handleCategories() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Categories.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle("Sistema Ferreteria - Categorías");
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("Error al navegar a Categorías: " + e.getMessage());
        }
    }

    @FXML
    public void handleReports() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Reports.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle("Sistema Ferreteria - Reportes");
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("Error al navegar a Reportes: " + e.getMessage());
        }
    }

    @FXML
    public void handleSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Settings.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle("Sistema Ferreteria - Configuración");
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("Error al navegar a Configuración: " + e.getMessage());
        }
    }

    @FXML
    public void handleUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Users.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle("Sistema Ferreteria - Usuarios");
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("Error al navegar a Usuarios: " + e.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle("Sistema Ferreteria - Login");
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("Error al navegar a Login: " + e.getMessage());
        }
    }
}
