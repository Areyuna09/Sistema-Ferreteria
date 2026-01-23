-- Script para inicializar categorías de la ferretería
INSERT OR IGNORE INTO categories (name, description, parent_id, active, created_at) VALUES
-- HERRAMIENTAS MANUALES
('HERRAMIENTAS MANUALES', 'Herramientas manuales en general', NULL, 1, datetime('now')),
('Martillos', 'Martillos de diferentes tipos', (SELECT id FROM categories WHERE name = 'HERRAMIENTAS MANUALES'), 1, datetime('now')),
('Destornilladores', 'Destornilladores varios tipos', (SELECT id FROM categories WHERE name = 'HERRAMIENTAS MANUALES'), 1, datetime('now')),
('Alicates', 'Alicates y pinzas', (SELECT id FROM categories WHERE name = 'HERRAMIENTAS MANUALES'), 1, datetime('now')),
('Llaves', 'Llaves varias', (SELECT id FROM categories WHERE name = 'HERRAMIENTAS MANUALES'), 1, datetime('now')),
('Serruchos y Sierras', 'Serruchos y sierras manuales', (SELECT id FROM categories WHERE name = 'HERRAMIENTAS MANUALES'), 1, datetime('now')),
('Herramientas de Carpintería', 'Cinceles, formones, etc', (SELECT id FROM categories WHERE name = 'HERRAMIENTAS MANUALES'), 1, datetime('now')),
('Herramientas de Corte', 'Tijeras, tenazas, cortafierros', (SELECT id FROM categories WHERE name = 'HERRAMIENTAS MANUALES'), 1, datetime('now')),

-- INSTRUMENTOS DE MEDICIÓN Y TRAZADO
('INSTRUMENTOS DE MEDICIÓN', 'Instrumentos de medición y trazado', NULL, 1, datetime('now')),
('Cintas Métricas', 'Cintas métricas y metros', (SELECT id FROM categories WHERE name = 'INSTRUMENTOS DE MEDICIÓN'), 1, datetime('now')),
('Niveles y Plomadas', 'Niveles, plomadas y escuadras', (SELECT id FROM categories WHERE name = 'INSTRUMENTOS DE MEDICIÓN'), 1, datetime('now')),
('Calibres y Micrómetros', 'Instrumentos de precisión', (SELECT id FROM categories WHERE name = 'INSTRUMENTOS DE MEDICIÓN'), 1, datetime('now')),

-- HERRAMIENTAS ELÉCTRICAS
('HERRAMIENTAS ELÉCTRICAS', 'Herramientas eléctricas', NULL, 1, datetime('now')),
('Taladros y Atornilladores', 'Taladros, atornilladores y percutores', (SELECT id FROM categories WHERE name = 'HERRAMIENTAS ELÉCTRICAS'), 1, datetime('now')),
('Amoladoras y Sierras', 'Amoladoras y sierras eléctricas', (SELECT id FROM categories WHERE name = 'HERRAMIENTAS ELÉCTRICAS'), 1, datetime('now')),
('Lijadoras y Pulidoras', 'Lijadoras, pulidoras y cepillos', (SELECT id FROM categories WHERE name = 'HERRAMIENTAS ELÉCTRICAS'), 1, datetime('now')),
('Herramientas Especializadas', 'Routers, ingletadoras, etc', (SELECT id FROM categories WHERE name = 'HERRAMIENTAS ELÉCTRICAS'), 1, datetime('now')),

-- ACCESORIOS PARA HERRAMIENTAS ELÉCTRICAS
('ACCESORIOS ELÉCTRICOS', 'Accesorios para herramientas eléctricas', NULL, 1, datetime('now')),
('Mechas y Brocas', 'Mechas, brocas y coronas', (SELECT id FROM categories WHERE name = 'ACCESORIOS ELÉCTRICOS'), 1, datetime('now')),
('Discos y Hojas', 'Discos de corte y hojas', (SELECT id FROM categories WHERE name = 'ACCESORIOS ELÉCTRICOS'), 1, datetime('now')),
('Lijas y Abrasivos', 'Lijas y materiales abrasivos', (SELECT id FROM categories WHERE name = 'ACCESORIOS ELÉCTRICOS'), 1, datetime('now')),
('Puntas y Adaptadores', 'Puntas y adaptadores varios', (SELECT id FROM categories WHERE name = 'ACCESORIOS ELÉCTRICOS'), 1, datetime('now')),

-- MATERIALES DE CONSTRUCCIÓN
('MATERIALES DE CONSTRUCCIÓN', 'Materiales de construcción', NULL, 1, datetime('now')),
('Materiales Secos', 'Cementos, cal, yeso, morteros', (SELECT id FROM categories WHERE name = 'MATERIALES DE CONSTRUCCIÓN'), 1, datetime('now')),
('Mampostería', 'Ladrillos, bloques, tejas', (SELECT id FROM categories WHERE name = 'MATERIALES DE CONSTRUCCIÓN'), 1, datetime('now')),
('Hierros y Metales', 'Varillas, perfiles, chapas', (SELECT id FROM categories WHERE name = 'MATERIALES DE CONSTRUCCIÓN'), 1, datetime('now')),
('Maderas y Derivados', 'Maderas, tableros, molduras', (SELECT id FROM categories WHERE name = 'MATERIALES DE CONSTRUCCIÓN'), 1, datetime('now')),

-- PINTURAS Y ACCESORIOS
('PINTURAS', 'Pinturas y recubrimientos', NULL, 1, datetime('now')),
('Pinturas', 'Látex, esmaltes, barnices', (SELECT id FROM categories WHERE name = 'PINTURAS'), 1, datetime('now')),
('Accesorios de Pintura', 'Pinceles, rodillos, espátulas', (SELECT id FROM categories WHERE name = 'PINTURAS'), 1, datetime('now')),

-- PLOMERÍA
('PLOMERÍA', 'Plomería y cañerías', NULL, 1, datetime('now')),
('Caños PVC y Accesorios', 'Caños PVC y accesorios', (SELECT id FROM categories WHERE name = 'PLOMERÍA'), 1, datetime('now')),
('Griferías', 'Canillas, mezcladoras, llaves', (SELECT id FROM categories WHERE name = 'PLOMERÍA'), 1, datetime('now')),
('Agua Caliente y Gas', 'Termotanques, caños de gas', (SELECT id FROM categories WHERE name = 'PLOMERÍA'), 1, datetime('now')),
('Tanques y Depósitos', 'Tanques de agua y accesorios', (SELECT id FROM categories WHERE name = 'PLOMERÍA'), 1, datetime('now')),

-- ELECTRICIDAD
('ELECTRICIDAD', 'Electricidad e instalación', NULL, 1, datetime('now')),
('Cables y Conductores', 'Cables eléctricos varios', (SELECT id FROM categories WHERE name = 'ELECTRICIDAD'), 1, datetime('now')),
('Instalación Eléctrica', 'Cajas, caños, fichas', (SELECT id FROM categories WHERE name = 'ELECTRICIDAD'), 1, datetime('now')),
('Tableros y Protecciones', 'Térmicas, disyuntores, gabinetes', (SELECT id FROM categories WHERE name = 'ELECTRICIDAD'), 1, datetime('now')),
('Iluminación', 'Lámparas, artefactos, accesorios', (SELECT id FROM categories WHERE name = 'ELECTRICIDAD'), 1, datetime('now')),

-- TORNILLERÍA Y FIJACIONES
('TORNILLERÍA', 'Tornillería y bulonería', NULL, 1, datetime('now')),
('Tornillos y Bulones', 'Tornillos, bulones, tuercas', (SELECT id FROM categories WHERE name = 'TORNILLERÍA'), 1, datetime('now')),
('Fijaciones Especiales', 'Tarugos, tacos, clavos', (SELECT id FROM categories WHERE name = 'TORNILLERÍA'), 1, datetime('now')),

-- ADHESIVOS Y SELLADORES
('ADHESIVOS Y SELLADORES', 'Adhesivos y selladores', NULL, 1, datetime('now')),
('Colas y Pegamentos', 'Colas y pegamentos varios', (SELECT id FROM categories WHERE name = 'ADHESIVOS Y SELLADORES'), 1, datetime('now')),
('Siliconas y Selladores', 'Siliconas, masillas, cintas', (SELECT id FROM categories WHERE name = 'ADHESIVOS Y SELLADORES'), 1, datetime('now')),

-- CERRAJERÍA
('CERRAJERÍA', 'Cerrajería y herrajes', NULL, 1, datetime('now')),
('Cerraduras y Candados', 'Cerraduras, candados, seguridad', (SELECT id FROM categories WHERE name = 'CERRAJERÍA'), 1, datetime('now')),
('Herrajes para Muebles', 'Bisagras, correderas, manijas', (SELECT id FROM categories WHERE name = 'CERRAJERÍA'), 1, datetime('now')),

-- VIDRIOS Y SANITARIOS
('VIDRIOS Y SANITARIOS', 'Vidrios y sanitarios', NULL, 1, datetime('now')),
('Vidrios y Accesorios', 'Vidrios, espejos, burletes', (SELECT id FROM categories WHERE name = 'VIDRIOS Y SANITARIOS'), 1, datetime('now')),
('Sanitarios', 'Inodoros, vanitorios, piletas', (SELECT id FROM categories WHERE name = 'VIDRIOS Y SANITARIOS'), 1, datetime('now')),

-- VENTILACIÓN Y CLIMATIZACIÓN
('VENTILACIÓN Y CLIMATIZACIÓN', 'Ventilación y climatización', NULL, 1, datetime('now')),
('Ventilación', 'Extractores, turbinas, ventiladores', (SELECT id FROM categories WHERE name = 'VENTILACIÓN Y CLIMATIZACIÓN'), 1, datetime('now')),
('Climatización', 'Calefacción, estufas, termostatos', (SELECT id FROM categories WHERE name = 'VENTILACIÓN Y CLIMATIZACIÓN'), 1, datetime('now')),

-- SEGURIDAD Y PROTECCIÓN
('SEGURIDAD Y PROTECCIÓN', 'Seguridad e higiene', NULL, 1, datetime('now')),
('Seguridad', 'Matafuegos, detectores, señalética', (SELECT id FROM categories WHERE name = 'SEGURIDAD Y PROTECCIÓN'), 1, datetime('now')),
('Protección Personal', 'EPP - Guantes, cascos, antiparras', (SELECT id FROM categories WHERE name = 'SEGURIDAD Y PROTECCIÓN'), 1, datetime('now')),

-- JARDÍN Y EXTERIOR
('JARDÍN Y EXTERIOR', 'Jardín y exterior', NULL, 1, datetime('now')),
('Herramientas de Jardín', 'Palas, rastrillos, azadas', (SELECT id FROM categories WHERE name = 'JARDÍN Y EXTERIOR'), 1, datetime('now')),
('Riego y Accesorios', 'Mangueras, aspersores, conectores', (SELECT id FROM categories WHERE name = 'JARDÍN Y EXTERIOR'), 1, datetime('now')),

-- ESCALERAS Y ANDAMIOS
('ESCALERAS Y ANDAMIOS', 'Escaleras y andamios', NULL, 1, datetime('now')),
('Escaleras', 'Escaleras tijera, extensibles, telescópicas', (SELECT id FROM categories WHERE name = 'ESCALERAS Y ANDAMIOS'), 1, datetime('now')),
('Andamios y Accesorios', 'Andamios, banquetas, tablones', (SELECT id FROM categories WHERE name = 'ESCALERAS Y ANDAMIOS'), 1, datetime('now')),

-- LIMPIEZA Y MANTENIMIENTO
('LIMPIEZA Y MANTENIMIENTO', 'Artículos de limpieza', NULL, 1, datetime('now')),
('Limpieza', 'Escobas, trapeadores, baldes', (SELECT id FROM categories WHERE name = 'LIMPIEZA Y MANTENIMIENTO'), 1, datetime('now')),
('Mantenimiento', 'Lubricantes, grasas, aceites', (SELECT id FROM categories WHERE name = 'LIMPIEZA Y MANTENIMIENTO'), 1, datetime('now')),

-- AUTOMOTRIZ
('AUTOMOTRIZ', 'Artículos automotrices', NULL, 1, datetime('now')),
('Lubricantes y Fluidos', 'Aceites, refrigerantes, líquidos', (SELECT id FROM categories WHERE name = 'AUTOMOTRIZ'), 1, datetime('now')),
('Repuestos y Accesorios', 'Baterías, lámparas, filtros', (SELECT id FROM categories WHERE name = 'AUTOMOTRIZ'), 1, datetime('now')),

-- SOLDADURA
('SOLDADURA', 'Soldadura y accesorios', NULL, 1, datetime('now')),
('Equipos de Soldadura', 'Soldadoras y accesorios', (SELECT id FROM categories WHERE name = 'SOLDADURA'), 1, datetime('now')),
('Consumibles de Soldadura', 'Electrodos, alambres, gases', (SELECT id FROM categories WHERE name = 'SOLDADURA'), 1, datetime('now')),

-- NAVAL Y PISCINAS
('NAVAL Y PISCINAS', 'Ferretería naval y piscinas', NULL, 1, datetime('now')),
('Piscinas', 'Bombas, filtros, químicos', (SELECT id FROM categories WHERE name = 'NAVAL Y PISCINAS'), 1, datetime('now')),
('Náutica', 'Cabos, amarras, aparejos', (SELECT id FROM categories WHERE name = 'NAVAL Y PISCINAS'), 1, datetime('now')),

-- CAMPING Y OUTDOOR
('CAMPING Y OUTDOOR', 'Camping y outdoor', NULL, 1, datetime('now')),
('Camping', 'Carpas, lonas, sleeping bags', (SELECT id FROM categories WHERE name = 'CAMPING Y OUTDOOR'), 1, datetime('now')),
('Outdoor', 'Faroles, linternas, herramientas', (SELECT id FROM categories WHERE name = 'CAMPING Y OUTDOOR'), 1, datetime('now')),

-- VARIOS
('VARIOS', 'Artículos varios y misceláneos', NULL, 1, datetime('now')),
('Organización', 'Cajas, estanterías, gabinetes', (SELECT id FROM categories WHERE name = 'VARIOS'), 1, datetime('now')),
('Eléctricos Varios', 'Transformadores, adaptadores', (SELECT id FROM categories WHERE name = 'VARIOS'), 1, datetime('now')),
('Herramientas Varias', 'Biromes, marcadores, etiquetas', (SELECT id FROM categories WHERE name = 'VARIOS'), 1, datetime('now'));
