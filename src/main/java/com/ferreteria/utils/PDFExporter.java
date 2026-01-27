package com.ferreteria.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utilidad para generar reportes en formato PDF usando Apache PDFBox.
 *
 * @author Sistema Ferretería
 * @version 1.0
 */
public class PDFExporter {
    private static final Logger LOGGER = Logger.getLogger(PDFExporter.class.getName());
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    // Configuración de diseño
    private static final float MARGIN = 50;
    private static final float TITLE_FONT_SIZE = 16;
    private static final float HEADER_FONT_SIZE = 12;
    private static final float TEXT_FONT_SIZE = 10;
    private static final float ROW_HEIGHT = 20;
    private static final float CELL_PADDING = 5;

    /**
     * Genera un PDF con el reporte de ventas.
     *
     * @param productData Lista de datos de productos vendidos
     * @param reportPeriod Período del reporte (YearMonth)
     * @param paymentTotals Totales por método de pago
     * @param statistics Estadísticas generales (totalRecaudado, totalVentas, etc.)
     * @param outputFile Archivo de salida donde se guardará el PDF
     * @throws IOException Si hay un error al generar el PDF
     */
    public static void generateReportPDF(
            List<ReportRow> productData,
            YearMonth reportPeriod,
            Map<String, BigDecimal> paymentTotals,
            Map<String, Object> statistics,
            File outputFile) throws IOException {

        LOGGER.info("Generando PDF en: " + outputFile.getAbsolutePath());
        LOGGER.info("Total de productos a exportar: " + productData.size());

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float yPosition;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                yPosition = page.getMediaBox().getHeight() - MARGIN;

                // Encabezado del documento
                yPosition = drawHeader(contentStream, reportPeriod, yPosition);
                yPosition -= 30;

                // Estadísticas generales
                yPosition = drawStatistics(contentStream, statistics, yPosition);
                yPosition -= 30;

                // Métodos de pago
                yPosition = drawPaymentMethods(contentStream, paymentTotals, yPosition);
                yPosition -= 30;
            }

            // Dibujar tabla de productos con soporte para múltiples páginas
            drawProductsTableMultiPage(document, productData, yPosition, page);

            document.save(outputFile);
            LOGGER.info("PDF generado exitosamente con " + document.getNumberOfPages() + " página(s)");
        }
    }

    /**
     * Dibuja el encabezado del documento.
     */
    private static float drawHeader(PDPageContentStream contentStream, YearMonth period, float yPosition)
            throws IOException {

        // Título del reporte
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, TITLE_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("REPORTE DE VENTAS");
        contentStream.endText();

        yPosition -= 25;

        // Período
        String monthName = period.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
        String periodText = monthName + " " + period.getYear();

        contentStream.setFont(PDType1Font.HELVETICA, HEADER_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Periodo: " + periodText);
        contentStream.endText();

        yPosition -= 20;

        // Fecha de generación
        contentStream.setFont(PDType1Font.HELVETICA, TEXT_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Fecha de generacion: " + java.time.LocalDate.now().toString());
        contentStream.endText();

        // Línea separadora
        yPosition -= 15;
        contentStream.setStrokingColor(Color.LIGHT_GRAY);
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPosition);
        contentStream.lineTo(PDRectangle.A4.getWidth() - MARGIN, yPosition);
        contentStream.stroke();

        return yPosition - 10;
    }

    /**
     * Dibuja la sección de estadísticas.
     */
    private static float drawStatistics(PDPageContentStream contentStream, Map<String, Object> stats, float yPosition)
            throws IOException {

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, HEADER_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Estadisticas Generales");
        contentStream.endText();

        yPosition -= 25;

        BigDecimal totalRecaudado = (BigDecimal) stats.get("totalRecaudado");
        Integer totalVentas = (Integer) stats.get("totalVentas");
        BigDecimal promedioVenta = (BigDecimal) stats.get("promedioVenta");
        BigDecimal ventaMaxima = (BigDecimal) stats.get("ventaMaxima");

        contentStream.setFont(PDType1Font.HELVETICA, TEXT_FONT_SIZE);

        // Total recaudado
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Total Recaudado: " + CURRENCY_FORMAT.format(totalRecaudado));
        contentStream.endText();
        yPosition -= 18;

        // Total de ventas
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Total de Ventas: " + totalVentas);
        contentStream.endText();
        yPosition -= 18;

        // Promedio por venta
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Promedio por Venta: " + CURRENCY_FORMAT.format(promedioVenta));
        contentStream.endText();
        yPosition -= 18;

        // Venta máxima
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Venta Maxima: " + CURRENCY_FORMAT.format(ventaMaxima));
        contentStream.endText();

        return yPosition - 10;
    }

    /**
     * Dibuja la sección de métodos de pago.
     */
    private static float drawPaymentMethods(PDPageContentStream contentStream,
            Map<String, BigDecimal> paymentTotals, float yPosition) throws IOException {

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, HEADER_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Metodos de Pago");
        contentStream.endText();

        yPosition -= 25;

        contentStream.setFont(PDType1Font.HELVETICA, TEXT_FONT_SIZE);

        for (Map.Entry<String, BigDecimal> entry : paymentTotals.entrySet()) {
            String methodName = formatPaymentMethodName(entry.getKey());
            String amount = CURRENCY_FORMAT.format(entry.getValue());

            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText(methodName + ": " + amount);
            contentStream.endText();
            yPosition -= 18;
        }

        return yPosition - 10;
    }

    /**
     * Dibuja la tabla de productos con soporte para múltiples páginas.
     */
    private static void drawProductsTableMultiPage(PDDocument document, List<ReportRow> productData,
            float startYPosition, PDPage firstPage) throws IOException {

        float pageWidth = firstPage.getMediaBox().getWidth();
        float pageHeight = firstPage.getMediaBox().getHeight();

        // Configuración de columnas
        float tableWidth = pageWidth - (2 * MARGIN);
        float col1Width = tableWidth * 0.05f;
        float col2Width = tableWidth * 0.30f;
        float col3Width = tableWidth * 0.20f;
        float col4Width = tableWidth * 0.15f;
        float col5Width = tableWidth * 0.15f;
        float col6Width = tableWidth * 0.15f;
        float[] columnWidths = {col1Width, col2Width, col3Width, col4Width, col5Width, col6Width};
        String[] headers = {"#", "Producto", "Variante", "Cantidad", "Precio Unit.", "Total"};

        float yPosition = startYPosition;
        int rowNumber = 1;
        int currentRowIndex = 0;
        boolean isFirstPage = true;

        while (currentRowIndex < productData.size()) {
            PDPage currentPage;
            PDPageContentStream contentStream;

            if (isFirstPage) {
                // Usar la primera página existente
                currentPage = firstPage;
                contentStream = new PDPageContentStream(document, currentPage, PDPageContentStream.AppendMode.APPEND, true);
                isFirstPage = false;
            } else {
                // Crear nueva página
                currentPage = new PDPage(PDRectangle.A4);
                document.addPage(currentPage);
                contentStream = new PDPageContentStream(document, currentPage);
                yPosition = pageHeight - MARGIN;

                // Encabezado de continuación
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, HEADER_FONT_SIZE);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN, yPosition);
                contentStream.showText("Productos Vendidos (continuacion)");
                contentStream.endText();
                yPosition -= 30;
            }

            try {
                // Solo dibujar título y encabezado de tabla en la primera iteración de la primera página
                if (currentRowIndex == 0) {
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, HEADER_FONT_SIZE);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN, yPosition);
                    contentStream.showText("Productos Vendidos (" + productData.size() + " productos)");
                    contentStream.endText();
                    yPosition -= 30;
                }

                // Encabezados de tabla
                yPosition = drawTableHeader(contentStream, yPosition, columnWidths, headers);
                yPosition -= ROW_HEIGHT;

                // Dibujar filas hasta que se acabe el espacio o los datos
                contentStream.setFont(PDType1Font.HELVETICA, TEXT_FONT_SIZE);

                while (currentRowIndex < productData.size() && yPosition > MARGIN + 30) {
                    ReportRow row = productData.get(currentRowIndex);
                    float xPosition = MARGIN;

                    // Número
                    drawCell(contentStream, String.valueOf(rowNumber), xPosition, yPosition, col1Width, true);
                    xPosition += col1Width;

                    // Producto
                    drawCell(contentStream, truncateText(row.getProducto(), 25), xPosition, yPosition, col2Width, false);
                    xPosition += col2Width;

                    // Variante
                    drawCell(contentStream, truncateText(row.getVariante(), 15), xPosition, yPosition, col3Width, false);
                    xPosition += col3Width;

                    // Cantidad
                    drawCell(contentStream, String.valueOf(row.getCantidad()), xPosition, yPosition, col4Width, true);
                    xPosition += col4Width;

                    // Precio
                    drawCell(contentStream, CURRENCY_FORMAT.format(row.getPrecio()), xPosition, yPosition, col5Width, true);
                    xPosition += col5Width;

                    // Total
                    drawCell(contentStream, CURRENCY_FORMAT.format(row.getTotal()), xPosition, yPosition, col6Width, true);

                    yPosition -= ROW_HEIGHT;
                    rowNumber++;
                    currentRowIndex++;
                }

            } finally {
                contentStream.close();
            }
        }
    }

    /**
     * Dibuja la tabla de productos vendidos (método legacy para compatibilidad).
     */
    private static float drawProductsTable(PDPageContentStream contentStream,
            List<ReportRow> productData, float yPosition, float pageWidth) throws IOException {

        contentStream.setFont(PDType1Font.HELVETICA_BOLD, HEADER_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Productos Vendidos");
        contentStream.endText();

        yPosition -= 30;

        // Configuración de columnas
        float tableWidth = pageWidth - (2 * MARGIN);
        float col1Width = tableWidth * 0.05f;
        float col2Width = tableWidth * 0.30f;
        float col3Width = tableWidth * 0.20f;
        float col4Width = tableWidth * 0.15f;
        float col5Width = tableWidth * 0.15f;
        float col6Width = tableWidth * 0.15f;

        // Encabezados de tabla
        yPosition = drawTableHeader(contentStream, yPosition,
            new float[]{col1Width, col2Width, col3Width, col4Width, col5Width, col6Width},
            new String[]{"#", "Producto", "Variante", "Cantidad", "Precio Unit.", "Total"});

        yPosition -= ROW_HEIGHT;

        // Filas de datos
        contentStream.setFont(PDType1Font.HELVETICA, TEXT_FONT_SIZE);
        int rowNumber = 1;

        for (ReportRow row : productData) {
            if (yPosition < MARGIN + 50) {
                break;
            }

            float xPosition = MARGIN;

            drawCell(contentStream, String.valueOf(rowNumber), xPosition, yPosition, col1Width, true);
            xPosition += col1Width;

            drawCell(contentStream, truncateText(row.getProducto(), 25), xPosition, yPosition, col2Width, false);
            xPosition += col2Width;

            drawCell(contentStream, truncateText(row.getVariante(), 15), xPosition, yPosition, col3Width, false);
            xPosition += col3Width;

            drawCell(contentStream, String.valueOf(row.getCantidad()), xPosition, yPosition, col4Width, true);
            xPosition += col4Width;

            drawCell(contentStream, CURRENCY_FORMAT.format(row.getPrecio()), xPosition, yPosition, col5Width, true);
            xPosition += col5Width;

            drawCell(contentStream, CURRENCY_FORMAT.format(row.getTotal()), xPosition, yPosition, col6Width, true);

            yPosition -= ROW_HEIGHT;
            rowNumber++;
        }

        return yPosition;
    }

    /**
     * Dibuja el encabezado de la tabla.
     */
    private static float drawTableHeader(PDPageContentStream contentStream, float yPosition,
            float[] columnWidths, String[] headers) throws IOException {

        // Fondo gris para encabezados
        contentStream.setNonStrokingColor(new Color(240, 240, 240));
        contentStream.addRect(MARGIN, yPosition - ROW_HEIGHT + 5,
            sumArray(columnWidths), ROW_HEIGHT);
        contentStream.fill();
        contentStream.setNonStrokingColor(Color.BLACK);

        // Textos de encabezado
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, TEXT_FONT_SIZE);
        float xPosition = MARGIN;

        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(xPosition + CELL_PADDING, yPosition - 15);
            contentStream.showText(headers[i]);
            contentStream.endText();
            xPosition += columnWidths[i];
        }

        // Línea inferior del encabezado
        contentStream.setStrokingColor(Color.LIGHT_GRAY);
        contentStream.setLineWidth(1);
        contentStream.moveTo(MARGIN, yPosition - ROW_HEIGHT + 5);
        contentStream.lineTo(MARGIN + sumArray(columnWidths), yPosition - ROW_HEIGHT + 5);
        contentStream.stroke();

        return yPosition;
    }

    /**
     * Dibuja una celda de la tabla.
     */
    private static void drawCell(PDPageContentStream contentStream, String text,
            float x, float y, float width, boolean center) throws IOException {

        contentStream.beginText();

        float textWidth = PDType1Font.HELVETICA.getStringWidth(text) / 1000 * TEXT_FONT_SIZE;
        float xOffset = center ? (width - textWidth) / 2 : CELL_PADDING;

        contentStream.newLineAtOffset(x + xOffset, y - 15);
        contentStream.showText(text);
        contentStream.endText();
    }

    /**
     * Formatea el nombre del método de pago.
     */
    private static String formatPaymentMethodName(String method) {
        switch (method.toLowerCase()) {
            case "efectivo":
                return "Efectivo";
            case "tarjeta_debito":
                return "Tarjeta Debito";
            case "tarjeta_credito":
                return "Tarjeta Credito";
            case "transferencia":
                return "Transferencia";
            default:
                return method;
        }
    }

    /**
     * Trunca un texto si excede la longitud máxima.
     */
    private static String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }

    /**
     * Suma los valores de un array de floats.
     */
    private static float sumArray(float[] array) {
        float sum = 0;
        for (float value : array) {
            sum += value;
        }
        return sum;
    }

    /**
     * Clase para representar una fila del reporte.
     */
    public static class ReportRow {
        private final String producto;
        private final String variante;
        private final Integer cantidad;
        private final BigDecimal precio;
        private final BigDecimal total;

        public ReportRow(String producto, String variante, Integer cantidad,
                        BigDecimal precio, BigDecimal total) {
            this.producto = producto;
            this.variante = variante;
            this.cantidad = cantidad;
            this.precio = precio;
            this.total = total;
        }

        public String getProducto() { return producto; }
        public String getVariante() { return variante; }
        public Integer getCantidad() { return cantidad; }
        public BigDecimal getPrecio() { return precio; }
        public BigDecimal getTotal() { return total; }
    }
}
