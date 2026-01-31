package com.ferreteria.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controlador simple para la gestión de ventas.
 * Implementado desde cero con funcionalidad básica.
 */
public class SalesController {
    
    @FXML private Label ventasHoyLabel;
    @FXML private Label ventasMesLabel;
    @FXML private Label cantidadHoyLabel;
    @FXML private Label promedioLabel;
    
    @FXML private DatePicker fechaDesde;
    @FXML private DatePicker fechaHasta;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField searchField;
    
    @FXML private TableView<String> ventasTable;
    @FXML private TableColumn<String, Integer> colId;
    @FXML private TableColumn<String, String> colFecha;
    @FXML private TableColumn<String, String> colProductos;
    @FXML private TableColumn<String, String> colVendedor;
    @FXML private TableColumn<String, String> colTotal;
    @FXML private TableColumn<String, String> colEstado;
    @FXML private TableColumn<String, Void> colAcciones;
    
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Label paginaLabel;
    
    @FXML
    public void initialize() {
        setupFilters();
        setupSalesTable();
        setupPagination();
        loadStats();
        loadSales();
    }
    
    private void setupFilters() {
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
            "Todos", "Completadas", "Anuladas"
        );
        statusFilter.setItems(statusOptions);
        statusFilter.setValue("Todos");
    }
    
    private void setupSalesTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colVendedor.setCellValueFactory(new PropertyValueFactory<>("vendedor"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        
        colProductos.setCellValueFactory(param -> 
            new SimpleStringProperty("Venta #" + param.getValue()));
        
        colEstado.setCellValueFactory(param -> 
            new SimpleStringProperty("Completada"));
        
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnVer = new Button("Ver");
            private final HBox container = new HBox(5, btnVer);
            
            {
                btnVer.getStyleClass().addAll("action-button", "small");
                btnVer.setOnAction(e -> {
                    String venta = getTableView().getItems().get(getIndex());
                    handleViewDetail(venta);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
        
        ventasTable.setItems(getSampleSales());
    }
    
    private void setupPagination() {
        updatePagination();
    }
    
    private void loadStats() {
        // Datos de ejemplo
        ventasHoyLabel.setText("$15,230.50");
        ventasMesLabel.setText("$125,450.00");
        cantidadHoyLabel.setText("23");
        promedioLabel.setText("$662.20");
    }
    
    private void loadSales() {
        // Cargar datos de ejemplo
        ventasTable.setItems(getSampleSales());
        updatePagination();
    }
    
    private void updatePagination() {
        paginaLabel.setText("Página 1 de 1");
        btnAnterior.setDisable(true);
        btnSiguiente.setDisable(true);
    }
    
    private ObservableList<String> getSampleSales() {
        ObservableList<String> sales = FXCollections.observableArrayList();
        
        // Datos de ejemplo
        for (int i = 1; i <= 10; i++) {
            sales.add("Venta " + i);
        }
        
        return sales;
    }
    
    @FXML
    public void handleNuevaVenta() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/NewSale.fxml"));
            if (loader.getLocation() == null) {
                showError("No se encontró el archivo del punto de venta");
                return;
            }
            
            Parent root = loader.load();
            
            Stage stage = (Stage) ventasTable.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle("Punto de Venta - POS");
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (IOException e) {
            showError("Error al abrir el punto de venta: " + e.getMessage());
        }
    }

    @FXML
    public void handleFiltrar() {
        loadSales();
    }

    @FXML
    public void handleLimpiarFiltros() {
        fechaDesde.setValue(null);
        fechaHasta.setValue(null);
        statusFilter.setValue("Todos");
        searchField.clear();
        loadSales();
    }

    @FXML
    public void handlePaginaAnterior() {
        // Implementación simple
    }

    @FXML
    public void handlePaginaSiguiente() {
        // Implementación simple
    }

    private void handleViewDetail(String venta) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalle de Venta");
        alert.setHeaderText("Venta: " + venta);
        alert.setContentText("Información detallada de la venta");
        alert.showAndWait();
    }
    
    // ==================== NAVEGACIÓN ====================
    
    @FXML
    private void handleDashboard() {
        navigateTo("/views/Dashboard.fxml", "Sistema Ferretería - Dashboard");
    }
    
    @FXML
    private void handleProducts() {
        navigateTo("/views/Products.fxml", "Sistema Ferretería - Productos");
    }
    
    @FXML
    private void handleCategories() {
        navigateTo("/views/Categories.fxml", "Sistema Ferretería - Categorías");
    }
    
    @FXML
    private void handleReports() {
        navigateTo("/views/Reports.fxml", "Sistema Ferretería - Reportes");
    }
    
    @FXML
    private void handleSettings() {
        navigateTo("/views/Settings.fxml", "Sistema Ferretería - Configuración");
    }
    
    @FXML
    private void handleUsers() {
        navigateTo("/views/Users.fxml", "Sistema Ferretería - Usuarios");
    }
    
    @FXML
    private void handleLogout() {
        navigateTo("/views/Login.fxml", "Sistema Ferretería - Login");
    }
    
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            Stage stage = (Stage) ventasTable.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle(title);
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (IOException e) {
            showError("Error al cargar la vista: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
