-- Solo inserta IDs ausentes. Nunca actualiza precios, stock ni pedidos existentes.
INSERT INTO products (id, name, category, description, price_clp, weight_grams, stock, image_key)
SELECT 1, 'Almendras naturales', 'Frutos secos', 'Almendras naturales en bolsa de 250 gramos.', 3490, 250, 20, 'nut'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 1);

INSERT INTO products (id, name, category, description, price_clp, weight_grams, stock, image_key)
SELECT 2, 'Nueces mariposa', 'Frutos secos', 'Nueces en mitades en bolsa de 250 gramos.', 3290, 250, 12, 'nut'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 2);

INSERT INTO products (id, name, category, description, price_clp, weight_grams, stock, image_key)
SELECT 3, 'Maní tostado', 'Frutos secos', 'Maní tostado en bolsa de 500 gramos.', 2490, 500, 30, 'nut'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 3);

INSERT INTO products (id, name, category, description, price_clp, weight_grams, stock, image_key)
SELECT 4, 'Castañas de cajú', 'Frutos secos', 'Castañas de cajú de 250 gramos.', 4490, 250, 0, 'nut'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 4);

INSERT INTO products (id, name, category, description, price_clp, weight_grams, stock, image_key)
SELECT 5, 'Pasas rubias', 'Deshidratados', 'Pasas rubias en bolsa de 250 gramos.', 1990, 250, 18, 'fruit'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 5);

INSERT INTO products (id, name, category, description, price_clp, weight_grams, stock, image_key)
SELECT 6, 'Cranberries', 'Deshidratados', 'Cranberries de 250 gramos.', 2790, 250, 8, 'fruit'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 6);

INSERT INTO products (id, name, category, description, price_clp, weight_grams, stock, image_key)
SELECT 7, 'Semillas de chía', 'Semillas', 'Semillas de chía en bolsa de 250 gramos.', 2290, 250, 15, 'seed'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 7);

INSERT INTO products (id, name, category, description, price_clp, weight_grams, stock, image_key)
SELECT 8, 'Semillas de zapallo', 'Semillas', 'Semillas de zapallo de 250 gramos.', 2990, 250, 6, 'seed'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE id = 8);
