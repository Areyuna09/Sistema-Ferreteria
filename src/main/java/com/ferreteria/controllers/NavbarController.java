package com.ferreteria.controllers;

import com.ferreteria.Main;
import com.ferreteria.models.User;
import com.ferreteria.utils.AppLogger;
import com.ferreteria.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Controlador del Navbar reutilizable.
 * Maneja la navegación entre módulos.
 */
public class NavbarController {

    @FXML private Button btnDashboard;
    @FXML private Button btnProductos;
    @FXML private Button btnVentas;
    @FXML private Button btnReportes;
    @FXML private Button btnUsuarios;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    private String currentView = "";

    @FXML
    public void initialize() {
        loadUserInfo();
    }

    private void loadUserInfo() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userRoleLabel.setText(user.getRole().getValue());
        }
    }

    /**
     * Marca el botón activo según la vista actual.
     */
    public void setActiveView(String viewName) {
        this.currentView = viewName;

        // Resetear todos
        btnDashboard.getStyleClass().remove("nav-button-active");
        btnProductos.getStyleClass().remove("nav-button-active");
        btnVentas.getStyleClass().remove("nav-button-active");
        btnReportes.getStyleClass().remove("nav-button-active");
        btnUsuarios.getStyleClass().remove("nav-button-active");

        // Activar el correspondiente
        switch (viewName.toLowerCase()) {
            case "dashboard" -> btnDashboard.getStyleClass().add("nav-button-active");
            case "productos", "products" -> btnProductos.getStyleClass().add("nav-button-active");
            case "ventas", "sales" -> btnVentas.getStyleClass().add("nav-button-active");
            case "reportes", "reports" -> btnReportes.getStyleClass().add("nav-button-active");
            case "usuarios", "users" -> btnUsuarios.getStyleClass().add("nav-button-active");
        }
    }

    @FXML
    public void handleDashboard() {
        if (!"dashboard".equals(currentView)) {
            AppLogger.info("NAV", "Navegando a Dashboard");
            Main.navigateTo("/views/Dashboard.fxml", "Sistema Ferreteria - Dashboard");
        }
    }

    @FXML
    public void handleProducts() {
        if (!"productos".equals(currentView)) {
            AppLogger.info("NAV", "Navegando a Productos");
            Main.navigateTo("/views/Products.fxml", "Sistema Ferreteria - Productos");
        }
    }

    @FXML
    public void handleSales() {
        if (!"sales".equals(currentView) && !"ventas".equals(currentView)) {
            AppLogger.info("NAV", "Navegando a Ventas");
            Main.navigateTo("/views/Sales.fxml", "Sistema Ferreteria - Ventas");
        }
    }

    @FXML
    public void handleReports() {
        if (!"reportes".equals(currentView)) {
            AppLogger.info("NAV", "Navegando a Reportes");
            Main.navigateTo("/views/Reports.fxml", "Sistema Ferreteria - Reportes");
        }
    }

    @FXML
    public void handleUsers() {
        if (!SessionManager.getInstance().isAdmin()) {
            return;
        }
        if (!"usuarios".equals(currentView)) {
            AppLogger.info("NAV", "Navegando a Usuarios");
            Main.navigateTo("/views/Users.fxml", "Sistema Ferreteria - Usuarios");
        }
    }

    @FXML
    public void handleLogout() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            AppLogger.logLogout(user.getUsername());
        }
        SessionManager.getInstance().logout();
        Main.navigateTo("/views/Login.fxml", "Ferreteria - Sistema de Gestion");
    }
}
