package com.ferreteria.controllers;

import com.ferreteria.Main;
import com.ferreteria.models.*;
import com.ferreteria.models.dao.*;
import com.ferreteria.utils.DateTimePickerDialog;
import com.ferreteria.utils.SessionManager;
import com.ferreteria.utils.TicketGenerator;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Point of Sale (POS).
 * Handles new sale creation.
 */
public class NewSaleController {

    @FXML private Label fechaLabel;
    @FXML private Label vendedorLabel;
    @FXML private Label turnoLabel;

    @FXML private TextField searchField;
    @FXML private ListView<ProductVariant> productosListView;

    @FXML private VBox productoSeleccionadoBox;
    @FXML private Label productoNombreLabel;
    @FXML private Label productoPrecioLabel;
    @FXML private Label productoStockLabel;
    @FXML private TextField cantidadField;

    @FXML private VBox carritoContainer;
    @FXML private Label subtotalLabel;
    @FXML private Label itemsCountLabel;
    @FXML private Label totalLabel;

    @FXML private CheckBox chkEfectivo;
    @FXML private CheckBox chkTarjeta;
    @FXML private CheckBox chkTransferencia;
    @FXML private TextField montoEfectivo;
    @FXML private TextField montoTarjeta;
    @FXML private TextField montoTransferencia;
    @FXML private HBox efectivoVueltoBox;
    @FXML private TextField recibidoField;
    @FXML private Label cambioLabel;
    @FXML private Label pagoValidacionLabel;
    @FXML private TextField notasField;
    @FXML private Button confirmarBtn;
    @FXML private Button btnCambiarFecha;

    private ProductVariantDAO variantDAO;
    private SaleDAO saleDAO;

    private ProductVariant productoSeleccionado;
    private List<CartItem> carrito = new ArrayList<>();
    private LocalDateTime fechaVentaPersonalizada = null;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy - HH:mm");
    private final ZoneId sanJuanZone = ZoneId.of("America/Argentina/San_Juan");

    @FXML
    public void initialize() {
        variantDAO = new ProductVariantDAO(DatabaseConfig.getInstance());
        saleDAO = new SaleDAO(DatabaseConfig.getInstance());

        setupUI();
        setupProductList();
        setupPaymentToggle();
    }

    private void setupUI() {
        // Zona horaria de San Juan, Argentina
        ZonedDateTime now = ZonedDateTime.now(sanJuanZone);
        fechaVentaPersonalizada = now.toLocalDateTime();

        actualizarFechaLabel();

        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            vendedorLabel.setText(user.getFullName());
            turnoLabel.setText("Turno: " + (now.getHour() < 14 ? "Mañana" : "Tarde"));
        }

        // Cargar productos iniciales (últimos agregados o más vendidos)
        List<ProductVariant> productos = variantDAO.listarDisponibles();
        productosListView.setItems(FXCollections.observableArrayList(productos));
    }

    private void actualizarFechaLabel() {
        if (fechaVentaPersonalizada != null) {
            fechaLabel.setText(fechaVentaPersonalizada.format(dateFormatter));
        }
    }

    @FXML
    public void handleCambiarFecha() {
        DateTimePickerDialog.showForNewSale(fechaVentaPersonalizada)
            .ifPresent(newDateTime -> {
                fechaVentaPersonalizada = newDateTime;
                actualizarFechaLabel();
            });
    }

    private void setupProductList() {
        productosListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ProductVariant item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox container = new HBox(12);
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.setPadding(new Insets(8, 12, 8, 12));

                    VBox info = new VBox(2);
                    Label nombre = new Label(item.getDisplayName());
                    nombre.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

                    Label detalles = new Label(String.format("SKU: %s | Stock: %d",
                        item.getSku() != null ? item.getSku() : "N/A",
                        item.getStock()));
                    detalles.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

                    info.getChildren().addAll(nombre, detalles);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label precio = new Label("$" + String.format("%,.2f", item.getSalePrice()));
                    precio.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0ea5e9;");

                    container.getChildren().addAll(info, spacer, precio);
                    setGraphic(container);
                }
            }
        });

        productosListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                ProductVariant selected = productosListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    seleccionarProducto(selected);
                }
            } else if (e.getClickCount() == 2) {
                ProductVariant selected = productosListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    seleccionarProducto(selected);
                    handleAgregarAlCarrito();
                }
            }
        });
    }

    private void setupPaymentToggle() {
        // Cuando cambia cualquier checkbox, actualizar UI
        chkEfectivo.selectedProperty().addListener((obs, old, selected) -> actualizarUIPagos());
        chkTarjeta.selectedProperty().addListener((obs, old, selected) -> actualizarUIPagos());
        chkTransferencia.selectedProperty().addListener((obs, old, selected) -> actualizarUIPagos());

        // Validar cuando cambian los montos (solo si hay múltiples métodos)
        montoEfectivo.textProperty().addListener((obs, old, newVal) -> validarPagosMultiples());
        montoTarjeta.textProperty().addListener((obs, old, newVal) -> validarPagosMultiples());
        montoTransferencia.textProperty().addListener((obs, old, newVal) -> validarPagosMultiples());

        // Estado inicial
        actualizarUIPagos();
    }

    private int contarMetodosSeleccionados() {
        int count = 0;
        if (chkEfectivo.isSelected()) count++;
        if (chkTarjeta.isSelected()) count++;
        if (chkTransferencia.isSelected()) count++;
        return count;
    }

    private void actualizarUIPagos() {
        int metodosSeleccionados = contarMetodosSeleccionados();
        BigDecimal total = calcularTotal();
        String totalStr = String.format("%.2f", total);

        // Mostrar campos de monto para los métodos seleccionados
        montoEfectivo.setVisible(chkEfectivo.isSelected());
        montoEfectivo.setManaged(chkEfectivo.isSelected());
        montoTarjeta.setVisible(chkTarjeta.isSelected());
        montoTarjeta.setManaged(chkTarjeta.isSelected());
        montoTransferencia.setVisible(chkTransferencia.isSelected());
        montoTransferencia.setManaged(chkTransferencia.isSelected());

        // Si hay un solo método, autocompletar con el total
        if (metodosSeleccionados == 1) {
            if (chkEfectivo.isSelected()) montoEfectivo.setText(totalStr);
            if (chkTarjeta.isSelected()) montoTarjeta.setText(totalStr);
            if (chkTransferencia.isSelected()) montoTransferencia.setText(totalStr);
        }

        // Recibido/Vuelto solo visible si efectivo está seleccionado
        efectivoVueltoBox.setVisible(chkEfectivo.isSelected());
        efectivoVueltoBox.setManaged(chkEfectivo.isSelected());

        // Limpiar campos si se deselecciona
        if (!chkEfectivo.isSelected()) {
            montoEfectivo.clear();
            recibidoField.clear();
            cambioLabel.setText("$0.00");
        }
        if (!chkTarjeta.isSelected()) montoTarjeta.clear();
        if (!chkTransferencia.isSelected()) montoTransferencia.clear();

        validarPagosMultiples();
    }

    private void validarPagosMultiples() {
        BigDecimal total = calcularTotal();
        int metodosSeleccionados = contarMetodosSeleccionados();

        // Si el carrito está vacío, no mostrar validación
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            pagoValidacionLabel.setText("");
            return;
        }

        if (metodosSeleccionados == 0) {
            pagoValidacionLabel.setText("Seleccione un método de pago");
            pagoValidacionLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        // Si hay un solo método, no necesita validar montos
        if (metodosSeleccionados == 1) {
            pagoValidacionLabel.setText("");
            return;
        }

        // Múltiples métodos: validar que la suma sea igual al total
        BigDecimal sumaPagos = BigDecimal.ZERO;
        if (chkEfectivo.isSelected()) {
            sumaPagos = sumaPagos.add(parseMonto(montoEfectivo.getText()));
        }
        if (chkTarjeta.isSelected()) {
            sumaPagos = sumaPagos.add(parseMonto(montoTarjeta.getText()));
        }
        if (chkTransferencia.isSelected()) {
            sumaPagos = sumaPagos.add(parseMonto(montoTransferencia.getText()));
        }

        BigDecimal diferencia = total.subtract(sumaPagos);

        if (diferencia.compareTo(BigDecimal.ZERO) > 0) {
            pagoValidacionLabel.setText("Faltan: $" + String.format("%.2f", diferencia));
            pagoValidacionLabel.setStyle("-fx-text-fill: #dc2626;");
        } else if (diferencia.compareTo(BigDecimal.ZERO) < 0) {
            pagoValidacionLabel.setText("Sobran: $" + String.format("%.2f", diferencia.abs()));
            pagoValidacionLabel.setStyle("-fx-text-fill: #f59e0b;");
        } else {
            pagoValidacionLabel.setText("OK");
            pagoValidacionLabel.setStyle("-fx-text-fill: #22c55e;");
        }
    }

    private BigDecimal parseMonto(String text) {
        try {
            String clean = text.replace("$", "").replace(",", "").trim();
            if (clean.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(clean);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void seleccionarProducto(ProductVariant producto) {
        productoSeleccionado = producto;
        productoNombreLabel.setText(producto.getDisplayName());
        productoPrecioLabel.setText("$" + String.format("%,.2f", producto.getSalePrice()));
        productoStockLabel.setText("Stock disponible: " + producto.getStock());
        cantidadField.setText("1");

        productoSeleccionadoBox.setVisible(true);
        productoSeleccionadoBox.setManaged(true);
    }

    @FXML
    public void handleBuscar() {
        String query = searchField.getText().trim();
        List<ProductVariant> resultados;

        if (query.isEmpty()) {
            resultados = variantDAO.listarDisponibles();
        } else {
            resultados = variantDAO.buscar(query, 50);
        }

        productosListView.setItems(FXCollections.observableArrayList(resultados));
    }

    @FXML
    public void handleIncrementarCantidad() {
        int cantidad = parseCantidad();
        if (productoSeleccionado != null && cantidad < productoSeleccionado.getStock()) {
            cantidadField.setText(String.valueOf(cantidad + 1));
        }
    }

    @FXML
    public void handleDecrementarCantidad() {
        int cantidad = parseCantidad();
        if (cantidad > 1) {
            cantidadField.setText(String.valueOf(cantidad - 1));
        }
    }

    private int parseCantidad() {
        try {
            return Integer.parseInt(cantidadField.getText().trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @FXML
    public void handleAgregarAlCarrito() {
        if (productoSeleccionado == null) return;

        int cantidad = parseCantidad();
        if (cantidad <= 0) {
            showAlert("Error", "La cantidad debe ser mayor a 0", Alert.AlertType.WARNING);
            return;
        }

        // Verificar stock disponible considerando lo que ya está en el carrito
        int enCarrito = carrito.stream()
            .filter(i -> i.getVariant().getId() == productoSeleccionado.getId())
            .mapToInt(i -> i.getQuantity())
            .sum();

        if (cantidad + enCarrito > productoSeleccionado.getStock()) {
            showAlert("Stock Insuficiente",
                String.format("Solo hay %d unidades disponibles (%d ya en carrito)",
                    productoSeleccionado.getStock(), enCarrito),
                Alert.AlertType.WARNING);
            return;
        }

        // Buscar si ya existe en el carrito
        CartItem existente = carrito.stream()
            .filter(i -> i.getVariant().getId() == productoSeleccionado.getId())
            .findFirst()
            .orElse(null);

        if (existente != null) {
            existente.incrementQuantity(cantidad);
        } else {
            carrito.add(new CartItem(productoSeleccionado, cantidad));
        }

        actualizarCarritoUI();
        limpiarSeleccion();
    }

    private void limpiarSeleccion() {
        productoSeleccionado = null;
        productoSeleccionadoBox.setVisible(false);
        productoSeleccionadoBox.setManaged(false);
        productosListView.getSelectionModel().clearSelection();
        searchField.clear();
        handleBuscar();
    }

    private void actualizarCarritoUI() {
        carritoContainer.getChildren().clear();

        if (carrito.isEmpty()) {
            Label empty = new Label("El carrito está vacío");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
            empty.setPadding(new Insets(40, 0, 40, 0));
            carritoContainer.getChildren().add(empty);
            carritoContainer.setAlignment(Pos.CENTER);
        } else {
            carritoContainer.setAlignment(Pos.TOP_LEFT);
            for (CartItem item : carrito) {
                carritoContainer.getChildren().add(crearItemCarritoUI(item));
            }
        }

        // Actualizar totales
        BigDecimal subtotal = calcularTotal();
        int totalItems = carrito.stream().mapToInt(i -> i.getQuantity()).sum();

        subtotalLabel.setText("$" + String.format("%,.2f", subtotal));
        itemsCountLabel.setText(String.valueOf(totalItems));
        totalLabel.setText("$" + String.format("%,.2f", subtotal));

        // Actualizar montos de pago (autocompleta si hay un solo método)
        actualizarUIPagos();

        // Habilitar/deshabilitar botón confirmar
        confirmarBtn.setDisable(carrito.isEmpty());
    }

    private HBox crearItemCarritoUI(CartItem item) {
        HBox container = new HBox(8);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(12));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        VBox info = new VBox(2);
        Label nombre = new Label(item.getVariant().getDisplayName());
        nombre.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        nombre.setWrapText(true);
        nombre.setMaxWidth(180);

        Label precio = new Label("$" + String.format("%,.2f", item.getVariant().getSalePrice()) + " c/u");
        precio.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        info.getChildren().addAll(nombre, precio);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Controles de cantidad
        Button btnMenos = new Button("-");
        btnMenos.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 4; -fx-min-width: 28; -fx-min-height: 28;");
        btnMenos.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.decrementQuantity();
                actualizarCarritoUI();
            }
        });

        Label cantLabel = new Label(String.valueOf(item.getQuantity()));
        cantLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 30; -fx-alignment: center;");
        cantLabel.setAlignment(Pos.CENTER);

        Button btnMas = new Button("+");
        btnMas.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 4; -fx-min-width: 28; -fx-min-height: 28;");
        btnMas.setOnAction(e -> {
            if (item.getQuantity() < item.getVariant().getStock()) {
                item.incrementQuantity(1);
                actualizarCarritoUI();
            }
        });

        HBox qtyBox = new HBox(4, btnMenos, cantLabel, btnMas);
        qtyBox.setAlignment(Pos.CENTER);

        // Subtotal
        BigDecimal subtotal = item.getSubtotal();
        Label subtotalLbl = new Label("$" + String.format("%,.2f", subtotal));
        subtotalLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0f172a; -fx-min-width: 70; -fx-alignment: center-right;");

        // Eliminar
        Button btnEliminar = new Button("×");
        btnEliminar.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-background-radius: 4; -fx-font-weight: bold;");
        btnEliminar.setOnAction(e -> {
            carrito.remove(item);
            actualizarCarritoUI();
        });

        container.getChildren().addAll(info, qtyBox, subtotalLbl, btnEliminar);
        return container;
    }

    private BigDecimal calcularTotal() {
        return carrito.stream()
            .map(CartItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @FXML
    public void handleVaciarCarrito() {
        if (carrito.isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Vaciar Carrito");
        confirm.setHeaderText("¿Vaciar todo el carrito?");
        confirm.setContentText("Se eliminarán todos los productos del carrito.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            carrito.clear();
            actualizarCarritoUI();
        }
    }

    @FXML
    public void handleRecibidoChanged() {
        try {
            String text = recibidoField.getText().replace("$", "").replace(",", "").trim();
            if (text.isEmpty()) {
                cambioLabel.setText("$0.00");
                return;
            }

            BigDecimal recibido = new BigDecimal(text);
            BigDecimal total = calcularTotal();
            BigDecimal cambio = recibido.subtract(total);

            if (cambio.compareTo(BigDecimal.ZERO) >= 0) {
                cambioLabel.setText("$" + String.format("%,.2f", cambio));
                cambioLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #15803d;");
            } else {
                cambioLabel.setText("Falta: $" + String.format("%,.2f", cambio.abs()));
                cambioLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc2626;");
            }
        } catch (NumberFormatException e) {
            cambioLabel.setText("$0.00");
        }
    }

    @FXML
    public void handleConfirmarVenta() {
        if (carrito.isEmpty()) {
            showAlert("Error", "El carrito está vacío", Alert.AlertType.WARNING);
            return;
        }

        BigDecimal total = calcularTotal();
        int metodosSeleccionados = contarMetodosSeleccionados();

        // Validar que al menos un método de pago esté seleccionado
        if (metodosSeleccionados == 0) {
            showAlert("Error", "Seleccione al menos un método de pago", Alert.AlertType.WARNING);
            return;
        }

        // Crear pagos usando los montos ingresados
        List<SalePayment> pagos = new ArrayList<>();
        BigDecimal sumaPagos = BigDecimal.ZERO;

        if (chkEfectivo.isSelected()) {
            BigDecimal montoEf = parseMonto(montoEfectivo.getText());
            if (montoEf.compareTo(BigDecimal.ZERO) > 0) {
                sumaPagos = sumaPagos.add(montoEf);
                pagos.add(new SalePayment.Builder()
                    .paymentMethod(SalePayment.PaymentMethod.CASH)
                    .amount(montoEf)
                    .build());
            }
        }

        if (chkTarjeta.isSelected()) {
            BigDecimal montoTj = parseMonto(montoTarjeta.getText());
            if (montoTj.compareTo(BigDecimal.ZERO) > 0) {
                sumaPagos = sumaPagos.add(montoTj);
                pagos.add(new SalePayment.Builder()
                    .paymentMethod(SalePayment.PaymentMethod.DEBIT_CARD)
                    .amount(montoTj)
                    .build());
            }
        }

        if (chkTransferencia.isSelected()) {
            BigDecimal montoTr = parseMonto(montoTransferencia.getText());
            if (montoTr.compareTo(BigDecimal.ZERO) > 0) {
                sumaPagos = sumaPagos.add(montoTr);
                pagos.add(new SalePayment.Builder()
                    .paymentMethod(SalePayment.PaymentMethod.TRANSFER)
                    .amount(montoTr)
                    .build());
            }
        }

        // Validar que la suma de pagos cubra el total
        if (sumaPagos.compareTo(total) < 0) {
            showAlert("Pago Insuficiente",
                String.format("Faltan $%.2f para completar el pago", total.subtract(sumaPagos)),
                Alert.AlertType.WARNING);
            return;
        }

        // Confirmar
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Venta");
        confirm.setHeaderText("Total: $" + String.format("%,.2f", total));
        String detallesPago = pagos.stream()
            .map(p -> p.getPaymentMethod().getDisplayName() + ": $" + String.format("%.2f", p.getAmount()))
            .reduce((a, b) -> a + " + " + b)
            .orElse("");
        confirm.setContentText("Pagos: " + detallesPago + "\n¿Confirmar la venta?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            // Crear items de venta
            List<SaleItem> items = new ArrayList<>();
            for (CartItem item : carrito) {
                items.add(new SaleItem.Builder()
                    .variantId(item.getVariantId())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getVariant().getSalePrice())
                    .subtotal(item.getSubtotal())
                    .productName(item.getVariant().getProductName())
                    .variantName(item.getVariant().getVariantName())
                    .build());
            }

            // Crear venta con múltiples pagos
            User user = SessionManager.getInstance().getCurrentUser();
            Sale.Builder saleBuilder = new Sale.Builder()
                .userId(user.getId())
                .total(total)
                .status("completed")
                .notes(notasField.getText())
                .createdAt(fechaVentaPersonalizada)
                .items(items);

            for (SalePayment pago : pagos) {
                saleBuilder.addPayment(pago);
            }

            Sale sale = saleBuilder.build();

            // Guardar
            Sale createdSale = saleDAO.create(sale);

            // Actualizar fecha si es personalizada
            if (fechaVentaPersonalizada != null) {
                saleDAO.updateDateTime(createdSale.getId(), fechaVentaPersonalizada);
            }

            // Mostrar ticket con la fecha personalizada
            createdSale = new Sale.Builder()
                .id(createdSale.getId())
                .userId(createdSale.getUserId())
                .total(createdSale.getTotal())
                .status(createdSale.getStatus())
                .notes(createdSale.getNotes())
                .createdAt(fechaVentaPersonalizada)
                .items(createdSale.getItems())
                .build();
            mostrarTicket(createdSale);

            // Limpiar para nueva venta
            carrito.clear();
            actualizarCarritoUI();
            limpiarFormularioPago();

            // Resetear fecha a la hora actual
            fechaVentaPersonalizada = ZonedDateTime.now(sanJuanZone).toLocalDateTime();
            actualizarFechaLabel();

        } catch (Exception e) {
            showAlert("Error", "No se pudo crear la venta: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void limpiarFormularioPago() {
        chkEfectivo.setSelected(true);
        chkTarjeta.setSelected(false);
        chkTransferencia.setSelected(false);
        montoEfectivo.clear();
        montoTarjeta.clear();
        montoTransferencia.clear();
        recibidoField.clear();
        notasField.clear();
        cambioLabel.setText("$0.00");
        pagoValidacionLabel.setText("");
    }

    private void mostrarTicket(Sale sale) {
        String vendorName = SessionManager.getInstance().getCurrentUser().getFullName();
        TicketGenerator.showTicketDialog(sale, vendorName);
    }

    @FXML
    public void handleVolver() {
        if (!carrito.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Salir del POS");
            confirm.setHeaderText("¿Salir sin completar la venta?");
            confirm.setContentText("Se perderán los productos del carrito.");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }
        Main.navigateTo("/views/Sales.fxml", "Sistema Ferreteria - Ventas");
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
