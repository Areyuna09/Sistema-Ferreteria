package com.ferreteria.utils;

import com.ferreteria.models.*;
import com.ferreteria.models.dao.*;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dialog for editing all fields of an existing sale.
 */
public class SaleEditDialog {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Sale sale;
    private final SaleDAO saleDAO;
    private final ProductVariantDAO variantDAO;

    // Campos editables
    private LocalDateTime fechaEditada;
    private TextField notasField;
    private List<ItemEditRow> itemEdits = new ArrayList<>();
    private List<PaymentEditRow> paymentEdits = new ArrayList<>();
    private Label totalLabel;

    public SaleEditDialog(Sale sale, SaleDAO saleDAO) {
        this.sale = sale;
        this.saleDAO = saleDAO;
        this.variantDAO = new ProductVariantDAO(DatabaseConfig.getInstance());
        this.fechaEditada = sale.getCreatedAt();
    }

    /**
     * Shows the edit dialog and returns true if changes were saved.
     */
    public boolean showAndWait() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar Venta #" + sale.getId());
        dialog.setHeaderText(sale.isCompleted() ? "Venta Completada" : "Venta Anulada");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setPrefWidth(500);

        // === SECCION: Información General ===
        content.getChildren().add(createInfoSection());
        content.getChildren().add(new Separator());

        // === SECCION: Productos ===
        content.getChildren().add(createItemsSection());
        content.getChildren().add(new Separator());

        // === SECCION: Pagos ===
        content.getChildren().add(createPaymentsSection());
        content.getChildren().add(new Separator());

        // === TOTAL ===
        totalLabel = new Label("TOTAL: $" + String.format("%,.2f", sale.getTotal()));
        totalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0ea5e9;");
        content.getChildren().add(totalLabel);

        // Scroll para contenido largo
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(500);
        scroll.setStyle("-fx-background-color: transparent;");

        dialog.getDialogPane().setContent(scroll);

        ButtonType guardarBtn = new ButtonType("Guardar Cambios", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, cancelarBtn);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == guardarBtn) {
            return saveChanges();
        }
        return false;
    }

    private VBox createInfoSection() {
        VBox section = new VBox(8);

        Label title = new Label("INFORMACION GENERAL");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        section.getChildren().add(title);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        // Fecha
        Label fechaLbl = new Label("Fecha:");
        TextField fechaField = new TextField(fechaEditada.format(DATE_FORMAT));
        fechaField.setEditable(false);
        fechaField.setPrefWidth(150);
        Button btnCambiarFecha = new Button("Cambiar");
        btnCambiarFecha.setStyle("-fx-font-size: 10px;");
        btnCambiarFecha.setOnAction(e -> {
            DateTimePickerDialog.showForSaleEdit(sale.getId(), fechaEditada)
                .ifPresent(dt -> {
                    fechaEditada = dt;
                    fechaField.setText(dt.format(DATE_FORMAT));
                });
        });
        grid.add(fechaLbl, 0, 0);
        grid.add(new HBox(8, fechaField, btnCambiarFecha), 1, 0);

        // Vendedor (no editable)
        Label vendedorLbl = new Label("Vendedor:");
        Label vendedorVal = new Label(sale.getUserName());
        grid.add(vendedorLbl, 0, 1);
        grid.add(vendedorVal, 1, 1);

        // Notas
        Label notasLbl = new Label("Notas:");
        notasField = new TextField(sale.getNotes() != null ? sale.getNotes() : "");
        notasField.setPromptText("Agregar notas...");
        notasField.setPrefWidth(300);
        grid.add(notasLbl, 0, 2);
        grid.add(notasField, 1, 2);

        section.getChildren().add(grid);
        return section;
    }

    private VBox createItemsSection() {
        VBox section = new VBox(8);

        Label title = new Label("PRODUCTOS");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        section.getChildren().add(title);

        for (SaleItem item : sale.getItems()) {
            section.getChildren().add(createItemRow(item));
        }

        return section;
    }

    private HBox createItemRow(SaleItem item) {
        ItemEditRow editRow = new ItemEditRow(item);
        itemEdits.add(editRow);

        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4));
        row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 4;");

        // Nombre del producto (editable via botón)
        Label nombreLbl = new Label(item.getDisplayName());
        nombreLbl.setPrefWidth(160);
        nombreLbl.setStyle("-fx-font-size: 11px;");
        editRow.nombreLabel = nombreLbl;

        Button btnCambiar = new Button("Cambiar");
        btnCambiar.setStyle("-fx-font-size: 9px; -fx-padding: 2 6; -fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8;");
        btnCambiar.setOnAction(e -> {
            ProductVariant nuevo = showProductSearchDialog(item.getDisplayName());
            if (nuevo != null) {
                editRow.nuevoVariante = nuevo;
                nombreLbl.setText(nuevo.getDisplayName());
                nombreLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #1d4ed8; -fx-font-weight: bold;");
                editRow.precioLabel.setText("$" + String.format("%.2f", nuevo.getSalePrice()));
                updateItemSubtotal(editRow);
                updateTotal();
            }
        });
        editRow.btnCambiar = btnCambiar;

        // Precio unitario
        Label precioLbl = new Label("$" + String.format("%.2f", item.getUnitPrice()));
        precioLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
        precioLbl.setPrefWidth(60);
        editRow.precioLabel = precioLbl;

        // Cantidad
        Spinner<Integer> cantSpinner = new Spinner<>(0, 999, item.getQuantity());
        cantSpinner.setPrefWidth(65);
        cantSpinner.setEditable(true);
        cantSpinner.valueProperty().addListener((obs, old, newVal) -> {
            updateItemSubtotal(editRow);
            updateTotal();
            if (newVal == 0) {
                row.setStyle("-fx-background-color: #fef2f2; -fx-background-radius: 4; -fx-opacity: 0.6;");
            } else {
                row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 4; -fx-opacity: 1;");
            }
        });
        editRow.cantidadSpinner = cantSpinner;

        // Subtotal
        Label subtotalLbl = new Label("= $" + String.format("%.2f", item.getSubtotal()));
        subtotalLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        subtotalLbl.setPrefWidth(75);
        editRow.subtotalLabel = subtotalLbl;

        // Eliminar
        Button btnEliminar = new Button("X");
        btnEliminar.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-size: 9px; -fx-padding: 2 6;");
        btnEliminar.setOnAction(e -> {
            cantSpinner.getValueFactory().setValue(0);
        });

        row.getChildren().addAll(nombreLbl, btnCambiar, precioLbl, cantSpinner, subtotalLbl, btnEliminar);
        return row;
    }

    private void updateItemSubtotal(ItemEditRow edit) {
        BigDecimal precio = edit.nuevoVariante != null
            ? edit.nuevoVariante.getSalePrice()
            : edit.item.getUnitPrice();
        int cantidad = edit.cantidadSpinner.getValue();
        BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad));
        edit.subtotalLabel.setText("= $" + String.format("%.2f", subtotal));
    }

    private VBox createPaymentsSection() {
        VBox section = new VBox(8);

        Label title = new Label("METODOS DE PAGO");
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        section.getChildren().add(title);

        for (SalePayment payment : sale.getPayments()) {
            section.getChildren().add(createPaymentRow(payment));
        }

        return section;
    }

    private HBox createPaymentRow(SalePayment payment) {
        PaymentEditRow editRow = new PaymentEditRow(payment);
        paymentEdits.add(editRow);

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4));

        // Método de pago
        ComboBox<String> metodoCombo = new ComboBox<>();
        metodoCombo.getItems().addAll("Efectivo", "Tarjeta", "Transferencia");
        metodoCombo.setValue(payment.getPaymentMethodDisplayName());
        metodoCombo.setPrefWidth(120);
        editRow.metodoCombo = metodoCombo;

        // Monto
        TextField montoField = new TextField(String.format("%.2f", payment.getAmount()));
        montoField.setPrefWidth(100);
        montoField.textProperty().addListener((obs, old, newVal) -> updateTotal());
        editRow.montoField = montoField;

        row.getChildren().addAll(new Label("Metodo:"), metodoCombo, new Label("Monto: $"), montoField);
        return row;
    }

    private void updateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemEditRow edit : itemEdits) {
            if (edit.cantidadSpinner.getValue() > 0) {
                BigDecimal precio = edit.nuevoVariante != null
                    ? edit.nuevoVariante.getSalePrice()
                    : edit.item.getUnitPrice();
                total = total.add(precio.multiply(BigDecimal.valueOf(edit.cantidadSpinner.getValue())));
            }
        }
        totalLabel.setText("TOTAL: $" + String.format("%,.2f", total));
    }

    private ProductVariant showProductSearchDialog(String busquedaInicial) {
        Dialog<ProductVariant> dialog = new Dialog<>();
        dialog.setTitle("Cambiar Producto");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setPrefWidth(400);

        TextField searchField = new TextField();
        searchField.setPromptText("Buscar producto...");

        ListView<ProductVariant> listView = new ListView<>();
        listView.setPrefHeight(250);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ProductVariant item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                    item.getDisplayName() + " - $" + String.format("%.2f", item.getSalePrice()));
            }
        });

        // Buscar al escribir
        searchField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal.length() >= 2) {
                listView.setItems(FXCollections.observableArrayList(variantDAO.buscar(newVal, 30)));
            }
        });

        // Doble click para seleccionar
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                dialog.setResult(listView.getSelectionModel().getSelectedItem());
                dialog.close();
            }
        });

        content.getChildren().addAll(searchField, listView);
        dialog.getDialogPane().setContent(content);

        ButtonType seleccionarBtn = new ButtonType("Seleccionar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(seleccionarBtn, cancelarBtn);

        dialog.setResultConverter(btn ->
            btn == seleccionarBtn ? listView.getSelectionModel().getSelectedItem() : null);

        return dialog.showAndWait().orElse(null);
    }

    private boolean saveChanges() {
        try {
            var itemDAO = saleDAO.getItemDAO();

            // 1. Actualizar fecha
            if (!fechaEditada.equals(sale.getCreatedAt())) {
                saleDAO.updateDateTime(sale.getId(), fechaEditada);
            }

            // 2. Actualizar notas
            String notasNuevas = notasField.getText().trim();
            String notasOrig = sale.getNotes() != null ? sale.getNotes() : "";
            if (!notasNuevas.equals(notasOrig)) {
                saleDAO.updateNotes(sale.getId(), notasNuevas.isEmpty() ? null : notasNuevas);
            }

            // 3. Actualizar items
            BigDecimal nuevoTotal = BigDecimal.ZERO;
            for (ItemEditRow edit : itemEdits) {
                int cantidad = edit.cantidadSpinner.getValue();

                if (cantidad == 0) {
                    itemDAO.delete(edit.item.getId());
                } else if (edit.nuevoVariante != null) {
                    itemDAO.updateVariant(edit.item.getId(), edit.nuevoVariante.getId(),
                        edit.nuevoVariante.getSalePrice(), cantidad);
                    nuevoTotal = nuevoTotal.add(edit.nuevoVariante.getSalePrice().multiply(BigDecimal.valueOf(cantidad)));
                } else if (cantidad != edit.item.getQuantity()) {
                    BigDecimal subtotal = edit.item.getUnitPrice().multiply(BigDecimal.valueOf(cantidad));
                    itemDAO.updateQuantity(edit.item.getId(), cantidad, subtotal);
                    nuevoTotal = nuevoTotal.add(subtotal);
                } else {
                    nuevoTotal = nuevoTotal.add(edit.item.getSubtotal());
                }
            }

            // 4. Actualizar pagos (por ahora solo el monto)
            var paymentDAO = saleDAO.getPaymentDAO();
            for (PaymentEditRow edit : paymentEdits) {
                BigDecimal nuevoMonto = parseMonto(edit.montoField.getText());
                String nuevoMetodo = edit.metodoCombo.getValue();
                if (nuevoMonto.compareTo(edit.payment.getAmount()) != 0 ||
                    !nuevoMetodo.equals(edit.payment.getPaymentMethodDisplayName())) {
                    paymentDAO.update(edit.payment.getId(), convertMetodo(nuevoMetodo), nuevoMonto);
                }
            }

            // 5. Actualizar total
            saleDAO.updateTotal(sale.getId(), nuevoTotal);

            return true;
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se pudo guardar");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            return false;
        }
    }

    private BigDecimal parseMonto(String text) {
        try {
            return new BigDecimal(text.replace("$", "").replace(",", "").trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private SalePayment.PaymentMethod convertMetodo(String nombre) {
        return switch (nombre) {
            case "Tarjeta" -> SalePayment.PaymentMethod.DEBIT_CARD;
            case "Transferencia" -> SalePayment.PaymentMethod.TRANSFER;
            default -> SalePayment.PaymentMethod.CASH;
        };
    }

    // Clases internas para estado editable
    private static class ItemEditRow {
        final SaleItem item;
        ProductVariant nuevoVariante;
        Label nombreLabel;
        Label precioLabel;
        Spinner<Integer> cantidadSpinner;
        Label subtotalLabel;
        Button btnCambiar;

        ItemEditRow(SaleItem item) { this.item = item; }
    }

    private static class PaymentEditRow {
        final SalePayment payment;
        ComboBox<String> metodoCombo;
        TextField montoField;

        PaymentEditRow(SalePayment payment) { this.payment = payment; }
    }
}
