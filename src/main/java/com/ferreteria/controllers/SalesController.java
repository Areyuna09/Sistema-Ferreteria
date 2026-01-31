package com.ferreteria.controllers;

import com.ferreteria.models.Sale;
import com.ferreteria.models.dao.SaleDAO;
import com.ferreteria.utils.SessionManager;
import com.ferreteria.utils.AppLogger;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para la gestión de ventas.
 * Implementado desde cero basado en la estructura de Alan.
 */
public class SalesController {
    
    private static final Logger LOGGER = Logger.getLogger(SalesController.class.getName());
    private static final int ITEMS_POR_PAGINA = 20;
    
    private final SaleDAO saleDAO = new SaleDAO();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
    
    // Datos
    private ObservableList<Sale> salesList = FXCollections.observableArrayList();
    private int paginaActual = 0;
    private int totalPaginas = 1;
    
    // FXML - Estadísticas
    @FXML private Label ventasHoyLabel;
    @FXML private Label ventasMesLabel;
    @FXML private Label cantidadHoyLabel;
    @FXML private Label promedioLabel;
    
    // FXML - Filtros
    @FXML private DatePicker fechaDesde;
    @FXML private DatePicker fechaHasta;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField searchField;
    
    // FXML - Tabla
    @FXML private TableView<Sale> ventasTable;
    @FXML private TableColumn<Sale, Integer> colId;
    @FXML private TableColumn<Sale, String> colFecha;
    @FXML private TableColumn<Sale, String> colProductos;
    @FXML private TableColumn<Sale, String> colVendedor;
    @FXML private TableColumn<Sale, String> colTotal;
    @FXML private TableColumn<Sale, String> colEstado;
    @FXML private TableColumn<Sale, Void> colAcciones;
    
    // FXML - Paginación
    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Label paginaLabel;
    
    @FXML
    public void initialize() {
        AppLogger.info("VENTAS", "SalesController inicializado");
        
        setupFilters();
        setupSalesTable();
        setupPagination();
        
        loadStats();
        loadSales();
    }
    
    private void setupFilters() {
        // Configurar opciones del filtro de estado
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
            "Todos", "Completadas", "Anuladas"
        );
        statusFilter.setItems(statusOptions);
        statusFilter.setValue("Todos");
    }
    
    private void setupSalesTable() {
        // Configurar columnas
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFecha.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        colVendedor.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getUserName()));
        colTotal.setCellValueFactory(param -> 
            new SimpleStringProperty(currencyFormat.format(param.getValue().getTotal())));
        
        // Columna de productos (simplificada)
        colProductos.setCellValueFactory(param -> {
            Sale sale = param.getValue();
            // Simplificado: solo mostrar información básica
            return new SimpleStringProperty("Venta #" + sale.getId());
        });
        
        // Columna de estado
        colEstado.setCellValueFactory(param -> {
            Sale sale = param.getValue();
            String status = sale.getStatus();
            if ("cancelled".equals(status)) {
                return new SimpleStringProperty("Anulada");
            } else if ("completed".equals(status)) {
                return new SimpleStringProperty("Completada");
            } else {
                return new SimpleStringProperty("Pendiente");
            }
        });
        
        // Columna de acciones
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnVer = new Button("Ver");
            private final Button btnAnular = new Button("Anular");
            private final HBox container = new HBox(5, btnVer, btnAnular);
            
            {
                btnVer.getStyleClass().addAll("action-button", "small");
                btnAnular.getStyleClass().addAll("action-button", "danger", "small");

                btnVer.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    handleViewDetail(sale);
                });

                btnAnular.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    handleCancelSale(sale);
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
        
        ventasTable.setItems(salesList);
    }
    
    private void setupPagination() {
        updatePagination();
    }
    
    private void loadStats() {
        try {
            LocalDate hoy = LocalDate.now();
            int year = hoy.getYear();
            int month = hoy.getMonthValue();

            BigDecimal totalHoy = saleDAO.dailyTotal(hoy);
            BigDecimal totalMes = saleDAO.monthlyTotal(year, month);
            int cantidadHoy = saleDAO.dailyCount(hoy);

            ventasHoyLabel.setText(currencyFormat.format(totalHoy));
            ventasMesLabel.setText(currencyFormat.format(totalMes));
            cantidadHoyLabel.setText(String.valueOf(cantidadHoy));

            if (cantidadHoy > 0) {
                BigDecimal promedio = totalHoy.divide(BigDecimal.valueOf(cantidadHoy), 2, RoundingMode.HALF_UP);
                promedioLabel.setText(currencyFormat.format(promedio));
            } else {
                promedioLabel.setText(currencyFormat.format(BigDecimal.ZERO));
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cargando estadísticas", e);
            AppLogger.error("VENTAS", "Error al cargar estadísticas: " + e.getMessage(), e);
        }
    }
    
    private void loadSales() {
        try {
            List<Sale> sales;

            // Aplicar filtros
            String status = statusFilter.getValue();
            LocalDate desde = fechaDesde.getValue();
            LocalDate hasta = fechaHasta.getValue();
            String busquedaProducto = searchField.getText().trim().toLowerCase();

            if (desde != null && hasta != null) {
                sales = saleDAO.findByDateRange(desde, hasta);
            } else if ("Completadas".equals(status)) {
                sales = saleDAO.findCompleted();
            } else if ("Anuladas".equals(status)) {
                sales = saleDAO.findCancelled();
            } else {
                sales = saleDAO.findAll();
            }

            // Filtrar por status si hay rango de fechas
            if (desde != null && hasta != null && !"Todos".equals(status)) {
                String statusVal = "Completadas".equals(status) ? "completed" : "cancelled";
                sales = sales.stream()
                    .filter(s -> statusVal.equals(s.getStatus()))
                    .toList();
            }

            // Simplificado: no filtrar por productos ya que no tenemos acceso a los items

            // Paginación
            int total = sales.size();
            totalPaginas = (int) Math.ceil((double) total / ITEMS_POR_PAGINA);
            if (totalPaginas == 0) totalPaginas = 1;

            int desde_idx = paginaActual * ITEMS_POR_PAGINA;
            int hasta_idx = Math.min(desde_idx + ITEMS_POR_PAGINA, total);

            salesList.clear();
            if (desde_idx < total) {
                salesList.addAll(sales.subList(desde_idx, hasta_idx));
            }

            updatePagination();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cargando ventas", e);
            AppLogger.error("VENTAS", "Error al cargar ventas: " + e.getMessage(), e);
        }
    }
    
    private void updatePagination() {
        paginaLabel.setText("Página " + (paginaActual + 1) + " de " + totalPaginas);
        btnAnterior.setDisable(paginaActual == 0);
        btnSiguiente.setDisable(paginaActual >= totalPaginas - 1);
    }
    
    // ==================== MANEJO DE EVENTOS ====================
    
    @FXML
    public void handleNuevaVenta() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/NewSale.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ventasTable.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle("Nueva Venta - POS");
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al abrir nueva venta", e);
            showError("Error al abrir el punto de venta: " + e.getMessage());
        }
    }

    @FXML
    public void handleFiltrar() {
        paginaActual = 0;
        loadSales();
    }

    @FXML
    public void handleBuscarProducto() {
        paginaActual = 0;
        loadSales();
    }

    @FXML
    public void handleLimpiarFiltros() {
        fechaDesde.setValue(null);
        fechaHasta.setValue(null);
        statusFilter.setValue("Todos");
        searchField.clear();
        paginaActual = 0;
        loadSales();
    }

    @FXML
    public void handlePaginaAnterior() {
        if (paginaActual > 0) {
            paginaActual--;
            loadSales();
        }
    }

    @FXML
    public void handlePaginaSiguiente() {
        if (paginaActual < totalPaginas - 1) {
            paginaActual++;
            loadSales();
        }
    }

    private void handleViewDetail(Sale sale) {
        try {
            Optional<Sale> saleCompleta = saleDAO.findById(sale.getId());
            if (saleCompleta.isEmpty()) {
                showError("No se pudo cargar la venta");
                return;
            }

            Sale s = saleCompleta.get();
            StringBuilder sb = new StringBuilder();
            sb.append("Venta #").append(s.getId()).append("\n");
            sb.append("Fecha: ").append(s.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
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

    private void handleCancelSale(Sale sale) {
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
                loadStats();
                loadSales();
                AppLogger.info("VENTAS", "Venta #" + sale.getId() + " anulada por " + SessionManager.getInstance().getCurrentUser().getUsername());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al anular venta", e);
                showError("No se pudo anular la venta: " + e.getMessage());
            }
        }
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
        SessionManager.getInstance().logout();
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
}
