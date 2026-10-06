# -*- coding: utf-8 -*-
import os
import sys
import django

sys.path.append(r'C:\Users\User\Documents\pruebas de software\proyecto\adbrandAI\adbrand-admin-api')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
django.setup()

from django.db import connection

# SQL con caracteres Unicode explícitos (usando escapes \u)
sql = """
DELETE FROM contenido;
INSERT INTO contenido (usuario_id, tipo, red_social, tono, oferta, texto, estado, motivo_revision, fecha_creacion, fecha_actualizacion)
VALUES 
  (1, 'ANUNCIO', 'INSTAGRAM', 'CERCANO', 'Cigarrillos premium 2x1', '\u00a1No te pierdas esta oferta de cigarrillos premium! Compra 2 y lleva 1 gratis.', 'DUDOSO', '"cigarrillos": Publicidad de tabaco prohibida.', NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes'),
  (1, 'ANUNCIO', 'FACEBOOK', 'PROFESIONAL', 'T\u00e9 milagroso cura diabetes', 'Nuestro T\u00e9 natural cura la diabetes tipo 2 en 30 d\u00edas garantizado.', 'DUDOSO', '"cura la diabetes": Promesa m\u00e9dica enga\u00f1osa sin respaldo cient\u00edfico.', NOW() - INTERVAL '20 minutes', NOW() - INTERVAL '20 minutes'),
  (2, 'ANUNCIO', 'INSTAGRAM', 'DIVERTIDO', 'Apuestas deportivas seguras', 'Gana dinero f\u00e1cil con nuestras apuestas 100% seguras. Sin riesgo.', 'DUDOSO', '"apuestas": Publicidad de apuestas. "sin riesgo": Publicidad enga\u00f1osa.', NOW() - INTERVAL '10 minutes', NOW() - INTERVAL '10 minutes'),
  (3, 'ANUNCIO', 'FACEBOOK', 'ELEGANTE', 'Crema antiarrugas magia', 'Elimina 20 a\u00f1os de arrugas en una semana. Resultados milagrosos.', 'DUDOSO', '"elimina 20 a\u00f1os": Promesa exagerada. "milagrosos": Publicidad enga\u00f1osa.', NOW() - INTERVAL '5 minutes', NOW() - INTERVAL '5 minutes');
"""

with connection.cursor() as cursor:
    cursor.execute(sql)
    print("Datos insertados correctamente con encoding UTF-8.")