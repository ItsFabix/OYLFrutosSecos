-- No modificar puntos existentes al reiniciar; solo sembrar los IDs ausentes.
INSERT INTO pickup_points (id,title,reference,schedule,latitude,longitude)
SELECT 'centro','Punto Centro (demo)','Referencia: Plaza de Armas, Santiago',
       'Horario ficticio: Lun a vie, 10:00 a 18:00',-33.4372,-70.6506
WHERE NOT EXISTS (SELECT 1 FROM pickup_points WHERE id='centro');
INSERT INTO pickup_points (id,title,reference,schedule,latitude,longitude)
SELECT 'nunoa','Punto Ñuñoa (demo)','Referencia: Plaza Ñuñoa, Santiago',
       'Horario ficticio: Lun a vie, 11:00 a 17:00',-33.4554,-70.5982
WHERE NOT EXISTS (SELECT 1 FROM pickup_points WHERE id='nunoa');
