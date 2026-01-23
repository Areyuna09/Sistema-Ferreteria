-- Script para inicializar variantes de productos (Parte 1 - Herramientas)
INSERT OR IGNORE INTO product_variants (product_id, sku, variant_name, cost_price, sale_price, stock, min_stock, active, created_at, updated_at) VALUES
-- MARTILLOS
((SELECT id FROM products WHERE code = 'MART-001'), 'MART-001-STD', 'Estándar', 1800.00, 2500.00, 15, 5, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'MART-002'), 'MART-002-STD', 'Estándar', 1200.00, 1800.00, 20, 8, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'MART-003'), 'MART-003-STD', 'Estándar', 3500.00, 5200.00, 12, 4, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'MART-004'), 'MART-004-STD', 'Estándar', 8000.00, 12000.00, 8, 3, 1, datetime('now'), datetime('now')),

-- DESTORNILLADORES
((SELECT id FROM products WHERE code = 'DEST-001'), 'DEST-001-STD', 'Estándar', 800.00, 1200.00, 25, 10, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'DEST-002'), 'DEST-002-STD', 'Estándar', 900.00, 1400.00, 25, 10, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'DEST-003'), 'DEST-003-STD', 'Estándar', 1000.00, 1600.00, 20, 8, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'DEST-004'), 'DEST-004-STD', 'Estándar', 700.00, 1000.00, 30, 12, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'DEST-005'), 'DEST-005-STD', 'Estándar', 800.00, 1200.00, 30, 12, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'DEST-006'), 'DEST-006-STD', 'Estándar', 900.00, 1400.00, 25, 10, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'DEST-007'), 'DEST-007-STD', 'Estándar', 1500.00, 2200.00, 15, 6, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'DEST-008'), 'DEST-008-STD', 'Estándar', 1600.00, 2400.00, 15, 6, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'DEST-009'), 'DEST-009-STD', 'Estándar', 1700.00, 2600.00, 15, 6, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'DEST-010'), 'DEST-010-SET', 'Set 6 pzs', 3000.00, 4500.00, 10, 4, 1, datetime('now'), datetime('now')),

-- ALICATES Y PINZAS
((SELECT id FROM products WHERE code = 'ALIC-001'), 'ALIC-001-STD', 'Estándar', 1200.00, 1800.00, 20, 8, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'ALIC-002'), 'ALIC-002-STD', 'Estándar', 1500.00, 2200.00, 18, 7, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'ALIC-003'), 'ALIC-003-STD', 'Estándar', 1000.00, 1500.00, 22, 8, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'ALIC-004'), 'ALIC-004-STD', 'Estándar', 1100.00, 1600.00, 20, 8, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'ALIC-005'), 'ALIC-005-STD', 'Estándar', 1800.00, 2800.00, 15, 6, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'ALIC-006'), 'ALIC-006-STD', 'Estándar', 2000.00, 3200.00, 12, 5, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'PINZ-001'), 'PINZ-001-STD', 'Estándar', 1400.00, 2000.00, 18, 7, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'PINZ-002'), 'PINZ-002-STD', 'Estándar', 1000.00, 1500.00, 20, 8, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'PINZ-003'), 'PINZ-003-STD', 'Estándar', 1300.00, 1900.00, 18, 7, 1, datetime('now'), datetime('now')),

-- LLAVES
((SELECT id FROM products WHERE code = 'LLAV-001'), 'LLAV-001-STD', 'Estándar', 2500.00, 3800.00, 15, 6, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'LLAV-002'), 'LLAV-002-STD', 'Estándar', 3000.00, 4500.00, 12, 5, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'LLAV-003'), 'LLAV-003-STD', 'Estándar', 3500.00, 5200.00, 10, 4, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'LLAV-004'), 'LLAV-004-SET', 'Set 8-22mm', 8000.00, 12000.00, 8, 3, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'LLAV-005'), 'LLAV-005-SET', 'Set 8-22mm', 10000.00, 15000.00, 6, 2, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'LLAV-006'), 'LLAV-006-SET', 'Set métrico', 4000.00, 6000.00, 10, 4, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'LLAV-007'), 'LLAV-007-SET', 'Set Torx', 4500.00, 7000.00, 8, 3, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'LLAV-008'), 'LLAV-008-SET', 'Set tubo', 6000.00, 9000.00, 6, 2, 1, datetime('now'), datetime('now')),

-- SERRUCHOS Y SIERRAS
((SELECT id FROM products WHERE code = 'SERR-001'), 'SERR-001-STD', 'Estándar', 1800.00, 2800.00, 12, 5, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'SERR-002'), 'SERR-002-STD', 'Estándar', 2000.00, 3200.00, 12, 5, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'SERR-003'), 'SERR-003-STD', 'Estándar', 4000.00, 6500.00, 8, 3, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'SIER-001'), 'SIER-001-STD', 'Estándar', 2500.00, 3800.00, 10, 4, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'SIER-002'), 'SIER-002-STD', 'Estándar', 3500.00, 5500.00, 8, 3, 1, datetime('now'), datetime('now')),

-- HERRAMIENTAS DE CARPINTERÍA
((SELECT id FROM products WHERE code = 'CARP-001'), 'CARP-001-STD', 'Estándar', 1200.00, 1800.00, 15, 6, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CARP-002'), 'CARP-002-STD', 'Estándar', 1500.00, 2400.00, 12, 5, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CARP-003'), 'CARP-003-STD', 'Estándar', 2000.00, 3200.00, 10, 4, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CARP-004'), 'CARP-004-STD', 'Estándar', 1800.00, 2800.00, 12, 5, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CARP-005'), 'CARP-005-STD', 'Estándar', 2500.00, 3800.00, 10, 4, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CARP-006'), 'CARP-006-STD', 'Estándar', 800.00, 1200.00, 20, 8, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CARP-007'), 'CARP-007-STD', 'Estándar', 900.00, 1400.00, 18, 7, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CARP-008'), 'CARP-008-STD', 'Estándar', 850.00, 1300.00, 18, 7, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CARP-009'), 'CARP-009-STD', 'Estándar', 900.00, 1400.00, 18, 7, 1, datetime('now'), datetime('now')),

-- HERRAMIENTAS DE CORTE
((SELECT id FROM products WHERE code = 'CORTE-001'), 'CORTE-001-STD', 'Estándar', 3000.00, 4500.00, 10, 4, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-002'), 'CORTE-002-STD', 'Estándar', 2500.00, 3800.00, 12, 5, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-003'), 'CORTE-003-STD', 'Estándar', 2000.00, 3000.00, 15, 6, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-004'), 'CORTE-004-STD', 'Estándar', 1800.00, 2800.00, 15, 6, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-005'), 'CORTE-005-STD', 'Estándar', 4000.00, 6000.00, 8, 3, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-006'), 'CORTE-006-STD', 'Estándar', 1200.00, 1800.00, 20, 8, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-007'), 'CORTE-007-STD', 'Estándar', 2500.00, 3800.00, 12, 5, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-008'), 'CORTE-008-STD', 'Estándar', 500.00, 800.00, 30, 12, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-009'), 'CORTE-009-STD', 'Estándar', 1500.00, 2400.00, 15, 6, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-010'), 'CORTE-010-STD', 'Estándar', 2000.00, 3200.00, 12, 5, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-011'), 'CORTE-011-STD', 'Estándar', 3500.00, 5500.00, 8, 3, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-012'), 'CORTE-012-STD', 'Estándar', 2500.00, 4000.00, 10, 4, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-013'), 'CORTE-013-STD', 'Estándar', 8000.00, 12000.00, 6, 2, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-014'), 'CORTE-014-STD', 'Estándar', 1500.00, 2400.00, 15, 6, 1, datetime('now'), datetime('now')),
((SELECT id FROM products WHERE code = 'CORTE-015'), 'CORTE-015-STD', 'Estándar', 15000.00, 22000.00, 4, 2, 1, datetime('now'), datetime('now'));
