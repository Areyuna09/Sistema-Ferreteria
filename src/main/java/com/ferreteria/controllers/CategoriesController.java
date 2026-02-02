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
        setupCategoryComboBoxes();
        setupSearchListeners();

        loadCategories();
        loadSubcategories();
    }

    private void setupCategoriesTable() {
        catIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        catNameColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        catDescColumn.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getDescripcion() != null ? param.getValue().getDescripcion() : ""));

        catSubcatCountColumn.setCellValueFactory(param ->
            new SimpleIntegerProperty(categoryDAO.countSubcategories(param.getValue().getId())).asObject());
        catProductCountColumn.setCellValueFactory(param ->
            new SimpleIntegerProperty(categoryDAO.countProducts(param.getValue().getId())).asObject());

        catActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Editar");
            private final Button deleteButton = new Button("Eliminar");
            private final HBox buttons = new HBox(5, editButton, deleteButton);

            {
                editButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 11px;");
                deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 11px;");
                buttons.setAlignment(Pos.CENTER);

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
                setGraphic(empty ? null : buttons);
            }
        });

        categoriesTable.setItems(categoriesList);
    }

    private void setupSubcategoriesTable() {
        subIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        subNameColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        subDescColumn.setCellValueFactory(param ->
            new SimpleStringProperty(param.getValue().getDescripcion() != null ? param.getValue().getDescripcion() : ""));

        // Usar getCategoryName() del modelo en vez de consultar el DAO por cada fila
        subCategoryColumn.setCellValueFactory(param -> {
            String catName = param.getValue().getCategoryName();
            return new SimpleStringProperty(catName != null ? catName : "N/A");
        });

        subProductCountColumn.setCellValueFactory(param ->
            new SimpleIntegerProperty(subcategoryDAO.countProducts(param.getValue().getId())).asObject());

        subActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Editar");
            private final Button deleteButton = new Button("Eliminar");
            private final HBox buttons = new HBox(5, editButton, deleteButton);

            {
                editButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 11px;");
                deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-padding: 3 8; -fx-font-size: 11px;");
                buttons.setAlignment(Pos.CENTER);

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
                setGraphic(empty ? null : buttons);
            }
        });

        subcategoriesTable.setItems(subcategoriesList);
    }

    private void setupCategoryComboBoxes() {
        // Converter compartido para ambos ComboBox
        StringConverter<Category> converter = new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category == null ? "" : category.getNombre();
            }

            @Override
            public Category fromString(String string) {
                return null;
            }
        };

        parentCategoryCombo.setConverter(converter);
        filterCategoryCombo.setConverter(converter);

        // Filtrar subcategorías al cambiar selección del combo de filtro
        filterCategoryCombo.setOnAction(e -> filterSubcategoriesByCategory());
    }

    private void setupSearchListeners() {
        // Buscar al presionar Enter en los campos de búsqueda
        categorySearchField.setOnAction(e -> handleSearchCategory());
        subcategorySearchField.setOnAction(e -> handleSearchSubcategory());
    }

    // ==================== CARGA DE DATOS ====================

    private void loadCategories() {
        try {
            List<Category> categories = categoryDAO.findAll();
            categoriesList.setAll(categories);

            // Actualizar combo de formulario
            parentCategoryCombo.setItems(FXCollections.observableArrayList(categories));

            // Actualizar combo de filtro con opción "Todas" (null)
            ObservableList<Category> filterList = FXCollections.observableArrayList();
            filterList.add(null);
            filterList.addAll(categories);
            filterCategoryCombo.setItems(filterList);

            categoryCountLabel.setText(categories.size() + " categoría(s)");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cargando categorías", e);
            showError("Error al cargar las categorías: " + e.getMessage());
        }
    }

    private void loadSubcategories() {
        try {
            // Respetar la selección del filtro de categoría
            Category selectedFilter = filterCategoryCombo.getValue();
            List<Subcategory> subcategories;

            if (selectedFilter != null) {
                subcategories = subcategoryDAO.findByCategoryId(selectedFilter.getId());
            } else {
                subcategories = subcategoryDAO.findAll();
            }

            subcategoriesList.setAll(subcategories);
            subcategoryCountLabel.setText(subcategories.size() + " subcategoría(s)");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cargando subcategorías", e);
            showError("Error al cargar las subcategorías: " + e.getMessage());
        }
    }

    private void filterSubcategoriesByCategory() {
        loadSubcategories();
    }

    // ==================== MANEJO DE CATEGORÍAS ====================

    @FXML
    private void handleSaveCategory() {
        String nombre = categoryNameField.getText().trim();
        String descripcion = categoryDescField.getText().trim();

        if (nombre.isEmpty()) {
            showWarning("El nombre de la categoría es obligatorio");
            categoryNameField.requestFocus();
            return;
        }

        try {
            if (editingCategory != null) {
                if (categoryDAO.existsByNameExcludingId(nombre, editingCategory.getId())) {
                    showWarning("Ya existe una categoría con ese nombre");
                    return;
                }

                Category updatedCategory = new Category.Builder()
                    .id(editingCategory.getId())
                    .nombre(nombre)
                    .descripcion(descripcion.isEmpty() ? null : descripcion)
                    .active(true)
                    .build();

                categoryDAO.save(updatedCategory);
                showSuccess("Categoría actualizada correctamente");
            } else {
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
                loadSubcategories();
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
            parentCategoryCombo.requestFocus();
            return;
        }

        if (nombre.isEmpty()) {
            showWarning("El nombre de la subcategoría es obligatorio");
            subcategoryNameField.requestFocus();
            return;
        }

        try {
            if (editingSubcategory != null) {
                if (subcategoryDAO.existsByNameInCategoryExcludingId(nombre, parentCategory.getId(), editingSubcategory.getId())) {
                    showWarning("Ya existe una subcategoría con ese nombre en la categoría seleccionada");
                    return;
                }

                Subcategory updatedSubcategory = new Subcategory.Builder()
                    .id(editingSubcategory.getId())
                    .categoryId(parentCategory.getId())
                    .nombre(nombre)
                    .descripcion(descripcion.isEmpty() ? null : descripcion)
                    .active(true)
                    .build();

                subcategoryDAO.save(updatedSubcategory);
                showSuccess("Subcategoría actualizada correctamente");
            } else {
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
        subcategoryFormTitle.setText("Editar Subcategoría");

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
                loadCategories();
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
            boolean wasMaximized = stage.isMaximized();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();

            Scene scene = new Scene(root, currentWidth, currentHeight);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

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
