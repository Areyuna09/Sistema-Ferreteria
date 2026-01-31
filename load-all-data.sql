-- Script para cargar todos los datos SQL en la base de datos ferreteria.db
-- Ejecutar con: sqlite3 ferreteria.db < load-all-data.sql

-- Cargar categorías
.read src/main/resources/sql/categories_init.sql

-- Cargar productos (parte 1)
.read src/main/resources/sql/products_part1.sql

-- Cargar variantes (parte 1)
.read src/main/resources/sql/variants_part1.sql

-- Verificar datos cargados
SELECT '=== VERIFICACIÓN DE DATOS CARGADOS ===' as info;
SELECT 'Usuarios:' as tipo, COUNT(*) as cantidad FROM users
UNION ALL
SELECT 'Categorías:', COUNT(*) FROM categories
UNION ALL
SELECT 'Productos:', COUNT(*) FROM products
UNION ALL
SELECT 'Variantes:', COUNT(*) FROM product_variants
UNION ALL
SELECT 'Ventas:', COUNT(*) FROM sales
UNION ALL
SELECT 'Items de venta:', COUNT(*) FROM sale_items
UNION ALL
SELECT 'Pagos:', COUNT(*) FROM sale_payments;

-- Mostrar algunas categorías
SELECT '=== CATEGORÍAS CARGADAS ===' as info;
SELECT id, name, description FROM categories ORDER BY id LIMIT 10;

-- Mostrar algunos productos
SELECT '=== PRODUCTOS CARGADOS ===' as info;
SELECT id, code, name, category_id FROM products ORDER BY id LIMIT 10;

-- Mostrar algunas variantes
SELECT '=== VARIANTES CARGADAS ===' as info;
SELECT pv.id, p.code as product_code, pv.sku, pv.sale_price, pv.stock 
FROM product_variants pv 
JOIN products p ON pv.product_id = p.id 
ORDER BY pv.id LIMIT 10;
