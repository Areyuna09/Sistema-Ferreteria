package com.ferreteria.controllers;

import com.ferreteria.models.Sale;
import com.ferreteria.models.dao.ReportDAO;
import com.ferreteria.models.dao.SaleDAO;
import com.ferreteria.utils.SessionManager;
import com.ferreteria.utils.AppLogger;

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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para la pantalla de reportes de ventas.
 * Implementado desde cero basado en la estructura combinada de Omar y Alan.
 */
public class ReportsController {
    
    private static final Logger LOGGER = Logger.getLogger(ReportsController.class.getName());
    
    private final ReportDAO reportDAO = new ReportDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    
    // Rango de fechas seleccionado
    private LocalDate startDate;
    private LocalDate endDate;
    private String currentRangeType;
    
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
    @FXML private TableView<ProductSaleRow> productsTable;
    @FXML private TableColumn<ProductSaleRow, String> productColumn;
    @FXML private TableColumn<ProductSaleRow, String> variantColumn;
    @FXML private TableColumn<ProductSaleRow, Integer> quantityColumn;
    @FXML private TableColumn<ProductSaleRow, String> priceColumn;
    @FXML private TableColumn<ProductSaleRow, String> totalColumn;
    
    // FXML - Búsqueda de ventas
    @FXML private TableView<Sale> salesSearchTable;
    @FXML private TableColumn<Sale, Integer> searchIdColumn;
    @FXML private TableColumn<Sale, String> searchDateColumn;
    @FXML private TableColumn<Sale, String> searchTotalColumn;
    @FXML private TableColumn<Sale, String> searchStatusColumn;
    @FXML private TableColumn<Sale, String> searchSellerColumn;
    @FXML private TableColumn<Sale, Void> searchActionsColumn;
    
    // FXML - Estado vacío
    @FXML private VBox emptyStateContainer;
    
    // Datos de búsqueda
    private ObservableList<Sale> searchResults = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
        AppLogger.info("REPORTES", "ReportsController inicializado");
        
        setupRangeFilters();
        setupProductsTable();
        setupSearchTable();
        
        LOGGER.info("ReportsController inicializado correctamente");
    }
    
    private void setupRangeFilters() {
        // Opciones de tipo de rango
        ObservableList<String> rangeTypes = FXCollections.observableArrayList(
            "Hoy",
            "Esta Semana", 
            "Este Mes",
            "Este Año",
            "Personalizado"
        );
        rangeTypeCombo.setItems(rangeTypes);
        
        // Listener para cambio de tipo de rango
        rangeTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateDatePickersForRange(newVal);
            }
        });
        
        // Configurar DatePickers
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());
        
        // Seleccionar "Este Mes" por defecto
        rangeTypeCombo.getSelectionModel().select(2);
    }
    
    private void setupProductsTable() {
        productColumn.setCellValueFactory(new PropertyValueFactory<>("producto"));
        variantColumn.setCellValueFactory(new PropertyValueFactory<>("variante"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("precioFormatted"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalFormatted"));
        
        // Centrar columna de cantidad
        quantityColumn.setStyle("-fx-alignment: CENTER;");
        
        // Alinear a la derecha precios
        priceColumn.setStyle("-fx-alignment: CENTER_RIGHT;");
        totalColumn.setStyle("-fx-alignment: CENTER_RIGHT;");
        
        // Añadir numeración
        TableColumn<ProductSaleRow, Void> numberCol = new TableColumn<>("#");
        numberCol.setPrefWidth(60);
        numberCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });
        numberCol.setStyle("-fx-alignment: CENTER;");
        productsTable.getColumns().add(0, numberCol);
    }
    
    private void setupSearchTable() {
        searchIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        searchDateColumn.setCellValueFactory(param -> 
            new javafx.beans.property.SimpleStringProperty(dateFormat.format(param.getValue().getCreatedAt())));
        searchTotalColumn.setCellValueFactory(param -> 
            new javafx.beans.property.SimpleStringProperty(currencyFormat.format(param.getValue().getTotal())));
        searchSellerColumn.setCellValueFactory(param -> 
            new javafx.beans.property.SimpleStringProperty(param.getValue().getUserName()));
        
        // Columna de estado
        searchStatusColumn.setCellValueFactory(param -> {
            Sale sale = param.getValue();
            String status = sale.getStatus();
            if ("cancelled".equals(status)) {
                return new javafx.beans.property.SimpleStringProperty("Anulada");
            } else if ("completed".equals(status)) {
                return new javafx.beans.property.SimpleStringProperty("Completada");
            } else {
                return new javafx.beans.property.SimpleStringProperty("Pendiente");
            }
        });
        
        // Columna de acciones
        searchActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button btnVer = new Button("Ver");
            private final Button btnAnular = new Button("Anular");
            private final HBox container = new HBox(5, btnVer, btnAnular);
            
            {
                btnVer.getStyleClass().addAll("action-button", "small");
                btnAnular.getStyleClass().addAll("action-button", "danger", "small");

                btnVer.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    handleViewSearchDetail(sale);
                });

                btnAnular.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    handleCancelSearchSale(sale);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Sale sale = getTableView().getItems().get(getIndex());
                    btnAnular.setDisable("cancelled".equals(sale.getStatus()));
                    setGraphic(container);
                }
            }
        });
        
        salesSearchTable.setItems(searchResults);
    }
    
    /**
     * Actualiza los DatePickers según el tipo de rango seleccionado
     */
    private void updateDatePickersForRange(String rangeType) {
        LocalDate today = LocalDate.now();
        currentRangeType = rangeType;
        
        switch (rangeType) {
            case "Hoy":
                startDate = today;
                endDate = today;
                startDatePicker.setValue(today);
                endDatePicker.setValue(today);
                startDatePicker.setDisable(true);
                endDatePicker.setDisable(true);
                break;
                
            case "Esta Semana":
                startDate = today.minusDays(today.getDayOfWeek().getValue() - 1);
                endDate = startDate.plusDays(6);
                startDatePicker.setValue(startDate);
                endDatePicker.setValue(endDate);
                startDatePicker.setDisable(true);
                endDatePicker.setDisable(true);
                break;
                
            case "Este Mes":
                startDate = today.withDayOfMonth(1);
                endDate = today.withDayOfMonth(today.lengthOfMonth());
                startDatePicker.setValue(startDate);
                endDatePicker.setValue(endDate);
                startDatePicker.setDisable(true);
                endDatePicker.setDisable(true);
                break;
                
            case "Este Año":
                startDate = today.withDayOfYear(1);
                endDate = today.withDayOfYear(today.lengthOfYear());
                startDatePicker.setValue(startDate);
                endDatePicker.setValue(endDate);
                startDatePicker.setDisable(true);
                endDatePicker.setDisable(true);
                break;
                
            case "Personalizado":
                startDatePicker.setDisable(false);
                endDatePicker.setDisable(false);
                // Usar las fechas actuales de los pickers
                startDate = startDatePicker.getValue();
                endDate = endDatePicker.getValue();
                break;
        }
        
        LOGGER.info("Rango actualizado: " + rangeType + " (" + startDate + " a " + endDate + ")");
    }
    
    /**
     * Maneja la generación del reporte
     */
    @FXML
    private void handleGenerateReport() {
        if (!validateFilters()) {
            return;
        }
        
        try {
            // Obtener fechas de los DatePickers (por si es personalizado)
            if ("Personalizado".equals(currentRangeType)) {
                startDate = startDatePicker.getValue();
                endDate = endDatePicker.getValue();
            }
            
            LOGGER.info("Generando reporte para rango: " + startDate + " a " + endDate);
            
            // Mostrar indicador de carga
            showLoadingState();
            
            // Capturar variables finales para el thread
            final LocalDate finalStartDate = startDate;
            final LocalDate finalEndDate = endDate;
            
            // Ejecutar consultas en background
            new Thread(() -> {
                try {
                    var stats = reportDAO.getStatsByDateRange(finalStartDate, finalEndDate);
                    var paymentTotals = reportDAO.getPaymentMethodTotalsByRange(finalStartDate, finalEndDate);
                    var productsSummary = reportDAO.getProductSalesSummaryByRange(finalStartDate, finalEndDate);
                    var dailySales = reportDAO.getDailySalesByRange(finalStartDate, finalEndDate);
                    
                    // Actualizar UI en JavaFX thread
                    javafx.application.Platform.runLater(() -> {
                        updateStatistics(stats);
                        updatePaymentMethods(paymentTotals);
                        updateProductsTable(productsSummary);
                        updateChartByRange(dailySales);
                        showReportSections();
                        enableExportButtons();
                        
                        // Log de reporte generado
                        AppLogger.info("REPORTES", "Reporte generado para " + finalStartDate.getMonth() + " " + finalStartDate.getYear() +
                            " - Ventas: " + stats.get("totalVentas") + ", Total: $" + stats.get("totalRecaudado"));
                    });
                    
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error al generar reporte", e);
                    AppLogger.error("REPORTES", "Error al generar reporte: " + e.getMessage(), e);
                    javafx.application.Platform.runLater(() -> showError("Error al generar el reporte: " + e.getMessage()));
                }
            }).start();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en handleGenerateReport", e);
            showError("Error al procesar los filtros");
        }
    }
    
    /**
     * Valida que se hayan seleccionado las fechas correctamente
     */
    private boolean validateFilters() {
        if (rangeTypeCombo.getValue() == null) {
            showWarning("Por favor selecciona un tipo de rango");
            return false;
        }
        
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showWarning("Por favor selecciona las fechas del rango");
            return false;
        }
        
        if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            showWarning("La fecha inicial no puede ser posterior a la fecha final");
            return false;
        }
        
        return true;
    }
    
    /**
     * Actualiza las estadísticas en la UI
     */
    private void updateStatistics(Map<String, Object> stats) {
        BigDecimal totalSales = (BigDecimal) stats.get("totalRecaudado");
        Integer totalTransactions = (Integer) stats.get("totalVentas");
        BigDecimal avgSale = (BigDecimal) stats.get("promedioVenta");
        BigDecimal maxSale = (BigDecimal) stats.get("ventaMaxima");
        
        totalSalesLabel.setText(currencyFormat.format(totalSales));
        totalTransactionsLabel.setText(String.valueOf(totalTransactions));
        avgSaleLabel.setText(currencyFormat.format(avgSale));
        maxSaleLabel.setText(currencyFormat.format(maxSale));
    }
    
    /**
     * Actualiza los métodos de pago
     */
    private void updatePaymentMethods(Map<String, BigDecimal> paymentTotals) {
        paymentMethodsContainer.getChildren().clear();
        
        for (Map.Entry<String, BigDecimal> entry : paymentTotals.entrySet()) {
            VBox methodBox = new VBox(4);
            methodBox.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-padding: 12; -fx-alignment: center;");
            
            Label methodLabel = new Label(entry.getKey());
            methodLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #374151;");
            
            Label amountLabel = new Label(currencyFormat.format(entry.getValue()));
            amountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
            
            methodBox.getChildren().addAll(methodLabel, amountLabel);
            paymentMethodsContainer.getChildren().add(methodBox);
        }
    }
    
    /**
     * Actualiza la tabla de productos
     */
    private void updateProductsTable(List<Map<String, Object>> summary) {
        ObservableList<ProductSaleRow> data = FXCollections.observableArrayList();
        
        for (Map<String, Object> item : summary) {
            ProductSaleRow row = new ProductSaleRow(
                (String) item.get("producto"),
                (String) item.get("variante"),
                (Integer) item.get("cantidad"),
                (BigDecimal) item.get("precio"),
                (BigDecimal) item.get("total")
            );
            data.add(row);
        }
        
        productsTable.setItems(data);
    }
    
    /**
     * Actualiza el gráfico de barras
     */
    private void updateChartByRange(Map<String, BigDecimal> dailySales) {
        // Simplificado: solo mostrar mensaje de gráfico
        Label chartLabel = new Label("Gráfico de ventas diarias\n(Implementación simplificada)");
        chartLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b; -fx-alignment: center;");
        chartContainer.getChildren().setAll(chartLabel);
    }
    
    /**
     * Muestra las secciones del reporte
     */
    private void showReportSections() {
        emptyStateContainer.setManaged(false);
        emptyStateContainer.setVisible(false);
        
        statsContainer.setManaged(true);
        statsContainer.setVisible(true);
        
        paymentMethodsSection.setManaged(true);
        paymentMethodsSection.setVisible(true);
        
        productsSection.setManaged(true);
        productsSection.setVisible(true);
        
        chartSection.setManaged(true);
        chartSection.setVisible(true);
    }
    
    /**
     * Oculta las secciones y muestra estado vacío
     */
    private void showLoadingState() {
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
    
    /**
     * Habilita botones de exportación
     */
    private void enableExportButtons() {
        exportPdfBtn.setDisable(false);
        exportExcelBtn.setDisable(false);
    }
    
    /**
     * Deshabilita botones de exportación
     */
    private void disableExportButtons() {
        exportPdfBtn.setDisable(true);
        exportExcelBtn.setDisable(true);
    }
    
    // ==================== BÚSQUEDA DE VENTAS ====================
    
    @FXML
    private void handleSearchSales() {
        try {
            String saleIdText = searchSaleIdField.getText().trim();
            LocalDate searchDate = searchDatePicker.getValue();
            String amountText = searchAmountField.getText().trim();
            
            List<Sale> results = new ArrayList<>();
            
            // Buscar por ID
            if (!saleIdText.isEmpty()) {
                try {
                    int saleId = Integer.parseInt(saleIdText);
                    Optional<Sale> sale = saleDAO.findById(saleId);
                    if (sale.isPresent()) {
                        results.add(sale.get());
                    }
                } catch (NumberFormatException e) {
                    showWarning("El ID de venta debe ser un número válido");
                    return;
                }
            }
            
            // Buscar por fecha
            if (searchDate != null) {
                List<Sale> dateResults = saleDAO.findByDate(searchDate);
                if (results.isEmpty()) {
                    results = dateResults;
                } else {
                    // Filtrar resultados existentes por fecha
                    results = results.stream()
                        .filter(s -> s.getCreatedAt().toLocalDate().equals(searchDate))
                        .toList();
                }
            }
            
            // Buscar por monto
            if (!amountText.isEmpty()) {
                try {
                    BigDecimal amount = new BigDecimal(amountText);
                    List<Sale> amountResults = saleDAO.findByAmountRange(amount.subtract(BigDecimal.valueOf(100)), amount.add(BigDecimal.valueOf(100)));
                    if (results.isEmpty()) {
                        results = amountResults;
                    } else {
                        // Filtrar resultados existentes por monto
                        results = results.stream()
                            .filter(s -> Math.abs(s.getTotal().subtract(amount).doubleValue()) <= 100)
                            .toList();
                    }
                } catch (NumberFormatException e) {
                    showWarning("El monto debe ser un número válido");
                    return;
                }
            }
            
            // Si no hay filtros específicos, mostrar todas las ventas
            if (saleIdText.isEmpty() && searchDate == null && amountText.isEmpty()) {
                results = saleDAO.findAll();
            }
            
            searchResults.clear();
            searchResults.addAll(results);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en búsqueda de ventas", e);
            showError("Error al buscar ventas: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleClearSearchFilters() {
        searchSaleIdField.clear();
        searchDatePicker.setValue(null);
        searchAmountField.clear();
        searchResults.clear();
    }
    
    private void handleViewSearchDetail(Sale sale) {
        try {
            Optional<Sale> saleCompleta = saleDAO.findById(sale.getId());
            if (saleCompleta.isEmpty()) {
                showError("No se pudo cargar la venta");
                return;
            }
            
            Sale s = saleCompleta.get();
            StringBuilder sb = new StringBuilder();
            sb.append("Venta #").append(s.getId()).append("\n");
            sb.append("Fecha: ").append(dateFormat.format(s.getCreatedAt())).append("\n");
            sb.append("Vendedor: ").append(s.getUserName()).append("\n");
            sb.append("Estado: ").append("completed".equals(s.getStatus()) ? "Completada" : "cancelled".equals(s.getStatus()) ? "Anulada" : "Pendiente").append("\n\n");
            
            sb.append("--- ITEMS ---\n");
            sb.append("Información de items no disponible en esta versión simplificada\n");
            
            sb.append("\n--- PAGOS ---\n");
            sb.append("Información de pagos no disponible en esta versión simplificada\n");
            
            sb.append("\n").append("TOTAL: ").append(currencyFormat.format(s.getTotal()));
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Detalle de Venta");
            alert.setHeaderText("Venta #" + s.getId());
            alert.setContentText(sb.toString());
            alert.getDialogPane().setMinWidth(400);
            alert.showAndWait();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al ver detalle de venta", e);
            showError("Error al cargar el detalle: " + e.getMessage());
        }
    }
    
    private void handleCancelSearchSale(Sale sale) {
        if ("cancelled".equals(sale.getStatus())) {
            showWarning("Esta venta ya está anulada");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Anulación");
        confirm.setHeaderText("¿Anular venta #" + sale.getId() + "?");
        confirm.setContentText("Esta acción revertirá el stock de los productos. ¿Continuar?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                saleDAO.cancel(sale.getId());
                showSuccess("Venta anulada correctamente");
                handleSearchSales(); // Actualizar resultados
                AppLogger.info("REPORTES", "Venta #" + sale.getId() + " anulada desde reportes");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al anular venta", e);
                showError("No se pudo anular la venta: " + e.getMessage());
            }
        }
    }
    
    // ==================== EXPORTACIÓN ====================
    
    @FXML
    private void handleExportPDF() {
        showWarning("Exportación a PDF no disponible en esta versión simplificada");
    }
    
    @FXML
    private void handleExportExcel() {
        showWarning("Exportación a Excel no disponible en esta versión simplificada");
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
        SessionManager.getInstance().logout();
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
            LOGGER.log(Level.SEVERE, "Error al navegar a " + fxmlPath, e);
            showError("Error al cargar la vista: " + e.getMessage());
        }
    }
    
    // ==================== UTILIDADES UI ====================
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Advertencia");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // ==================== CLASE INTERNA PARA TABLA ====================
    
    /**
     * Representa una fila en la tabla de productos vendidos
     */
    public static class ProductSaleRow {
        private final String producto;
        private final String variante;
        private final Integer cantidad;
        private final BigDecimal precio;
        private final BigDecimal total;
        private final NumberFormat currencyFormat;
        
        public ProductSaleRow(String producto, String variante, Integer cantidad, 
                            BigDecimal precio, BigDecimal total) {
            this.producto = producto;
            this.variante = variante;
            this.cantidad = cantidad;
            this.precio = precio;
            this.total = total;
            this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        }
        
        public String getProducto() { return producto; }
        public String getVariante() { return variante; }
        public Integer getCantidad() { return cantidad; }
        public BigDecimal getPrecio() { return precio; }
        public BigDecimal getTotal() { return total; }
        
        public String getPrecioFormatted() {
            return currencyFormat.format(precio);
        }
        
        public String getTotalFormatted() {
            return currencyFormat.format(total);
        }
    }
}
