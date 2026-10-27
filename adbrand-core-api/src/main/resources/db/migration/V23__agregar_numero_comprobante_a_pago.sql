-- HU 28: número de comprobante del pago, por ejemplo ADB-000005.
-- Se genera a partir del id del pago, por eso la columna es NULL hasta que el usuario
-- pide su comprobante. Los pagos que ya existen siguen funcionando sin número.
ALTER TABLE pago ADD COLUMN numero_comprobante VARCHAR(20) NULL UNIQUE;
