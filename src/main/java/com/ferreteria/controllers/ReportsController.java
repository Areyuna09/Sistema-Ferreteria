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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // FXML - Gestión de Ventas (Anulación)
    @FXML private TextField searchSaleIdField;
    @FXML private DatePicker searchDatePicker;
    @FXML private TextField searchAmountField;
    @FXML private TableView<SaleSearchRow> salesSearchTable;
    @FXML private TableColumn<SaleSearchRow, String> searchIdColumn;
    @FXML private TableColumn<SaleSearchRow, String> searchDateColumn;
    @FXML private TableColumn<SaleSearchRow, String> searchTotalColumn;
    @FXML private TableColumn<SaleSearchRow, String> searchStatusColumn;
    @FXML private TableColumn<SaleSearchRow, String> searchSellerColumn;
    @FXML private TableColumn<SaleSearchRow, Void> searchActionsColumn;

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
        setupSalesSearchTable();
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
     * Configura la tabla de búsqueda de ventas para anular/eliminar
     */
    private void setupSalesSearchTable() {
        searchIdColumn.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        searchDateColumn.setCellValueFactory(new PropertyValueFactory<>("dateFormatted"));
        searchTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalFormatted"));
        searchStatusColumn.setCellValueFactory(new PropertyValueFactory<>("statusFormatted"));
        searchSellerColumn.setCellValueFactory(new PropertyValueFactory<>("sellerName"));

        // Estilo condicional para estado
        searchStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Anulada".equals(item)) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Columna de acciones
        searchActionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button btnAnular = new Button("Anular");
            private final Button btnEliminar = new Button("Eliminar");
            private final HBox container = new HBox(8, btnAnular, btnEliminar);

            {
                container.setAlignment(javafx.geometry.Pos.CENTER);
                btnAnular.getStyleClass().addAll("action-button-small", "warning");
                btnEliminar.getStyleClass().addAll("action-button-small", "danger");

                btnAnular.setOnAction(e -> {
                    SaleSearchRow row = getTableView().getItems().get(getIndex());
                    handleCancelSaleFromSearch(row);
                });

                btnEliminar.setOnAction(e -> {
                    SaleSearchRow row = getTableView().getItems().get(getIndex());
                    handleDeleteSalePermanently(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    SaleSearchRow row = getTableView().getItems().get(getIndex());
                    btnAnular.setDisable(row.isCancelled());
                    setGraphic(container);
                }
            }
        });
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
        return switch (methodLower) {
            case "efectivo" -> "💵";
            case "tarjeta_debito", "tarjeta_credito" -> "💳";
            case "transferencia" -> "🏦";
            default -> "💰";
        };
    }

    /**
     * Formatea el nombre del método de pago para mostrar
     */
    private String formatPaymentMethod(String method) {
        String methodLower = method.toLowerCase();
        return switch (methodLower) {
            case "efectivo" -> "Efectivo";
            case "tarjeta_debito" -> "Tarjeta Débito";
            case "tarjeta_credito" -> "Tarjeta Crédito";
            case "transferencia" -> "Transferencia";
            default -> method;
        };
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

    // ==================== ANULACIÓN Y ELIMINACIÓN DE VENTAS ====================

    /**
     * Busca ventas según los filtros aplicados
     */
    @FXML
    private void handleSearchSales() {
        String saleId = searchSaleIdField.getText().trim();
        LocalDate date = searchDatePicker.getValue();
        String amountStr = searchAmountField.getText().trim();

        List<Sale> sales = new ArrayList<>();
        SaleDAO saleDAO = new SaleDAO(DatabaseConfig.getInstance());

        try {
            // Buscar por ID si está presente
            if (!saleId.isEmpty()) {
                try {
                    int id = Integer.parseInt(saleId);
                    Optional<Sale> saleOpt = saleDAO.findById(id);
                    saleOpt.ifPresent(sales::add);
                } catch (NumberFormatException e) {
                    showError("ID de venta inválido");
                    return;
                }
            }
            // Buscar por fecha si está presente
            else if (date != null) {
                sales = saleDAO.findByDate(date);
            }
            // Mostrar todas las ventas recientes
            else {
                sales = saleDAO.findPaginated(50, 0);
            }

            // Filtrar por monto si está presente
            if (!amountStr.isEmpty()) {
                try {
                    BigDecimal amount = new BigDecimal(amountStr);
                    final BigDecimal tolerance = new BigDecimal("10.00");
                    sales = sales.stream()
                        .filter(s -> s.getTotal().subtract(amount).abs().compareTo(tolerance) <= 0)
                        .toList();
                } catch (NumberFormatException e) {
                    showError("Monto inválido");
                    return;
                }
            }

            // Mostrar resultados en la tabla
            displaySalesResults(sales);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error buscando ventas", e);
            showError("Error al buscar ventas: " + e.getMessage());
        }
    }

    /**
     * Muestra los resultados de búsqueda en la tabla
     */
    private void displaySalesResults(List<Sale> sales) {
        ObservableList<SaleSearchRow> rows = FXCollections.observableArrayList();

        for (Sale sale : sales) {
            rows.add(new SaleSearchRow(
                sale.getId(),
                sale.getCreatedAt(),
                sale.getTotal(),
                sale.getStatus(),
                sale.getUserName() != null ? sale.getUserName() : "Usuario"
            ));
        }

        salesSearchTable.setItems(rows);

        if (sales.isEmpty()) {
            showInfo("No se encontraron ventas con los criterios especificados");
        }
    }

    /**
     * Limpia los filtros de búsqueda
     */
    @FXML
    private void handleClearSearchFilters() {
        searchSaleIdField.clear();
        searchDatePicker.setValue(null);
        searchAmountField.clear();
        salesSearchTable.getItems().clear();
    }

    /**
     * Anula una venta desde la tabla de búsqueda
     */
    private void handleCancelSaleFromSearch(SaleSearchRow row) {
        if (row.isCancelled()) {
            showWarning("Esta venta ya está anulada");
            return;
        }

        // Obtener venta completa
        SaleDAO saleDAO = new SaleDAO(DatabaseConfig.getInstance());
        Optional<Sale> saleOpt = saleDAO.findById(row.getSaleId());

        if (saleOpt.isEmpty()) {
            showError("No se encontró la venta");
            return;
        }

        Sale sale = saleOpt.get();

        // Mostrar diálogo de confirmación con detalles
        showSaleCancellationDialog(sale);
    }

    /**
     * Muestra el diálogo de confirmación para anular una venta
     */
    private void showSaleCancellationDialog(Sale sale) {
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
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmar Anulación");
        confirmDialog.setHeaderText("Anular Venta #" + sale.getId());
        confirmDialog.setContentText(message.toString());
        
        ButtonType btnConfirmar = new ButtonType("Sí, Anular Venta");
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(btnConfirmar, btnCancelar);
        
        Optional<ButtonType> confirmation = confirmDialog.showAndWait();
        
        if (confirmation.isPresent() && confirmation.get() == btnConfirmar) {
            cancelSale(sale.getId());
        }
    }

    /**
     * Anula una venta y revierte el stock
     */
    private void cancelSale(int saleId) {
        SaleDAO saleDAO = new SaleDAO(DatabaseConfig.getInstance());
        
        try {
            saleDAO.cancel(saleId);
            
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Éxito");
            successAlert.setHeaderText("Venta Anulada Correctamente");
            successAlert.setContentText(
                "La venta #" + saleId + " ha sido anulada.\n" +
                "El stock de los productos ha sido revertido."
            );
            successAlert.showAndWait();
            
            LOGGER.info("Venta #" + saleId + " anulada correctamente");
            
            // Actualizar tabla de búsqueda
            handleSearchSales();
            
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Error al anular venta #" + saleId, e);
            showError("Error al anular la venta:\n" + e.getMessage());
        }
    }

    /**
     * Elimina definitivamente una venta del historial
     */
    private void handleDeleteSalePermanently(SaleSearchRow row) {
        Alert confirmDialog = new Alert(Alert.AlertType.WARNING);
        confirmDialog.setTitle("⚠️ ELIMINAR DEFINITIVAMENTE");
        confirmDialog.setHeaderText("Eliminar Venta #" + row.getSaleId() + " del Historial");
        
        StringBuilder message = new StringBuilder();
        message.append("🔴 ADVERTENCIA CRÍTICA 🔴\n\n");
        message.append("Esta acción es IRREVERSIBLE y eliminará:\n\n");
        message.append("✗ El registro de la venta\n");
        message.append("✗ Todos los items/productos de la venta\n");
        message.append("✗ Todos los pagos asociados\n");
        message.append("✗ TODO el historial de esta transacción\n\n");
        
        if (!row.isCancelled()) {
            message.append("⚠️ La venta NO está anulada.\n");
            message.append("   Se recomienda anularla primero para revertir el stock.\n\n");
        }
        
        message.append("Esta operación solo debe usarse para:\n");
        message.append("• Ventas de prueba\n");
        message.append("• Datos erróneos que deben ser removidos\n\n");
        message.append("¿Está COMPLETAMENTE SEGURO de eliminar esta venta?");
        
        confirmDialog.setContentText(message.toString());
        
        ButtonType btnEliminar = new ButtonType("SÍ, ELIMINAR DEFINITIVAMENTE", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(btnEliminar, btnCancelar);
        
        // Hacer el botón de eliminar más prominente
        confirmDialog.getDialogPane().lookupButton(btnEliminar).setStyle(
            "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold;"
        );
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        
        if (result.isPresent() && result.get() == btnEliminar) {
            // Segunda confirmación
            Alert secondConfirm = new Alert(Alert.AlertType.CONFIRMATION);
            secondConfirm.setTitle("Confirmación Final");
            secondConfirm.setHeaderText("¿Realmente desea continuar?");
            secondConfirm.setContentText("Esta es su última oportunidad para cancelar.\n\nLa venta será eliminada permanentemente.");
            
            Optional<ButtonType> finalConfirm = secondConfirm.showAndWait();
            
            if (finalConfirm.isPresent() && finalConfirm.get() == ButtonType.OK) {
                deleteSalePermanently(row.getSaleId());
            }
        }
    }

    /**
     * Ejecuta la eliminación definitiva de una venta
     */
    private void deleteSalePermanently(int saleId) {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 1. Eliminar pagos
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM sale_payments WHERE sale_id = ?")) {
                pstmt.setInt(1, saleId);
                pstmt.executeUpdate();
            }

            // 2. Eliminar items
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM sale_items WHERE sale_id = ?")) {
                pstmt.setInt(1, saleId);
                pstmt.executeUpdate();
            }

            // 3. Eliminar venta
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM sales WHERE id = ?")) {
                pstmt.setInt(1, saleId);
                int affected = pstmt.executeUpdate();
                
                if (affected == 0) {
                    throw new SQLException("No se pudo eliminar la venta");
                }
            }

            conn.commit();
            
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Venta Eliminada");
            successAlert.setHeaderText("Eliminación Exitosa");
            successAlert.setContentText(
                "La venta #" + saleId + " ha sido eliminada permanentemente del sistema.\n\n" +
                "Esta operación no se puede deshacer."
            );
            successAlert.showAndWait();
            
            LOGGER.warning("Venta #" + saleId + " eliminada PERMANENTEMENTE del sistema");
            
            // Actualizar tabla de búsqueda
            handleSearchSales();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error en rollback", ex);
                }
            }
            LOGGER.log(Level.SEVERE, "Error al eliminar venta permanentemente", e);
            showError("Error al eliminar la venta:\n" + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error restaurando autocommit", e);
                }
            }
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

    // ==================== CLASES INTERNAS ====================

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

    /**
     * Representa una fila en la tabla de búsqueda de ventas
     */
    public static class SaleSearchRow {
        private final int saleId;
        private final LocalDateTime createdAt;
        private final BigDecimal total;
        private final String status;
        private final String sellerName;
        private final NumberFormat currencyFormat;

        public SaleSearchRow(int saleId, LocalDateTime createdAt, BigDecimal total, 
                           String status, String sellerName) {
            this.saleId = saleId;
            this.createdAt = createdAt;
            this.total = total;
            this.status = status;
            this.sellerName = sellerName;
            this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        }

        public int getSaleId() { return saleId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public BigDecimal getTotal() { return total; }
        public String getStatus() { return status; }
        public String getSellerName() { return sellerName; }
        
        public boolean isCancelled() {
            return "cancelled".equals(status);
        }
        
        public String getDateFormatted() {
            return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
        
        public String getTotalFormatted() {
            return currencyFormat.format(total);
        }
        
        public String getStatusFormatted() {
            return "completed".equals(status) ? "Completada" : "Anulada";
        }
    }
}