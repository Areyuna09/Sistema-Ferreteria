package com.ferreteria.controllers;

import com.ferreteria.models.Product;
import com.ferreteria.models.Category;
import com.ferreteria.models.dao.DatabaseConfig;
import com.ferreteria.models.dao.CategoryDAO;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la vista de Productos con pestañas.
 */
public class ProductsController {

    // Pestañas
    @FXML private TabPane productsTabPane;
    
    // Pestaña 1: Inventario (Solo lectura)
    @FXML private TableView<Product> inventoryTable;
    @FXML private TextField inventorySearchField;
    @FXML private ComboBox<String> inventoryCategoryFilter;
    @FXML private TableColumn<Product, String> inventoryCodeColumn;
    @FXML private TableColumn<Product, String> inventoryNameColumn;
    @FXML private TableColumn<Product, String> inventoryCategoryColumn;
    @FXML private TableColumn<Product, String> inventoryLocationColumn;
    @FXML private TableColumn<Product, BigDecimal> inventoryPriceColumn;
    @FXML private TableColumn<Product, Integer> inventoryStockColumn;
    @FXML private TableColumn<Product, String> inventoryStatusColumn;
    @FXML private Label inventoryCountLabel;
    @FXML private Label lowStockLabel;
    @FXML private Label totalValueLabel;
    
    // Pestaña 2: Gestión (CRUD completo)
    @FXML private TableView<Product> managementTable;
    @FXML private TextField managementSearchField;
    @FXML private ComboBox<String> managementCategoryFilter;
    @FXML private TableColumn<Product, String> managementCodeColumn;
    @FXML private TableColumn<Product, String> managementNameColumn;
    @FXML private TableColumn<Product, String> managementCategoryColumn;
    @FXML private TableColumn<Product, String> managementLocationColumn;
    @FXML private TableColumn<Product, BigDecimal> managementPriceColumn;
    @FXML private TableColumn<Product, Integer> managementStockColumn;
    @FXML private TableColumn<Product, Void> managementActionsColumn;
    
    // Generales
    @FXML private Label dateLabel;
    @FXML private NavbarController navbarController;

    @FXML
    public void initialize() {
        System.out.println("=== INICIALIZANDO PRODUCTSCONTROLLER CON PESTAÑAS ===");
        if (navbarController != null) {
            navbarController.setActiveView("productos");
        }
        setupDateLabel();
        
        // Configurar ambas pestañas
        setupInventoryTab();
        setupManagementTab();
        
        // Cargar datos iniciales
        loadCategories();
        loadInventoryData();
        loadManagementData();
        
        // Verificar si hay un parámetro para abrir pestaña específica
        String tabToOpen = System.getProperty("open.tab");
        if (tabToOpen != null) {
            System.out.println("=== ABRIENDO PESTAÑA ESPECÍFICA: " + tabToOpen + " ===");
            javafx.application.Platform.runLater(() -> {
                openTab(tabToOpen);
                // Limpiar la propiedad para que no afecte futuras cargas
                System.clearProperty("open.tab");
            });
        }
        
        System.out.println("=== PRODUCTSCONTROLLER CON PESTAÑAS INICIALIZADO ===");
    }

    private void setupInventoryTab() {
        System.out.println("=== CONFIGURANDO PESTAÑA DE INVENTARIO ===");
        
        // Configurar columnas de inventario
        inventoryCodeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        inventoryNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        inventoryCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        inventoryLocationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        inventoryPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        inventoryStockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        
        // Columna de estado (stock bajo/normal)
        inventoryStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Product product = getTableRow().getItem();
                    if (product != null) {
                        int stock = product.getStock();
                        int minStock = product.getMinStock();
                        
                        if (stock <= minStock) {
                            setText("⚠️ Stock Bajo");
                            setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        } else if (stock <= minStock * 2) {
                            setText("⚡ Stock Medio");
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        } else {
                            setText("✅ Stock Normal");
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        }
                    }
                }
            }
        });
        
        // Configurar búsqueda de inventario
        setupInventorySearch();
        
        System.out.println("=== PESTAÑA DE INVENTARIO CONFIGURADA ===");
    }

    private void setupManagementTab() {
        System.out.println("=== CONFIGURANDO PESTAÑA DE GESTIÓN ===");
        
        // Configurar columnas de gestión
        managementCodeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        managementNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        managementCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        managementLocationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        managementPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        managementStockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        
        // Configurar botones de acciones (CRUD)
        managementActionsColumn.setCellFactory(createActionCellFactory());
        
        // Configurar búsqueda de gestión
        setupManagementSearch();
        
        System.out.println("=== PESTAÑA DE GESTIÓN CONFIGURADA ===");
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

    private void loadCategories() {
        System.out.println("=== CARGANDO CATEGORÍAS PARA AMBAS PESTAÑAS ===");
        
        try {
            // Categorías para inventario
            if (inventoryCategoryFilter != null) {
                inventoryCategoryFilter.getItems().clear();
                inventoryCategoryFilter.getItems().add("Todas las categorías");
            }
            
            // Categorías para gestión
            if (managementCategoryFilter != null) {
                managementCategoryFilter.getItems().clear();
                managementCategoryFilter.getItems().add("Todas las categorías");
            }
            
            // Obtener categorías de la BD
            CategoryDAO categoryDAO = new CategoryDAO();
            List<Category> categories = categoryDAO.findAll();
            System.out.println("=== CATEGORÍAS OBTENIDAS DE BD: " + categories.size() + " ===");
            
            for (Category category : categories) {
                if (category != null && category.getNombre() != null && !category.getNombre().trim().isEmpty()) {
                    String categoryName = category.getNombre();
                    
                    // Agregar a ambos ComboBox
                    if (inventoryCategoryFilter != null) {
                        inventoryCategoryFilter.getItems().add(categoryName);
                    }
                    if (managementCategoryFilter != null) {
                        managementCategoryFilter.getItems().add(categoryName);
                    }
                    
                    System.out.println("=== AGREGANDO CATEGORÍA: '" + categoryName + "' (ID: " + category.getId() + ") ===");
                }
            }
            
            // Seleccionar "Todas las categorías" por defecto en ambos
            if (inventoryCategoryFilter != null) {
                inventoryCategoryFilter.getSelectionModel().selectFirst();
            }
            if (managementCategoryFilter != null) {
                managementCategoryFilter.getSelectionModel().selectFirst();
            }
            
            System.out.println("=== CATEGORÍAS CARGADAS EN AMBAS PESTAÑAS ===");
            
        } catch (Exception e) {
            System.err.println("ERROR cargando categorías: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== MÉTODOS PARA PESTAÑA DE INVENTARIO ====================
    
    private void loadInventoryData() {
        System.out.println("=== CARGANDO DATOS DE INVENTARIO ===");
        List<Product> products = getAllProducts();
        inventoryTable.getItems().setAll(products);
        updateInventoryStats(products);
        System.out.println("=== DATOS DE INVENTARIO CARGADOS: " + products.size() + " productos ===");
    }
    
    private void updateInventoryStats(List<Product> products) {
        int totalCount = products.size();
        int lowStockCount = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (Product product : products) {
            if (product.getStock() <= product.getMinStock()) {
                lowStockCount++;
            }
            totalValue = totalValue.add(product.getPrice().multiply(new BigDecimal(product.getStock())));
        }
        
        inventoryCountLabel.setText(totalCount + " productos");
        lowStockLabel.setText(lowStockCount + " con stock bajo");
        totalValueLabel.setText("Valor total: $" + totalValue.toString());
    }
    
    private void setupInventorySearch() {
        inventorySearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterInventory();
        });
    }
    
    private void filterInventory() {
        String searchTerm = inventorySearchField.getText().toLowerCase().trim();
        String selectedCategory = inventoryCategoryFilter.getValue();
        
        List<Product> allProducts = getAllProducts();
        List<Product> filteredProducts = new ArrayList<>();
        
        for (Product product : allProducts) {
            boolean matchesSearch = searchTerm.isEmpty() || 
                product.getCode().toLowerCase().contains(searchTerm) ||
                product.getName().toLowerCase().contains(searchTerm) ||
                product.getCategory().toLowerCase().contains(searchTerm);
                
            boolean matchesCategory = selectedCategory == null || 
                selectedCategory.equals("Todas las categorías") ||
                product.getCategory().equals(selectedCategory);
                
            if (matchesSearch && matchesCategory) {
                filteredProducts.add(product);
            }
        }
        
        inventoryTable.getItems().setAll(filteredProducts);
        updateInventoryStats(filteredProducts);
    }
    
    @FXML
    private void handleInventoryRefresh() {
        System.out.println("=== REFRESH DE INVENTARIO ===");
        loadCategories(); // Recargar categorías
        loadInventoryData();
    }
    
    @FXML
    private void handleInventoryFilterByCategory() {
        System.out.println("=== FILTRANDO INVENTARIO POR CATEGORÍA ===");
        filterInventory();
    }
    
    @FXML
    private void handleInventoryClearFilter() {
        System.out.println("=== LIMPIANDO FILTROS DE INVENTARIO ===");
        inventorySearchField.clear();
        inventoryCategoryFilter.getSelectionModel().selectFirst();
        filterInventory();
    }
    
    // ==================== MÉTODOS PARA PESTAÑA DE GESTIÓN ====================
    
    private void loadManagementData() {
        System.out.println("=== CARGANDO DATOS DE GESTIÓN ===");
        List<Product> products = getAllProducts();
        managementTable.getItems().setAll(products);
        System.out.println("=== DATOS DE GESTIÓN CARGADOS: " + products.size() + " productos ===");
    }
    
    private void setupManagementSearch() {
        managementSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterManagement();
        });
    }
    
    private void filterManagement() {
        String searchTerm = managementSearchField.getText().toLowerCase().trim();
        String selectedCategory = managementCategoryFilter.getValue();
        
        List<Product> allProducts = getAllProducts();
        List<Product> filteredProducts = new ArrayList<>();
        
        for (Product product : allProducts) {
            boolean matchesSearch = searchTerm.isEmpty() || 
                product.getCode().toLowerCase().contains(searchTerm) ||
                product.getName().toLowerCase().contains(searchTerm) ||
                product.getCategory().toLowerCase().contains(searchTerm);
                
            boolean matchesCategory = selectedCategory == null || 
                selectedCategory.equals("Todas las categorías") ||
                product.getCategory().equals(selectedCategory);
                
            if (matchesSearch && matchesCategory) {
                filteredProducts.add(product);
            }
        }
        
        managementTable.getItems().setAll(filteredProducts);
    }
    
    @FXML
    private void handleManagementRefresh() {
        System.out.println("=== REFRESH DE GESTIÓN ===");
        loadCategories(); // Recargar categorías
        loadManagementData();
    }
    
    @FXML
    private void handleManagementFilterByCategory() {
        System.out.println("=== FILTRANDO GESTIÓN POR CATEGORÍA ===");
        filterManagement();
    }
    
    @FXML
    private void handleManagementClearFilter() {
        System.out.println("=== LIMPIANDO FILTROS DE GESTIÓN ===");
        managementSearchField.clear();
        managementCategoryFilter.getSelectionModel().selectFirst();
        filterManagement();
    }
    
    // ==================== MÉTODOS CRUD ====================
    
    private Callback<TableColumn<Product, Void>, TableCell<Product, Void>> createActionCellFactory() {
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
    public void handleNewProduct() {
        System.out.println("Abriendo diálogo de nuevo producto...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/NewProductDialog.fxml"));
            Parent root = loader.load();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Nuevo Producto");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(managementTable.getScene().getWindow());
            dialogStage.setResizable(false);
            dialogStage.centerOnScreen();
            
            NewProductDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            
            dialogStage.showAndWait();
            
            // Si se guardó el producto, recargar ambas pestañas
            if (controller.isSaveClicked()) {
                System.out.println("=== PRODUCTO GUARDADO - RECARGANDO AMBAS PESTAÑAS ===");
                loadInventoryData();
                loadManagementData();
            }
            
        } catch (Exception e) {
            System.err.println("Error abriendo diálogo de nuevo producto: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir el formulario de nuevo producto: " + e.getMessage());
        }
    }

    private void handleViewProduct(Product product) {
        if (product == null) {
            showAlert("Información", "Por favor seleccione un producto para ver");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProductDetailsDialog.fxml"));
            Parent root = loader.load();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Detalles del Producto - " + product.getName());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(managementTable.getScene().getWindow());
            dialogStage.setResizable(false);
            dialogStage.centerOnScreen();
            
            ProductDetailsDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setProduct(product);
            
            dialogStage.showAndWait();
            
        } catch (Exception e) {
            System.err.println("Error abriendo diálogo de detalles: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir el diálogo de detalles: " + e.getMessage());
        }
    }

    private void handleEditProduct(Product product) {
        if (product == null) {
            showAlert("Información", "Por favor seleccione un producto para editar");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/NewProductDialog.fxml"));
            Parent root = loader.load();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Editar Producto");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(managementTable.getScene().getWindow());
            dialogStage.setResizable(false);
            dialogStage.centerOnScreen();
            
            NewProductDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setProduct(product);
            
            dialogStage.showAndWait();
            
            // Si se guardó el producto, recargar ambas pestañas
            if (controller.isSaveClicked()) {
                System.out.println("=== PRODUCTO GUARDADO - RECARGANDO AMBAS PESTAÑAS ===");
                loadInventoryData();
                loadManagementData();
            }
            
        } catch (Exception e) {
            System.err.println("Error abriendo diálogo de edición: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir el formulario de edición: " + e.getMessage());
        }
    }

    private void handleDeleteProduct(Product product) {
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
                
                stmt.close();
                conn.close();
                
                if (rowsAffected1 > 0 || rowsAffected2 > 0) {
                    showAlert("Éxito", "Producto eliminado correctamente");
                    loadInventoryData();
                    loadManagementData();
                } else {
                    showAlert("Error", "No se pudo eliminar el producto");
                }
                
            } catch (Exception e) {
                System.err.println("Error eliminando producto: " + e.getMessage());
                showAlert("Error", "No se pudo eliminar el producto: " + e.getMessage());
            }
        }
    }
    
    // ==================== MÉTODO UTILIDAD ====================
    
    private List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        try {
            var conn = DatabaseConfig.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("""
                SELECT p.*, pv.sale_price, pv.cost_price, pv.stock, pv.min_stock,
                       c.name as category_name
                FROM products p
                JOIN product_variants pv ON p.id = pv.product_id
                LEFT JOIN categories c ON p.category_id = c.id
                WHERE p.active = 1 AND pv.active = 1
                ORDER BY p.name
                """);
            
            while (rs.next()) {
                Product product = new Product.Builder()
                    .id(rs.getInt("id"))
                    .code(rs.getString("code"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .category(rs.getString("category_name"))
                    .location(rs.getString("location"))
                    .price(rs.getBigDecimal("sale_price"))
                    .cost(rs.getBigDecimal("cost_price"))
                    .stock(rs.getInt("stock"))
                    .minStock(rs.getInt("min_stock"))
                    .active(rs.getBoolean("active"))
                    .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                    .build();
                products.add(product);
            }
            
            rs.close();
            stmt.close();
            conn.close();
            
        } catch (Exception e) {
            System.err.println("Error cargando productos: " + e.getMessage());
            e.printStackTrace();
        }
        return products;
    }
    
    // ==================== MÉTODO PARA ABRIR PESTAÑA ESPECÍFICA ====================
    
    /**
     * Abre una pestaña específica. Usado por el Dashboard.
     * @param tabName "inventory" para inventario, "management" para gestión
     */
    public void openTab(String tabName) {
        if ("inventory".equals(tabName)) {
            productsTabPane.getSelectionModel().select(0); // Primera pestaña
            System.out.println("=== ABIERTA PESTAÑA DE INVENTARIO ===");
        } else if ("management".equals(tabName)) {
            productsTabPane.getSelectionModel().select(1); // Segunda pestaña
            System.out.println("=== ABIERTA PESTAÑA DE GESTIÓN ===");
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
