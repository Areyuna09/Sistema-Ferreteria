package com.ferreteria.controllers;

import com.ferreteria.Main;
import com.ferreteria.models.*;
import com.ferreteria.models.dao.*;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

/**
 * Panel de Debug - Similar a Swagger para testing de métodos.
 * Permite probar los DAOs y ejecutar SQL directo.
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

        log("DEBUG PANEL INICIALIZADO");
        log("Base de datos: " + dbConfig.getDbPath());
        log("Fecha/Hora: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        log("─".repeat(50));
        log("Selecciona un módulo y método para comenzar...\n");
    }

    private void setupModulos() {
        metodosPorModulo = new LinkedHashMap<>();

        // === MÓDULO VENTAS ===
        List<MetodoInfo> metodosVentas = new ArrayList<>();
        metodosVentas.add(new MetodoInfo("findAll", "Lista todas las ventas", List.of(), () -> {
            var sales = saleDAO.findAll();
            return formatSales(sales);
        }));
        metodosVentas.add(new MetodoInfo("findCompleted", "Lista ventas completadas", List.of(), () -> {
            var sales = saleDAO.findCompleted();
            return formatSales(sales);
        }));
        metodosVentas.add(new MetodoInfo("findCancelled", "Lista ventas anuladas", List.of(), () -> {
            var sales = saleDAO.findCancelled();
            return formatSales(sales);
        }));
        metodosVentas.add(new MetodoInfo("findById", "Busca una venta por ID", List.of("id (int)"), () -> {
            int id = getIntParam("id (int)");
            var sale = saleDAO.findById(id);
            return sale.map(this::formatSaleDetail).orElse("Venta no encontrada con ID: " + id);
        }));
        metodosVentas.add(new MetodoInfo("findByDate", "Lista ventas de una fecha", List.of("fecha (yyyy-MM-dd)"), () -> {
            LocalDate fecha = getDateParam("fecha (yyyy-MM-dd)");
            var sales = saleDAO.findByDate(fecha);
            return formatSales(sales);
        }));
        metodosVentas.add(new MetodoInfo("dailyTotal", "Total vendido en una fecha", List.of("fecha (yyyy-MM-dd)"), () -> {
            LocalDate fecha = getDateParam("fecha (yyyy-MM-dd)");
            BigDecimal total = saleDAO.dailyTotal(fecha);
            return "Total del día " + fecha + ": $" + String.format("%,.2f", total);
        }));
        metodosVentas.add(new MetodoInfo("monthlyTotal", "Total vendido en un mes", List.of("año (int)", "mes (int)"), () -> {
            int year = getIntParam("año (int)");
            int month = getIntParam("mes (int)");
            BigDecimal total = saleDAO.monthlyTotal(year, month);
            return "Total del mes " + month + "/" + year + ": $" + String.format("%,.2f", total);
        }));
        metodosVentas.add(new MetodoInfo("dailyCount", "Cantidad de ventas en una fecha", List.of("fecha (yyyy-MM-dd)"), () -> {
            LocalDate fecha = getDateParam("fecha (yyyy-MM-dd)");
            int cantidad = saleDAO.dailyCount(fecha);
            return "Ventas del día " + fecha + ": " + cantidad;
        }));
        metodosVentas.add(new MetodoInfo("count", "Cantidad total de ventas", List.of(), () -> {
            return "Total de ventas en el sistema: " + saleDAO.count();
        }));
        metodosVentas.add(new MetodoInfo("cancel", "Anula una venta (revierte stock)", List.of("id (int)"), () -> {
            int id = getIntParam("id (int)");
            saleDAO.cancel(id);
            return "Venta #" + id + " anulada correctamente. Stock revertido.";
        }));
        metodosPorModulo.put("Ventas", metodosVentas);

        // === MÓDULO PRODUCTOS ===
        List<MetodoInfo> metodosProductos = new ArrayList<>();
        metodosProductos.add(new MetodoInfo("listarDisponibles", "Lista variantes con stock", List.of(), () -> {
            var variantes = variantDAO.listarDisponibles();
            return formatVariantes(variantes);
        }));
        metodosProductos.add(new MetodoInfo("listarStockBajo", "Lista variantes con stock bajo", List.of(), () -> {
            var variantes = variantDAO.listarStockBajo();
            if (variantes.isEmpty()) return "No hay productos con stock bajo";
            return formatVariantes(variantes);
        }));
        metodosProductos.add(new MetodoInfo("buscar", "Busca productos por texto", List.of("query (texto)", "limite (int)"), () -> {
            String query = getStringParam("query (texto)");
            int limite = getIntParam("limite (int)");
            var variantes = variantDAO.buscar(query, limite);
            return formatVariantes(variantes);
        }));
        metodosProductos.add(new MetodoInfo("buscarPorId", "Busca variante por ID", List.of("id (int)"), () -> {
            int id = getIntParam("id (int)");
            var variante = variantDAO.buscarPorId(id);
            return variante.map(this::formatVarianteDetalle).orElse("Variante no encontrada con ID: " + id);
        }));
        metodosProductos.add(new MetodoInfo("contarDisponibles", "Cantidad de variantes con stock", List.of(), () -> {
            return "Variantes disponibles: " + variantDAO.listarDisponibles().size();
        }));
        metodosPorModulo.put("Productos", metodosProductos);

        // === MÓDULO USUARIOS ===
        List<MetodoInfo> metodosUsuarios = new ArrayList<>();
        metodosUsuarios.add(new MetodoInfo("listarTodos", "Lista todos los usuarios", List.of(), () -> {
            var usuarios = userDAO.findAll();
            return formatUsuarios(usuarios);
        }));
        metodosUsuarios.add(new MetodoInfo("buscarPorId", "Busca usuario por ID", List.of("id (int)"), () -> {
            int id = getIntParam("id (int)");
            var usuario = userDAO.findById(id);
            return usuario.map(this::formatUsuarioDetalle).orElse("Usuario no encontrado con ID: " + id);
        }));
        metodosUsuarios.add(new MetodoInfo("buscarPorUsername", "Busca usuario por username", List.of("username (texto)"), () -> {
            String username = getStringParam("username (texto)");
            var usuario = userDAO.findByUsername(username);
            return usuario.map(this::formatUsuarioDetalle).orElse("Usuario no encontrado: " + username);
        }));
        metodosUsuarios.add(new MetodoInfo("contar", "Cantidad de usuarios", List.of(), () -> {
            return "Total de usuarios: " + userDAO.findAll().size();
        }));
        metodosUsuarios.add(new MetodoInfo("contarActivos", "Cantidad de usuarios activos", List.of(), () -> {
            return "Usuarios activos: " + userDAO.count();
        }));
        metodosPorModulo.put("Usuarios", metodosUsuarios);

        // Configurar combo
        moduloCombo.setItems(FXCollections.observableArrayList(metodosPorModulo.keySet()));
        moduloCombo.setValue("Ventas");
        handleModuloChange();
    }

    private void setupMetodosListView() {
        metodosListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(null);

                    // Crear label con mejor estilo
                    javafx.scene.control.Label label = new javafx.scene.control.Label(item);
                    label.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 13px; -fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;");
                    label.setPadding(new Insets(6, 10, 6, 10));

                    setGraphic(label);
                    setStyle("-fx-background-color: #2d2d2d; -fx-background-radius: 4; -fx-padding: 2;");

                    // Hover effect
                    setOnMouseEntered(e -> setStyle("-fx-background-color: #3d3d3d; -fx-background-radius: 4; -fx-padding: 2;"));
                    setOnMouseExited(e -> setStyle("-fx-background-color: #2d2d2d; -fx-background-radius: 4; -fx-padding: 2;"));
                }
            }
        });

        metodosListView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                String modulo = moduloCombo.getValue();
                var metodos = metodosPorModulo.get(modulo);
                metodoSeleccionado = metodos.stream()
                    .filter(m -> m.displayName().equals(newVal))
                    .findFirst()
                    .orElse(null);

                if (metodoSeleccionado != null) {
                    setupParametros(metodoSeleccionado);
                    ejecutarBtn.setDisable(false);
                }
            }
        });
    }

    @FXML
    public void handleModuloChange() {
        String modulo = moduloCombo.getValue();
        if (modulo == null) return;

        var metodos = metodosPorModulo.get(modulo);
        var nombres = metodos.stream().map(MetodoInfo::displayName).toList();
        metodosListView.setItems(FXCollections.observableArrayList(nombres));

        parametrosContainer.getChildren().clear();
        parametrosContainer.getChildren().add(new Label("Selecciona un método") {{
            setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        }});
        ejecutarBtn.setDisable(true);
        metodoSeleccionado = null;
    }

    private void setupParametros(MetodoInfo metodo) {
        parametrosContainer.getChildren().clear();
        camposParametros.clear();

        if (metodo.parametros().isEmpty()) {
            Label noParams = new Label("Sin parámetros requeridos");
            noParams.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 12px; -fx-font-style: italic;");
            parametrosContainer.getChildren().add(noParams);
        } else {
            for (String param : metodo.parametros()) {
                VBox paramBox = new VBox(4);
                Label label = new Label(param);
                label.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 12px; -fx-font-weight: bold;");

                TextField field = new TextField();
                field.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #22c55e; -fx-font-size: 13px; -fx-font-family: 'Consolas', monospace; -fx-border-color: #444; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");
                field.setPromptText("Ingresa " + param);

                // Valores por defecto útiles
                if (param.contains("fecha")) {
                    field.setText(LocalDate.now().toString());
                } else if (param.contains("limite")) {
                    field.setText("10");
                } else if (param.contains("año")) {
                    field.setText(String.valueOf(LocalDate.now().getYear()));
                } else if (param.contains("mes")) {
                    field.setText(String.valueOf(LocalDate.now().getMonthValue()));
                }

                camposParametros.put(param, field);
                paramBox.getChildren().addAll(label, field);
                parametrosContainer.getChildren().add(paramBox);
            }
        }
    }

    @FXML
    public void handleEjecutar() {
        if (metodoSeleccionado == null) return;

        log("\n" + "═".repeat(50));
        log("EJECUTANDO: " + moduloCombo.getValue() + "." + metodoSeleccionado.nombre());
        log("Descripción: " + metodoSeleccionado.descripcion());

        if (!metodoSeleccionado.parametros().isEmpty()) {
            log("Parámetros:");
            for (String param : metodoSeleccionado.parametros()) {
                String valor = camposParametros.get(param).getText();
                log("  - " + param + " = " + valor);
            }
        }

        log("─".repeat(50));

        try {
            long inicio = System.currentTimeMillis();
            String resultado = metodoSeleccionado.ejecutor().get();
            long duracion = System.currentTimeMillis() - inicio;

            log("RESULTADO:");
            log(resultado);
            log("─".repeat(50));
            log("Tiempo de ejecución: " + duracion + "ms");
            log("Estado: OK");

        } catch (Exception e) {
            log("ERROR: " + e.getMessage());
            log("Tipo: " + e.getClass().getSimpleName());
            e.printStackTrace();
        }

        log("═".repeat(50) + "\n");
    }

    @FXML
    public void handleEjecutarSQL() {
        String sql = sqlInput.getText().trim();
        if (sql.isEmpty()) {
            sqlOutput.setText("Ingresa una query SQL");
            return;
        }

        // Seguridad: solo permitir SELECT
        if (!sql.toUpperCase().startsWith("SELECT")) {
            sqlOutput.setText("ERROR: Solo se permiten queries SELECT por seguridad.\n\nPara modificar datos, usa los métodos del panel izquierdo.");
            return;
        }

        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            StringBuilder sb = new StringBuilder();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            // Headers
            for (int i = 1; i <= cols; i++) {
                sb.append(String.format("%-20s", meta.getColumnName(i)));
            }
            sb.append("\n").append("─".repeat(cols * 20)).append("\n");

            // Datos
            int filas = 0;
            while (rs.next() && filas < 100) {
                for (int i = 1; i <= cols; i++) {
                    String val = rs.getString(i);
                    if (val != null && val.length() > 18) {
                        val = val.substring(0, 15) + "...";
                    }
                    sb.append(String.format("%-20s", val != null ? val : "NULL"));
                }
                sb.append("\n");
                filas++;
            }

            sb.append("─".repeat(cols * 20)).append("\n");
            sb.append("Filas: ").append(filas).append(filas == 100 ? " (limitado)" : "");

            sqlOutput.setText(sb.toString());

        } catch (SQLException e) {
            sqlOutput.setText("ERROR SQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleVerTablas() {
        try (Connection conn = dbConfig.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {

            StringBuilder sb = new StringBuilder("TABLAS EN LA BASE DE DATOS:\n");
            sb.append("═".repeat(40)).append("\n\n");

            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                sb.append("• ").append(tableName).append("\n");
            }

            sqlOutput.setText(sb.toString());

        } catch (SQLException e) {
            sqlOutput.setText("ERROR: " + e.getMessage());
        }
    }

    @FXML
    public void handleVerEstructura() {
        try (Connection conn = dbConfig.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {

            StringBuilder sb = new StringBuilder("ESTRUCTURA DE LA BASE DE DATOS:\n");
            sb.append("═".repeat(50)).append("\n\n");

            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                sb.append("TABLE: ").append(tableName).append("\n");
                sb.append("─".repeat(40)).append("\n");

                try (ResultSet cols = conn.getMetaData().getColumns(null, null, tableName, "%")) {
                    while (cols.next()) {
                        String colName = cols.getString("COLUMN_NAME");
                        String colType = cols.getString("TYPE_NAME");
                        String nullable = cols.getString("IS_NULLABLE");
                        sb.append(String.format("  %-25s %-15s %s\n", colName, colType, "NO".equals(nullable) ? "NOT NULL" : ""));
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
        logDatos("Creando productos de demo...\n");

        try (Connection conn = dbConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // Verificar si ya hay productos
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products");
            rs.next();
            int count = rs.getInt(1);

            if (count > 0) {
                logDatos("Ya existen " + count + " productos en la base de datos.");
                logDatos("Para evitar duplicados, no se crearán nuevos.");
                return;
            }

            // Crear categoría
            stmt.execute("INSERT INTO categories (name, description) VALUES ('Herramientas', 'Herramientas manuales y eléctricas')");

            // Crear productos
            String[] productos = {
                "INSERT INTO products (code, name, category_id) VALUES ('MART001', 'Martillo de Carpintero', 1)",
                "INSERT INTO products (code, name, category_id) VALUES ('DEST002', 'Destornillador Phillips', 1)",
                "INSERT INTO products (code, name, category_id) VALUES ('LLAV003', 'Llave Francesa 10\"', 1)",
                "INSERT INTO products (code, name, category_id) VALUES ('TALA004', 'Taladro Percutor', 1)",
                "INSERT INTO products (code, name, category_id) VALUES ('SIER005', 'Sierra Circular', 1)"
            };

            for (String sql : productos) {
                stmt.execute(sql);
            }
            logDatos("✓ 5 productos creados");

            // Crear variantes con precios y stock
            String[] variantes = {
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (1, 'MART001-STD', 'Estándar', 1500, 2500, 50, 5)",
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (1, 'MART001-PRO', 'Profesional', 2500, 4500, 25, 3)",
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (2, 'DEST002-PH1', 'PH1', 300, 550, 100, 10)",
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (2, 'DEST002-PH2', 'PH2', 350, 600, 100, 10)",
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (3, 'LLAV003-10', '10 pulgadas', 2000, 3500, 30, 5)",
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (4, 'TALA004-500W', '500W', 15000, 25000, 10, 2)",
                "INSERT INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock) VALUES (5, 'SIER005-7', '7 1/4\"', 25000, 42000, 8, 2)"
            };

            for (String sql : variantes) {
                stmt.execute(sql);
            }
            logDatos("✓ 7 variantes creadas con precios y stock\n");
            logDatos("Productos de demo creados exitosamente!");

        } catch (SQLException e) {
            logDatos("ERROR: " + e.getMessage());
        }
    }

    @FXML
    public void handleCrearVentaTest() {
        datosOutput.clear();
        logDatos("Creando venta de prueba...\n");

        try {
            var variantes = variantDAO.listarDisponibles();
            if (variantes.size() < 2) {
                logDatos("ERROR: No hay suficientes productos. Crea productos demo primero.");
                return;
            }

            ProductVariant v1 = variantes.get(0);
            ProductVariant v2 = variantes.get(1);

            logDatos("Productos seleccionados:");
            logDatos("  - " + v1.getDisplayName() + " x2 = $" + v1.getSalePrice().multiply(BigDecimal.valueOf(2)));
            logDatos("  - " + v2.getDisplayName() + " x1 = $" + v2.getSalePrice());

            SaleItem item1 = new SaleItem.Builder()
                .variantId(v1.getId())
                .quantity(2)
                .unitPrice(v1.getSalePrice())
                .subtotal(v1.getSalePrice().multiply(BigDecimal.valueOf(2)))
                .productName(v1.getProductName())
                .variantName(v1.getVariantName())
                .build();

            SaleItem item2 = new SaleItem.Builder()
                .variantId(v2.getId())
                .quantity(1)
                .unitPrice(v2.getSalePrice())
                .subtotal(v2.getSalePrice())
                .productName(v2.getProductName())
                .variantName(v2.getVariantName())
                .build();

            BigDecimal total = item1.getSubtotal().add(item2.getSubtotal());

            SalePayment payment = new SalePayment.Builder()
                .paymentMethod(SalePayment.PaymentMethod.CASH)
                .amount(total)
                .build();

            Sale sale = new Sale.Builder()
                .userId(1)
                .total(total)
                .status("completed")
                .notes("Venta de prueba desde Debug Panel")
                .addItem(item1)
                .addItem(item2)
                .addPayment(payment)
                .build();

            Sale createdSale = saleDAO.create(sale);

            logDatos("\n✓ VENTA CREADA EXITOSAMENTE");
            logDatos("  ID: " + createdSale.getId());
            logDatos("  Total: $" + String.format("%,.2f", total));
            logDatos("  Items: 2");
            logDatos("  Pago: Efectivo");

        } catch (Exception e) {
            logDatos("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleVerStats() {
        datosOutput.clear();
        logDatos("ESTADÍSTICAS DEL SISTEMA");
        logDatos("═".repeat(40) + "\n");

        LocalDate hoy = LocalDate.now();
        int year = hoy.getYear();
        int month = hoy.getMonthValue();

        logDatos("VENTAS:");
        logDatos("  Total de ventas: " + saleDAO.count());
        logDatos("  Completadas: " + saleDAO.countCompleted());
        logDatos("  Ventas hoy: " + saleDAO.dailyCount(hoy));
        logDatos("  Total hoy: $" + String.format("%,.2f", saleDAO.dailyTotal(hoy)));
        logDatos("  Total mes: $" + String.format("%,.2f", saleDAO.monthlyTotal(year, month)));
        logDatos("  Total histórico: $" + String.format("%,.2f", saleDAO.overallTotal()));

        logDatos("\nPRODUCTOS:");
        logDatos("  Variantes disponibles: " + variantDAO.listarDisponibles().size());
        logDatos("  Con stock bajo: " + variantDAO.listarStockBajo().size());

        logDatos("\nUSUARIOS:");
        logDatos("  Total usuarios: " + userDAO.findAll().size());
        logDatos("  Usuarios activos: " + userDAO.count());

        logDatos("\n" + "═".repeat(40));
        logDatos("Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
    }

    @FXML
    public void handleLimpiarConsola() {
        consolaOutput.clear();
        log("Consola limpiada - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    @FXML
    public void handleCerrar() {
        Main.navigateTo("/views/Dashboard.fxml", "Sistema Ferreteria - Dashboard");
    }

    // === Helpers ===

    private void log(String message) {
        consolaOutput.appendText(message + "\n");
    }

    private void logDatos(String message) {
        datosOutput.appendText(message + "\n");
    }

    private int getIntParam(String param) {
        return Integer.parseInt(camposParametros.get(param).getText().trim());
    }

    private String getStringParam(String param) {
        return camposParametros.get(param).getText().trim();
    }

    private LocalDate getDateParam(String param) {
        return LocalDate.parse(camposParametros.get(param).getText().trim());
    }

    private String formatSales(List<Sale> sales) {
        if (sales.isEmpty()) return "No se encontraron ventas";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s %-20s %-15s %-12s %-10s\n", "ID", "Fecha", "Total", "Items", "Estado"));
        sb.append("─".repeat(70)).append("\n");

        for (Sale s : sales) {
            sb.append(String.format("%-8d %-20s $%-14s %-12d %-10s\n",
                s.getId(),
                s.getCreatedAt() != null ? s.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-",
                String.format("%,.2f", s.getTotal()),
                s.getTotalItems(),
                s.isCompleted() ? "OK" : "ANULADA"
            ));
        }

        sb.append("─".repeat(70)).append("\n");
        sb.append("Total: ").append(sales.size()).append(" ventas");
        return sb.toString();
    }

    private String formatSaleDetail(Sale s) {
        StringBuilder sb = new StringBuilder();
        sb.append("VENTA #").append(s.getId()).append("\n");
        sb.append("─".repeat(40)).append("\n");
        sb.append("Fecha: ").append(s.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
        sb.append("Usuario: ").append(s.getUserName() != null ? s.getUserName() : "ID " + s.getUserId()).append("\n");
        sb.append("Estado: ").append(s.isCompleted() ? "Completada" : "Anulada").append("\n");
        sb.append("Notas: ").append(s.getNotes() != null ? s.getNotes() : "-").append("\n\n");

        sb.append("ITEMS:\n");
        for (SaleItem item : s.getItems()) {
            sb.append(String.format("  • %s x%d = $%,.2f\n",
                item.getDisplayName(), item.getQuantity(), item.getSubtotal()));
        }

        sb.append("\nPAGOS:\n");
        for (SalePayment payment : s.getPayments()) {
            sb.append(String.format("  • %s: $%,.2f\n",
                payment.getPaymentMethodDisplayName(), payment.getAmount()));
        }

        sb.append("\n").append("─".repeat(40)).append("\n");
        sb.append("TOTAL: $").append(String.format("%,.2f", s.getTotal()));
        return sb.toString();
    }

    private String formatVariantes(List<ProductVariant> variantes) {
        if (variantes.isEmpty()) return "No se encontraron productos";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s %-30s %-15s %-10s %-8s\n", "ID", "Producto", "SKU", "Precio", "Stock"));
        sb.append("─".repeat(75)).append("\n");

        for (ProductVariant v : variantes) {
            String nombre = v.getDisplayName();
            if (nombre.length() > 28) nombre = nombre.substring(0, 25) + "...";

            sb.append(String.format("%-6d %-30s %-15s $%-9s %-8d\n",
                v.getId(),
                nombre,
                v.getSku() != null ? v.getSku() : "-",
                String.format("%,.2f", v.getSalePrice()),
                v.getStock()
            ));
        }

        sb.append("─".repeat(75)).append("\n");
        sb.append("Total: ").append(variantes.size()).append(" productos");
        return sb.toString();
    }

    private String formatVarianteDetalle(ProductVariant v) {
        StringBuilder sb = new StringBuilder();
        sb.append("PRODUCTO #").append(v.getId()).append("\n");
        sb.append("─".repeat(40)).append("\n");
        sb.append("Nombre: ").append(v.getDisplayName()).append("\n");
        sb.append("SKU: ").append(v.getSku()).append("\n");
        sb.append("Precio costo: $").append(String.format("%,.2f", v.getCostPrice())).append("\n");
        sb.append("Precio venta: $").append(String.format("%,.2f", v.getSalePrice())).append("\n");
        sb.append("Stock actual: ").append(v.getStock()).append("\n");
        sb.append("Stock mínimo: ").append(v.getMinStock()).append("\n");
        return sb.toString();
    }

    private String formatUsuarios(List<User> usuarios) {
        if (usuarios.isEmpty()) return "No se encontraron usuarios";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s %-20s %-25s %-15s %-8s\n", "ID", "Username", "Nombre", "Rol", "Activo"));
        sb.append("─".repeat(80)).append("\n");

        for (User u : usuarios) {
            sb.append(String.format("%-6d %-20s %-25s %-15s %-8s\n",
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getRole().getValue(),
                u.isActive() ? "Sí" : "No"
            ));
        }

        sb.append("─".repeat(80)).append("\n");
        sb.append("Total: ").append(usuarios.size()).append(" usuarios");
        return sb.toString();
    }

    private String formatUsuarioDetalle(User u) {
        StringBuilder sb = new StringBuilder();
        sb.append("USUARIO #").append(u.getId()).append("\n");
        sb.append("─".repeat(40)).append("\n");
        sb.append("Username: ").append(u.getUsername()).append("\n");
        sb.append("Nombre: ").append(u.getFullName()).append("\n");
        sb.append("Rol: ").append(u.getRole().getValue()).append("\n");
        sb.append("Activo: ").append(u.isActive() ? "Sí" : "No").append("\n");
        sb.append("Creado: ").append(u.getCreatedAt() != null ?
            u.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-").append("\n");
        return sb.toString();
    }

    /**
     * Record para almacenar información de métodos expuestos.
     */
    private record MetodoInfo(String nombre, String descripcion, List<String> parametros, Supplier<String> ejecutor) {
        String displayName() {
            return nombre + (parametros.isEmpty() ? "()" : "(" + parametros.size() + " params)");
        }
    }
}
