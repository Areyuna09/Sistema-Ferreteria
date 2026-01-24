package com.ferreteria.controllers;

import com.ferreteria.Main;
import com.ferreteria.models.*;
import com.ferreteria.models.dao.*;
import com.ferreteria.utils.AppLogger;
import com.ferreteria.utils.DebugFormatter;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

/**
 * Panel de Debug para testing de métodos DAO y SQL.
 */
public class DebugController {

    @FXML private ComboBox<String> moduloCombo;
    @FXML private ListView<String> metodosListView;
    @FXML private VBox parametrosContainer;
    @FXML private Button ejecutarBtn;
    @FXML private TextArea consolaOutput;
    @FXML private TextArea sqlInput;
    @FXML private TextArea sqlOutput;
    @FXML private TextArea datosOutput;
    @FXML private TextArea logsOutput;

    private DatabaseConfig dbConfig;
    private SaleDAO saleDAO;
    private ProductVariantDAO variantDAO;
    private UserDAO userDAO;

    private Map<String, List<MetodoInfo>> metodosPorModulo;
    private MetodoInfo metodoSeleccionado;
    private Map<String, TextField> camposParametros = new HashMap<>();

    @FXML
    public void initialize() {
        dbConfig = DatabaseConfig.getInstance();
        saleDAO = new SaleDAO(dbConfig);
        variantDAO = new ProductVariantDAO(dbConfig);
        userDAO = new UserDAO(dbConfig);

        setupModulos();
        setupMetodosListView();
        setupLogsTab();

        log("DEBUG PANEL INICIALIZADO");
        log("Base de datos: " + dbConfig.getDbPath());
        log("─".repeat(50));
    }

    private void setupModulos() {
        metodosPorModulo = new LinkedHashMap<>();
        metodosPorModulo.put("Ventas", createVentasMetodos());
        metodosPorModulo.put("Productos", createProductosMetodos());
        metodosPorModulo.put("Usuarios", createUsuariosMetodos());

        moduloCombo.setItems(FXCollections.observableArrayList(metodosPorModulo.keySet()));
        moduloCombo.setValue("Ventas");
        handleModuloChange();
    }

    private List<MetodoInfo> createVentasMetodos() {
        List<MetodoInfo> metodos = new ArrayList<>();
        metodos.add(new MetodoInfo("findAll", "Lista todas las ventas", List.of(),
            () -> DebugFormatter.formatSales(saleDAO.findAll())));
        metodos.add(new MetodoInfo("findCompleted", "Lista ventas completadas", List.of(),
            () -> DebugFormatter.formatSales(saleDAO.findCompleted())));
        metodos.add(new MetodoInfo("findCancelled", "Lista ventas anuladas", List.of(),
            () -> DebugFormatter.formatSales(saleDAO.findCancelled())));
        metodos.add(new MetodoInfo("findById", "Busca venta por ID", List.of("id (int)"),
            () -> saleDAO.findById(getIntParam("id (int)"))
                .map(DebugFormatter::formatSaleDetail).orElse("Venta no encontrada")));
        metodos.add(new MetodoInfo("dailyTotal", "Total vendido en fecha", List.of("fecha (yyyy-MM-dd)"),
            () -> "Total: $" + String.format("%,.2f", saleDAO.dailyTotal(getDateParam("fecha (yyyy-MM-dd)")))));
        metodos.add(new MetodoInfo("monthlyTotal", "Total vendido en mes", List.of("año (int)", "mes (int)"),
            () -> "Total: $" + String.format("%,.2f", saleDAO.monthlyTotal(getIntParam("año (int)"), getIntParam("mes (int)")))));
        metodos.add(new MetodoInfo("cancel", "Anula una venta", List.of("id (int)"), () -> {
            saleDAO.cancel(getIntParam("id (int)"));
            return "Venta anulada correctamente";
        }));
        return metodos;
    }

    private List<MetodoInfo> createProductosMetodos() {
        List<MetodoInfo> metodos = new ArrayList<>();
        metodos.add(new MetodoInfo("listarDisponibles", "Variantes con stock", List.of(),
            () -> DebugFormatter.formatVariantes(variantDAO.listarDisponibles())));
        metodos.add(new MetodoInfo("listarStockBajo", "Variantes con stock bajo", List.of(),
            () -> DebugFormatter.formatVariantes(variantDAO.listarStockBajo())));
        metodos.add(new MetodoInfo("buscar", "Busca productos", List.of("query (texto)", "limite (int)"),
            () -> DebugFormatter.formatVariantes(variantDAO.buscar(getStringParam("query (texto)"), getIntParam("limite (int)")))));
        metodos.add(new MetodoInfo("buscarPorId", "Busca variante por ID", List.of("id (int)"),
            () -> variantDAO.buscarPorId(getIntParam("id (int)"))
                .map(DebugFormatter::formatVarianteDetalle).orElse("Variante no encontrada")));
        return metodos;
    }

    private List<MetodoInfo> createUsuariosMetodos() {
        List<MetodoInfo> metodos = new ArrayList<>();
        metodos.add(new MetodoInfo("listarTodos", "Lista todos los usuarios", List.of(),
            () -> DebugFormatter.formatUsuarios(userDAO.findAll())));
        metodos.add(new MetodoInfo("buscarPorId", "Busca por ID", List.of("id (int)"),
            () -> userDAO.findById(getIntParam("id (int)"))
                .map(DebugFormatter::formatUsuarioDetalle).orElse("Usuario no encontrado")));
        metodos.add(new MetodoInfo("buscarPorUsername", "Busca por username", List.of("username (texto)"),
            () -> userDAO.findByUsername(getStringParam("username (texto)"))
                .map(DebugFormatter::formatUsuarioDetalle).orElse("Usuario no encontrado")));
        return metodos;
    }

    private void setupMetodosListView() {
        metodosListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                var metodos = metodosPorModulo.get(moduloCombo.getValue());
                metodoSeleccionado = metodos.stream()
                    .filter(m -> m.displayName().equals(newVal)).findFirst().orElse(null);
                if (metodoSeleccionado != null) {
                    setupParametros(metodoSeleccionado);
                    ejecutarBtn.setDisable(false);
                }
            }
        });
    }

    private void setupLogsTab() {
        if (logsOutput != null) {
            AppLogger.setUiOutput(logsOutput);
            logsOutput.setText("=== LOGS DEL SISTEMA ===\n");
        }
    }

    @FXML
    public void handleModuloChange() {
        String modulo = moduloCombo.getValue();
        if (modulo == null) return;

        var metodos = metodosPorModulo.get(modulo);
        metodosListView.setItems(FXCollections.observableArrayList(
            metodos.stream().map(MetodoInfo::displayName).toList()));

        parametrosContainer.getChildren().clear();
        parametrosContainer.getChildren().add(new Label("Selecciona un método") {{
            setStyle("-fx-text-fill: #666;");
        }});
        ejecutarBtn.setDisable(true);
        metodoSeleccionado = null;
    }

    private void setupParametros(MetodoInfo metodo) {
        parametrosContainer.getChildren().clear();
        camposParametros.clear();

        if (metodo.parametros().isEmpty()) {
            parametrosContainer.getChildren().add(new Label("Sin parámetros") {{
                setStyle("-fx-text-fill: #22c55e; -fx-font-style: italic;");
            }});
        } else {
            for (String param : metodo.parametros()) {
                VBox box = new VBox(4);
                Label lbl = new Label(param);
                lbl.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                TextField field = new TextField();
                field.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #22c55e;");
                setDefaultValue(field, param);
                camposParametros.put(param, field);
                box.getChildren().addAll(lbl, field);
                parametrosContainer.getChildren().add(box);
            }
        }
    }

    private void setDefaultValue(TextField field, String param) {
        if (param.contains("fecha")) field.setText(LocalDate.now().toString());
        else if (param.contains("limite")) field.setText("10");
        else if (param.contains("año")) field.setText(String.valueOf(LocalDate.now().getYear()));
        else if (param.contains("mes")) field.setText(String.valueOf(LocalDate.now().getMonthValue()));
    }

    @FXML
    public void handleEjecutar() {
        if (metodoSeleccionado == null) return;

        log("\n" + "═".repeat(50));
        log("EJECUTANDO: " + moduloCombo.getValue() + "." + metodoSeleccionado.nombre());

        try {
            long inicio = System.currentTimeMillis();
            String resultado = metodoSeleccionado.ejecutor().get();
            log("RESULTADO:\n" + resultado);
            log("Tiempo: " + (System.currentTimeMillis() - inicio) + "ms | Estado: OK");
        } catch (Exception e) {
            log("ERROR: " + e.getMessage());
            AppLogger.error("Debug", e.getMessage(), e);
        }
        log("═".repeat(50) + "\n");
    }

    @FXML
    public void handleEjecutarSQL() {
        String sql = sqlInput.getText().trim();
        if (sql.isEmpty() || !sql.toUpperCase().startsWith("SELECT")) {
            sqlOutput.setText("Solo se permiten queries SELECT");
            return;
        }

        try (Statement stmt = dbConfig.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            sqlOutput.setText(formatResultSet(rs));
        } catch (SQLException e) {
            sqlOutput.setText("ERROR: " + e.getMessage());
        }
    }

    private String formatResultSet(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        for (int i = 1; i <= cols; i++) sb.append(String.format("%-20s", meta.getColumnName(i)));
        sb.append("\n").append("─".repeat(cols * 20)).append("\n");

        int filas = 0;
        while (rs.next() && filas++ < 100) {
            for (int i = 1; i <= cols; i++) {
                String val = rs.getString(i);
                sb.append(String.format("%-20s", val != null ? (val.length() > 18 ? val.substring(0, 15) + "..." : val) : "NULL"));
            }
            sb.append("\n");
        }
        sb.append("Filas: ").append(filas);
        return sb.toString();
    }

    @FXML
    public void handleVerTablas() {
        try (ResultSet rs = dbConfig.getConnection().getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            StringBuilder sb = new StringBuilder("TABLAS:\n" + "═".repeat(30) + "\n");
            while (rs.next()) sb.append("• ").append(rs.getString("TABLE_NAME")).append("\n");
            sqlOutput.setText(sb.toString());
        } catch (SQLException e) {
            sqlOutput.setText("ERROR: " + e.getMessage());
        }
    }

    @FXML
    public void handleVerEstructura() {
        try (ResultSet rs = dbConfig.getConnection().getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            StringBuilder sb = new StringBuilder("ESTRUCTURA:\n" + "═".repeat(50) + "\n\n");
            while (rs.next()) {
                String table = rs.getString("TABLE_NAME");
                sb.append("TABLE: ").append(table).append("\n").append("─".repeat(40)).append("\n");
                try (ResultSet cols = dbConfig.getConnection().getMetaData().getColumns(null, null, table, "%")) {
                    while (cols.next()) {
                        sb.append(String.format("  %-25s %s\n", cols.getString("COLUMN_NAME"), cols.getString("TYPE_NAME")));
                    }
                }
                sb.append("\n");
            }
            sqlOutput.setText(sb.toString());
        } catch (SQLException e) {
            sqlOutput.setText("ERROR: " + e.getMessage());
        }
    }

    @FXML
    public void handleCrearProductosDemo() {
        datosOutput.clear();
        logDatos("Creando productos demo...");
        try (Statement stmt = dbConfig.getConnection().createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products");
            rs.next();
            if (rs.getInt(1) > 0) {
                logDatos("Ya existen productos. No se crean duplicados.");
                return;
            }
            stmt.execute("INSERT INTO categories (name, description) VALUES ('Herramientas', 'Herramientas manuales')");
            stmt.execute("INSERT INTO products (code, name, category_id) VALUES ('MART001', 'Martillo', 1)");
            stmt.execute("INSERT INTO products (code, name, category_id) VALUES ('DEST002', 'Destornillador', 1)");
            stmt.execute("INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (1, 'MART001-STD', 'Estándar', 1500, 2500, 50, 5)");
            stmt.execute("INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (2, 'DEST002-PH2', 'PH2', 350, 600, 100, 10)");
            logDatos("✓ Productos demo creados");
        } catch (SQLException e) {
            logDatos("ERROR: " + e.getMessage());
        }
    }

    @FXML
    public void handleCrearVentaTest() {
        datosOutput.clear();
        try {
            var variantes = variantDAO.listarDisponibles();
            if (variantes.isEmpty()) {
                logDatos("No hay productos. Crea demo primero.");
                return;
            }
            ProductVariant v = variantes.get(0);
            SaleItem item = new SaleItem.Builder().variantId(v.getId()).quantity(1)
                .unitPrice(v.getSalePrice()).subtotal(v.getSalePrice())
                .productName(v.getProductName()).variantName(v.getVariantName()).build();
            SalePayment payment = new SalePayment.Builder()
                .paymentMethod(SalePayment.PaymentMethod.CASH).amount(v.getSalePrice()).build();
            Sale sale = new Sale.Builder().userId(1).total(v.getSalePrice()).status("completed")
                .addItem(item).addPayment(payment).build();
            Sale created = saleDAO.create(sale);
            logDatos("✓ Venta #" + created.getId() + " creada - $" + v.getSalePrice());
        } catch (Exception e) {
            logDatos("ERROR: " + e.getMessage());
        }
    }

    @FXML
    public void handleVerStats() {
        datosOutput.clear();
        LocalDate hoy = LocalDate.now();
        logDatos("ESTADÍSTICAS\n" + "═".repeat(40));
        logDatos("Ventas totales: " + saleDAO.count());
        logDatos("Ventas hoy: " + saleDAO.dailyCount(hoy));
        logDatos("Total hoy: $" + String.format("%,.2f", saleDAO.dailyTotal(hoy)));
        logDatos("Productos disponibles: " + variantDAO.listarDisponibles().size());
        logDatos("Usuarios activos: " + userDAO.count());
    }

    @FXML public void handleLimpiarConsola() { consolaOutput.clear(); }
    @FXML public void handleLimpiarLogs() { if (logsOutput != null) logsOutput.clear(); }

    @FXML
    public void handleAbrirArchivoLog() {
        try {
            File logFile = new File("logs/sistema.log");
            if (logFile.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(logFile);
            } else {
                logsOutput.appendText("Archivo no encontrado: logs/sistema.log\n");
            }
        } catch (Exception e) {
            logsOutput.appendText("Error abriendo archivo: " + e.getMessage() + "\n");
        }
    }

    @FXML public void handleCerrar() { Main.closeDebugPanel(); }

    private void log(String msg) { consolaOutput.appendText(msg + "\n"); }
    private void logDatos(String msg) { datosOutput.appendText(msg + "\n"); }
    private int getIntParam(String p) { return Integer.parseInt(camposParametros.get(p).getText().trim()); }
    private String getStringParam(String p) { return camposParametros.get(p).getText().trim(); }
    private LocalDate getDateParam(String p) { return LocalDate.parse(camposParametros.get(p).getText().trim()); }

    private record MetodoInfo(String nombre, String descripcion, List<String> parametros, Supplier<String> ejecutor) {
        String displayName() { return nombre + (parametros.isEmpty() ? "()" : "(" + parametros.size() + " params)"); }
    }
}
