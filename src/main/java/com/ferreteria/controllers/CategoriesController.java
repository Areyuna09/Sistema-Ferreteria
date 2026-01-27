package com.ferreteria.controllers;

import com.ferreteria.models.Category;
import com.ferreteria.models.Subcategory;
import com.ferreteria.models.dao.CategoryDAO;
import com.ferreteria.models.dao.SubcategoryDAO;
import com.ferreteria.utils.SessionManager;

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
 * Maneja las operaciones CRUD y la interfaz de usuario.
 *
 * @author Sistema Ferretería
 * @version 1.0
 */
public class CategoriesController {

    private static final Logger LOGGER = Logger.getLogger(CategoriesController.class.getName());

    // DAOs
    private final CategoryDAO categoryDAO;
    private final SubcategoryDAO subcategoryDAO;

    // Estado de edición
    private Category editingCategory = null;
    private Subcategory editingSubcategory = null;

    // ==================== FXML - Navbar ====================
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;

    // ==================== FXML - TabPane ====================
    @FXML private TabPane tabPane;

    // ==================== FXML - Categorías ====================
    @FXML private Label categoryFormTitle;
    @FXML private TextField categoryNameField;
    @FXML private TextField categoryDescField;
    @FXML private Button saveCategoryBtn;
    @FXML private Button cancelCategoryBtn;
    @FXML private TextField categorySearchField;
    @FXML private Label categoryCountLabel;
    @FXML private TableView<Category> categoriesTable;
    @FXML private TableColumn<Category, Integer> catIdColumn;
    @FXML private TableColumn<Category, String> catNameColumn;
    @FXML private TableColumn<Category, String> catDescColumn;
    @FXML private TableColumn<Category, Integer> catSubcatCountColumn;
    @FXML private TableColumn<Category, Integer> catProductCountColumn;
    @FXML private TableColumn<Category, Void> catActionsColumn;

    // ==================== FXML - Subcategorías ====================
    @FXML private Label subcategoryFormTitle;
    @FXML private ComboBox<Category> parentCategoryCombo;
    @FXML private TextField subcategoryNameField;
    @FXML private TextField subcategoryDescField;
    @FXML private Button saveSubcategoryBtn;
    @FXML private Button cancelSubcategoryBtn;
    @FXML private ComboBox<Category> filterCategoryCombo;
    @FXML private TextField subcategorySearchField;
    @FXML private Label subcategoryCountLabel;
    @FXML private TableView<Subcategory> subcategoriesTable;
    @FXML private TableColumn<Subcategory, Integer> subIdColumn;
    @FXML private TableColumn<Subcategory, String> subCategoryColumn;
    @FXML private TableColumn<Subcategory, String> subNameColumn;
    @FXML private TableColumn<Subcategory, String> subDescColumn;
    @FXML private TableColumn<Subcategory, Integer> subProductCountColumn;
    @FXML private TableColumn<Subcategory, Void> subActionsColumn;

    // Listas observables
    private ObservableList<Category> categoriesList = FXCollections.observableArrayList();
    private ObservableList<Subcategory> subcategoriesList = FXCollections.observableArrayList();

    public CategoriesController() {
        this.categoryDAO = new CategoryDAO();
        this.subcategoryDAO = new SubcategoryDAO();
    }

    @FXML
    private void initialize() {
        setupUserInfo();
        setupCategoriesTable();
        setupSubcategoriesTable();
        setupCategoryComboBoxes();
        setupSearchListeners();
        loadCategories();
        loadSubcategories();
        LOGGER.info("CategoriesController inicializado");
    }

    // ==================== CONFIGURACIÓN INICIAL ====================

    private void setupUserInfo() {
        var currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText(currentUser.getFullName());
            roleLabel.setText(currentUser.getRole().getValue());
        }
    }

    private void setupCategoriesTable() {
        catIdColumn.setCellValueFactory(data ->
            new SimpleIntegerProperty(data.getValue().getId()).asObject());

        catNameColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getNombre()));

        catDescColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getDescripcion()));

        catSubcatCountColumn.setCellValueFactory(data ->
            new SimpleIntegerProperty(categoryDAO.countSubcategories(data.getValue().getId())).asObject());

        catProductCountColumn.setCellValueFactory(data ->
            new SimpleIntegerProperty(categoryDAO.countProducts(data.getValue().getId())).asObject());

        setupCategoryActionsColumn();
        categoriesTable.setItems(categoriesList);
    }

    private void setupCategoryActionsColumn() {
        catActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Editar");
            private final Button deleteBtn = new Button("Eliminar");
            private final HBox buttons = new HBox(8, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-table-edit");
                deleteBtn.getStyleClass().add("btn-table-delete");
                buttons.setAlignment(Pos.CENTER);

                editBtn.setOnAction(e -> {
                    Category cat = getTableView().getItems().get(getIndex());
                    handleEditCategory(cat);
                });

                deleteBtn.setOnAction(e -> {
                    Category cat = getTableView().getItems().get(getIndex());
                    handleDeleteCategory(cat);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void setupSubcategoriesTable() {
        subIdColumn.setCellValueFactory(data ->
            new SimpleIntegerProperty(data.getValue().getId()).asObject());

        subCategoryColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCategoryName()));

        subNameColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getNombre()));

        subDescColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getDescripcion()));

        subProductCountColumn.setCellValueFactory(data ->
            new SimpleIntegerProperty(subcategoryDAO.countProducts(data.getValue().getId())).asObject());

        setupSubcategoryActionsColumn();
        subcategoriesTable.setItems(subcategoriesList);
    }

    private void setupSubcategoryActionsColumn() {
        subActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Editar");
            private final Button deleteBtn = new Button("Eliminar");
            private final HBox buttons = new HBox(8, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-table-edit");
                deleteBtn.getStyleClass().add("btn-table-delete");
                buttons.setAlignment(Pos.CENTER);

                editBtn.setOnAction(e -> {
                    Subcategory sub = getTableView().getItems().get(getIndex());
                    handleEditSubcategory(sub);
                });

                deleteBtn.setOnAction(e -> {
                    Subcategory sub = getTableView().getItems().get(getIndex());
                    handleDeleteSubcategory(sub);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
    }

    private void setupCategoryComboBoxes() {
        StringConverter<Category> converter = new StringConverter<>() {
            @Override
            public String toString(Category cat) {
                return cat != null ? cat.getNombre() : "";
            }

            @Override
            public Category fromString(String s) {
                return null;
            }
        };

        parentCategoryCombo.setConverter(converter);
        filterCategoryCombo.setConverter(converter);

        // Listener para filtrar subcategorías cuando cambia la selección
        filterCategoryCombo.setOnAction(e -> filterSubcategoriesByCategory());
    }

    private void setupSearchListeners() {
        // Buscar al presionar Enter en campo de búsqueda de categorías
        categorySearchField.setOnAction(e -> handleSearchCategory());

        // Buscar al presionar Enter en campo de búsqueda de subcategorías
        subcategorySearchField.setOnAction(e -> handleSearchSubcategory());
    }

    // ==================== CARGA DE DATOS ====================

    private void loadCategories() {
        List<Category> categories = categoryDAO.findAll();
        categoriesList.setAll(categories);
        categoryCountLabel.setText(categories.size() + " categoria(s)");

        // Actualizar ComboBoxes
        ObservableList<Category> comboList = FXCollections.observableArrayList(categories);
        parentCategoryCombo.setItems(comboList);

        // Para el filtro, agregar opción "Todas"
        ObservableList<Category> filterList = FXCollections.observableArrayList();
        filterList.add(null); // Representa "Todas"
        filterList.addAll(categories);
        filterCategoryCombo.setItems(filterList);
    }

    private void loadSubcategories() {
        Category selectedFilter = filterCategoryCombo.getValue();
        List<Subcategory> subcategories;

        if (selectedFilter != null) {
            subcategories = subcategoryDAO.findByCategoryId(selectedFilter.getId());
        } else {
            subcategories = subcategoryDAO.findAll();
        }

        subcategoriesList.setAll(subcategories);
        subcategoryCountLabel.setText(subcategories.size() + " subcategoria(s)");
    }

    private void filterSubcategoriesByCategory() {
        loadSubcategories();
    }

    // ==================== CRUD CATEGORÍAS ====================

    @FXML
    private void handleSaveCategory() {
        String nombre = categoryNameField.getText().trim();
        String descripcion = categoryDescField.getText().trim();

        // Validar nombre requerido
        if (nombre.isEmpty()) {
            showWarning("El nombre de la categoría es requerido");
            categoryNameField.requestFocus();
            return;
        }

        try {
            if (editingCategory != null) {
                // Actualizar categoría existente
                if (categoryDAO.existsByNameExcludingId(nombre, editingCategory.getId())) {
                    showWarning("Ya existe una categoría con ese nombre");
                    return;
                }

                Category updated = new Category.Builder()
                    .id(editingCategory.getId())
                    .nombre(nombre)
                    .descripcion(descripcion.isEmpty() ? null : descripcion)
                    .active(true)
                    .build();

                categoryDAO.save(updated);
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
            loadSubcategories();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error guardando categoría", e);
            showError("Error al guardar la categoría: " + e.getMessage());
        }
    }

    private void handleEditCategory(Category category) {
        editingCategory = category;
        categoryFormTitle.setText("Editar Categoria");
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
        categoryFormTitle.setText("Nueva Categoria");
        categoryNameField.clear();
        categoryDescField.clear();
        saveCategoryBtn.setText("Guardar");
        cancelCategoryBtn.setVisible(false);
        cancelCategoryBtn.setManaged(false);
    }

    private void handleDeleteCategory(Category category) {
        int subcatCount = categoryDAO.countSubcategories(category.getId());
        int productCount = categoryDAO.countProducts(category.getId());

        if (subcatCount > 0 || productCount > 0) {
            showWarning("No se puede eliminar la categoría.\n" +
                "Tiene " + subcatCount + " subcategoría(s) y " + productCount + " producto(s) asociados.");
            return;
        }

        Optional<ButtonType> result = showConfirmation(
            "Eliminar Categoria",
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
        categoryCountLabel.setText(results.size() + " categoria(s)");
    }

    @FXML
    private void handleClearCategorySearch() {
        categorySearchField.clear();
        loadCategories();
    }

    // ==================== CRUD SUBCATEGORÍAS ====================

    @FXML
    private void handleSaveSubcategory() {
        Category parentCategory = parentCategoryCombo.getValue();
        String nombre = subcategoryNameField.getText().trim();
        String descripcion = subcategoryDescField.getText().trim();

        // Validar categoría padre
        if (parentCategory == null) {
            showWarning("Debe seleccionar una categoría padre");
            parentCategoryCombo.requestFocus();
            return;
        }

        // Validar nombre requerido
        if (nombre.isEmpty()) {
            showWarning("El nombre de la subcategoría es requerido");
            subcategoryNameField.requestFocus();
            return;
        }

        try {
            if (editingSubcategory != null) {
                // Actualizar subcategoría existente
                if (subcategoryDAO.existsByNameInCategoryExcludingId(nombre, parentCategory.getId(),
                        editingSubcategory.getId())) {
                    showWarning("Ya existe una subcategoría con ese nombre en la categoría seleccionada");
                    return;
                }

                Subcategory updated = new Subcategory.Builder()
                    .id(editingSubcategory.getId())
                    .categoryId(parentCategory.getId())
                    .nombre(nombre)
                    .descripcion(descripcion.isEmpty() ? null : descripcion)
                    .active(true)
                    .build();

                subcategoryDAO.save(updated);
                showSuccess("Subcategoría actualizada correctamente");
            } else {
                // Crear nueva subcategoría
                if (subcategoryDAO.existsByNameInCategory(nombre, parentCategory.getId())) {
                    showWarning("Ya existe una subcategoría con ese nombre en la categoría seleccionada");
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
        subcategoryFormTitle.setText("Editar Subcategoria");

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
        subcategoryFormTitle.setText("Nueva Subcategoria");
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
            "Eliminar Subcategoria",
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
        subcategoryCountLabel.setText(results.size() + " subcategoria(s)");
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
        showInfo("Módulo de Productos en desarrollo");
    }

    @FXML
    private void handleSales() {
        showInfo("Módulo de Ventas en desarrollo");
    }

    @FXML
    private void handleReports() {
        navigateTo("/views/Reports.fxml", "Sistema Ferretería - Reportes");
    }

    @FXML
    private void handleUsers() {
        showInfo("Módulo de Usuarios en desarrollo");
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

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();

            Scene scene = new Scene(root, currentWidth, currentHeight);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            if (fxmlPath.contains("Reports")) {
                scene.getStylesheets().add(getClass().getResource("/styles/reports.css").toExternalForm());
            }

            stage.setTitle(title);

            if (fxmlPath.contains("Login")) {
                stage.setMaximized(false);
                stage.setResizable(false);
                stage.setScene(scene);
                stage.setWidth(1100);
                stage.setHeight(650);
                stage.centerOnScreen();
            } else {
                stage.setResizable(true);
                stage.setScene(scene);
                if (wasMaximized) {
                    stage.setMaximized(true);
                }
            }

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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
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
