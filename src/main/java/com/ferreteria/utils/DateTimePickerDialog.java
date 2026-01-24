package com.ferreteria.utils;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Utility dialog for picking date and time.
 */
public class DateTimePickerDialog {

    private static final ZoneId SAN_JUAN_ZONE = ZoneId.of("America/Argentina/San_Juan");

    /**
     * Shows a dialog to pick date and time.
     *
     * @param title dialog title
     * @param header dialog header
     * @param initialDateTime initial date/time value
     * @return Optional with selected date/time, empty if cancelled
     */
    public static Optional<LocalDateTime> show(String title, String header, LocalDateTime initialDateTime) {
        Dialog<LocalDateTime> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        ButtonType guardarBtn = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 100, 10, 10));

        DatePicker datePicker = new DatePicker(initialDateTime.toLocalDate());
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, initialDateTime.getHour());
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, initialDateTime.getMinute());

        hourSpinner.setEditable(true);
        minuteSpinner.setEditable(true);
        hourSpinner.setPrefWidth(70);
        minuteSpinner.setPrefWidth(70);

        Button btnAhora = new Button("Ahora");
        btnAhora.setOnAction(e -> {
            ZonedDateTime now = ZonedDateTime.now(SAN_JUAN_ZONE);
            datePicker.setValue(now.toLocalDate());
            hourSpinner.getValueFactory().setValue(now.getHour());
            minuteSpinner.getValueFactory().setValue(now.getMinute());
        });

        grid.add(new Label("Fecha:"), 0, 0);
        grid.add(datePicker, 1, 0, 3, 1);
        grid.add(new Label("Hora:"), 0, 1);
        grid.add(hourSpinner, 1, 1);
        grid.add(new Label(":"), 2, 1);
        grid.add(minuteSpinner, 3, 1);
        grid.add(btnAhora, 4, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == guardarBtn) {
                return LocalDateTime.of(
                    datePicker.getValue(),
                    LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue())
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    /**
     * Shows a dialog to edit a sale's date/time.
     *
     * @param saleId sale ID for display
     * @param currentDateTime current date/time of the sale
     * @return Optional with new date/time, empty if cancelled
     */
    public static Optional<LocalDateTime> showForSaleEdit(int saleId, LocalDateTime currentDateTime) {
        return show("Editar Fecha de Venta", "Venta #" + saleId, currentDateTime);
    }

    /**
     * Shows a dialog to select date/time for a new sale.
     *
     * @param currentDateTime current date/time
     * @return Optional with selected date/time, empty if cancelled
     */
    public static Optional<LocalDateTime> showForNewSale(LocalDateTime currentDateTime) {
        return show("Cambiar Fecha/Hora de Venta", "Seleccione la fecha y hora para esta venta", currentDateTime);
    }
}
