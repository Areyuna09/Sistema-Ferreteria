package com.ferreteria.utils;

import com.ferreteria.models.Sale;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

import java.time.format.DateTimeFormatter;

/**
 * Utility class for generating and displaying sale tickets.
 */
public class TicketGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Generates ticket content for a sale.
     *
     * @param sale the sale
     * @param vendorName name of the seller
     * @return formatted ticket string
     */
    public static String generate(Sale sale, String vendorName) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════\n");
        sb.append("         FERRETERIA\n");
        sb.append("═══════════════════════════════\n\n");
        sb.append("Venta #").append(sale.getId()).append("\n");
        sb.append("Fecha: ").append(sale.getCreatedAt().format(DATE_FORMAT)).append("\n");
        sb.append("Vendedor: ").append(vendorName).append("\n\n");

        sb.append("───────────────────────────────\n");
        sale.getItems().forEach(item -> {
            sb.append(String.format("%s\n  %d x $%,.2f = $%,.2f\n",
                item.getDisplayName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()));
        });

        sb.append("───────────────────────────────\n");
        sb.append(String.format("TOTAL: $%,.2f\n", sale.getTotal()));
        sb.append("───────────────────────────────\n\n");
        sb.append("    ¡Gracias por su compra!\n");
        sb.append("═══════════════════════════════\n");

        return sb.toString();
    }

    /**
     * Shows a ticket dialog for a completed sale.
     *
     * @param sale the sale
     * @param vendorName name of the seller
     */
    public static void showTicketDialog(Sale sale, String vendorName) {
        String ticketContent = generate(sale, vendorName);

        Alert ticket = new Alert(Alert.AlertType.INFORMATION);
        ticket.setTitle("Venta Completada");
        ticket.setHeaderText("Venta #" + sale.getId() + " registrada exitosamente");

        TextArea textArea = new TextArea(ticketContent);
        textArea.setEditable(false);
        textArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        textArea.setPrefRowCount(18);
        textArea.setPrefColumnCount(35);

        ticket.getDialogPane().setContent(textArea);
        ticket.getDialogPane().setMinWidth(400);
        ticket.showAndWait();
    }
}
