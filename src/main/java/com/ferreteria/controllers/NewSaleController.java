package com.ferreteria.controllers;

import com.ferreteria.Main;
import com.ferreteria.models.*;
import com.ferreteria.models.dao.*;
import com.ferreteria.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @FXML private ToggleButton btnEfectivo;
    @FXML private ToggleButton btnTarjeta;
    @FXML private ToggleButton btnTransferencia;
    @FXML private ToggleGroup paymentGroup;
    @FXML private HBox efectivoBox;
    @FXML private TextField recibidoField;
    @FXML private Label cambioLabel;
    @FXML private TextField notasField;
    @FXML private Button confirmarBtn;

    private ProductVariantDAO variantDAO;
    private SaleDAO saleDAO;

    private ProductVariant productoSeleccionado;
    private List<CartItem> carrito = new ArrayList<>();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy - HH:mm");

    @FXML
    public void initialize() {
        variantDAO = new ProductVariantDAO(DatabaseConfig.getInstance());
        saleDAO = new SaleDAO(DatabaseConfig.getInstance());

        setupUI();
        setupProductList();
        setupPaymentToggle();
    }

    private void setupUI() {
        fechaLabel.setText(LocalDateTime.now().format(dateFormatter));

        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            vendedorLabel.setText(user.getFullName());
            turnoLabel.setText("Turno: " + (LocalDateTime.now().getHour() < 14 ? "Mañana" : "Tarde"));
        }

        // Cargar productos iniciales (últimos agregados o más vendidos)
        List<ProductVariant> productos = variantDAO.listarDisponibles();
        productosListView.setItems(FXCollections.observableArrayList(productos));
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
        paymentGroup.selectedToggleProperty().addListener((obs, old, newVal) -> {
            efectivoBox.setVisible(btnEfectivo.isSelected());
            efectivoBox.setManaged(btnEfectivo.isSelected());
        });
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
            .filter(i -> i.variante.getId() == productoSeleccionado.getId())
            .mapToInt(i -> i.cantidad)
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
            .filter(i -> i.variante.getId() == productoSeleccionado.getId())
            .findFirst()
            .orElse(null);

        if (existente != null) {
            existente.cantidad += cantidad;
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
        int totalItems = carrito.stream().mapToInt(i -> i.cantidad).sum();

        subtotalLabel.setText("$" + String.format("%,.2f", subtotal));
        itemsCountLabel.setText(String.valueOf(totalItems));
        totalLabel.setText("$" + String.format("%,.2f", subtotal));

        // Habilitar/deshabilitar botón confirmar
        confirmarBtn.setDisable(carrito.isEmpty());
    }

    private HBox crearItemCarritoUI(CartItem item) {
        HBox container = new HBox(8);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(12));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        VBox info = new VBox(2);
        Label nombre = new Label(item.variante.getDisplayName());
        nombre.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        nombre.setWrapText(true);
        nombre.setMaxWidth(180);

        Label precio = new Label("$" + String.format("%,.2f", item.variante.getSalePrice()) + " c/u");
        precio.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        info.getChildren().addAll(nombre, precio);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Controles de cantidad
        Button btnMenos = new Button("-");
        btnMenos.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 4; -fx-min-width: 28; -fx-min-height: 28;");
        btnMenos.setOnAction(e -> {
            if (item.cantidad > 1) {
                item.cantidad--;
                actualizarCarritoUI();
            }
        });

        Label cantLabel = new Label(String.valueOf(item.cantidad));
        cantLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 30; -fx-alignment: center;");
        cantLabel.setAlignment(Pos.CENTER);

        Button btnMas = new Button("+");
        btnMas.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 4; -fx-min-width: 28; -fx-min-height: 28;");
        btnMas.setOnAction(e -> {
            if (item.cantidad < item.variante.getStock()) {
                item.cantidad++;
                actualizarCarritoUI();
            }
        });

        HBox qtyBox = new HBox(4, btnMenos, cantLabel, btnMas);
        qtyBox.setAlignment(Pos.CENTER);

        // Subtotal
        BigDecimal subtotal = item.variante.getSalePrice().multiply(BigDecimal.valueOf(item.cantidad));
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
            .map(i -> i.variante.getSalePrice().multiply(BigDecimal.valueOf(i.cantidad)))
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

        // Determinar método de pago
        SalePayment.PaymentMethod metodo = SalePayment.PaymentMethod.CASH;
        if (btnTarjeta.isSelected()) {
            metodo = SalePayment.PaymentMethod.DEBIT_CARD;
        } else if (btnTransferencia.isSelected()) {
            metodo = SalePayment.PaymentMethod.TRANSFER;
        }

        BigDecimal total = calcularTotal();

        // Validar pago en efectivo
        if (btnEfectivo.isSelected()) {
            try {
                String text = recibidoField.getText().replace("$", "").replace(",", "").trim();
                if (!text.isEmpty()) {
                    BigDecimal recibido = new BigDecimal(text);
                    if (recibido.compareTo(total) < 0) {
                        showAlert("Pago Insuficiente", "El monto recibido es menor al total", Alert.AlertType.WARNING);
                        return;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        // Confirmar
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Venta");
        confirm.setHeaderText("Total: $" + String.format("%,.2f", total));
        confirm.setContentText("¿Confirmar la venta?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            // Crear items de venta
            List<SaleItem> items = new ArrayList<>();
            for (CartItem item : carrito) {
                items.add(new SaleItem.Builder()
                    .variantId(item.variante.getId())
                    .quantity(item.cantidad)
                    .unitPrice(item.variante.getSalePrice())
                    .subtotal(item.variante.getSalePrice().multiply(BigDecimal.valueOf(item.cantidad)))
                    .productName(item.variante.getProductName())
                    .variantName(item.variante.getVariantName())
                    .build());
            }

            // Crear pago
            SalePayment pago = new SalePayment.Builder()
                .paymentMethod(metodo)
                .amount(total)
                .build();

            // Crear venta
            User user = SessionManager.getInstance().getCurrentUser();
            Sale sale = new Sale.Builder()
                .userId(user.getId())
                .total(total)
                .status("completed")
                .notes(notasField.getText())
                .items(items)
                .addPayment(pago)
                .build();

            // Guardar
            Sale createdSale = saleDAO.create(sale);

            // Mostrar ticket
            mostrarTicket(createdSale);

            // Limpiar para nueva venta
            carrito.clear();
            actualizarCarritoUI();
            recibidoField.clear();
            notasField.clear();
            cambioLabel.setText("$0.00");

        } catch (Exception e) {
            showAlert("Error", "No se pudo crear la venta: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void mostrarTicket(Sale sale) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════\n");
        sb.append("         FERRETERIA\n");
        sb.append("═══════════════════════════════\n\n");
        sb.append("Venta #").append(sale.getId()).append("\n");
        sb.append("Fecha: ").append(sale.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
        sb.append("Vendedor: ").append(SessionManager.getInstance().getCurrentUser().getFullName()).append("\n\n");

        sb.append("───────────────────────────────\n");
        sale.getItems().forEach(item -> {
            sb.append(String.format("%s\n  %d x $%,.2f = $%,.2f\n",
                item.getDisplayName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()));
        });

        sb.append("───────────────────────────────\n");
        sb.append(String.format("TOTAL: $%,.2f\n", sale.getTotal()));
        sb.append("───────────────────────────────\n\n");
        sb.append("    ¡Gracias por su compra!\n");
        sb.append("═══════════════════════════════\n");

        Alert ticket = new Alert(Alert.AlertType.INFORMATION);
        ticket.setTitle("Venta Completada");
        ticket.setHeaderText("Venta #" + sale.getId() + " registrada exitosamente");

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        textArea.setPrefRowCount(18);
        textArea.setPrefColumnCount(35);

        ticket.getDialogPane().setContent(textArea);
        ticket.getDialogPane().setMinWidth(400);
        ticket.showAndWait();
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

    /**
     * Internal class for cart items.
     */
    private static class CartItem {
        ProductVariant variante;
        int cantidad;

        CartItem(ProductVariant variante, int cantidad) {
            this.variante = variante;
            this.cantidad = cantidad;
        }
    }
}
