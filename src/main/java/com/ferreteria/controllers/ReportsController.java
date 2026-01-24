package com.ferreteria.controllers;

import com.ferreteria.models.Sale;
import com.ferreteria.models.SaleItem;
import com.ferreteria.models.dao.DatabaseConfig;
import com.ferreteria.models.dao.ReportDAO;
import com.ferreteria.models.dao.SaleDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para la pantalla de reportes de ventas.
 * Gestiona filtros, consultas y visualización de datos estadísticos.
 * 
 * @author Sistema Ferretería
 * @version 1.0
 */
public class ReportsController {
    private static final Logger LOGGER = Logger.getLogger(ReportsController.class.getName());
    private final ReportDAO reportDAO;
    private final NumberFormat currencyFormat;
    private YearMonth selectedPeriod;

    // FXML - Filtros
    @FXML private ComboBox<String> monthCombo;
    @FXML private ComboBox<Integer> yearCombo;

    // FXML - Botones
    @FXML private Button exportPdfBtn;
    @FXML private Button exportExcelBtn;

    // FXML - Navbar
    @FXML private NavbarController navbarController;

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

    // FXML - Estado vacío
    @FXML private VBox emptyStateContainer;

    public ReportsController() {
        this.reportDAO = new ReportDAO();
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
    }

    /**
     * Inicializa el controlador después de cargar el FXML
     */
    @FXML
    private void initialize() {
        if (navbarController != null) {
            navbarController.setActiveView("reportes");
        }
        setupFilters();
        setupProductsTable();
        LOGGER.info("ReportsController inicializado correctamente");
    }

    /**
     * Configura los ComboBox de filtros (mes y año)
     */
    private void setupFilters() {
        // Llenar combo de meses
        ObservableList<String> months = FXCollections.observableArrayList();
        for (Month month : Month.values()) {
            String monthName = month.getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            months.add(monthName.substring(0, 1).toUpperCase() + monthName.substring(1));
        }
        monthCombo.setItems(months);
        
        // Seleccionar mes actual por defecto
        int currentMonth = YearMonth.now().getMonthValue();
        monthCombo.getSelectionModel().select(currentMonth - 1);

        // Llenar combo de años (últimos 5 años)
        ObservableList<Integer> years = FXCollections.observableArrayList();
        int currentYear = YearMonth.now().getYear();
        for (int i = 0; i < 5; i++) {
            years.add(currentYear - i);
        }
        yearCombo.setItems(years);
        yearCombo.getSelectionModel().select(0); // Año actual
    }

    /**
     * Configura las columnas de la tabla de productos
     */
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

    /**
     * Maneja la generación del reporte
     */
    @FXML
    private void handleGenerateReport() {
        if (!validateFilters()) {
            return;
        }

        try {
            // Obtener período seleccionado
            int monthIndex = monthCombo.getSelectionModel().getSelectedIndex() + 1;
            int year = yearCombo.getValue();
            selectedPeriod = YearMonth.of(year, monthIndex);

            LOGGER.info("Generando reporte para: " + selectedPeriod);

            // Mostrar indicador de carga
            showLoadingState();

            // Ejecutar consultas en background
            new Thread(() -> {
                try {
                    Map<String, Object> stats = reportDAO.getMonthlyStats(selectedPeriod);
                    Map<String, BigDecimal> paymentTotals = reportDAO.getPaymentMethodTotals(selectedPeriod);
                    List<Map<String, Object>> productsSummary = reportDAO.getProductSalesSummary(selectedPeriod);

                    // Actualizar UI en JavaFX thread
                    Platform.runLater(() -> {
                        updateStatistics(stats);
                        updatePaymentMethods(paymentTotals);
                        updateProductsTable(productsSummary);
                        updateChart(reportDAO.getDailySales(selectedPeriod));
                        showReportSections();
                        enableExportButtons();
                    });

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error al generar reporte", e);
                    Platform.runLater(() -> showError("Error al generar el reporte: " + e.getMessage()));
                }
            }).start();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en handleGenerateReport", e);
            showError("Error al procesar los filtros");
        }
    }

    /**
     * Valida que se hayan seleccionado mes y año
     */
    private boolean validateFilters() {
        if (monthCombo.getValue() == null || yearCombo.getValue() == null) {
            showWarning("Por favor selecciona un mes y un año");
            return false;
        }
        return true;
    }

    /**
     * Actualiza las tarjetas de estadísticas
     */
    private void updateStatistics(Map<String, Object> stats) {
        BigDecimal total = (BigDecimal) stats.get("totalRecaudado");
        int transactions = (Integer) stats.get("totalVentas");
        BigDecimal average = (BigDecimal) stats.get("promedioVenta");
        BigDecimal max = (BigDecimal) stats.get("ventaMaxima");

        totalSalesLabel.setText(currencyFormat.format(total));
        totalTransactionsLabel.setText(String.valueOf(transactions));
        avgSaleLabel.setText(currencyFormat.format(average));
        maxSaleLabel.setText(currencyFormat.format(max));
    }

    /**
     * Actualiza la sección de métodos de pago
     */
    private void updatePaymentMethods(Map<String, BigDecimal> totals) {
        paymentMethodsContainer.getChildren().clear();

        if (totals.isEmpty()) {
            Label noData = new Label("No hay datos de métodos de pago");
            noData.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
            paymentMethodsContainer.getChildren().add(noData);
            return;
        }

        // Crear una card por cada método de pago
        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            VBox card = createPaymentMethodCard(entry.getKey(), entry.getValue());
            paymentMethodsContainer.getChildren().add(card);
        }
    }

    /**
     * Crea una tarjeta visual para un método de pago
     */
    private VBox createPaymentMethodCard(String method, BigDecimal amount) {
        VBox card = new VBox(8);
        card.getStyleClass().add("payment-card");

        // Icono según método
        String icon = getPaymentIcon(method);
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 28px;");

        // Monto
        Label amountLabel = new Label(currencyFormat.format(amount));
        amountLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // Nombre del método
        Label methodLabel = new Label(formatPaymentMethod(method));
        methodLabel.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold;");

        card.getChildren().addAll(iconLabel, amountLabel, methodLabel);
        return card;
    }

    /**
     * Actualiza la tabla de productos vendidos
     */
    private void updateProductsTable(List<Map<String, Object>> summary) {
        ObservableList<ProductSaleRow> rows = FXCollections.observableArrayList();

        for (Map<String, Object> item : summary) {
            ProductSaleRow row = new ProductSaleRow(
                (String) item.get("producto"),
                (String) item.get("variante"),
                (Integer) item.get("cantidad"),
                (BigDecimal) item.get("precio"),
                (BigDecimal) item.get("total")
            );
            rows.add(row);
        }

        productsTable.setItems(rows);
    }

    /**
     * Actualiza el gráfico de ventas por día
     */
    private void updateChart(Map<Integer, BigDecimal> dailySales) {
        chartContainer.getChildren().clear();
        
        if (dailySales == null || dailySales.isEmpty()) {
            VBox emptyChart = new VBox(20);
            emptyChart.setAlignment(javafx.geometry.Pos.CENTER);
            emptyChart.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12; -fx-padding: 60;");
            
            Label icon = new Label("📊");
            icon.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1;");
            
            Label message = new Label("No hay ventas registradas en este mes");
            message.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");
            
            emptyChart.getChildren().addAll(icon, message);
            chartContainer.getChildren().add(emptyChart);
            
            LOGGER.info("No hay datos de ventas para el gráfico");
            return;
        }

        LOGGER.info("Generando gráfico con " + dailySales.size() + " días de datos");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        
        xAxis.setLabel("Día del Mes");
        yAxis.setLabel("Monto (ARS)");
        
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Ventas Diarias - " + 
            selectedPeriod.getMonth().getDisplayName(java.time.format.TextStyle.FULL, new Locale("es", "ES")) + 
            " " + selectedPeriod.getYear());
        barChart.setLegendVisible(false);
        barChart.setAnimated(true);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Ventas");
        
        for (Map.Entry<Integer, BigDecimal> entry : dailySales.entrySet()) {
            String day = String.valueOf(entry.getKey());
            double amount = entry.getValue().doubleValue();
            series.getData().add(new XYChart.Data<>(day, amount));
            LOGGER.info("Día " + day + ": $" + amount);
        }
        
        barChart.getData().add(series);
        barChart.setPrefHeight(350);
        barChart.setStyle("-fx-background-color: transparent;");
        
        chartContainer.getChildren().add(barChart);
    }

    /**
     * Muestra las secciones del reporte y oculta el estado vacío
     */
    private void showReportSections() {
        emptyStateContainer.setVisible(false);
        emptyStateContainer.setManaged(false);

        statsContainer.setVisible(true);
        statsContainer.setManaged(true);

        paymentMethodsSection.setVisible(true);
        paymentMethodsSection.setManaged(true);

        chartSection.setVisible(true);
        chartSection.setManaged(true);

        productsSection.setVisible(true);
        productsSection.setManaged(true);
    }

    /**
     * Muestra estado de carga mientras se generan reportes
     */
    private void showLoadingState() {
        LOGGER.info("Cargando datos del reporte...");
    }

    /**
     * Habilita los botones de exportación
     */
    private void enableExportButtons() {
        exportPdfBtn.setDisable(false);
        exportExcelBtn.setDisable(false);
    }

    /**
     * Obtiene el icono según método de pago
     */
    private String getPaymentIcon(String method) {
        String methodLower = method.toLowerCase();
        switch (methodLower) {
            case "efectivo":
                return "💵";
            case "tarjeta_debito":
                return "💳";
            case "tarjeta_credito":
                return "💳";
            case "transferencia":
                return "🏦";
            default:
                return "💰";
        }
    }

    /**
     * Formatea el nombre del método de pago para mostrar
     */
    private String formatPaymentMethod(String method) {
        String methodLower = method.toLowerCase();
        switch (methodLower) {
            case "efectivo":
                return "Efectivo";
            case "tarjeta_debito":
                return "Tarjeta Débito";
            case "tarjeta_credito":
                return "Tarjeta Crédito";
            case "transferencia":
                return "Transferencia";
            default:
                return method;
        }
    }

    // ==================== MÉTODOS DE EXPORTACIÓN ====================

    @FXML
    private void handleExportPDF() {
        showInfo("Funcionalidad de exportación a PDF en desarrollo");
    }

    @FXML
    private void handleExportExcel() {
        showInfo("Funcionalidad de exportación a Excel en desarrollo");
    }

    // ==================== ANULACIÓN DE VENTAS ====================

    /**
     * Maneja la búsqueda de una venta para anular.
     * Busca por ID de venta.
     */
    @FXML
    private void handleSearchSaleToCancel() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Buscar Venta");
        dialog.setHeaderText("Anular Venta");
        dialog.setContentText("Ingrese el ID de la venta:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                int saleId = Integer.parseInt(result.get().trim());
                showSaleCancellationDialog(saleId);
            } catch (NumberFormatException e) {
                showError("ID inválido. Debe ingresar un número.");
            }
        }
    }

    /**
     * Muestra el diálogo de confirmación para anular una venta.
     * Verifica que la venta exista y no esté anulada antes de mostrar el diálogo.
     * 
     * @param saleId ID de la venta a anular
     */
    private void showSaleCancellationDialog(int saleId) {
        SaleDAO saleDAO = new SaleDAO(DatabaseConfig.getInstance());
        
        // Buscar la venta
        Optional<Sale> saleOpt = saleDAO.findById(saleId);
        
        if (saleOpt.isEmpty()) {
            showError("No se encontró una venta con ID: " + saleId);
            return;
        }
        
        Sale sale = saleOpt.get();
        
        // Verificar que no esté ya anulada
        if (sale.isCancelled()) {
            showWarning("Esta venta ya está anulada.");
            return;
        }
        
        // Construir mensaje de confirmación con detalles
        StringBuilder message = new StringBuilder();
        message.append("DETALLES DE LA VENTA:\n\n");
        message.append("ID: ").append(sale.getId()).append("\n");
        message.append("Fecha: ").append(
            sale.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        ).append("\n");
        message.append("Vendedor: ").append(sale.getUserName() != null ? sale.getUserName() : "N/A").append("\n");
        message.append("Total: ").append(currencyFormat.format(sale.getTotal())).append("\n\n");
        
        message.append("PRODUCTOS:\n");
        for (SaleItem item : sale.getItems()) {
            message.append("• ").append(item.getDisplayName())
                   .append(" x").append(item.getQuantity())
                   .append(" = ").append(currencyFormat.format(item.getSubtotal()))
                   .append("\n");
        }
        
        message.append("\n⚠️ ADVERTENCIA:\n");
        message.append("• El stock de los productos será revertido\n");
        message.append("• Esta acción no se puede deshacer\n");
        message.append("• La venta quedará marcada como ANULADA\n\n");
        message.append("¿Está seguro que desea anular esta venta?");
        
        // Mostrar diálogo de confirmación
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmar Anulación");
        confirmDialog.setHeaderText("Anular Venta #" + saleId);
        confirmDialog.setContentText(message.toString());
        
        // Personalizar botones
        ButtonType btnConfirmar = new ButtonType("Sí, Anular Venta");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(btnConfirmar, btnCancelar);
        
        Optional<ButtonType> confirmation = confirmDialog.showAndWait();
        
        if (confirmation.isPresent() && confirmation.get() == btnConfirmar) {
            cancelSale(saleId);
        }
    }

    /**
     * Anula una venta y revierte el stock de los productos.
     * Ejecuta la operación en una transacción para garantizar consistencia.
     * 
     * @param saleId ID de la venta a anular
     */
    private void cancelSale(int saleId) {
        SaleDAO saleDAO = new SaleDAO(DatabaseConfig.getInstance());
        
        try {
            // Ejecutar anulación (SaleDAO maneja la transacción internamente)
            saleDAO.cancel(saleId);
            
            // Mostrar mensaje de éxito
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Éxito");
            successAlert.setHeaderText("Venta Anulada Correctamente");
            successAlert.setContentText(
                "La venta #" + saleId + " ha sido anulada.\n" +
                "El stock de los productos ha sido revertido."
            );
            successAlert.showAndWait();
            
            LOGGER.info("Venta #" + saleId + " anulada correctamente");
            
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Error al anular venta #" + saleId, e);
            showError("Error al anular la venta:\n" + e.getMessage());
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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
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