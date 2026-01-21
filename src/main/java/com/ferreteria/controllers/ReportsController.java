package com.ferreteria.controllers;

import com.ferreteria.models.Product;
import com.ferreteria.models.dao.DatabaseConfig;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la vista de Reportes.
 */
public class ReportsController {

    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private TableView<ReportItem> reportsTable;
    @FXML private TableColumn<ReportItem, String> descriptionColumn;
    @FXML private TableColumn<ReportItem, String> detailsColumn;
    @FXML private TableColumn<ReportItem, BigDecimal> amountColumn;
    @FXML private TableColumn<ReportItem, String> dateColumn;
    @FXML private Label totalLabel;
    @FXML private Label countLabel;

    @FXML
    public void initialize() {
        setupReportTypes();
        setupTableColumns();
        setupDatePickers();
    }

    private void setupReportTypes() {
        reportTypeComboBox.getItems().addAll(
            "Reporte de Ventas",
            "Reporte de Productos",
            "Reporte de Stock Mínimo",
            "Reporte de Inventario Valorizado"
        );
        reportTypeComboBox.getSelectionModel().selectFirst();
    }

    private void setupTableColumns() {
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Formatear monto
        amountColumn.setCellFactory(column -> new TableCell<ReportItem, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText("$" + amount.toString());
                }
            }
        });
    }

    private void setupDatePickers() {
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());
    }

    @FXML
    public void handleGenerateReport() {
        String reportType = reportTypeComboBox.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showAlert("Error", "Por favor seleccione un rango de fechas válido");
            return;
        }

        if (startDate.isAfter(endDate)) {
            showAlert("Error", "La fecha de inicio no puede ser posterior a la fecha de fin");
            return;
        }

        clearTable();
        
        switch (reportType) {
            case "Reporte de Ventas":
                generateSalesReport(startDate, endDate);
                break;
            case "Reporte de Productos":
                generateProductsReport();
                break;
            case "Reporte de Stock Mínimo":
                generateLowStockReport();
                break;
            case "Reporte de Inventario Valorizado":
                generateInventoryValueReport();
                break;
        }
    }

    private void generateSalesReport(LocalDate startDate, LocalDate endDate) {
        List<ReportItem> reportData = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        
        try {
            var conn = DatabaseConfig.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            
            // Simulación de datos de ventas (en un caso real vendrían de una tabla de ventas)
            String sql = String.format("""
                SELECT '%s' as description, 'Venta de productos' as details, 
                       %.2f as amount, '%s' as date
                UNION ALL
                SELECT '%s' as description, 'Venta multiple' as details, 
                       %.2f as amount, '%s' as date
                """, 
                "Venta #" + (int)(Math.random() * 1000), 
                1500.50 + Math.random() * 500, 
                startDate.plusDays((int)(Math.random() * 30)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                "Venta #" + (int)(Math.random() * 1000), 
                2300.75 + Math.random() * 700, 
                startDate.plusDays((int)(Math.random() * 30)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            );
            
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                ReportItem item = new ReportItem(
                    rs.getString("description"),
                    rs.getString("details"),
                    rs.getBigDecimal("amount"),
                    rs.getString("date")
                );
                reportData.add(item);
                total = total.add(item.getAmount());
            }
            
            reportsTable.getItems().setAll(reportData);
            updateSummary(total, reportData.size());
            
        } catch (Exception e) {
            System.err.println("Error generando reporte de ventas: " + e.getMessage());
            showAlert("Error", "No se pudo generar el reporte de ventas: " + e.getMessage());
        }
    }

    private void generateProductsReport() {
        List<ReportItem> reportData = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        
        try {
            var conn = DatabaseConfig.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            
            ResultSet rs = stmt.executeQuery("""
                SELECT p.name, c.name as category, pv.sale_price as price, pv.stock 
                FROM products p
                LEFT JOIN categories c ON p.category_id = c.id
                LEFT JOIN product_variants pv ON p.id = pv.product_id
                WHERE p.active = 1 AND pv.active = 1 
                ORDER BY c.name, p.name
                """);
            
            while (rs.next()) {
                BigDecimal totalValue = rs.getBigDecimal("price").multiply(BigDecimal.valueOf(rs.getInt("stock")));
                ReportItem item = new ReportItem(
                    rs.getString("name"),
                    rs.getString("category") + " | Stock: " + rs.getInt("stock"),
                    totalValue,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                );
                reportData.add(item);
                total = total.add(totalValue);
            }
            
            reportsTable.getItems().setAll(reportData);
            updateSummary(total, reportData.size());
            
        } catch (Exception e) {
            System.err.println("Error generando reporte de productos: " + e.getMessage());
            showAlert("Error", "No se pudo generar el reporte de productos: " + e.getMessage());
        }
    }

    private void generateLowStockReport() {
        List<ReportItem> reportData = new ArrayList<>();
        
        try {
            var conn = DatabaseConfig.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            
            ResultSet rs = stmt.executeQuery("""
                SELECT p.name, c.name as category, pv.stock, pv.min_stock 
                FROM products p
                JOIN categories c ON p.category_id = c.id
                JOIN product_variants pv ON p.id = pv.product_id
                WHERE p.active = 1 AND pv.active = 1 AND pv.stock <= pv.min_stock 
                ORDER BY pv.stock ASC
                """);
            
            while (rs.next()) {
                ReportItem item = new ReportItem(
                    rs.getString("name"),
                    rs.getString("category") + " | Stock actual: " + rs.getInt("stock") + " | Mínimo: " + rs.getInt("min_stock"),
                    BigDecimal.ZERO,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                );
                reportData.add(item);
            }
            
            reportsTable.getItems().setAll(reportData);
            updateSummary(BigDecimal.ZERO, reportData.size());
            
        } catch (Exception e) {
            System.err.println("Error generando reporte de stock mínimo: " + e.getMessage());
            showAlert("Error", "No se pudo generar el reporte de stock mínimo: " + e.getMessage());
        }
    }

    private void generateInventoryValueReport() {
        List<ReportItem> reportData = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        
        try {
            var conn = DatabaseConfig.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            
            ResultSet rs = stmt.executeQuery("""
                SELECT c.name as category, COUNT(*) as count, SUM(pv.sale_price * pv.stock) as total_value
                FROM products p
                JOIN categories c ON p.category_id = c.id
                JOIN product_variants pv ON p.id = pv.product_id
                WHERE p.active = 1 AND pv.active = 1 
                GROUP BY c.name 
                ORDER BY total_value DESC
                """);
            
            while (rs.next()) {
                BigDecimal categoryValue = rs.getBigDecimal("total_value");
                ReportItem item = new ReportItem(
                    rs.getString("category"),
                    "Total productos: " + rs.getInt("count"),
                    categoryValue,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                );
                reportData.add(item);
                total = total.add(categoryValue);
            }
            
            reportsTable.getItems().setAll(reportData);
            updateSummary(total, reportData.size());
            
        } catch (Exception e) {
            System.err.println("Error generando reporte de inventario valorizado: " + e.getMessage());
            showAlert("Error", "No se pudo generar el reporte de inventario valorizado: " + e.getMessage());
        }
    }

    private void clearTable() {
        reportsTable.getItems().clear();
        totalLabel.setText("Total: $0.00");
        countLabel.setText("Registros: 0");
    }

    private void updateSummary(BigDecimal total, int count) {
        totalLabel.setText("Total: $" + total.toString());
        countLabel.setText("Registros: " + count);
    }

    @FXML
    public void handleExportReport() {
        if (reportsTable.getItems().isEmpty()) {
            showAlert("Información", "No hay datos para exportar. Genere un reporte primero.");
            return;
        }
        
        showAlert("Exportar Reporte", "Función de exportación en desarrollo.\n\nSe exportarán " + 
                  reportsTable.getItems().size() + " registros.");
        // TODO: Implementar exportación a PDF/Excel
    }

    @FXML
    public void handleBack() {
        navigateToDashboard();
    }

    private void navigateToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Dashboard.fxml"));
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            Stage stage = (Stage) reportsTable.getScene().getWindow();
            stage.setTitle("Sistema Ferretería - Dashboard");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Clase interna para representar items del reporte.
     */
    public static class ReportItem {
        private final String description;
        private final String details;
        private final BigDecimal amount;
        private final String date;

        public ReportItem(String description, String details, BigDecimal amount, String date) {
            this.description = description;
            this.details = details;
            this.amount = amount;
            this.date = date;
        }

        public String getDescription() { return description; }
        public String getDetails() { return details; }
        public BigDecimal getAmount() { return amount; }
        public String getDate() { return date; }
    }
}
