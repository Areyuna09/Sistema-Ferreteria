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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;

import com.ferreteria.models.dao.ReportDAO;
import com.ferreteria.utils.PDFExporter;
import com.ferreteria.utils.ExcelExporter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para la pantalla de reportes de ventas.
 * Conecta con ReportDAO para datos reales y exporta a PDF/Excel.
 */
public class ReportsController {
    private static final Logger LOGGER = Logger.getLogger(ReportsController.class.getName());
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

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
    @FXML private TableView<ProductRow> productsTable;
    @FXML private TableColumn<ProductRow, String> productColumn;
    @FXML private TableColumn<ProductRow, String> variantColumn;
    @FXML private TableColumn<ProductRow, Integer> quantityColumn;
    @FXML private TableColumn<ProductRow, String> priceColumn;
    @FXML private TableColumn<ProductRow, String> totalColumn;

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

    // Estado del reporte actual
    private final ReportDAO reportDAO = new ReportDAO();
    private Map<String, Object> currentStats;
    private List<Map<String, Object>> currentProductsSummary;
    private Map<String, BigDecimal> currentPaymentTotals;
    private YearMonth selectedPeriod;
    private LocalDate currentStartDate;
    private LocalDate currentEndDate;
    private boolean reportGenerated = false;

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

        // Actualizar fechas al cambiar tipo de rango
        rangeTypeCombo.setOnAction(e -> updateDateRange());
    }

    private void updateDateRange() {
        String rangeType = rangeTypeCombo.getValue();
        if (rangeType == null) return;

        LocalDate today = LocalDate.now();
        switch (rangeType) {
            case "Hoy":
                startDatePicker.setValue(today);
                endDatePicker.setValue(today);
                break;
            case "Esta Semana":
                startDatePicker.setValue(today.minusDays(today.getDayOfWeek().getValue() - 1));
                endDatePicker.setValue(today);
                break;
            case "Este Mes":
                startDatePicker.setValue(today.withDayOfMonth(1));
                endDatePicker.setValue(today.withDayOfMonth(today.lengthOfMonth()));
                break;
            case "Este Año":
                startDatePicker.setValue(today.withDayOfYear(1));
                endDatePicker.setValue(today.withMonth(12).withDayOfMonth(31));
                break;
            case "Personalizado":
                // Dejar las fechas como están para que el usuario las modifique
                break;
        }
    }

    private void setupProductsTable() {
        productColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProducto()));
        variantColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVariante()));
        quantityColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCantidad()).asObject());
        priceColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPrecioFormatted()));
        totalColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTotalFormatted()));

        // Centrar columna de cantidad
        quantityColumn.setStyle("-fx-alignment: CENTER;");
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
                btnVer.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 11px;");
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

        currentStartDate = startDatePicker.getValue();
        currentEndDate = endDatePicker.getValue();

        if (currentStartDate.isAfter(currentEndDate)) {
            showError("La fecha de inicio no puede ser posterior a la fecha de fin");
            return;
        }

        // Asignar selectedPeriod basado en el rango
        selectedPeriod = YearMonth.from(currentStartDate);

        try {
            // Obtener datos reales del DAO
            currentStats = reportDAO.getStatsByDateRange(currentStartDate, currentEndDate);
            currentProductsSummary = reportDAO.getProductSalesSummaryByRange(currentStartDate, currentEndDate);
            currentPaymentTotals = reportDAO.getPaymentMethodTotalsByRange(currentStartDate, currentEndDate);

            // Actualizar UI
            updateStatistics();
            updatePaymentMethods();
            updateProductsTable();

            reportGenerated = true;
            showReportSections();

            LOGGER.info("Reporte generado para rango: " + currentStartDate + " a " + currentEndDate);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar reporte", e);
            showError("Error al generar el reporte: " + e.getMessage());
        }
    }

    private void updateStatistics() {
        if (currentStats == null) return;

        BigDecimal totalRecaudado = getStatBigDecimal("totalRecaudado");
        Integer totalVentas = getStatInteger("totalVentas");
        BigDecimal promedioVenta = getStatBigDecimal("promedioVenta");
        BigDecimal ventaMaxima = getStatBigDecimal("ventaMaxima");

        totalSalesLabel.setText(CURRENCY_FORMAT.format(totalRecaudado));
        totalTransactionsLabel.setText(String.valueOf(totalVentas));
        avgSaleLabel.setText(CURRENCY_FORMAT.format(promedioVenta));
        maxSaleLabel.setText(CURRENCY_FORMAT.format(ventaMaxima));
    }

    private void updatePaymentMethods() {
        paymentMethodsContainer.getChildren().clear();

        if (currentPaymentTotals == null || currentPaymentTotals.isEmpty()) {
            Label noDataLabel = new Label("Sin datos de métodos de pago");
            noDataLabel.getStyleClass().add("empty-subtext");
            paymentMethodsContainer.getChildren().add(noDataLabel);
            return;
        }

        for (Map.Entry<String, BigDecimal> entry : currentPaymentTotals.entrySet()) {
            String methodName = formatPaymentMethodName(entry.getKey());
            BigDecimal amount = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;

            VBox card = new VBox(4);
            card.getStyleClass().add("stat-card");

            Label nameLabel = new Label(methodName);
            nameLabel.getStyleClass().add("card-label");

            Label amountLabel = new Label(CURRENCY_FORMAT.format(amount));
            amountLabel.getStyleClass().add("card-value");

            card.getChildren().addAll(nameLabel, amountLabel);
            paymentMethodsContainer.getChildren().add(card);
        }
    }

    private void updateProductsTable() {
        ObservableList<ProductRow> rows = FXCollections.observableArrayList();

        if (currentProductsSummary != null) {
            for (Map<String, Object> data : currentProductsSummary) {
                String producto = data.get("producto") != null ? data.get("producto").toString() : "N/A";
                String variante = data.get("variante") != null ? data.get("variante").toString() : "N/A";
                int cantidad = data.get("cantidad") != null ? ((Number) data.get("cantidad")).intValue() : 0;
                BigDecimal precio = data.get("precio") instanceof BigDecimal
                        ? (BigDecimal) data.get("precio") : BigDecimal.ZERO;
                BigDecimal total = data.get("total") instanceof BigDecimal
                        ? (BigDecimal) data.get("total") : BigDecimal.ZERO;

                rows.add(new ProductRow(producto, variante, cantidad, precio, total));
            }
        }

        productsTable.setItems(rows);
    }

    private boolean validateReportData() {
        if (!reportGenerated) {
            showError("Por favor genera un reporte antes de exportar");
            return false;
        }
        // Allow export if we have statistics, even without product data
        if (currentStats == null || currentStats.isEmpty()) {
            showError("No hay datos de estadísticas para exportar. Genera un reporte primero.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleExportPDF() {
        if (!validateReportData()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte PDF");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));
        fileChooser.setInitialFileName("reporte_ventas_" + currentStartDate + "_" + currentEndDate + ".pdf");

        Stage stage = (Stage) rangeTypeCombo.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file == null) return;

        try {
            List<PDFExporter.ReportRow> pdfRows = convertToPDFReportRows();
            Map<String, BigDecimal> paymentTotals = currentPaymentTotals != null
                    ? currentPaymentTotals : Collections.emptyMap();

            PDFExporter.generateReportPDF(
                pdfRows,
                selectedPeriod,
                paymentTotals,
                currentStats,
                file
            );

            LOGGER.info("PDF exportado: " + file.getAbsolutePath());
            openFile(file);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al exportar PDF", e);
            showError("Error al exportar PDF: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportExcel() {
        if (!validateReportData()) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx"));
        fileChooser.setInitialFileName("reporte_ventas_" + currentStartDate + "_" + currentEndDate + ".xlsx");

        Stage stage = (Stage) rangeTypeCombo.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file == null) return;

        try {
            List<ExcelExporter.ReportRow> excelRows = convertToExcelReportRows();
            Map<String, BigDecimal> paymentTotals = currentPaymentTotals != null
                    ? currentPaymentTotals : Collections.emptyMap();

            ExcelExporter.generateReportExcel(
                excelRows,
                selectedPeriod,
                paymentTotals,
                currentStats,
                file
            );

            LOGGER.info("Excel exportado: " + file.getAbsolutePath());
            openFile(file);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al exportar Excel", e);
            showError("Error al exportar Excel: " + e.getMessage());
        }
    }

    private List<PDFExporter.ReportRow> convertToPDFReportRows() {
        List<PDFExporter.ReportRow> rows = new ArrayList<>();
        if (currentProductsSummary == null) return rows;

        for (Map<String, Object> data : currentProductsSummary) {
            String producto = data.get("producto") != null ? data.get("producto").toString() : "N/A";
            String variante = data.get("variante") != null ? data.get("variante").toString() : "N/A";
            int cantidad = data.get("cantidad") != null ? ((Number) data.get("cantidad")).intValue() : 0;
            BigDecimal precio = data.get("precio") instanceof BigDecimal
                    ? (BigDecimal) data.get("precio") : BigDecimal.ZERO;
            BigDecimal total = data.get("total") instanceof BigDecimal
                    ? (BigDecimal) data.get("total") : BigDecimal.ZERO;

            rows.add(new PDFExporter.ReportRow(producto, variante, cantidad, precio, total));
        }
        return rows;
    }

    private List<ExcelExporter.ReportRow> convertToExcelReportRows() {
        List<ExcelExporter.ReportRow> rows = new ArrayList<>();
        if (currentProductsSummary == null) return rows;

        for (Map<String, Object> data : currentProductsSummary) {
            String producto = data.get("producto") != null ? data.get("producto").toString() : "N/A";
            String variante = data.get("variante") != null ? data.get("variante").toString() : "N/A";
            int cantidad = data.get("cantidad") != null ? ((Number) data.get("cantidad")).intValue() : 0;
            BigDecimal precio = data.get("precio") instanceof BigDecimal
                    ? (BigDecimal) data.get("precio") : BigDecimal.ZERO;
            BigDecimal total = data.get("total") instanceof BigDecimal
                    ? (BigDecimal) data.get("total") : BigDecimal.ZERO;

            rows.add(new ExcelExporter.ReportRow(producto, variante, cantidad, precio, total));
        }
        return rows;
    }

    // ==================== HELPERS ====================

    private BigDecimal getStatBigDecimal(String key) {
        if (currentStats == null) return BigDecimal.ZERO;
        Object val = currentStats.get(key);
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        return BigDecimal.ZERO;
    }

    private Integer getStatInteger(String key) {
        if (currentStats == null) return 0;
        Object val = currentStats.get(key);
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }

    private String formatPaymentMethodName(String method) {
        if (method == null) return "Desconocido";
        switch (method.toLowerCase()) {
            case "efectivo": return "Efectivo";
            case "tarjeta_debito": return "Tarjeta Débito";
            case "tarjeta_credito": return "Tarjeta Crédito";
            case "transferencia": return "Transferencia";
            default: return method;
        }
    }

    // ==================== BÚSQUEDA DE VENTAS ====================

    @FXML
    private void handleSearchSales() {
        salesSearchTable.setItems(FXCollections.observableArrayList());
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
            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();

            Scene scene = new Scene(root, currentWidth, currentHeight);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            stage.setTitle(title);

            if (fxmlPath.contains("Login")) {
                stage.setMaximized(false);
                stage.setResizable(false);
                stage.setScene(scene);
                stage.setWidth(1100);
                stage.setHeight(650);
                stage.centerOnScreen();
            } else {
                stage.setResizable(true);
                stage.setScene(scene);
                if (wasMaximized) {
                    stage.setMaximized(true);
                }
            }

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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openFile(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                showInfo("Archivo exportado en:\n" + file.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo abrir el archivo automáticamente", e);
            showInfo("Archivo exportado en:\n" + file.getAbsolutePath());
        }
    }

    // ==================== CLASE INTERNA PARA TABLA ====================

    public static class ProductRow {
        private final String producto;
        private final String variante;
        private final int cantidad;
        private final BigDecimal precio;
        private final BigDecimal total;

        public ProductRow(String producto, String variante, int cantidad, BigDecimal precio, BigDecimal total) {
            this.producto = producto;
            this.variante = variante;
            this.cantidad = cantidad;
            this.precio = precio != null ? precio : BigDecimal.ZERO;
            this.total = total != null ? total : BigDecimal.ZERO;
        }

        public String getProducto() { return producto != null ? producto : "N/A"; }
        public String getVariante() { return variante != null ? variante : "N/A"; }
        public int getCantidad() { return cantidad; }
        public BigDecimal getPrecio() { return precio; }
        public BigDecimal getTotal() { return total; }
        public String getPrecioFormatted() {
            return NumberFormat.getCurrencyInstance(new Locale("es", "AR")).format(precio);
        }
        public String getTotalFormatted() {
            return NumberFormat.getCurrencyInstance(new Locale("es", "AR")).format(total);
        }
    }
}
