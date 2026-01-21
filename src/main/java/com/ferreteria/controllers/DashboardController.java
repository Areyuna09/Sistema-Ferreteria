package com.ferreteria.controllers;

import com.ferreteria.models.User;
import com.ferreteria.models.dao.DatabaseConfig;
import com.ferreteria.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controlador del Dashboard principal.
 */
public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label dateLabel;
    @FXML private Label totalProductsLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label todaySalesLabel;
    @FXML private Label totalUsersLabel;

    @FXML
    public void initialize() {
        loadUserInfo();
        loadStats();
    }

    private void loadUserInfo() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Bienvenido, " + user.getFullName());
            roleLabel.setText("Rol: " + user.getRole().getValue());
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM yyyy");
        dateLabel.setText(LocalDateTime.now().format(formatter));
    }

    private void loadStats() {
        try {
            var conn = DatabaseConfig.getInstance().getConnection();
            Statement stmt = conn.createStatement();

            // Total productos
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products WHERE active = 1");
            if (rs.next()) {
                totalProductsLabel.setText(String.valueOf(rs.getInt(1)));
            }

            // Variantes con stock bajo
            rs = stmt.executeQuery("SELECT COUNT(*) FROM product_variants WHERE active = 1 AND stock <= min_stock");
            if (rs.next()) {
                lowStockLabel.setText(String.valueOf(rs.getInt(1)));
            }

            // Ventas de hoy
            rs = stmt.executeQuery("SELECT COALESCE(SUM(total), 0) FROM sales WHERE date(created_at) = date('now')");
            if (rs.next()) {
                todaySalesLabel.setText("$" + String.format("%.2f", rs.getDouble(1)));
            }

            // Total usuarios
            rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE active = 1");
            if (rs.next()) {
                totalUsersLabel.setText(String.valueOf(rs.getInt(1)));
            }

        } catch (Exception e) {
            System.err.println("Error cargando estadísticas: " + e.getMessage());
        }
    }

    @FXML
    public void handleLogout() {
        SessionManager.getInstance().logout();
        navigateToLogin();
    }

    @FXML
    public void handleProducts() {
        navigateToProducts();
    }

    @FXML
    public void handleSales() {
        System.out.println("Navegando a Ventas...");
        // TODO: Implementar vista de ventas
    }

    @FXML
    public void handleReports() {
        navigateToReports();
    }

    @FXML
    public void handleUsers() {
        if (!SessionManager.getInstance().isAdmin()) {
            System.out.println("Acceso denegado: solo administradores");
            return;
        }
        System.out.println("Navegando a Usuarios...");
        // TODO: Implementar vista de usuarios
    }

    private void navigateToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            Scene scene = new Scene(root, 400, 500);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setTitle("Sistema Ferretería - Login");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToProducts() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Products.fxml"));
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setTitle("Sistema Ferretería - Productos");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToReports() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Reports.fxml"));
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setTitle("Sistema Ferretería - Reportes");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
