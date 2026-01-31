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
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Controlador simple para la pantalla de reportes de ventas.
 * Implementado desde cero con funcionalidad básica.
 */
public class ReportsController {
    
    // FXML - Filtros
    @FXML private ComboBox<String> rangeTypeCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    
    // FXML - Búsqueda de ventas
    @FXML private TextField searchSaleIdField;
    @FXML private DatePicker searchDatePicker;
    @FXML private TextField searchAmountField;
    
    // FXML - Botones
    @FXML private Button exportPdfBtn;
    @FXML private Button exportExcelBtn;
    
    // FXML - Estadísticas
    @FXML private HBox statsContainer;
    @FXML private Label totalSalesLabel;
    @FXML private Label totalTransactionsLabel;
    @FXML private Label avgSaleLabel;
    @FXML private Label maxSaleLabel;
    
    // FXML - Métodos de Pago
    @FXML private VBox paymentMethodsSection;
    @FXML private HBox paymentMethodsContainer;
    
    // FXML - Gráfico
    @FXML private VBox chartSection;
    @FXML private StackPane chartContainer;
    
    // FXML - Tabla de Productos
    @FXML private VBox productsSection;
    @FXML private TableView<String> productsTable;
    @FXML private TableColumn<String, String> productColumn;
    @FXML private TableColumn<String, String> variantColumn;
    @FXML private TableColumn<String, Integer> quantityColumn;
    @FXML private TableColumn<String, String> priceColumn;
    @FXML private TableColumn<String, String> totalColumn;
    
    // FXML - Búsqueda de ventas
    @FXML private TableView<String> salesSearchTable;
    @FXML private TableColumn<String, Integer> searchIdColumn;
    @FXML private TableColumn<String, String> searchDateColumn;
    @FXML private TableColumn<String, String> searchTotalColumn;
    @FXML private TableColumn<String, String> searchStatusColumn;
    @FXML private TableColumn<String, String> searchSellerColumn;
    @FXML private TableColumn<String, Void> searchActionsColumn;
    
    // FXML - Estado vacío
    @FXML private VBox emptyStateContainer;
    
    @FXML
    public void initialize() {
        setupRangeFilters();
        setupProductsTable();
        setupSearchTable();
        
        // Mostrar estado vacío por defecto
        showEmptyState();
    }
    
    private void setupRangeFilters() {
        ObservableList<String> rangeTypes = FXCollections.observableArrayList(
            "Hoy",
            "Esta Semana", 
            "Este Mes",
            "Este Año",
            "Personalizado"
        );
        rangeTypeCombo.setItems(rangeTypes);
        rangeTypeCombo.getSelectionModel().select(2); // "Este Mes" por defecto
        
        // Configurar fechas por defecto
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.withDayOfMonth(1));
        endDatePicker.setValue(today.withDayOfMonth(today.lengthOfMonth()));
    }
    
    private void setupProductsTable() {
        productColumn.setCellValueFactory(new PropertyValueFactory<>("producto"));
        variantColumn.setCellValueFactory(new PropertyValueFactory<>("variante"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("precio"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        
        // Centrar columna de cantidad
        quantityColumn.setStyle("-fx-alignment: CENTER;");
        
        productsTable.setItems(getSampleProducts());
    }
    
    private void setupSearchTable() {
        searchIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        searchDateColumn.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        searchTotalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        searchSellerColumn.setCellValueFactory(new PropertyValueFactory<>("vendedor"));
        
        searchStatusColumn.setCellValueFactory(param -> 
            new SimpleStringProperty("Completada"));
        
        searchActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button btnVer = new Button("Ver");
            private final HBox container = new HBox(5, btnVer);
            
            {
                btnVer.getStyleClass().addAll("action-button", "small");
                btnVer.setOnAction(e -> {
                    String venta = getTableView().getItems().get(getIndex());
                    handleViewSearchDetail(venta);
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
        
        salesSearchTable.setItems(getSampleSales());
    }
    
    private void showEmptyState() {
        emptyStateContainer.setManaged(true);
        emptyStateContainer.setVisible(true);
        
        statsContainer.setManaged(false);
        statsContainer.setVisible(false);
        
        paymentMethodsSection.setManaged(false);
        paymentMethodsSection.setVisible(false);
        
        chartSection.setManaged(false);
        chartSection.setVisible(false);
        
        productsSection.setManaged(false);
        productsSection.setVisible(false);
        
        disableExportButtons();
    }
    
    private void showReportSections() {
        emptyStateContainer.setManaged(false);
        emptyStateContainer.setVisible(false);
        
        statsContainer.setManaged(true);
        statsContainer.setVisible(true);
        
        paymentMethodsSection.setManaged(true);
        paymentMethodsSection.setVisible(true);
        
        chartSection.setManaged(true);
        chartSection.setVisible(true);
        
        productsSection.setManaged(true);
        productsSection.setVisible(true);
        
        enableExportButtons();
    }
    
    private void disableExportButtons() {
        exportPdfBtn.setDisable(true);
        exportExcelBtn.setDisable(true);
    }
    
    private void enableExportButtons() {
        exportPdfBtn.setDisable(false);
        exportExcelBtn.setDisable(false);
    }
    
    private ObservableList<String> getSampleProducts() {
        ObservableList<String> products = FXCollections.observableArrayList();
        
        // Datos de ejemplo
        for (int i = 1; i <= 5; i++) {
            products.add("Producto " + i);
        }
        
        return products;
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
    private void handleGenerateReport() {
        // Validar filtros
        if (rangeTypeCombo.getValue() == null) {
            showError("Por favor selecciona un tipo de rango");
            return;
        }
        
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showError("Por favor selecciona las fechas del rango");
            return;
        }
        
        // Mostrar reporte
        updateStatistics();
        showReportSections();
    }
    
    private void updateStatistics() {
        // Datos de ejemplo
        totalSalesLabel.setText("$125,450.00");
        totalTransactionsLabel.setText("234");
        avgSaleLabel.setText("$535.90");
        maxSaleLabel.setText("$3,250.00");
    }
    
    @FXML
    private void handleSearchSales() {
        // Implementación simple de búsqueda
        salesSearchTable.setItems(getSampleSales());
    }
    
    @FXML
    private void handleClearSearchFilters() {
        searchSaleIdField.clear();
        searchDatePicker.setValue(null);
        searchAmountField.clear();
        salesSearchTable.setItems(FXCollections.observableArrayList());
    }
    
    private void handleViewSearchDetail(String venta) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalle de Venta");
        alert.setHeaderText("Venta: " + venta);
        alert.setContentText("Información detallada de la venta");
        alert.showAndWait();
    }
    
    @FXML
    private void handleExportPDF() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Exportar PDF");
        alert.setHeaderText("Exportación a PDF");
        alert.setContentText("Función de exportación a PDF no implementada en esta versión");
        alert.showAndWait();
    }
    
    @FXML
    private void handleExportExcel() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Exportar Excel");
        alert.setHeaderText("Exportación a Excel");
        alert.setContentText("Función de exportación a Excel no implementada en esta versión");
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
    private void handleSales() {
        navigateTo("/views/Sales.fxml", "Sistema Ferretería - Ventas");
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
            
            Stage stage = (Stage) rangeTypeCombo.getScene().getWindow();
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
