package com.ferreteria.controllers;

import com.ferreteria.models.User;
import com.ferreteria.models.UserRole;
import com.ferreteria.models.dao.DatabaseConfig;
import com.ferreteria.models.dao.UserDAO;
import com.ferreteria.utils.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import org.mindrot.jbcrypt.BCrypt;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for user management screen.
 * Only accessible by administrators.
 */
public class UsersController {

    @FXML private NavbarController navbarController;

    @FXML private Label formTitle;
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Label messageLabel;
    @FXML private Button btnDelete;

    @FXML private TextField searchField;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colCreatedAt;
    @FXML private TableColumn<User, String> colStatus;

    private UserDAO userDAO;
    private ObservableList<User> usersList;
    private User selectedUser;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAdmin()) {
            showMessage("Acceso denegado. Solo administradores.", true);
            return;
        }

        if (navbarController != null) {
            navbarController.setActiveView("usuarios");
        }

        userDAO = new UserDAO(DatabaseConfig.getInstance());
        usersList = FXCollections.observableArrayList();

        setupRoleCombo();
        setupTable();
        loadUsers();
    }

    private void setupRoleCombo() {
        roleCombo.setItems(FXCollections.observableArrayList(
            "Administrador", "Vendedor", "Supervisor"
        ));
        roleCombo.setValue("Vendedor");
    }

    private void setupTable() {
        colId.setCellValueFactory(cell ->
            new SimpleStringProperty(String.valueOf(cell.getValue().getId())));

        colUsername.setCellValueFactory(cell ->
            new SimpleStringProperty(cell.getValue().getUsername()));

        colFullName.setCellValueFactory(cell ->
            new SimpleStringProperty(cell.getValue().getFullName()));

        colRole.setCellValueFactory(cell -> {
            String role = cell.getValue().getRole().getValue();
            String display = role.substring(0, 1).toUpperCase() + role.substring(1);
            return new SimpleStringProperty(display);
        });

        colCreatedAt.setCellValueFactory(cell -> {
            var dt = cell.getValue().getCreatedAt();
            return new SimpleStringProperty(dt != null ? dt.format(DATE_FORMAT) : "-");
        });

        colStatus.setCellValueFactory(cell ->
            new SimpleStringProperty(cell.getValue().isActive() ? "Activo" : "Inactivo"));

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Activo".equals(item)) {
                        setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    }
                }
            }
        });

        usersTable.setItems(usersList);

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadUserToForm(newVal);
            }
        });
    }

    private void loadUsers() {
        usersList.clear();
        usersList.addAll(userDAO.findAll());
    }

    private void loadUserToForm(User user) {
        selectedUser = user;
        formTitle.setText("Editar Usuario #" + user.getId());

        usernameField.setText(user.getUsername());
        fullNameField.setText(user.getFullName());
        passwordField.clear();
        confirmPasswordField.clear();

        String role = user.getRole().getValue();
        roleCombo.setValue(role.substring(0, 1).toUpperCase() + role.substring(1));

        btnDelete.setVisible(true);
        if (user.isActive()) {
            btnDelete.setText("Desactivar");
            btnDelete.setStyle("-fx-background-color: #f97316;");
        } else {
            btnDelete.setText("Eliminar");
            btnDelete.setStyle("-fx-background-color: #dc2626;");
        }
        clearMessage();
    }

    @FXML
    public void handleSave() {
        String username = usernameField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String roleStr = roleCombo.getValue();

        if (!validateForm(username, fullName, password, confirmPassword)) {
            return;
        }

        try {
            UserRole role = UserRole.fromString(roleStr.toLowerCase());

            if (selectedUser == null) {
                createNewUser(username, fullName, password, role);
            } else {
                updateExistingUser(username, fullName, password, role);
            }

            loadUsers();
            handleClear();
            showMessage("Usuario guardado correctamente", false);

        } catch (Exception e) {
            showMessage("Error: " + e.getMessage(), true);
        }
    }

    private boolean validateForm(String username, String fullName, String password, String confirmPassword) {
        if (username.isEmpty()) {
            showMessage("El username es requerido", true);
            return false;
        }

        if (username.length() < 3) {
            showMessage("El username debe tener al menos 3 caracteres", true);
            return false;
        }

        if (fullName.isEmpty()) {
            showMessage("El nombre completo es requerido", true);
            return false;
        }

        if (selectedUser == null) {
            if (password.isEmpty()) {
                showMessage("La contraseña es requerida para nuevos usuarios", true);
                return false;
            }
            if (password.length() < 6) {
                showMessage("La contraseña debe tener al menos 6 caracteres", true);
                return false;
            }
            if (!password.equals(confirmPassword)) {
                showMessage("Las contraseñas no coinciden", true);
                return false;
            }
        } else if (!password.isEmpty()) {
            if (password.length() < 6) {
                showMessage("La contraseña debe tener al menos 6 caracteres", true);
                return false;
            }
            if (!password.equals(confirmPassword)) {
                showMessage("Las contraseñas no coinciden", true);
                return false;
            }
        }

        Optional<User> existing = userDAO.findByUsername(username);
        if (existing.isPresent()) {
            if (selectedUser == null || existing.get().getId() != selectedUser.getId()) {
                showMessage("El username ya existe", true);
                return false;
            }
        }

        return true;
    }

    private void createNewUser(String username, String fullName, String password, UserRole role) {
        String hashedPassword = hashPassword(password);

        User newUser = new User.Builder()
            .username(username)
            .fullName(fullName)
            .passwordHash(hashedPassword)
            .role(role)
            .active(true)
            .build();

        userDAO.save(newUser);
    }

    private void updateExistingUser(String username, String fullName, String password, UserRole role) {
        User.Builder builder = new User.Builder()
            .id(selectedUser.getId())
            .username(username)
            .fullName(fullName)
            .role(role)
            .active(selectedUser.isActive());

        if (!password.isEmpty()) {
            builder.passwordHash(hashPassword(password));
            userDAO.updatePassword(selectedUser.getId(), hashPassword(password));
        }

        User updatedUser = builder.build();
        userDAO.save(updatedUser);
    }

    @FXML
    public void handleClear() {
        selectedUser = null;
        formTitle.setText("Nuevo Usuario");

        usernameField.clear();
        fullNameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        roleCombo.setValue("Vendedor");

        btnDelete.setVisible(false);
        btnDelete.setText("Eliminar");
        btnDelete.setStyle("");
        usersTable.getSelectionModel().clearSelection();
        clearMessage();
    }

    @FXML
    public void handleDelete() {
        if (selectedUser == null) {
            return;
        }

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser.getId() == selectedUser.getId()) {
            showMessage("No puedes eliminar tu propio usuario", true);
            return;
        }

        if (selectedUser.isActive()) {
            handleDeactivateUser();
        } else {
            handlePermanentDelete();
        }
    }

    private void handleDeactivateUser() {
        if (selectedUser.isAdmin() && countActiveAdmins() <= 1) {
            showMessage("No se puede eliminar. Debe haber al menos un administrador", true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Desactivar Usuario");
        confirm.setHeaderText("Desactivar usuario: " + selectedUser.getUsername());
        confirm.setContentText("El usuario sera desactivado y no podra iniciar sesion. Continuar?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            userDAO.delete(selectedUser.getId());
            loadUsers();
            handleClear();
            showMessage("Usuario desactivado correctamente", false);
        }
    }

    private void handlePermanentDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar Permanentemente");
        confirm.setHeaderText("ELIMINAR PERMANENTEMENTE: " + selectedUser.getUsername());
        confirm.setContentText("Esta accion NO se puede deshacer. El usuario sera eliminado de la base de datos. Continuar?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            userDAO.deletePermanently(selectedUser.getId());
            loadUsers();
            handleClear();
            showMessage("Usuario eliminado permanentemente", false);
        }
    }

    @FXML
    public void handleSearch() {
        String search = searchField.getText().trim().toLowerCase();
        if (search.isEmpty()) {
            loadUsers();
            return;
        }

        List<User> filtered = userDAO.findAll().stream()
            .filter(u -> u.getUsername().toLowerCase().contains(search) ||
                        (u.getFullName() != null && u.getFullName().toLowerCase().contains(search)))
            .toList();

        usersList.clear();
        usersList.addAll(filtered);
    }

    @FXML
    public void handleClearSearch() {
        searchField.clear();
        loadUsers();
    }

    private int countActiveAdmins() {
        return (int) userDAO.findAllActive().stream()
            .filter(User::isAdmin)
            .count();
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        if (isError) {
            messageLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
        } else {
            messageLabel.setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
        }
    }

    private void clearMessage() {
        messageLabel.setText("");
    }
}
