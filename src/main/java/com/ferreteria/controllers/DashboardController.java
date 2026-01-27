package com.ferreteria.controllers;

import com.ferreteria.Main;
import com.ferreteria.models.dao.DatabaseConfig;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controlador del Dashboard principal.
 */
public class DashboardController {

    @FXML private NavbarController navbarController;
    @FXML private Label dateLabel;
    @FXML private Label totalProductsLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label todaySalesLabel;
    @FXML private Label totalUsersLabel;

    @FXML
    public void initialize() {
        if (navbarController != null) {
            navbarController.setActiveView("dashboard");
        }
        loadDate();
        loadStats();
    }

    private void loadDate() {
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
            rs = stmt.executeQuery("""
                SELECT COUNT(*) FROM product_variants pv
                JOIN products p ON pv.product_id = p.id
                WHERE pv.active = 1 AND p.active = 1 AND pv.stock <= pv.min_stock
                """);
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

    // Acciones rápidas
    @FXML
    public void handleNewSale() {
        Main.navigateTo("/views/NuevaVenta.fxml", "Sistema Ferreteria - Nueva Venta");
    }

    @FXML
    public void handleNewProduct() {
        Main.navigateTo("/views/Products.fxml", "Sistema Ferreteria - Productos");
    }

    @FXML
    public void handleInventory() {
        Main.navigateTo("/views/Products.fxml", "Sistema Ferreteria - Productos");
    }

    @FXML
    public void handleGenerateReport() {
        Main.navigateTo("/views/Reports.fxml", "Sistema Ferreteria - Reportes");
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
