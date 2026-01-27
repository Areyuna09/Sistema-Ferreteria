package com.ferreteria.controllers;

import com.ferreteria.models.Product;
import com.ferreteria.models.dao.DatabaseConfig;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador del diálogo para crear/editar productos.
 */
public class NewProductDialogController {

    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField locationField;
    @FXML private TextField priceField;
    @FXML private TextField costField;
    @FXML private TextField stockField;
    @FXML private TextField minStockField;
    @FXML private TextArea descriptionArea;

    private Stage dialogStage;
    private boolean saveClicked = false;
    private Product editingProduct;

    @FXML
    public void initialize() {
        System.out.println("Inicializando NewProductDialogController...");
        try {
            loadCategories();
            setupNumericFields();
            System.out.println("NewProductDialogController inicializado correctamente");
        } catch (Exception e) {
            System.err.println("Error inicializando NewProductDialogController: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCategories() {
        System.out.println("Cargando categorías...");
        categoryComboBox.getItems().clear();
        
        try {
            var conn = DatabaseConfig.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM categories WHERE active = 1 ORDER BY name");
            
            List<String> categories = new ArrayList<>();
            while (rs.next()) {
                categories.add(rs.getString("name"));
            }
            
            System.out.println("Se encontraron " + categories.size() + " categorías en la base de datos");
            categoryComboBox.getItems().addAll(categories);
            
            // Si no hay categorías, agregar algunas por defecto
            if (categories.isEmpty()) {
                System.out.println("No hay categorías en la BD, agregando categorías por defecto");
                categoryComboBox.getItems().addAll(
                    "Herramientas", "Electricidad", "Fontanería", 
                    "Jardinería", "Pintura", "Construcción", "Otros"
                );
            }
            
        } catch (Exception e) {
            System.err.println("Error cargando categorías: " + e.getMessage());
            e.printStackTrace();
            // Agregar categorías por defecto en caso de error
            System.out.println("Agregando categorías por defecto debido a error");
            categoryComboBox.getItems().addAll(
                "Herramientas", "Electricidad", "Fontanería", 
                "Jardinería", "Pintura", "Construcción", "Otros"
            );
        }
    }

    private void setupNumericFields() {
        // Solo permitir números y punto decimal en campos de precio
        priceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                priceField.setText(oldVal);
            }
        });

        costField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                costField.setText(oldVal);
            }
        });

        // Solo permitir números enteros en campos de stock
        stockField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                stockField.setText(oldVal);
            }
        });

        minStockField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                minStockField.setText(oldVal);
            }
        });
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setProduct(Product product) {
        this.editingProduct = product;
        
        if (product != null) {
            // Modo edición - cargar datos del producto
            codeField.setText(product.getCode());
            nameField.setText(product.getName());
            categoryComboBox.setValue(product.getCategory());
            locationField.setText(product.getLocation());
            priceField.setText(product.getPrice().toString());
            costField.setText(product.getCost().toString());
            stockField.setText(String.valueOf(product.getStock()));
            minStockField.setText(String.valueOf(product.getMinStock()));
            descriptionArea.setText(product.getDescription());
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handleSave() {
        if (isInputValid()) {
            saveClicked = true;
            
            try {
                if (editingProduct != null) {
                    updateProduct();
                } else {
                    createProduct();
                }
                
                dialogStage.close();
            } catch (Exception e) {
                showAlert("Error", "No se pudo guardar el producto: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";

        // Validar código único
        String code = codeField.getText().trim();
        if (!code.isEmpty() && isCodeDuplicate(code)) {
            errorMessage += "El código '" + code + "' ya existe en otro producto.\n";
        }

        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errorMessage += "El nombre del producto es requerido.\n";
        }

        if (priceField.getText() == null || priceField.getText().trim().isEmpty()) {
            errorMessage += "El precio de venta es requerido.\n";
        } else {
            try {
                BigDecimal price = new BigDecimal(priceField.getText());
                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    errorMessage += "El precio de venta no puede ser negativo.\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "El precio de venta debe ser un número válido.\n";
            }
        }

        if (stockField.getText() == null || stockField.getText().trim().isEmpty()) {
            errorMessage += "El stock inicial es requerido.\n";
        } else {
            try {
                int stock = Integer.parseInt(stockField.getText());
                if (stock < 0) {
                    errorMessage += "El stock no puede ser negativo.\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "El stock debe ser un número entero válido.\n";
            }
        }

        if (costField.getText() != null && !costField.getText().trim().isEmpty()) {
            try {
                BigDecimal cost = new BigDecimal(costField.getText());
                if (cost.compareTo(BigDecimal.ZERO) < 0) {
                    errorMessage += "El precio de costo no puede ser negativo.\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "El precio de costo debe ser un número válido.\n";
            }
        }

        if (minStockField.getText() != null && !minStockField.getText().trim().isEmpty()) {
            try {
                int minStock = Integer.parseInt(minStockField.getText());
                if (minStock < 0) {
                    errorMessage += "El stock mínimo no puede ser negativo.\n";
                }
            } catch (NumberFormatException e) {
                errorMessage += "El stock mínimo debe ser un número entero válido.\n";
            }
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            showAlert("Campos Inválidos", errorMessage);
            return false;
        }
    }

    private void createProduct() throws Exception {
        var conn = DatabaseConfig.getInstance().getConnection();
        
        // Primero, obtener o crear la categoría
        int categoryId = getOrCreateCategory(categoryComboBox.getValue());
        
        // Insertar producto
        String productSql = """
            INSERT INTO products (code, name, description, category_id, location, active, created_at)
            VALUES (?, ?, ?, ?, ?, 1, datetime('now', 'localtime'))
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(productSql, Statement.RETURN_GENERATED_KEYS)) {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                pstmt.setNull(1, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(1, code);
            }
            pstmt.setString(2, nameField.getText().trim());
            pstmt.setString(3, descriptionArea.getText().trim());
            pstmt.setInt(4, categoryId);
            pstmt.setString(5, locationField.getText().trim());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new Exception("No se pudo crear el producto");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int productId = generatedKeys.getInt(1);

                    // Insertar variante del producto
                    String variantSql = """
                        INSERT INTO product_variants (product_id, variant_name, sale_price, cost_price, stock, min_stock, active, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, 1, datetime('now', 'localtime'))
                        """;

                    try (PreparedStatement variantStmt = conn.prepareStatement(variantSql)) {
                        variantStmt.setInt(1, productId);
                        variantStmt.setString(2, "Estándar");
                        variantStmt.setBigDecimal(3, new BigDecimal(priceField.getText()));
                        variantStmt.setBigDecimal(4, costField.getText().trim().isEmpty() ?
                            BigDecimal.ZERO : new BigDecimal(costField.getText()));
                        variantStmt.setInt(5, Integer.parseInt(stockField.getText()));
                        variantStmt.setInt(6, minStockField.getText().trim().isEmpty() ?
                            5 : Integer.parseInt(minStockField.getText()));

                        variantStmt.executeUpdate();
                    }
                }
            }
        }
        
        showAlert("Éxito", "Producto creado correctamente");
    }

    private void updateProduct() throws Exception {
        var conn = DatabaseConfig.getInstance().getConnection();
        
        // Actualizar producto
        String productSql = """
            UPDATE products SET 
                code = ?, name = ?, description = ?, 
                category_id = ?, location = ?
            WHERE id = ?
            """;
        
        int categoryId = getOrCreateCategory(categoryComboBox.getValue());
        String code = codeField.getText().trim();

        try (PreparedStatement pstmt = conn.prepareStatement(productSql)) {
            if (code.isEmpty()) {
                pstmt.setNull(1, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(1, code);
            }
            pstmt.setString(2, nameField.getText().trim());
            pstmt.setString(3, descriptionArea.getText().trim());
            pstmt.setInt(4, categoryId);
            pstmt.setString(5, locationField.getText().trim());
            pstmt.setInt(6, editingProduct.getId());

            pstmt.executeUpdate();
        }
        
        // Actualizar variante del producto
        String variantSql = """
            UPDATE product_variants SET 
                sale_price = ?, cost_price = ?, 
                stock = ?, min_stock = ?
            WHERE product_id = ?
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(variantSql)) {
            pstmt.setBigDecimal(1, new BigDecimal(priceField.getText()));
            pstmt.setBigDecimal(2, costField.getText().trim().isEmpty() ? 
                BigDecimal.ZERO : new BigDecimal(costField.getText()));
            pstmt.setInt(3, Integer.parseInt(stockField.getText()));
            pstmt.setInt(4, minStockField.getText().trim().isEmpty() ? 
                5 : Integer.parseInt(minStockField.getText()));
            pstmt.setInt(5, editingProduct.getId());
            
            pstmt.executeUpdate();
        }
        
        showAlert("Éxito", "Producto actualizado correctamente");
    }

    private int getOrCreateCategory(String categoryName) throws Exception {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return 1; // Categoría por defecto
        }
        
        var conn = DatabaseConfig.getInstance().getConnection();
        
        // Buscar categoría existente
        String selectSql = "SELECT id FROM categories WHERE name = ? AND active = 1";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, categoryName.trim());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        
        // Crear nueva categoría si no existe
        String insertSql = "INSERT INTO categories (name, active, created_at) VALUES (?, 1, datetime('now', 'localtime'))";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, categoryName.trim());
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }
        
        return 1; // Categoría por defecto si todo falla
    }

    private boolean isCodeDuplicate(String code) {
        try {
            var conn = DatabaseConfig.getInstance().getConnection();
            String sql = "SELECT id FROM products WHERE code = ? AND active = 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, code);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int existingId = rs.getInt("id");
                    // Si estamos editando, permitir el mismo código del producto actual
                    if (editingProduct != null && editingProduct.getId() == existingId) {
                        return false;
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Error verificando código duplicado: " + e.getMessage());
        }
        return false;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
