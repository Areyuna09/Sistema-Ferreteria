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
}
