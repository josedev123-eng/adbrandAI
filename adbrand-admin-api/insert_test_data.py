import os
import sys
import django

# Configurar Django
sys.path.append(r'C:\Users\User\Documents\pruebas de software\proyecto\adbrandAI\adbrand-admin-api')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
django.setup()

from django.db import connection

with connection.cursor() as cursor:
    cursor.execute("DELETE FROM contenido;")
    cursor.execute("""
        INSERT INTO contenido (usuario_id, tipo, red_social, tono, oferta, texto, estado, motivo_revision, fecha_creacion, fecha_actualizacion)
        VALUES 
          (1, 'ANUNCIO', 'INSTAGRAM', 'CERCANO', 'Cigarrillos premium 2x1', '¡No te pierdas esta oferta de cigarrillos premium! Compra 2 y lleva 1 gratis.', 'DUDOSO', '"cigarrillos": Publicidad de tabaco prohibida.', NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes'),
          (1, 'ANUNCIO', 'FACEBOOK', 'PROFESIONAL', 'Té milagroso cura diabetes', 'Nuestro Té natural cura la diabetes tipo 2 en 30 días garantizado.', 'DUDOSO', '"cura la diabetes": Promesa médica engañosa sin respaldo científico.', NOW() - INTERVAL '20 minutes', NOW() - INTERVAL '20 minutes'),
          (2, 'ANUNCIO', 'INSTAGRAM', 'DIVERTIDO', 'Apuestas deportivas seguras', 'Gana dinero fácil con nuestras apuestas 100% seguras. Sin riesgo.', 'DUDOSO', '"apuestas": Publicidad de apuestas. "sin riesgo": Publicidad engañosa.', NOW() - INTERVAL '10 minutes', NOW() - INTERVAL '10 minutes'),
          (3, 'ANUNCIO', 'FACEBOOK', 'ELEGANTE', 'Crema antiarrugas magia', 'Elimina 20 años de arrugas en una semana. Resultados milagrosos.', 'DUDOSO', '"elimina 20 años": Promesa exagerada. "milagrosos": Publicidad engañosa.', NOW() - INTERVAL '5 minutes', NOW() - INTERVAL '5 minutes');
    """)
    print("Datos limpiados e insertados correctamente.")