package com.ferreteria.infrastructure.ui;

import com.ferreteria.application.usecases.LoginUseCase;
import com.ferreteria.application.usecases.SessionManager;
import com.ferreteria.domain.entities.User;
import com.ferreteria.domain.exceptions.AuthenticationException;
import com.ferreteria.infrastructure.persistence.DatabaseConfig;
import com.ferreteria.infrastructure.persistence.SqliteUserRepository;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Controlador de la pantalla de Login.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final LoginUseCase loginUseCase;

    public LoginController() {
        var userRepository = new SqliteUserRepository(DatabaseConfig.getInstance());
        this.loginUseCase = new LoginUseCase(userRepository);
    }

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);

        // Enter en password hace login
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
            User user = loginUseCase.execute(username, password);
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
