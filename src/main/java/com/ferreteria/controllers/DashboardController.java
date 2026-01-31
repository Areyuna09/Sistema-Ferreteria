package com.ferreteria.controllers;

import com.ferreteria.models.dao.DatabaseConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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
    public void handleNewProduct() {
        navbarController.handleProducts();
    }

    @FXML
    public void handleProducts() {
        navbarController.handleProducts();
    }

    @FXML
    public void handleSales() {
        // Ventas no está implementado todavía, mostrar mensaje
        System.out.println("Módulo de ventas en desarrollo");
    }

    @FXML
    public void handleCategories() {
        // Navegar a Categorías
        navbarController.handleCategories();
    }

    @FXML
    public void handleReports() {
        navbarController.handleReports();
    }
}
