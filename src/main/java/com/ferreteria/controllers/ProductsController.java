package com.ferreteria.controllers;

import com.ferreteria.models.Product;
import com.ferreteria.models.dao.DatabaseConfig;
import com.ferreteria.utils.SessionManager;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la vista de Productos.
 */
public class ProductsController {

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, Integer> idColumn;
    @FXML private TableColumn<Product, String> codeColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, BigDecimal> priceColumn;
    @FXML private TableColumn<Product, Integer> stockColumn;
    @FXML private TableColumn<Product, Void> actionsColumn;

    @FXML
    public void initialize() {
        setupTableColumns();
        loadProducts();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));

        // Formatear precio
        priceColumn.setCellFactory(column -> new TableCell<Product, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText("$" + price.toString());
                }
            }
        });

        // Columna de acciones con botones Editar y Eliminar
        actionsColumn.setCellFactory(createActionsCellFactory());
    }

    private Callback<TableColumn<Product, Void>, TableCell<Product, Void>> createActionsCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<Product, Void> call(final TableColumn<Product, Void> param) {
                return new TableCell<>() {
                    private final Button editButton = new Button("Editar");
                    private final Button deleteButton = new Button("Eliminar");
                    private final HBox buttonsContainer = new HBox(5, editButton, deleteButton);

                    {
                        // Estilo de los botones
                        editButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 5 10;");
                        deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 5 10;");

                        // Acción del botón Editar
                        editButton.setOnAction(event -> {
                            Product product = getTableView().getItems().get(getIndex());
                            handleEditProduct(product);
                        });

                        // Acción del botón Eliminar
                        deleteButton.setOnAction(event -> {
                            Product product = getTableView().getItems().get(getIndex());
                            handleDeleteProduct(product);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(buttonsContainer);
                        }
                    }
                };
            }
        };
    }

    private void loadProducts() {
        List<Product> products = new ArrayList<>();
        
        try {
            var conn = DatabaseConfig.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            
            ResultSet rs = stmt.executeQuery("""
                SELECT id, code, name, description, category, price, cost, 
                       stock, min_stock, location, active, created_at 
                FROM products 
                WHERE active = 1 
                ORDER BY name
                """);
            
            while (rs.next()) {
                Product product = new Product.Builder()
                    .id(rs.getInt("id"))
                    .code(rs.getString("code"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .category(rs.getString("category"))
                    .price(rs.getBigDecimal("price"))
                    .cost(rs.getBigDecimal("cost"))
                    .stock(rs.getInt("stock"))
                    .minStock(rs.getInt("min_stock"))
                    .location(rs.getString("location"))
                    .active(rs.getBoolean("active"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .build();
                
                products.add(product);
            }
            
            productsTable.getItems().setAll(products);
            
        } catch (Exception e) {
            System.err.println("Error cargando productos: " + e.getMessage());
            showAlert("Error", "No se pudieron cargar los productos: " + e.getMessage());
        }
    }

    @FXML
    public void handleEditProduct(Product product) {
        if (product == null) {
            showAlert("Información", "Por favor seleccione un producto para editar");
            return;
        }
        
        showAlert("Editar Producto", "Función de editar producto en desarrollo.\nProducto seleccionado: " + product.getName());
        // TODO: Implementar ventana de edición de producto
    }

    @FXML
    public void handleDeleteProduct(Product product) {
        if (product == null) {
            showAlert("Información", "Por favor seleccione un producto para eliminar");
            return;
        }
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmar Eliminación");
        confirmDialog.setHeaderText("¿Está seguro de eliminar este producto?");
        confirmDialog.setContentText("Producto: " + product.getName() + "\n\nEsta acción no se puede deshacer.");
        
        if (confirmDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                var conn = DatabaseConfig.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                
                // Marcar como inactivo en lugar de eliminar físicamente
                String sql = "UPDATE products SET active = 0 WHERE id = " + product.getId();
                int rowsAffected = stmt.executeUpdate(sql);
                
                if (rowsAffected > 0) {
                    showAlert("Éxito", "Producto eliminado correctamente");
                    loadProducts(); // Recargar la tabla
                } else {
                    showAlert("Error", "No se pudo eliminar el producto");
                }
                
            } catch (Exception e) {
                System.err.println("Error eliminando producto: " + e.getMessage());
                showAlert("Error", "No se pudo eliminar el producto: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleNewProduct() {
        showAlert("Nuevo Producto", "Función de nuevo producto en desarrollo");
        // TODO: Implementar ventana de nuevo producto
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

            Stage stage = (Stage) productsTable.getScene().getWindow();
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
}
