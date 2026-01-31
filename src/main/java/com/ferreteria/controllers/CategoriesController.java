package com.ferreteria.controllers;

import com.ferreteria.models.Category;
import com.ferreteria.models.Subcategory;
import com.ferreteria.models.dao.CategoryDAO;
import com.ferreteria.models.dao.SubcategoryDAO;
import com.ferreteria.utils.SessionManager;
import com.ferreteria.utils.AppLogger;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para la gestión de categorías y subcategorías.
 * Implementado desde cero basado en la estructura de Omar.
 */
public class CategoriesController {
    
    private static final Logger LOGGER = Logger.getLogger(CategoriesController.class.getName());
    
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final SubcategoryDAO subcategoryDAO = new SubcategoryDAO();
    
    // Datos
    private ObservableList<Category> categoriesList = FXCollections.observableArrayList();
    private ObservableList<Subcategory> subcategoriesList = FXCollections.observableArrayList();
    
    // Estado de edición
    private Category editingCategory = null;
    private Subcategory editingSubcategory = null;
    
    // FXML - Formulario de Categorías
    @FXML private Label categoryFormTitle;
    @FXML private TextField categoryNameField;
    @FXML private TextField categoryDescField;
    @FXML private Button saveCategoryBtn;
    @FXML private Button cancelCategoryBtn;
    
    // FXML - Búsqueda de Categorías
    @FXML private TextField categorySearchField;
    @FXML private Label categoryCountLabel;
    
    // FXML - Tabla de Categorías
    @FXML private TableView<Category> categoriesTable;
    @FXML private TableColumn<Category, Integer> catIdColumn;
    @FXML private TableColumn<Category, String> catNameColumn;
    @FXML private TableColumn<Category, String> catDescColumn;
    @FXML private TableColumn<Category, Integer> catSubcatCountColumn;
    @FXML private TableColumn<Category, Integer> catProductCountColumn;
    @FXML private TableColumn<Category, Void> catActionsColumn;
    
    // FXML - Formulario de Subcategorías
    @FXML private Label subcategoryFormTitle;
    @FXML private ComboBox<Category> parentCategoryCombo;
    @FXML private TextField subcategoryNameField;
    @FXML private TextField subcategoryDescField;
    @FXML private Button saveSubcategoryBtn;
    @FXML private Button cancelSubcategoryBtn;
    
    // FXML - Búsqueda de Subcategorías
    @FXML private ComboBox<Category> filterCategoryCombo;
    @FXML private TextField subcategorySearchField;
    @FXML private Label subcategoryCountLabel;
    
    // FXML - Tabla de Subcategorías
    @FXML private TableView<Subcategory> subcategoriesTable;
    @FXML private TableColumn<Subcategory, Integer> subIdColumn;
    @FXML private TableColumn<Subcategory, String> subCategoryColumn;
    @FXML private TableColumn<Subcategory, String> subNameColumn;
    @FXML private TableColumn<Subcategory, String> subDescColumn;
    @FXML private TableColumn<Subcategory, Integer> subProductCountColumn;
    @FXML private TableColumn<Subcategory, Void> subActionsColumn;
    
    @FXML
    public void initialize() {
        AppLogger.info("CATEGORIAS", "CategoriesController inicializado");
        
        setupCategoriesTable();
        setupSubcategoriesTable();
        setupCategoryComboBox();
        
        loadCategories();
        loadSubcategories();
    }
    
    private void setupCategoriesTable() {
        // Configurar columnas
        catIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        catNameColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        catDescColumn.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getDescripcion() != null ? param.getValue().getDescripcion() : ""));
        
        // Columnas calculadas
        catSubcatCountColumn.setCellValueFactory(param -> 
            new SimpleIntegerProperty(categoryDAO.countSubcategories(param.getValue().getId())).asObject());
        catProductCountColumn.setCellValueFactory(param -> 
            new SimpleIntegerProperty(categoryDAO.countProducts(param.getValue().getId())).asObject());
        
        // Columna de acciones
        catActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Editar");
            private final Button deleteButton = new Button("Eliminar");
            private final HBox buttons = new HBox(5, editButton, deleteButton);
            
            {
                editButton.getStyleClass().addAll("action-button", "small");
                deleteButton.getStyleClass().addAll("action-button", "danger", "small");
                
                editButton.setOnAction(e -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleEditCategory(category);
                });
                
                deleteButton.setOnAction(e -> {
                    Category category = getTableView().getItems().get(getIndex());
                    handleDeleteCategory(category);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });
    }
    
    private void setupSubcategoriesTable() {
        // Configurar columnas
        subIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        subNameColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        subDescColumn.setCellValueFactory(param -> 
            new SimpleStringProperty(param.getValue().getDescripcion() != null ? param.getValue().getDescripcion() : ""));
        
        // Columna de categoría padre
        subCategoryColumn.setCellValueFactory(param -> {
            Optional<Category> parent = categoryDAO.findById(param.getValue().getCategoryId());
            return new SimpleStringProperty(parent.isPresent() ? parent.get().getNombre() : "N/A");
        });
        
        // Columna calculada de productos
        subProductCountColumn.setCellValueFactory(param -> 
            new SimpleIntegerProperty(subcategoryDAO.countProducts(param.getValue().getId())).asObject());
        
        // Columna de acciones
        subActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Editar");
            private final Button deleteButton = new Button("Eliminar");
            private final HBox buttons = new HBox(5, editButton, deleteButton);
            
            {
                editButton.getStyleClass().addAll("action-button", "small");
                deleteButton.getStyleClass().addAll("action-button", "danger", "small");
                
                editButton.setOnAction(e -> {
                    Subcategory subcategory = getTableView().getItems().get(getIndex());
                    handleEditSubcategory(subcategory);
                });
                
                deleteButton.setOnAction(e -> {
                    Subcategory subcategory = getTableView().getItems().get(getIndex());
                    handleDeleteSubcategory(subcategory);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                }
            }
        });
    }
    
    private void setupCategoryComboBox() {
        parentCategoryCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category == null ? "" : category.getNombre();
            }
            
            @Override
            public Category fromString(String string) {
                return null;
            }
        });
    }
    
    // ==================== CARGA DE DATOS ====================
    
    private void loadCategories() {
        try {
            List<Category> categories = categoryDAO.findAll();
            categoriesList.setAll(categories);
            categoriesTable.setItems(categoriesList);
            
            // Actualizar combos
            parentCategoryCombo.setItems(categoriesList);
            filterCategoryCombo.setItems(FXCollections.observableArrayList(categories));
            filterCategoryCombo.getItems().add(0, null); // Opción "Todas"
            
            categoryCountLabel.setText(categories.size() + " categoría(s)");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cargando categorías", e);
            showError("Error al cargar las categorías: " + e.getMessage());
        }
    }
    
    private void loadSubcategories() {
        try {
            List<Subcategory> subcategories = subcategoryDAO.findAll();
            subcategoriesList.setAll(subcategories);
            subcategoriesTable.setItems(subcategoriesList);
            
            subcategoryCountLabel.setText(subcategories.size() + " subcategoría(s)");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cargando subcategorías", e);
            showError("Error al cargar las subcategorías: " + e.getMessage());
        }
    }
    
    // ==================== MANEJO DE CATEGORÍAS ====================
    
    @FXML
    private void handleSaveCategory() {
        String nombre = categoryNameField.getText().trim();
        String descripcion = categoryDescField.getText().trim();
        
        if (nombre.isEmpty()) {
            showWarning("El nombre de la categoría es obligatorio");
            return;
        }
        
        try {
            if (editingCategory != null) {
                // Actualizar categoría existente
                if (categoryDAO.existsByNameExcludingId(nombre, editingCategory.getId())) {
                    showWarning("Ya existe una categoría con ese nombre");
                    return;
                }
                
                // Crear nueva categoría con datos actualizados
                Category updatedCategory = new Category.Builder()
                    .id(editingCategory.getId())
                    .nombre(nombre)
                    .descripcion(descripcion.isEmpty() ? null : descripcion)
                    .build();
                
                categoryDAO.save(updatedCategory);
                showSuccess("Categoría actualizada correctamente");
            } else {
                // Crear nueva categoría
                if (categoryDAO.existsByName(nombre)) {
                    showWarning("Ya existe una categoría con ese nombre");
                    return;
                }
                
                Category newCategory = new Category.Builder()
                    .nombre(nombre)
                    .descripcion(descripcion.isEmpty() ? null : descripcion)
                    .build();
                
                categoryDAO.save(newCategory);
                showSuccess("Categoría creada correctamente");
            }
            
            handleCancelCategory();
            loadCategories();
            loadSubcategories(); // Actualizar contadores
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error guardando categoría", e);
            showError("Error al guardar la categoría: " + e.getMessage());
        }
    }
    
    private void handleEditCategory(Category category) {
        editingCategory = category;
        categoryFormTitle.setText("Editar Categoría");
        
        categoryNameField.setText(category.getNombre());
        categoryDescField.setText(category.getDescripcion() != null ? category.getDescripcion() : "");
        
        saveCategoryBtn.setText("Actualizar");
        cancelCategoryBtn.setVisible(true);
        cancelCategoryBtn.setManaged(true);
        
        categoryNameField.requestFocus();
    }
    
    @FXML
    private void handleCancelCategory() {
        editingCategory = null;
        categoryFormTitle.setText("Nueva Categoría");
        
        categoryNameField.clear();
        categoryDescField.clear();
        
        saveCategoryBtn.setText("Guardar");
        cancelCategoryBtn.setVisible(false);
        cancelCategoryBtn.setManaged(false);
    }
    
    private void handleDeleteCategory(Category category) {
        int subcategoryCount = categoryDAO.countSubcategories(category.getId());
        int productCount = categoryDAO.countProducts(category.getId());
        
        if (subcategoryCount > 0 || productCount > 0) {
            showWarning("No se puede eliminar la categoría.\n" +
                "Tiene " + subcategoryCount + " subcategoría(s) y " + productCount + " producto(s) asociados.");
            return;
        }
        
        Optional<ButtonType> result = showConfirmation(
            "Eliminar Categoría",
            "¿Está seguro de eliminar la categoría '" + category.getNombre() + "'?",
            "Esta acción no se puede deshacer."
        );
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                categoryDAO.delete(category.getId());
                showSuccess("Categoría eliminada correctamente");
                loadCategories();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error eliminando categoría", e);
                showError("Error al eliminar: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleSearchCategory() {
        String searchTerm = categorySearchField.getText().trim();
        List<Category> results;
        
        if (searchTerm.isEmpty()) {
            results = categoryDAO.findAll();
        } else {
            results = categoryDAO.search(searchTerm);
        }
        
        categoriesList.setAll(results);
        categoryCountLabel.setText(results.size() + " categoría(s)");
    }
    
    @FXML
    private void handleClearCategorySearch() {
        categorySearchField.clear();
        loadCategories();
    }
    
    // ==================== MANEJO DE SUBCATEGORÍAS ====================
    
    @FXML
    private void handleSaveSubcategory() {
        Category parentCategory = parentCategoryCombo.getValue();
        String nombre = subcategoryNameField.getText().trim();
        String descripcion = subcategoryDescField.getText().trim();
        
        if (parentCategory == null) {
            showWarning("Debe seleccionar una categoría padre");
            return;
        }
        
        if (nombre.isEmpty()) {
            showWarning("El nombre de la subcategoría es obligatorio");
            return;
        }
        
        try {
            if (editingSubcategory != null) {
                // Actualizar subcategoría existente
                if (subcategoryDAO.existsByNameInCategoryExcludingId(nombre, parentCategory.getId(), editingSubcategory.getId())) {
                    showWarning("Ya existe una subcategoría con ese nombre");
                    return;
                }
                
                // Crear nueva subcategoría con datos actualizados
                Subcategory updatedSubcategory = new Subcategory.Builder()
                    .id(editingSubcategory.getId())
                    .categoryId(parentCategory.getId())
                    .nombre(nombre)
                    .descripcion(descripcion.isEmpty() ? null : descripcion)
                    .build();
                
                subcategoryDAO.save(updatedSubcategory);
                showSuccess("Subcategoría actualizada correctamente");
            } else {
                // Crear nueva subcategoría
                if (subcategoryDAO.existsByNameInCategory(nombre, parentCategory.getId())) {
                    showWarning("Ya existe una subcategoría con ese nombre");
                    return;
                }
                
                Subcategory newSubcategory = new Subcategory.Builder()
                    .categoryId(parentCategory.getId())
                    .nombre(nombre)
                    .descripcion(descripcion.isEmpty() ? null : descripcion)
                    .build();
                
                subcategoryDAO.save(newSubcategory);
                showSuccess("Subcategoría creada correctamente");
            }
            
            handleCancelSubcategory();
            loadCategories();
            loadSubcategories();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error guardando subcategoría", e);
            showError("Error al guardar la subcategoría: " + e.getMessage());
        }
    }
    
    private void handleEditSubcategory(Subcategory subcategory) {
        editingSubcategory = subcategory;
        subcategoryFormTitle.setText("Editar Subcategoría");
        
        // Seleccionar la categoría padre en el combo
        for (Category cat : parentCategoryCombo.getItems()) {
            if (cat != null && cat.getId() == subcategory.getCategoryId()) {
                parentCategoryCombo.setValue(cat);
                break;
            }
        }
        
        subcategoryNameField.setText(subcategory.getNombre());
        subcategoryDescField.setText(subcategory.getDescripcion() != null ? subcategory.getDescripcion() : "");
        
        saveSubcategoryBtn.setText("Actualizar");
        cancelSubcategoryBtn.setVisible(true);
        cancelSubcategoryBtn.setManaged(true);
        
        subcategoryNameField.requestFocus();
    }
    
    @FXML
    private void handleCancelSubcategory() {
        editingSubcategory = null;
        subcategoryFormTitle.setText("Nueva Subcategoría");
        
        parentCategoryCombo.setValue(null);
        subcategoryNameField.clear();
        subcategoryDescField.clear();
        
        saveSubcategoryBtn.setText("Guardar");
        cancelSubcategoryBtn.setVisible(false);
        cancelSubcategoryBtn.setManaged(false);
    }
    
    private void handleDeleteSubcategory(Subcategory subcategory) {
        int productCount = subcategoryDAO.countProducts(subcategory.getId());
        
        if (productCount > 0) {
            showWarning("No se puede eliminar la subcategoría.\n" +
                "Tiene " + productCount + " producto(s) asociados.");
            return;
        }
        
        Optional<ButtonType> result = showConfirmation(
            "Eliminar Subcategoría",
            "¿Está seguro de eliminar la subcategoría '" + subcategory.getNombre() + "'?",
            "Esta acción no se puede deshacer."
        );
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                subcategoryDAO.delete(subcategory.getId());
                showSuccess("Subcategoría eliminada correctamente");
                loadCategories(); // Actualizar contadores
                loadSubcategories();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error eliminando subcategoría", e);
                showError("Error al eliminar: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleSearchSubcategory() {
        String searchTerm = subcategorySearchField.getText().trim();
        Category filterCategory = filterCategoryCombo.getValue();
        List<Subcategory> results;
        
        if (searchTerm.isEmpty() && filterCategory == null) {
            results = subcategoryDAO.findAll();
        } else if (searchTerm.isEmpty()) {
            results = subcategoryDAO.findByCategoryId(filterCategory.getId());
        } else if (filterCategory == null) {
            results = subcategoryDAO.search(searchTerm);
        } else {
            results = subcategoryDAO.searchInCategory(filterCategory.getId(), searchTerm);
        }
        
        subcategoriesList.setAll(results);
        subcategoryCountLabel.setText(results.size() + " subcategoría(s)");
    }
    
    @FXML
    private void handleClearSubcategorySearch() {
        subcategorySearchField.clear();
        filterCategoryCombo.setValue(null);
        loadSubcategories();
    }
    
    // ==================== NAVEGACIÓN ====================
    
    @FXML
    private void handleDashboard() {
        navigateTo("/views/Dashboard.fxml", "Sistema Ferretería - Dashboard");
    }
    
    @FXML
    private void handleProducts() {
        navigateTo("/views/Products.fxml", "Sistema Ferretería - Productos");
    }
    
    @FXML
    private void handleSales() {
        navigateTo("/views/Sales.fxml", "Sistema Ferretería - Ventas");
    }
    
    @FXML
    private void handleReports() {
        navigateTo("/views/Reports.fxml", "Sistema Ferretería - Reportes");
    }
    
    @FXML
    private void handleSettings() {
        navigateTo("/views/Settings.fxml", "Sistema Ferretería - Configuración");
    }
    
    @FXML
    private void handleUsers() {
        navigateTo("/views/Users.fxml", "Sistema Ferretería - Usuarios");
    }
    
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        navigateTo("/views/Login.fxml", "Sistema Ferretería - Login");
    }
    
    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            Stage stage = (Stage) categoriesTable.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            
            stage.setTitle(title);
            stage.setScene(scene);
            stage.centerOnScreen();
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al navegar a " + fxmlPath, e);
            showError("Error al cargar la vista: " + e.getMessage());
        }
    }
    
    // ==================== UTILIDADES UI ====================
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Advertencia");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private Optional<ButtonType> showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait();
    }
}
