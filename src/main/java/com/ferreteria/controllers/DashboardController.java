package com.ferreteria.controllers;

import com.ferreteria.Main;
import com.ferreteria.models.User;
import com.ferreteria.models.dao.DatabaseConfig;
import com.ferreteria.utils.SessionManager;

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
        System.out.println("Navegando a Productos...");
        // TODO: Implementar vista de productos
    }

    @FXML
    public void handleSales() {
        Main.navigateTo("/views/Ventas.fxml", "Sistema Ferreteria - Ventas");
    }

    @FXML
    public void handleReports() {
        System.out.println("Navegando a Reportes...");
        // TODO: Implementar vista de reportes
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
        Main.navigateTo("/views/Login.fxml", "Ferreteria - Sistema de Gestion");
    }
}
