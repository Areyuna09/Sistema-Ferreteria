-- Verificar datos insertados
SELECT 'Usuarios:' as tabla, COUNT(*) as cantidad FROM users
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

-- Ver ventas recientes
SELECT
    DATE(created_at) as fecha,
    COUNT(*) as cantidad_ventas,
    SUM(total) as total_vendido
FROM sales
GROUP BY DATE(created_at)
ORDER BY fecha DESC
LIMIT 10;
