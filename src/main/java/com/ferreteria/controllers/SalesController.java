package com.ferreteria.controllers;

import com.ferreteria.Main;
import com.ferreteria.models.Sale;
import com.ferreteria.models.dao.DatabaseConfig;
import com.ferreteria.models.dao.SaleDAO;
import com.ferreteria.utils.SaleEditDialog;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the sales management view.
 */
public class SalesController {

    @FXML private NavbarController navbarController;

    @FXML private Label ventasHoyLabel;
    @FXML private Label ventasMesLabel;
    @FXML private Label cantidadHoyLabel;
    @FXML private Label promedioLabel;

    @FXML private DatePicker fechaDesde;
    @FXML private DatePicker fechaHasta;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField searchField;

    @FXML private TableView<Sale> ventasTable;
    @FXML private TableColumn<Sale, String> colId;
    @FXML private TableColumn<Sale, String> colFecha;
    @FXML private TableColumn<Sale, String> colProductos;
    @FXML private TableColumn<Sale, String> colVendedor;
    @FXML private TableColumn<Sale, String> colTotal;
    @FXML private TableColumn<Sale, String> colEstado;
    @FXML private TableColumn<Sale, Void> colAcciones;

    @FXML private Button btnAnterior;
    @FXML private Button btnSiguiente;
    @FXML private Label paginaLabel;

    private SaleDAO saleDAO;
    private ObservableList<Sale> salesList;
    private int paginaActual = 0;
    private int totalPaginas = 1;
    private static final int ITEMS_POR_PAGINA = 15;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        if (navbarController != null) {
            navbarController.setActiveView("ventas");
        }

        saleDAO = new SaleDAO(DatabaseConfig.getInstance());
        salesList = FXCollections.observableArrayList();

        setupStatusFilter();
        setupTable();
        loadStats();
        loadSales();
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
            private final Button btnEliminar = new Button("Eliminar");
            private final HBox container = new HBox(6);

            {
                container.setAlignment(Pos.CENTER);
                btnVer.getStyleClass().addAll("action-button-small", "accent");
                btnVer.setStyle("-fx-font-size: 10px; -fx-padding: 4 8;");
                btnAnular.getStyleClass().addAll("action-button-small", "danger");
                btnAnular.setStyle("-fx-font-size: 10px; -fx-padding: 4 8;");
                btnEliminar.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 4 8; -fx-background-radius: 4;");

                btnVer.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    handleViewDetail(sale);
                });

                btnAnular.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    handleCancelSale(sale);
                });

                btnEliminar.setOnAction(e -> {
                    Sale sale = getTableView().getItems().get(getIndex());
                    handleDeleteSale(sale);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Sale sale = getTableView().getItems().get(getIndex());
                    container.getChildren().clear();
                    container.getChildren().add(btnVer);

                    if (sale.isCancelled()) {
                        // Venta anulada: mostrar solo Ver y Eliminar
                        container.getChildren().add(btnEliminar);
                    } else {
                        // Venta activa: mostrar Ver y Anular
                        container.getChildren().add(btnAnular);
                    }
                    setGraphic(container);
                }
            }
        });

        ventasTable.setItems(salesList);
    }

    private void loadStats() {
        LocalDate hoy = LocalDate.now();
        int year = hoy.getYear();
        int month = hoy.getMonthValue();

        BigDecimal totalHoy = saleDAO.dailyTotal(hoy);
        BigDecimal totalMes = saleDAO.monthlyTotal(year, month);
        int cantidadHoy = saleDAO.dailyCount(hoy);

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

    private void loadSales() {
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
                .filter(s -> s.getStatus().equals(statusVal))
                .toList();
        }

        // Cargar items para cada venta (para mostrar cantidad y para filtrar)
        sales = sales.stream()
            .map(s -> saleDAO.findById(s.getId()).orElse(s))
            .toList();

        // Filtrar por producto si hay búsqueda
        if (!busquedaProducto.isEmpty()) {
            sales = sales.stream()
                .filter(s -> s.getItems().stream()
                    .anyMatch(item ->
                        (item.getProductName() != null && item.getProductName().toLowerCase().contains(busquedaProducto)) ||
                        (item.getVariantName() != null && item.getVariantName().toLowerCase().contains(busquedaProducto)) ||
                        (item.getDisplayName() != null && item.getDisplayName().toLowerCase().contains(busquedaProducto))
                    ))
                .toList();
        }

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
    }

    private void updatePagination() {
        paginaLabel.setText("Pagina " + (paginaActual + 1) + " de " + totalPaginas);
        btnAnterior.setDisable(paginaActual == 0);
        btnSiguiente.setDisable(paginaActual >= totalPaginas - 1);
    }

    @FXML
    public void handleNuevaVenta() {
        Main.navigateTo("/views/NewSale.fxml", "Nueva Venta - POS");
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
        Optional<Sale> saleCompleta = saleDAO.findById(sale.getId());
        if (saleCompleta.isEmpty()) {
            showAlert("Error", "No se pudo cargar la venta", Alert.AlertType.ERROR);
            return;
        }

        Sale s = saleCompleta.get();
        SaleEditDialog editDialog = new SaleEditDialog(s, saleDAO);

        if (editDialog.showAndWait()) {
            showAlert("Éxito", "Venta actualizada correctamente", Alert.AlertType.INFORMATION);
            loadStats();
            loadSales();
        }
    }

    private void handleCancelSale(Sale sale) {
        if (sale.isCancelled()) {
            showAlert("Aviso", "Esta venta ya está anulada", Alert.AlertType.WARNING);
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
                showAlert("Éxito", "Venta anulada correctamente", Alert.AlertType.INFORMATION);
                loadStats();
                loadSales();
            } catch (Exception e) {
                showAlert("Error", "No se pudo anular la venta: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void handleDeleteSale(Sale sale) {
        if (!sale.isCancelled()) {
            showAlert("Aviso", "Solo se pueden eliminar ventas anuladas", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Eliminación");
        confirm.setHeaderText("¿Eliminar permanentemente la venta #" + sale.getId() + "?");
        confirm.setContentText("Esta acción no se puede deshacer. ¿Continuar?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                saleDAO.delete(sale.getId());
                showAlert("Éxito", "Venta eliminada correctamente", Alert.AlertType.INFORMATION);
                loadStats();
                loadSales();
            } catch (Exception e) {
                showAlert("Error", "No se pudo eliminar la venta: " + e.getMessage(), Alert.AlertType.ERROR);
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
}
