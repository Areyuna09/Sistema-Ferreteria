package com.ferreteria.controllers;

import com.ferreteria.Main;
import com.ferreteria.models.User;
import com.ferreteria.models.dao.DatabaseConfig;
import com.ferreteria.models.dao.UserDAO;
import com.ferreteria.utils.AuthenticationException;
import com.ferreteria.utils.SessionManager;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

/**
 * Controlador de la pantalla de Login.
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisible;
    @FXML private CheckBox showPasswordCheck;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private ImageView backgroundImage;
    @FXML private VBox brandPanel;
    @FXML private VBox formPanel;

    private final UserDAO userDAO;

    public LoginController() {
        this.userDAO = new UserDAO(DatabaseConfig.getInstance());
    }

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        passwordField.setOnAction(e -> handleLogin());
        passwordVisible.setOnAction(e -> handleLogin());

        setupShowPassword();
        setupBackground();
        playEntryAnimations();
    }

    private void setupShowPassword() {
        passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());
        showPasswordCheck.selectedProperty().addListener((obs, oldVal, show) -> {
            passwordField.setVisible(!show);
            passwordVisible.setVisible(show);
            if (show) {
                passwordVisible.requestFocus();
                passwordVisible.positionCaret(passwordVisible.getText().length());
            } else {
                passwordField.requestFocus();
                passwordField.positionCaret(passwordField.getText().length());
            }
        });
    }

    private void setupBackground() {
        try {
            Image bgImage = new Image(getClass().getResourceAsStream("/images/background.jpg"));
            backgroundImage.setImage(bgImage);

            GaussianBlur blur = new GaussianBlur(25);
            backgroundImage.setEffect(blur);

            // Bind image size to parent for responsive scaling
            backgroundImage.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    backgroundImage.fitWidthProperty().bind(newScene.widthProperty());
                    backgroundImage.fitHeightProperty().bind(newScene.heightProperty());
                }
            });
        } catch (Exception e) {
            System.err.println("No se pudo cargar imagen de fondo: " + e.getMessage());
        }
    }

    private void playEntryAnimations() {
        brandPanel.setOpacity(0);
        brandPanel.setTranslateY(20);
        formPanel.setOpacity(0);
        formPanel.setTranslateY(20);

        FadeTransition fadeBrand = new FadeTransition(Duration.millis(400), brandPanel);
        fadeBrand.setFromValue(0);
        fadeBrand.setToValue(1);
        fadeBrand.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slideBrand = new TranslateTransition(Duration.millis(400), brandPanel);
        slideBrand.setFromY(20);
        slideBrand.setToY(0);
        slideBrand.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeForm = new FadeTransition(Duration.millis(400), formPanel);
        fadeForm.setFromValue(0);
        fadeForm.setToValue(1);
        fadeForm.setDelay(Duration.millis(100));
        fadeForm.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slideForm = new TranslateTransition(Duration.millis(400), formPanel);
        slideForm.setFromY(20);
        slideForm.setToY(0);
        slideForm.setDelay(Duration.millis(100));
        slideForm.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition parallel = new ParallelTransition(
            fadeBrand, slideBrand, fadeForm, slideForm
        );
        parallel.play();
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
        Main.navigateTo("/views/Dashboard.fxml", "Sistema Ferreteria - Dashboard");
    }
}
