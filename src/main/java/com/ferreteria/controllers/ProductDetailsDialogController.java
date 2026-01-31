package com.ferreteria.controllers;

import com.ferreteria.models.Product;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Controlador para el diálogo de detalles de producto.
 */
public class ProductDetailsDialogController {

    @FXML private Label codeLabel;
    @FXML private Label nameLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label categoryLabel;
    @FXML private Label locationLabel;
    @FXML private Label priceLabel;
    @FXML private Label costLabel;
    @FXML private Label profitLabel;
    @FXML private Label stockLabel;
    @FXML private Label stockStatus;
    @FXML private Label minStockLabel;
    @FXML private Label idLabel;
    @FXML private Label activeLabel;
    @FXML private Label createdAtLabel;

    private Stage dialogStage;
    private Product product;

    @FXML
    public void initialize() {
        // Inicialización si es necesaria
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setProduct(Product product) {
        this.product = product;
        updateFields();
    }

    private void updateFields() {
        if (product == null) return;

        // Información básica
        codeLabel.setText(product.getCode() != null ? product.getCode() : "N/A");
        nameLabel.setText(product.getName() != null ? product.getName() : "N/A");
        descriptionLabel.setText(product.getDescription() != null && !product.getDescription().isEmpty() 
            ? product.getDescription() : "Sin descripción");
        categoryLabel.setText(product.getCategory() != null ? product.getCategory() : "N/A");
        locationLabel.setText(product.getLocation() != null ? product.getLocation() : "N/A");

        // Información financiera
        priceLabel.setText("$" + product.getPrice().toString());
        costLabel.setText("$" + product.getCost().toString());
        
        BigDecimal profit = product.getProfit();
        profitLabel.setText("$" + profit.toString());
        
        // Colorear la ganancia según sea positiva o negativa
        if (profit.compareTo(BigDecimal.ZERO) > 0) {
            profitLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        } else if (profit.compareTo(BigDecimal.ZERO) < 0) {
            profitLabel.setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
        } else {
            profitLabel.setStyle("-fx-text-fill: #666; -fx-font-weight: normal;");
        }

        // Información de inventario
        stockLabel.setText(String.valueOf(product.getStock()));
        minStockLabel.setText(String.valueOf(product.getMinStock()));

        // Estado del stock
        if (product.isLowStock()) {
            stockStatus.setText("⚠️ Stock Bajo");
            stockStatus.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;");
        } else {
            stockStatus.setText("✅ Stock Normal");
            stockStatus.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: normal;");
        }

        // Información del sistema
        idLabel.setText(String.valueOf(product.getId()));
        activeLabel.setText(product.isActive() ? "✅ Activo" : "❌ Inactivo");
        activeLabel.setStyle(product.isActive() 
            ? "-fx-text-fill: #4CAF50;" 
            : "-fx-text-fill: #f44336;");

        // Fecha de creación
        if (product.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            createdAtLabel.setText(product.getCreatedAt().format(formatter));
        } else {
            createdAtLabel.setText("N/A");
        }
    }

    @FXML
    public void handleClose() {
        dialogStage.close();
    }
}
