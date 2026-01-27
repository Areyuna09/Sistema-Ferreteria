package com.ferreteria.controllers;

import com.ferreteria.models.dao.ReportDAO;
import com.ferreteria.utils.SessionManager;
import com.ferreteria.utils.PDFExporter;
import com.ferreteria.utils.ExcelExporter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

    // Datos del reporte actual (para exportación)
    private Map<String, Object> currentStatistics;
    private Map<String, BigDecimal> currentPaymentTotals;
    private List<Map<String, Object>> currentProductsSummary;

    // FXML - Filtros
    @FXML private ComboBox<String> monthCombo;
    @FXML private ComboBox<Integer> yearCombo;

    // FXML - Botones
    @FXML private Button exportPdfBtn;
    @FXML private Button exportExcelBtn;

    // FXML - Navbar
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;

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
        setupUserInfo();
        setupFilters();
        setupProductsTable();
        LOGGER.info("ReportsController inicializado correctamente");
    }

    /**
     * Configura la información del usuario en la navbar
     */
    private void setupUserInfo() {
        var currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText(currentUser.getFullName());
            roleLabel.setText(currentUser.getRole().getValue());
        }
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
                        // Guardar datos para exportación
                        currentStatistics = stats;
                        currentPaymentTotals = paymentTotals;
                        currentProductsSummary = productsSummary;

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
            
            Label icon = new Label("Sin Datos");
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
        
        xAxis.setLabel("Dia del Mes");
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
            LOGGER.info("Dia " + day + ": $" + amount);
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
        // Aquí podrías mostrar un spinner o mensaje de "Cargando..."
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
                return "EFEC";
            case "tarjeta_debito":
                return "T-DEB";
            case "tarjeta_credito":
                return "T-CRE";
            case "transferencia":
                return "TRANS";
            default:
                return "PAGO";
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
                return "Tarjeta Debito";
            case "tarjeta_credito":
                return "Tarjeta Credito";
            case "transferencia":
                return "Transferencia";
            default:
                return method;
        }
    }

    // ==================== MÉTODOS DE EXPORTACIÓN ====================

    /**
     * Maneja la exportación del reporte a PDF.
     */
    @FXML
    private void handleExportPDF() {
        if (!validateReportData()) {
            showWarning("Por favor genera un reporte antes de exportar");
            return;
        }

        try {
            // Configurar FileChooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar Reporte PDF");
            fileChooser.setInitialFileName(generateFileName("pdf"));

            // Configurar carpeta por defecto
            File defaultDirectory = new File(System.getProperty("user.home"), "Reportes");
            if (!defaultDirectory.exists()) {
                defaultDirectory.mkdirs();
            }
            fileChooser.setInitialDirectory(defaultDirectory);

            // Filtro de extensión
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Archivos PDF (*.pdf)", "*.pdf");
            fileChooser.getExtensionFilters().add(extFilter);

            // Mostrar diálogo de guardado
            File file = fileChooser.showSaveDialog(exportPdfBtn.getScene().getWindow());

            if (file != null) {
                // Asegurar extensión .pdf
                if (!file.getName().toLowerCase().endsWith(".pdf")) {
                    file = new File(file.getAbsolutePath() + ".pdf");
                }

                // Si el archivo existe, intentar eliminarlo primero
                if (file.exists()) {
                    if (!file.delete()) {
                        showError("No se puede sobrescribir el archivo.\nPuede estar abierto en otra aplicación.\nCierre el archivo e intente de nuevo.");
                        return;
                    }
                }

                // Convertir datos a formato del exportador
                List<PDFExporter.ReportRow> reportRows = convertToReportRows(currentProductsSummary);

                // Generar PDF
                final File finalFile = file;
                new Thread(() -> {
                    try {
                        PDFExporter.generateReportPDF(
                            reportRows,
                            selectedPeriod,
                            currentPaymentTotals,
                            currentStatistics,
                            finalFile
                        );

                        Platform.runLater(() -> {
                            showSuccess("Reporte exportado exitosamente a:\n" + finalFile.getAbsolutePath());
                            openFile(finalFile);
                        });

                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error al exportar PDF", e);
                        Platform.runLater(() -> showError("Error al exportar PDF: " + e.getMessage()));
                    }
                }).start();
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en handleExportPDF", e);
            showError("Error al configurar la exportación: " + e.getMessage());
        }
    }

    /**
     * Maneja la exportación del reporte a Excel.
     */
    @FXML
    private void handleExportExcel() {
        if (!validateReportData()) {
            showWarning("Por favor genera un reporte antes de exportar");
            return;
        }

        try {
            // Configurar FileChooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Guardar Reporte Excel");
            fileChooser.setInitialFileName(generateFileName("xlsx"));

            // Configurar carpeta por defecto
            File defaultDirectory = new File(System.getProperty("user.home"), "Reportes");
            if (!defaultDirectory.exists()) {
                defaultDirectory.mkdirs();
            }
            fileChooser.setInitialDirectory(defaultDirectory);

            // Filtro de extensión
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Archivos Excel (*.xlsx)", "*.xlsx");
            fileChooser.getExtensionFilters().add(extFilter);

            // Mostrar diálogo de guardado
            File file = fileChooser.showSaveDialog(exportExcelBtn.getScene().getWindow());

            if (file != null) {
                // Asegurar extensión .xlsx
                if (!file.getName().toLowerCase().endsWith(".xlsx")) {
                    file = new File(file.getAbsolutePath() + ".xlsx");
                }

                // Si el archivo existe, intentar eliminarlo primero
                if (file.exists()) {
                    if (!file.delete()) {
                        showError("No se puede sobrescribir el archivo.\nPuede estar abierto en otra aplicación.\nCierre el archivo e intente de nuevo.");
                        return;
                    }
                }

                // Convertir datos a formato del exportador
                List<ExcelExporter.ReportRow> reportRows = convertToExcelReportRows(currentProductsSummary);

                // Generar Excel
                final File finalFile = file;
                new Thread(() -> {
                    try {
                        ExcelExporter.generateReportExcel(
                            reportRows,
                            selectedPeriod,
                            currentPaymentTotals,
                            currentStatistics,
                            finalFile
                        );

                        Platform.runLater(() -> {
                            showSuccess("Reporte exportado exitosamente a:\n" + finalFile.getAbsolutePath());
                            openFile(finalFile);
                        });

                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error al exportar Excel", e);
                        Platform.runLater(() -> showError("Error al exportar Excel: " + e.getMessage()));
                    }
                }).start();
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en handleExportExcel", e);
            showError("Error al configurar la exportación: " + e.getMessage());
        }
    }

    /**
     * Valida que existan datos de reporte para exportar.
     */
    private boolean validateReportData() {
        return currentStatistics != null &&
               currentPaymentTotals != null &&
               currentProductsSummary != null &&
               !currentProductsSummary.isEmpty();
    }

    /**
     * Genera un nombre de archivo sugerido para la exportación.
     */
    private String generateFileName(String extension) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String date = dateFormat.format(new Date());
        return "reporte_ventas_" + date + "." + extension;
    }

    /**
     * Convierte los datos del reporte al formato ReportRow de PDFExporter.
     */
    private List<PDFExporter.ReportRow> convertToReportRows(List<Map<String, Object>> summary) {
        return summary.stream()
            .map(item -> new PDFExporter.ReportRow(
                (String) item.get("producto"),
                (String) item.get("variante"),
                (Integer) item.get("cantidad"),
                (BigDecimal) item.get("precio"),
                (BigDecimal) item.get("total")
            ))
            .collect(Collectors.toList());
    }

    /**
     * Convierte los datos del reporte al formato ReportRow de ExcelExporter.
     */
    private List<ExcelExporter.ReportRow> convertToExcelReportRows(List<Map<String, Object>> summary) {
        return summary.stream()
            .map(item -> new ExcelExporter.ReportRow(
                (String) item.get("producto"),
                (String) item.get("variante"),
                (Integer) item.get("cantidad"),
                (BigDecimal) item.get("precio"),
                (BigDecimal) item.get("total")
            ))
            .collect(Collectors.toList());
    }

    /**
     * Abre un archivo con la aplicación predeterminada del sistema.
     */
    private void openFile(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (file.exists()) {
                    desktop.open(file);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo abrir el archivo automáticamente", e);
            // No mostramos error al usuario, solo registramos el log
        }
    }

    // ==================== NAVEGACIÓN ====================

    @FXML
    private void handleDashboard() {
        navigateTo("/views/Dashboard.fxml", "Sistema Ferretería - Dashboard");
    }

    @FXML
    private void handleProducts() {
        showInfo("Módulo de Productos en desarrollo");
    }

    @FXML
    private void handleCategories() {
        navigateTo("/views/Categories.fxml", "Sistema Ferretería - Categorías");
    }

    @FXML
    private void handleSales() {
        showInfo("Módulo de Ventas en desarrollo");
    }

    @FXML
    private void handleUsers() {
        showInfo("Módulo de Usuarios en desarrollo");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        navigateTo("/views/Login.fxml", "Sistema Ferretería - Login");
    }

    /**
     * Navega a una vista específica
     *
     * @param fxmlPath Ruta del archivo FXML
     * @param title Título de la ventana
     */
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) monthCombo.getScene().getWindow();

            // Guardar estado actual de la ventana
            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();

            Scene scene = new Scene(root, currentWidth, currentHeight);

            // Cargar estilos base
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            // Cargar estilos adicionales según la vista
            if (fxmlPath.contains("Reports")) {
                scene.getStylesheets().add(getClass().getResource("/styles/reports.css").toExternalForm());
            }

            stage.setTitle(title);

            // Ajustar ventana según destino
            if (fxmlPath.contains("Login")) {
                stage.setMaximized(false);
                stage.setResizable(false);
                stage.setScene(scene);
                stage.setWidth(900);
                stage.setHeight(600);
                stage.centerOnScreen();
            } else {
                stage.setResizable(true);
                stage.setScene(scene);
                // Restaurar estado maximizado si estaba maximizado
                if (wasMaximized) {
                    stage.setMaximized(true);
                }
            }

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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
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