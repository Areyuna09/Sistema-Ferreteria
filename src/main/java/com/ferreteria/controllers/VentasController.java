package com.ferreteria.controllers;

import com.ferreteria.Main;
import com.ferreteria.models.User;
import com.ferreteria.models.Venta;
import com.ferreteria.models.dao.DatabaseConfig;
import com.ferreteria.models.dao.VentaDAO;
import com.ferreteria.utils.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controlador para la vista de gestión de ventas.
 */
public class VentasController {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label ventasHoyLabel;
    @FXML private Label ventasMesLabel;
    @FXML private Label cantidadHoyLabel;
    @FXML private Label promedioLabel;

    @FXML private DatePicker fechaDesde;
    @FXML private DatePicker fechaHasta;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField searchField;

    @FXML private TableView<Venta> ventasTable;
    @FXML private TableColumn<Venta, String> colId;
    @FXML private TableColumn<Venta, String> colFecha;
    @FXML private TableColumn<Venta, String> colProductos;
    @FXML private TableColumn<Venta, String> colVendedor;
    @FXML private TableColumn<Venta, String> colTotal;
    @FXML private TableColumn<Venta, String> colEstado;
    @FXML private TableColumn<Venta, Void> colAcciones;

    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Label paginaLabel;

    private VentaDAO ventaDAO;
    private ObservableList<Venta> ventasList;
    private int paginaActual = 0;
    private int totalPaginas = 1;
    private static final int ITEMS_POR_PAGINA = 15;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        ventaDAO = new VentaDAO(DatabaseConfig.getInstance());
        ventasList = FXCollections.observableArrayList();

        loadUserInfo();
        setupStatusFilter();
        setupTable();
        loadStats();
        loadVentas();
    }

    private void loadUserInfo() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            welcomeLabel.setText(user.getFullName());
            roleLabel.setText(user.getRole().getValue());
        }
    }

    private void setupStatusFilter() {
        statusFilter.setItems(FXCollections.observableArrayList(
            "Todos", "Completadas", "Anuladas"
        ));
        statusFilter.setValue("Todos");
    }

    private void setupTable() {
        // Columna # (ID formateado)
        colId.setCellValueFactory(cellData -> {
            int id = cellData.getValue().getId();
            return new SimpleStringProperty(String.format("V-%04d", id));
        });

        colFecha.setCellValueFactory(cellData -> {
            LocalDateTime fecha = cellData.getValue().getCreatedAt();
            return new SimpleStringProperty(fecha != null ? fecha.format(dateFormatter) : "");
        });

        // Columna Productos (lista de productos vendidos)
        colProductos.setCellValueFactory(cellData -> {
            var items = cellData.getValue().getItems();
            if (items == null || items.isEmpty()) {
                return new SimpleStringProperty("-");
            }
            // Mostrar primeros 2 productos
            String productos = items.stream()
                .limit(2)
                .map(item -> item.getProductName() != null ? item.getProductName() : item.getDisplayName())
                .collect(java.util.stream.Collectors.joining(", "));
            if (items.size() > 2) {
                productos += " (+" + (items.size() - 2) + ")";
            }
            return new SimpleStringProperty(productos);
        });

        colVendedor.setCellValueFactory(cellData -> {
            String nombre = cellData.getValue().getUserName();
            return new SimpleStringProperty(nombre != null ? nombre : "Usuario");
        });

        colTotal.setCellValueFactory(cellData -> {
            BigDecimal total = cellData.getValue().getTotal();
            return new SimpleStringProperty("$" + String.format("%,.2f", total));
        });

        colEstado.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            String display = "completed".equals(status) ? "Completada" : "Anulada";
            return new SimpleStringProperty(display);
        });

        // Estilo condicional para estado
        colEstado.setCellFactory(column -> new TableCell<>() {
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
        colAcciones.setCellFactory(column -> new TableCell<>() {
            private final Button btnVer = new Button("Ver");
            private final Button btnAnular = new Button("Anular");
            private final HBox container = new HBox(8, btnVer, btnAnular);

            {
                container.setAlignment(Pos.CENTER);
                btnVer.getStyleClass().addAll("action-button-small", "accent");
                btnAnular.getStyleClass().addAll("action-button-small", "danger");

                btnVer.setOnAction(e -> {
                    Venta venta = getTableView().getItems().get(getIndex());
                    handleVerDetalle(venta);
                });

                btnAnular.setOnAction(e -> {
                    Venta venta = getTableView().getItems().get(getIndex());
                    handleAnularVenta(venta);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Venta venta = getTableView().getItems().get(getIndex());
                    btnAnular.setDisable(venta.isCancelled());
                    setGraphic(container);
                }
            }
        });

        ventasTable.setItems(ventasList);
    }

    private void loadStats() {
        LocalDate hoy = LocalDate.now();
        int year = hoy.getYear();
        int month = hoy.getMonthValue();

        BigDecimal totalHoy = ventaDAO.totalDelDia(hoy);
        BigDecimal totalMes = ventaDAO.totalDelMes(year, month);
        int cantidadHoy = ventaDAO.contarDelDia(hoy);

        ventasHoyLabel.setText("$" + String.format("%,.2f", totalHoy));
        ventasMesLabel.setText("$" + String.format("%,.2f", totalMes));
        cantidadHoyLabel.setText(String.valueOf(cantidadHoy));

        if (cantidadHoy > 0) {
            BigDecimal promedio = totalHoy.divide(BigDecimal.valueOf(cantidadHoy), 2, java.math.RoundingMode.HALF_UP);
            promedioLabel.setText("$" + String.format("%,.2f", promedio));
        } else {
            promedioLabel.setText("$0.00");
        }
    }

    private void loadVentas() {
        List<Venta> ventas;

        // Aplicar filtros
        String status = statusFilter.getValue();
        LocalDate desde = fechaDesde.getValue();
        LocalDate hasta = fechaHasta.getValue();
        String busquedaProducto = searchField.getText().trim().toLowerCase();

        if (desde != null && hasta != null) {
            ventas = ventaDAO.listarPorRangoFechas(desde, hasta);
        } else if ("Completadas".equals(status)) {
            ventas = ventaDAO.listarCompletadas();
        } else if ("Anuladas".equals(status)) {
            ventas = ventaDAO.listarAnuladas();
        } else {
            ventas = ventaDAO.listarTodas();
        }

        // Filtrar por status si hay rango de fechas
        if (desde != null && hasta != null && !"Todos".equals(status)) {
            String statusVal = "Completadas".equals(status) ? "completed" : "cancelled";
            ventas = ventas.stream()
                .filter(v -> v.getStatus().equals(statusVal))
                .toList();
        }

        // Cargar items para cada venta (para mostrar cantidad y para filtrar)
        ventas = ventas.stream()
            .map(v -> ventaDAO.buscarPorId(v.getId()).orElse(v))
            .toList();

        // Filtrar por producto si hay búsqueda
        if (!busquedaProducto.isEmpty()) {
            ventas = ventas.stream()
                .filter(v -> v.getItems().stream()
                    .anyMatch(item ->
                        (item.getProductName() != null && item.getProductName().toLowerCase().contains(busquedaProducto)) ||
                        (item.getVariantName() != null && item.getVariantName().toLowerCase().contains(busquedaProducto)) ||
                        (item.getDisplayName() != null && item.getDisplayName().toLowerCase().contains(busquedaProducto))
                    ))
                .toList();
        }

        // Paginación
        int total = ventas.size();
        totalPaginas = (int) Math.ceil((double) total / ITEMS_POR_PAGINA);
        if (totalPaginas == 0) totalPaginas = 1;

        int desde_idx = paginaActual * ITEMS_POR_PAGINA;
        int hasta_idx = Math.min(desde_idx + ITEMS_POR_PAGINA, total);

        ventasList.clear();
        if (desde_idx < total) {
            ventasList.addAll(ventas.subList(desde_idx, hasta_idx));
        }

        updatePaginacion();
    }

    private void updatePaginacion() {
        paginaLabel.setText("Pagina " + (paginaActual + 1) + " de " + totalPaginas);
        btnAnterior.setDisable(paginaActual == 0);
        btnSiguiente.setDisable(paginaActual >= totalPaginas - 1);
    }

    @FXML
    public void handleNuevaVenta() {
        Main.navigateTo("/views/NuevaVenta.fxml", "Nueva Venta - POS");
    }

    @FXML
    public void handleFiltrar() {
        paginaActual = 0;
        loadVentas();
    }

    @FXML
    public void handleBuscarProducto() {
        paginaActual = 0;
        loadVentas();
    }

    @FXML
    public void handleLimpiarFiltros() {
        fechaDesde.setValue(null);
        fechaHasta.setValue(null);
        statusFilter.setValue("Todos");
        searchField.clear();
        paginaActual = 0;
        loadVentas();
    }

    @FXML
    public void handlePaginaAnterior() {
        if (paginaActual > 0) {
            paginaActual--;
            loadVentas();
        }
    }

    @FXML
    public void handlePaginaSiguiente() {
        if (paginaActual < totalPaginas - 1) {
            paginaActual++;
            loadVentas();
        }
    }

    private void handleVerDetalle(Venta venta) {
        Optional<Venta> ventaCompleta = ventaDAO.buscarPorId(venta.getId());
        if (ventaCompleta.isEmpty()) {
            showAlert("Error", "No se pudo cargar la venta", Alert.AlertType.ERROR);
            return;
        }

        Venta v = ventaCompleta.get();
        StringBuilder sb = new StringBuilder();
        sb.append("Venta #").append(v.getId()).append("\n");
        sb.append("Fecha: ").append(v.getCreatedAt().format(dateFormatter)).append("\n");
        sb.append("Vendedor: ").append(v.getUserName()).append("\n");
        sb.append("Estado: ").append(v.isCompleted() ? "Completada" : "Anulada").append("\n\n");

        sb.append("--- ITEMS ---\n");
        v.getItems().forEach(item -> {
            sb.append(String.format("• %s x%d = $%,.2f\n",
                item.getDisplayName(),
                item.getQuantity(),
                item.getSubtotal()));
        });

        sb.append("\n--- PAGOS ---\n");
        v.getPagos().forEach(pago -> {
            sb.append(String.format("• %s: $%,.2f\n",
                pago.getPaymentMethodDisplayName(),
                pago.getAmount()));
        });

        sb.append("\n").append("TOTAL: $").append(String.format("%,.2f", v.getTotal()));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalle de Venta");
        alert.setHeaderText("Venta #" + v.getId());
        alert.setContentText(sb.toString());
        alert.getDialogPane().setMinWidth(400);
        alert.showAndWait();
    }

    private void handleAnularVenta(Venta venta) {
        if (venta.isCancelled()) {
            showAlert("Aviso", "Esta venta ya está anulada", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Anulación");
        confirm.setHeaderText("¿Anular venta #" + venta.getId() + "?");
        confirm.setContentText("Esta acción revertirá el stock de los productos. ¿Continuar?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                ventaDAO.anular(venta.getId());
                showAlert("Éxito", "Venta anulada correctamente", Alert.AlertType.INFORMATION);
                loadStats();
                loadVentas();
            } catch (Exception e) {
                showAlert("Error", "No se pudo anular la venta: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Navegación
    @FXML public void handleDashboard() {
        Main.navigateTo("/views/Dashboard.fxml", "Sistema Ferreteria - Dashboard");
    }

    @FXML public void handleProducts() {
        System.out.println("Navegando a Productos...");
    }

    @FXML public void handleReports() {
        System.out.println("Navegando a Reportes...");
    }

    @FXML public void handleUsers() {
        if (!SessionManager.getInstance().isAdmin()) {
            showAlert("Acceso Denegado", "Solo administradores pueden acceder", Alert.AlertType.WARNING);
            return;
        }
        System.out.println("Navegando a Usuarios...");
    }

    @FXML public void handleLogout() {
        SessionManager.getInstance().logout();
        Main.navigateTo("/views/Login.fxml", "Ferreteria - Sistema de Gestion");
    }
}
