package com.ferreteria.controllers;

import com.ferreteria.models.User;
import com.ferreteria.models.dao.DatabaseConfig;
import com.ferreteria.models.dao.UserDAO;
import com.ferreteria.utils.AuthenticationException;
import com.ferreteria.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

/**
 * Controlador de la pantalla de Login.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final UserDAO userDAO;

    public LoginController() {
        this.userDAO = new UserDAO(DatabaseConfig.getInstance());
    }

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        passwordField.setOnAction(e -> handleLogin());
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Complete todos los campos");
            return;
        }

        try {
            loginButton.setDisable(true);
            User user = authenticate(username, password);
            SessionManager.getInstance().setCurrentUser(user);
            navigateToDashboard();
        } catch (AuthenticationException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Error de conexión");
            e.printStackTrace();
        } finally {
            loginButton.setDisable(false);
        }
    }

    private User authenticate(String username, String password) {
        Optional<User> userOpt = userDAO.findByUsername(username);

        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Usuario no encontrado");
        }

        User user = userOpt.get();

        if (!user.isActive()) {
            throw new AuthenticationException("Usuario desactivado");
        }

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new AuthenticationException("Contraseña incorrecta");
        }

        return user;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        passwordField.clear();
    }

    private void navigateToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/Dashboard.fxml"));
            Scene scene = new Scene(root, 1200, 700);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("Sistema Ferretería - Dashboard");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error cargando dashboard");
        }
    }
}
