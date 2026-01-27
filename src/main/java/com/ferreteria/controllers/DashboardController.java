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
import java.util.Locale;

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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM yyyy", new Locale("es", "ES"));
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
        System.out.println("Navegando a Productos...");
        // TODO: Implementar vista de productos
    }

    @FXML
    public void handleCategories() {
        navigateTo("/views/Categories.fxml", "Sistema Ferretería - Categorías");
    }

    @FXML
    public void handleSales() {
        System.out.println("Navegando a Ventas...");
        // TODO: Implementar vista de ventas
    }

    @FXML
    public void handleReports() {
        System.out.println("Navegando a Reportes...");
        navigateTo("/views/Reports.fxml", "Sistema Ferretería - Reportes");
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

    /**
     * Navega a una vista específica
     *
     * @param fxmlPath Ruta del archivo FXML
     * @param title Título de la ventana
     */
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();

            // Guardar estado actual de la ventana
            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();

            Scene scene = new Scene(root, currentWidth, currentHeight);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            // Agregar CSS adicional para reportes si es necesario
            if (fxmlPath.contains("Reports")) {
                scene.getStylesheets().add(getClass().getResource("/styles/reports.css").toExternalForm());
            }

            stage.setTitle(title);
            stage.setResizable(true);
            stage.setScene(scene);

            // Restaurar estado maximizado si estaba maximizado
            if (wasMaximized) {
                stage.setMaximized(true);
            }

        } catch (Exception e) {
            System.err.println("Error al navegar a " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
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
}