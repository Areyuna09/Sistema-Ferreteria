package com.ferreteria.controllers;

import com.ferreteria.models.dao.DatabaseConfig;

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
import javafx.stage.Stage;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controlador para la gestión de Ventas.
 */
public class SalesController {

    @FXML private TableView<VentaRow> salesTable;
    @FXML private TableColumn<VentaRow, Integer> idColumn;
    @FXML private TableColumn<VentaRow, String> dateColumn;
    @FXML private TableColumn<VentaRow, String> customerColumn;
    @FXML private TableColumn<VentaRow, BigDecimal> totalColumn;
    @FXML private TableColumn<VentaRow, String> paymentColumn;
    @FXML private TableColumn<VentaRow, String> statusColumn;
    @FXML private TableColumn<VentaRow, Void> actionsColumn;
    @FXML private Label totalSalesLabel;
    @FXML private Label salesCountLabel;
    @FXML private TextField searchField;

    private ObservableList<VentaRow> salesData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadSales();
        setupSearch();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        customerColumn.setCellValueFactory(new PropertyValueFactory<>("customer"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        paymentColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Formatear columna de total
        totalColumn.setCellFactory(column -> new TableCell<VentaRow, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null) {
                    setText(null);
                } else {
                    setText("$" + total.toString());
                }
            }
        });

        // Columna de acciones
        actionsColumn.setCellFactory(createActionsCellFactory());
    }

    private Callback<TableColumn<VentaRow, Void>, TableCell<VentaRow, Void>> createActionsCellFactory() {
        return param -> new TableCell<>() {
            private final Button viewButton = new Button("Ver");
            private final Button deleteButton = new Button("Eliminar");
            private final HBox buttonsContainer = new HBox(5, viewButton, deleteButton);

            {
                viewButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 11px;");
                deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 11px;");

                viewButton.setOnAction(event -> {
                    VentaRow venta = getTableView().getItems().get(getIndex());
                    handleViewSale(venta);
                });

                deleteButton.setOnAction(event -> {
                    VentaRow venta = getTableView().getItems().get(getIndex());
                    handleDeleteSale(venta);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonsContainer);
                }
            }
        };
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterSales(newVal);
        });
    }

    private void filterSales(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            salesTable.setItems(salesData);
            return;
        }

        String lowerSearchText = searchText.toLowerCase().trim();
        ObservableList<VentaRow> filteredSales = FXCollections.observableArrayList();

        for (VentaRow venta : salesData) {
            boolean matchId = String.valueOf(venta.getId()).contains(lowerSearchText);
            boolean matchCustomer = venta.getCustomer().toLowerCase().contains(lowerSearchText);
            boolean matchPayment = venta.getPaymentMethod().toLowerCase().contains(lowerSearchText);

            if (matchId || matchCustomer || matchPayment) {
                filteredSales.add(venta);
            }
        }

        salesTable.setItems(filteredSales);
    }

    private void loadSales() {
        salesData.clear();
        List<VentaRow> ventas = new ArrayList<>();
        BigDecimal totalVentas = BigDecimal.ZERO;

        try {
            var conn = DatabaseConfig.getInstance().getConnection();
            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery("""
                SELECT s.id, s.created_at, COALESCE(s.customer_name, 'Cliente General') as customer,
                       s.total, s.payment_method, s.status
                FROM sales s
                WHERE s.active = 1
                ORDER BY s.created_at DESC
                """);

            while (rs.next()) {
                    VentaRow venta = new VentaRow(
                        rs.getInt("id"),
                        rs.getString("created_at"),
                        rs.getString("customer"),
                        rs.getBigDecimal("total"),
                        rs.getString("payment_method"),
                        rs.getString("status")
                    );
                    ventas.add(venta);
                    totalVentas = totalVentas.add(venta.getTotal());
                }

            } catch (Exception e) {
                System.err.println("Error cargando ventas: " + e.getMessage());
            }

        salesData.addAll(ventas);
        salesTable.setItems(salesData);

        // Actualizar estadísticas
        totalSalesLabel.setText("$" + totalVentas.toString());
        salesCountLabel.setText(String.valueOf(ventas.size()));
    }

    @FXML
    public void handleNewSale() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Sales.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) salesTable.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle("Nueva Venta");
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (Exception e) {
            showAlert("Error", "No se pudo abrir el formulario de nueva venta: " + e.getMessage());
        }
    }

    @FXML
    public void handleRefresh() {
        loadSales();
    }

    private void handleViewSale(VentaRow venta) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalles de Venta");
        alert.setHeaderText(null);
        alert.setContentText(
            "ID: " + venta.getId() + "\n" +
            "Fecha: " + venta.getDate() + "\n" +
            "Cliente: " + venta.getCustomer() + "\n" +
            "Total: $" + venta.getTotal() + "\n" +
            "Método de Pago: " + venta.getPaymentMethod() + "\n" +
            "Estado: " + venta.getStatus()
        );
        alert.showAndWait();
    }

    private void handleDeleteSale(VentaRow venta) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmar Eliminación");
        confirmDialog.setHeaderText("¿Está seguro de eliminar esta venta?");
        confirmDialog.setContentText("Venta ID: " + venta.getId() + "\nCliente: " + venta.getCustomer() + "\n\nEsta acción no se puede deshacer.");

        if (confirmDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                var conn = DatabaseConfig.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                
                String sql = "UPDATE sales SET active = 0 WHERE id = " + venta.getId();
                int rowsAffected = stmt.executeUpdate(sql);
                
                if (rowsAffected > 0) {
                    showAlert("Éxito", "Venta eliminada correctamente");
                    loadSales();
                } else {
                    showAlert("Error", "No se pudo eliminar la venta");
                }
                
            } catch (Exception e) {
                showAlert("Error", "No se pudo eliminar la venta: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Dashboard.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) salesTable.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle("Sistema Ferreteria - Dashboard");
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("Error al navegar al Dashboard: " + e.getMessage());
        }
    }

    @FXML
    public void handleProducts() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Products.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) salesTable.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle("Sistema Ferreteria - Productos");
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("Error al navegar a Productos: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Clase interna para representar una venta en la tabla
    public static class VentaRow {
        private final Integer id;
        private final String date;
        private final String customer;
        private final BigDecimal total;
        private final String paymentMethod;
        private final String status;

        public VentaRow(Integer id, String date, String customer, BigDecimal total, String paymentMethod, String status) {
            this.id = id;
            this.date = date;
            this.customer = customer;
            this.total = total;
            this.paymentMethod = paymentMethod;
            this.status = status;
        }

        public Integer getId() { return id; }
        public String getDate() { return date; }
        public String getCustomer() { return customer; }
        public BigDecimal getTotal() { return total; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getStatus() { return status; }
    }
}
