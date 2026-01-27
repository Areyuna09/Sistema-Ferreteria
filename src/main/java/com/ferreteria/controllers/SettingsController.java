package com.ferreteria.controllers;

import com.ferreteria.models.BusinessConfig;
import com.ferreteria.models.dao.BusinessConfigDAO;
import com.ferreteria.models.dao.DatabaseConfig;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para la pantalla de configuración del negocio.
 */
public class SettingsController {
    private static final Logger LOGGER = Logger.getLogger(SettingsController.class.getName());

    @FXML private NavbarController navbarController;
    @FXML private TextField businessNameField;
    @FXML private TextField cuitField;
    @FXML private TextArea addressArea;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label lastUpdatedLabel;
    @FXML private Button saveButton;

    private BusinessConfigDAO configDAO;
    private BusinessConfig currentConfig;

    @FXML
    public void initialize() {
        if (navbarController != null) {
            navbarController.setActiveView("settings");
        }

        configDAO = new BusinessConfigDAO(DatabaseConfig.getInstance());
        setupValidation();
        loadConfig();

        LOGGER.info("SettingsController inicializado");
    }

    /**
     * Configura las validaciones en tiempo real de los campos.
     */
    private void setupValidation() {
        // Validación CUIT en tiempo real
        cuitField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                if (BusinessConfig.isValidCuit(newVal)) {
                    cuitField.setStyle("-fx-border-color: #22c55e; -fx-border-width: 2;");
                } else {
                    cuitField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
                }
            } else {
                cuitField.setStyle("");
            }
        });

        // Validación email en tiempo real
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                if (BusinessConfig.isValidEmail(newVal)) {
                    emailField.setStyle("-fx-border-color: #22c55e; -fx-border-width: 2;");
                } else {
                    emailField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
                }
            } else {
                emailField.setStyle("");
            }
        });

        // Auto-formatear CUIT cuando pierde el foco
        cuitField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (wasFocused && !isNowFocused) {
                String text = cuitField.getText();
                if (text != null && !text.isBlank()) {
                    String digitsOnly = BusinessConfig.formatCuitDigitsOnly(text);
                    if (digitsOnly.length() == 11) {
                        cuitField.setText(BusinessConfig.formatCuitWithDashes(digitsOnly));
                    }
                }
            }
        });
    }

    /**
     * Carga la configuración existente desde la base de datos.
     */
    private void loadConfig() {
        try {
            Optional<BusinessConfig> configOpt = configDAO.getConfig();

            if (configOpt.isPresent()) {
                currentConfig = configOpt.get();
                populateFields(currentConfig);
                lastUpdatedLabel.setText("Última actualización: " + 
                    formatDateTime(currentConfig.getUpdatedAt()));
            } else {
                lastUpdatedLabel.setText("No hay configuración guardada");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al cargar configuración", e);
            showError("Error al cargar la configuración: " + e.getMessage());
        }
    }

    /**
     * Llena los campos del formulario con los datos de configuración.
     * 
     * @param config Configuración a mostrar
     */
    private void populateFields(BusinessConfig config) {
        businessNameField.setText(config.getBusinessName() != null ? config.getBusinessName() : "");
        cuitField.setText(config.getCuit() != null ? config.getCuit() : "");
        addressArea.setText(config.getAddress() != null ? config.getAddress() : "");
        phoneField.setText(config.getPhone() != null ? config.getPhone() : "");
        emailField.setText(config.getEmail() != null ? config.getEmail() : "");
    }

    /**
     * Maneja el evento de guardar la configuración.
     */
    @FXML
    private void handleSave() {
        if (!validateFields()) {
            return;
        }

        try {
            BusinessConfig config = new BusinessConfig.Builder()
                .businessName(businessNameField.getText().trim())
                .cuit(getFieldValue(cuitField))
                .address(getFieldValue(addressArea))
                .phone(getFieldValue(phoneField))
                .email(getFieldValue(emailField))
                .build();

            boolean saved = configDAO.saveConfig(config);

            if (saved) {
                showSuccess("Configuración guardada correctamente");
                loadConfig(); // Recargar para actualizar fecha
            } else {
                showError("No se pudo guardar la configuración");
            }

        } catch (IllegalArgumentException e) {
            showError("Error de validación: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al guardar configuración", e);
            showError("Error al guardar: " + e.getMessage());
        }
    }

    /**
     * Valida todos los campos del formulario.
     * 
     * @return true si todos los campos son válidos
     */
    private boolean validateFields() {
        StringBuilder errors = new StringBuilder();

        // Validar nombre del negocio (requerido)
        String businessName = businessNameField.getText();
        if (businessName == null || businessName.isBlank()) {
            errors.append("• El nombre del negocio es requerido\n");
        }

        // Validar CUIT (opcional, pero si tiene valor debe ser válido)
        String cuit = cuitField.getText();
        if (cuit != null && !cuit.isBlank() && !BusinessConfig.isValidCuit(cuit)) {
            errors.append("• Formato de CUIT inválido. Use: XX-XXXXXXXX-X\n");
        }

        // Validar email (opcional, pero si tiene valor debe ser válido)
        String email = emailField.getText();
        if (email != null && !email.isBlank() && !BusinessConfig.isValidEmail(email)) {
            errors.append("• Formato de email inválido\n");
        }

        if (errors.length() > 0) {
            showError("Errores de validación:\n\n" + errors.toString());
            return false;
        }

        return true;
    }

    /**
     * Maneja el evento de restablecer el formulario.
     */
    @FXML
    private void handleReset() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar");
        confirm.setHeaderText("¿Descartar cambios?");
        confirm.setContentText("Se perderán los cambios no guardados.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            loadConfig();
        }
    }

    // Métodos auxiliares

    private String getFieldValue(TextField field) {
        String value = field.getText();
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    private String getFieldValue(TextArea area) {
        String value = area.getText();
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}