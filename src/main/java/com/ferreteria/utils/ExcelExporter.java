package com.ferreteria.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
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
 * Utilidad para generar reportes en formato Excel (.xlsx) usando Apache POI.
 *
 * @author Sistema Ferretería
 * @version 1.0
 */
public class ExcelExporter {
    private static final Logger LOGGER = Logger.getLogger(ExcelExporter.class.getName());
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    /**
     * Genera un archivo Excel con el reporte de ventas.
     *
     * @param productData Lista de datos de productos vendidos
     * @param reportPeriod Período del reporte (YearMonth)
     * @param paymentTotals Totales por método de pago
     * @param statistics Estadísticas generales (totalRecaudado, totalVentas, etc.)
     * @param outputFile Archivo de salida donde se guardará el Excel
     * @throws IOException Si hay un error al generar el archivo
     */
    public static void generateReportExcel(
            List<ReportRow> productData,
            YearMonth reportPeriod,
            Map<String, BigDecimal> paymentTotals,
            Map<String, Object> statistics,
            File outputFile) throws IOException {

        LOGGER.info("Generando Excel en: " + outputFile.getAbsolutePath());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte de Ventas");

            // Crear estilos
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle normalStyle = createNormalStyle(workbook);
            CellStyle totalStyle = createTotalStyle(workbook);

            int rowNum = 0;

            // Título del reporte
            rowNum = createTitle(sheet, rowNum, reportPeriod, titleStyle);
            rowNum += 2; // Espacio

            // Estadísticas generales
            rowNum = createStatisticsSection(sheet, rowNum, statistics, headerStyle, normalStyle, currencyStyle);
            rowNum += 2; // Espacio

            // Métodos de pago
            rowNum = createPaymentMethodsSection(sheet, rowNum, paymentTotals, headerStyle, normalStyle, currencyStyle);
            rowNum += 2; // Espacio

            // Tabla de productos
            rowNum = createProductsTable(sheet, rowNum, productData, headerStyle, normalStyle, currencyStyle, totalStyle);

            // Auto-ajustar ancho de columnas
            autoSizeColumns(sheet, 6);

            // Guardar archivo
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                workbook.write(fileOut);
            }

            LOGGER.info("Excel generado exitosamente");
        }
    }

    /**
     * Crea el título del reporte.
     */
    private static int createTitle(Sheet sheet, int rowNum, YearMonth period, CellStyle titleStyle) {
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("REPORTE DE VENTAS");
        titleCell.setCellStyle(titleStyle);

        // Fusionar celdas para el título
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));

        // Período
        Row periodRow = sheet.createRow(rowNum++);
        Cell periodCell = periodRow.createCell(0);
        String monthName = period.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
        periodCell.setCellValue("Período: " + monthName + " " + period.getYear());

        // Fecha de generación
        Row dateRow = sheet.createRow(rowNum++);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue("Fecha de generación: " + java.time.LocalDate.now().toString());

        return rowNum;
    }

    /**
     * Crea la sección de estadísticas.
     */
    private static int createStatisticsSection(Sheet sheet, int rowNum,
            Map<String, Object> statistics, CellStyle headerStyle,
            CellStyle normalStyle, CellStyle currencyStyle) {

        // Encabezado de sección
        Row headerRow = sheet.createRow(rowNum++);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("Estadísticas Generales");
        headerCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));

        BigDecimal totalRecaudado = (BigDecimal) statistics.get("totalRecaudado");
        Integer totalVentas = (Integer) statistics.get("totalVentas");
        BigDecimal promedioVenta = (BigDecimal) statistics.get("promedioVenta");
        BigDecimal ventaMaxima = (BigDecimal) statistics.get("ventaMaxima");

        // Total Recaudado
        Row row1 = sheet.createRow(rowNum++);
        row1.createCell(0).setCellValue("Total Recaudado:");
        Cell totalCell = row1.createCell(1);
        totalCell.setCellValue(totalRecaudado.doubleValue());
        totalCell.setCellStyle(currencyStyle);

        // Total de Ventas
        Row row2 = sheet.createRow(rowNum++);
        row2.createCell(0).setCellValue("Total de Ventas:");
        row2.createCell(1).setCellValue(totalVentas);

        // Promedio por Venta
        Row row3 = sheet.createRow(rowNum++);
        row3.createCell(0).setCellValue("Promedio por Venta:");
        Cell avgCell = row3.createCell(1);
        avgCell.setCellValue(promedioVenta.doubleValue());
        avgCell.setCellStyle(currencyStyle);

        // Venta Máxima
        Row row4 = sheet.createRow(rowNum++);
        row4.createCell(0).setCellValue("Venta Máxima:");
        Cell maxCell = row4.createCell(1);
        maxCell.setCellValue(ventaMaxima.doubleValue());
        maxCell.setCellStyle(currencyStyle);

        return rowNum;
    }

    /**
     * Crea la sección de métodos de pago.
     */
    private static int createPaymentMethodsSection(Sheet sheet, int rowNum,
            Map<String, BigDecimal> paymentTotals, CellStyle headerStyle,
            CellStyle normalStyle, CellStyle currencyStyle) {

        // Encabezado de sección
        Row headerRow = sheet.createRow(rowNum++);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("Métodos de Pago");
        headerCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));

        // Datos de métodos de pago
        for (Map.Entry<String, BigDecimal> entry : paymentTotals.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(formatPaymentMethodName(entry.getKey()) + ":");
            Cell amountCell = row.createCell(1);
            amountCell.setCellValue(entry.getValue().doubleValue());
            amountCell.setCellStyle(currencyStyle);
        }

        return rowNum;
    }

    /**
     * Crea la tabla de productos vendidos.
     */
    private static int createProductsTable(Sheet sheet, int rowNum,
            List<ReportRow> productData, CellStyle headerStyle,
            CellStyle normalStyle, CellStyle currencyStyle, CellStyle totalStyle) {

        // Encabezado de sección
        Row sectionHeaderRow = sheet.createRow(rowNum++);
        Cell sectionHeaderCell = sectionHeaderRow.createCell(0);
        sectionHeaderCell.setCellValue("Productos Vendidos");
        sectionHeaderCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));

        rowNum++; // Espacio

        // Encabezados de tabla
        Row tableHeaderRow = sheet.createRow(rowNum++);
        String[] headers = {"#", "Producto", "Variante", "Cantidad", "Precio Unitario", "Total"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = tableHeaderRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Datos de productos
        int itemNumber = 1;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (ReportRow row : productData) {
            Row dataRow = sheet.createRow(rowNum++);

            // Número
            dataRow.createCell(0).setCellValue(itemNumber++);

            // Producto
            dataRow.createCell(1).setCellValue(row.getProducto());

            // Variante
            dataRow.createCell(2).setCellValue(row.getVariante() != null ? row.getVariante() : "N/A");

            // Cantidad
            dataRow.createCell(3).setCellValue(row.getCantidad());

            // Precio Unitario
            Cell priceCell = dataRow.createCell(4);
            priceCell.setCellValue(row.getPrecio().doubleValue());
            priceCell.setCellStyle(currencyStyle);

            // Total
            Cell totalCell = dataRow.createCell(5);
            totalCell.setCellValue(row.getTotal().doubleValue());
            totalCell.setCellStyle(currencyStyle);

            grandTotal = grandTotal.add(row.getTotal());
        }

        // Fila de totales
        Row totalRow = sheet.createRow(rowNum++);
        Cell totalLabelCell = totalRow.createCell(4);
        totalLabelCell.setCellValue("TOTAL:");
        totalLabelCell.setCellStyle(totalStyle);

        Cell grandTotalCell = totalRow.createCell(5);
        grandTotalCell.setCellValue(grandTotal.doubleValue());
        grandTotalCell.setCellStyle(totalStyle);

        return rowNum;
    }

    /**
     * Crea el estilo para el título.
     */
    private static CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Crea el estilo para encabezados.
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Crea el estilo para celdas de moneda.
     */
    private static CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    /**
     * Crea el estilo normal.
     */
    private static CellStyle createNormalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    /**
     * Crea el estilo para la fila de totales.
     */
    private static CellStyle createTotalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setBorderTop(BorderStyle.MEDIUM);
        return style;
    }

    /**
     * Auto-ajusta el ancho de las columnas.
     */
    private static void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            // Agregar un poco de padding
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, currentWidth + 1000);
        }
    }

    /**
     * Formatea el nombre del método de pago.
     */
    private static String formatPaymentMethodName(String method) {
        switch (method.toLowerCase()) {
            case "efectivo":
                return "Efectivo";
            case "tarjeta_debito":
                return "Tarjeta Débito";
            case "tarjeta_credito":
                return "Tarjeta Crédito";
            case "transferencia":
                return "Transferencia";
            default:
                return method;
        }
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
