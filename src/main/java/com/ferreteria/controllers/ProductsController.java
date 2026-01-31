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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la vista de Productos.
 */
public class ProductsController {

    @FXML private TableView<Product> productsTable;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private TableColumn<Product, String> codeColumn;
    @FXML private TableColumn<Product, String> nameColumn;
    @FXML private TableColumn<Product, String> categoryColumn;
    @FXML private TableColumn<Product, String> locationColumn;
    @FXML private TableColumn<Product, BigDecimal> priceColumn;
    @FXML private TableColumn<Product, Integer> stockColumn;
    @FXML private TableColumn<Product, Void> actionsColumn;
    @FXML private Label dateLabel;
    @FXML private NavbarController navbarController;

    @FXML
    public void initialize() {
        System.out.println("=== INICIALIZANDO PRODUCTSCONTROLLER ===");
        if (navbarController != null) {
            navbarController.setActiveView("productos");
        }
        setupDateLabel();
        setupTableColumns();
        setupSearchField();
        loadProducts();
        System.out.println("=== PRODUCTSCONTROLLER INICIALIZADO ===");
    }

    private void setupDateLabel() {
        if (dateLabel != null) {
            try {
                // Zona horaria de San Juan, Argentina
                ZoneId sanJuanZone = ZoneId.of("America/Argentina/San_Juan");
                ZonedDateTime sanJuanDateTime = ZonedDateTime.now(sanJuanZone);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy - HH:mm");
                String formattedDateTime = sanJuanDateTime.format(formatter);

                dateLabel.setText("San Juan, Argentina | " + formattedDateTime);
            } catch (Exception e) {
                // Fallback a fecha local
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                dateLabel.setText(LocalDateTime.now().format(formatter));
            }
        }
    }

    private void setupTableColumns() {
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
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

        // Columna de acciones con botones Ver, Editar y Eliminar
        actionsColumn.setCellFactory(createActionsCellFactory());
    }
    
    private void setupSearchField() {
        // Configurar el listener para búsqueda en tiempo real
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts(newValue);
        });
    }
    
    private void filterProducts(String searchText) {
        List<Product> allProducts = (List<Product>) productsTable.getUserData();
        if (allProducts == null) {
            allProducts = new ArrayList<>();
            productsTable.setUserData(allProducts);
        }
        
        if (searchText == null || searchText.trim().isEmpty()) {
            productsTable.getItems().setAll(allProducts);
            return;
        }
        
        String lowerSearchText = searchText.toLowerCase().trim();
        List<Product> filteredProducts = new ArrayList<>();
        
        for (Product product : allProducts) {
            boolean codeMatch = product.getCode() != null && product.getCode().toLowerCase().contains(lowerSearchText);
            boolean nameMatch = product.getName() != null && product.getName().toLowerCase().contains(lowerSearchText);
            boolean categoryMatch = product.getCategory() != null && product.getCategory().toLowerCase().contains(lowerSearchText);
            boolean locationMatch = product.getLocation() != null && product.getLocation().toLowerCase().contains(lowerSearchText);

            if (codeMatch || nameMatch || categoryMatch || locationMatch) {
                filteredProducts.add(product);
            }
        }
        
        productsTable.getItems().setAll(filteredProducts);
    }

    private Callback<TableColumn<Product, Void>, TableCell<Product, Void>> createActionsCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<Product, Void> call(final TableColumn<Product, Void> param) {
                return new TableCell<>() {
                    private final Button viewButton = new Button("Ver");
                    private final Button editButton = new Button("Editar");
                    private final Button deleteButton = new Button("Eliminar");
                    private final HBox buttonsContainer = new HBox(3, viewButton, editButton, deleteButton);

                    {
                        // Estilo de los botones
                        viewButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 11px;");
                        editButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 11px;");
                        deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 11px;");

                        // Acción del botón Ver
                        viewButton.setOnAction(event -> {
                            Product product = getTableView().getItems().get(getIndex());
                            handleViewProduct(product);
                        });

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

    @FXML
    public void loadProducts() {
        System.out.println("Cargando productos iniciales...");
        List<Product> products = new ArrayList<>();
        
        try {
            var conn = DatabaseConfig.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            
            ResultSet rs = stmt.executeQuery("""
                SELECT p.id, p.code, p.name, p.description, p.category_id, c.name as category,
                       COALESCE(pv.sale_price, 0) as price, COALESCE(pv.cost_price, 0) as cost, 
                       COALESCE(pv.stock, 0) as stock, COALESCE(pv.min_stock, 5) as min_stock,
                       p.location, p.active, p.created_at 
                FROM products p
                LEFT JOIN categories c ON p.category_id = c.id
                LEFT JOIN product_variants pv ON p.id = pv.product_id AND pv.active = 1
                WHERE p.active = 1
                ORDER BY p.name
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
            
            productsTable.setUserData(products);
            productsTable.getItems().setAll(products);
            System.out.println("Productos iniciales cargados: " + products.size());
            
        } catch (Exception e) {
            System.err.println("Error cargando productos iniciales: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRefresh() {
        System.out.println("=== BOTÓN REFRESH PRESIONADO ===");
        
        // Mostrar mensaje inmediato en la tabla
        productsTable.setPlaceholder(new javafx.scene.control.Label("🔄 Refrescando..."));
        
        // Forzar un refresh inmediato de la UI
        productsTable.refresh();
        
        // Ejecutar en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                System.out.println("Iniciando carga de productos...");
                
                var conn = DatabaseConfig.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                
                ResultSet rs = stmt.executeQuery("""
                    SELECT p.id, p.code, p.name, p.description, p.category_id, c.name as category,
                           COALESCE(pv.sale_price, 0) as price, COALESCE(pv.cost_price, 0) as cost, 
                           COALESCE(pv.stock, 0) as stock, COALESCE(pv.min_stock, 5) as min_stock,
                           p.location, p.active, p.created_at 
                    FROM products p
                    LEFT JOIN categories c ON p.category_id = c.id
                    LEFT JOIN product_variants pv ON p.id = pv.product_id AND pv.active = 1
                    WHERE p.active = 1
                    ORDER BY p.name
                    """);
                
                List<Product> products = new ArrayList<>();
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
                
                System.out.println("Se encontraron " + products.size() + " productos");
                
                // Actualizar la UI en el hilo de JavaFX
                javafx.application.Platform.runLater(() -> {
                    try {
                        System.out.println("Actualizando UI en hilo JavaFX...");
                        
                        // Limpiar completamente la tabla
                        productsTable.getItems().clear();
                        
                        // Agregar los nuevos productos
                        productsTable.getItems().setAll(products);
                        
                        // Actualizar userData para el filtrado
                        productsTable.setUserData(products);
                        
                        // Quitar el placeholder
                        productsTable.setPlaceholder(null);
                        
                        // Forzar refresh de la tabla
                        productsTable.refresh();
                        
                        // MOVER LA TABLA AL INICIO
                        productsTable.scrollTo(0);
                        
                        // También hacer scroll del contenedor si es un ScrollPane
                        if (productsTable.getParent() instanceof javafx.scene.control.ScrollPane) {
                            ((javafx.scene.control.ScrollPane) productsTable.getParent()).setVvalue(0);
                        }
                        
                        System.out.println("Tabla actualizada con " + productsTable.getItems().size() + " productos y movida al inicio");
                        
                        // Mostrar alerta para confirmar visualmente
                        showAlert("Recarga Completada", "Se recargaron " + products.size() + " productos correctamente");
                        
                    } catch (Exception e) {
                        System.err.println("Error actualizando UI: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                
            } catch (Exception e) {
                System.err.println("Error en handleRefresh: " + e.getMessage());
                e.printStackTrace();
                
                javafx.application.Platform.runLater(() -> {
                    productsTable.setPlaceholder(new javafx.scene.control.Label("❌ Error al recargar"));
                    showAlert("Error", "No se pudieron recargar los productos: " + e.getMessage());
                });
            }
        }).start();
        
        System.out.println("=== FIN HANDLE REFRESH ===");
    }

    @FXML
    public void handleFilterByCategory() {
        System.out.println("ProductsController.handleFilterByCategory() LLAMADO");
        // Implementación básica del filtro
        loadProducts();
    }

    @FXML
    public void handleClearFilter() {
        System.out.println("ProductsController.handleClearFilter() LLAMADO");
        if (categoryFilter != null) {
            categoryFilter.getSelectionModel().clearSelection();
        }
        loadProducts();
    }

    @FXML
    public void handleViewProduct(Product product) {
        if (product == null) {
            showAlert("Información", "Por favor seleccione un producto para ver");
            return;
        }
        
        try {
            // Cargar el diálogo de detalles
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProductDetailsDialog.fxml"));
            Parent root = loader.load();
            
            // Crear el escenario del diálogo
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Detalles del Producto - " + product.getName());
            Scene scene = new Scene(root);
            
            // Cargar los estilos CSS
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(productsTable.getScene().getWindow());
            dialogStage.setResizable(false);
            dialogStage.centerOnScreen();
            
            // Obtener el controlador y configurarlo
            ProductDetailsDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setProduct(product);
            
            // Mostrar el diálogo
            dialogStage.showAndWait();
            
        } catch (Exception e) {
            System.err.println("Error abriendo diálogo de detalles: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir el diálogo de detalles: " + e.getMessage());
        }
    }

    @FXML
    public void handleEditProduct(Product product) {
        if (product == null) {
            showAlert("Información", "Por favor seleccione un producto para editar");
            return;
        }
        
        try {
            // Cargar el diálogo
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/NewProductDialog.fxml"));
            Parent root = loader.load();
            
            // Crear el escenario del diálogo
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Editar Producto");
            Scene scene = new Scene(root);
            
            // Cargar los estilos CSS
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(productsTable.getScene().getWindow());
            dialogStage.setResizable(false);
            dialogStage.centerOnScreen();
            
            // Obtener el controlador y configurarlo para edición
            NewProductDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setProduct(product); // Pasar el producto a editar
            
            // Mostrar el diálogo y esperar a que se cierre
            dialogStage.showAndWait();
            
            // Si se guardó el producto, recargar la tabla
            if (controller.isSaveClicked()) {
                loadProducts();
            }
            
        } catch (Exception e) {
            System.err.println("Error abriendo diálogo de edición: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir el formulario de edición: " + e.getMessage());
        }
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
                
                // Marcar como inactivo en ambas tablas
                String sql = "UPDATE products SET active = 0 WHERE id = " + product.getId();
                int rowsAffected1 = stmt.executeUpdate(sql);
                
                sql = "UPDATE product_variants SET active = 0 WHERE product_id = " + product.getId();
                int rowsAffected2 = stmt.executeUpdate(sql);
                
                if (rowsAffected1 > 0 || rowsAffected2 > 0) {
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
        System.out.println("Abriendo diálogo de nuevo producto...");
        try {
            // Cargar el diálogo
            System.out.println("Cargando FXML: /views/NewProductDialog.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/NewProductDialog.fxml"));
            if (loader.getLocation() == null) {
                System.err.println("ERROR: No se encontró el archivo FXML");
                showAlert("Error", "No se encontró el archivo del formulario");
                return;
            }
            
            Parent root = loader.load();
            System.out.println("FXML cargado correctamente");
            
            // Crear el escenario del diálogo
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Nuevo Producto");
            Scene scene = new Scene(root);
            
            // Cargar los estilos CSS
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(productsTable.getScene().getWindow());
            dialogStage.setResizable(false);
            dialogStage.centerOnScreen();
            
            // Obtener el controlador y configurarlo
            NewProductDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            
            System.out.println("Mostrando diálogo...");
            // Mostrar el diálogo y esperar a que se cierre
            dialogStage.showAndWait();
            
            // Si se guardó el producto, recargar la tabla
            if (controller.isSaveClicked()) {
                loadProducts();
            }
            
        } catch (Exception e) {
            System.err.println("Error abriendo diálogo de nuevo producto: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir el formulario de nuevo producto: " + e.getMessage());
        }
    }

    // Navegación manejada por NavbarController

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
