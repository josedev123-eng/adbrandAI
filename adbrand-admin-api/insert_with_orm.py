# -*- coding: utf-8 -*-
import os
import sys
import django

sys.path.append(r'C:\Users\User\Documents\pruebas de software\proyecto\adbrandAI\adbrand-admin-api')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
django.setup()

from moderacion.models import Contenido
from django.utils import timezone
from datetime import timedelta

Contenido.objects.all().delete()

now = timezone.now()

Contenido.objects.create(
    usuario_id=1, tipo='ANUNCIO', red_social='INSTAGRAM', tono='CERCANO',
    oferta='Cigarrillos premium 2x1',
    texto='\u00a1No te pierdas esta oferta de cigarrillos premium! Compra 2 y lleva 1 gratis.',
    estado='DUDOSO',
    motivo_revision='"cigarrillos": Publicidad de tabaco prohibida.',
    fecha_creacion=now - timedelta(minutes=30),
    fecha_actualizacion=now - timedelta(minutes=30),
)

Contenido.objects.create(
    usuario_id=1, tipo='ANUNCIO', red_social='FACEBOOK', tono='PROFESIONAL',
    oferta='T\u00e9 milagroso cura diabetes',
    texto='Nuestro T\u00e9 natural cura la diabetes tipo 2 en 30 d\u00edas garantizado.',
    estado='DUDOSO',
    motivo_revision='"cura la diabetes": Promesa m\u00e9dica enga\u00f1osa sin respaldo cient\u00edfico.',
    fecha_creacion=now - timedelta(minutes=20),
    fecha_actualizacion=now - timedelta(minutes=20),
)

Contenido.objects.create(
    usuario_id=2, tipo='ANUNCIO', red_social='INSTAGRAM', tono='DIVERTIDO',
    oferta='Apuestas deportivas seguras',
    texto='Gana dinero f\u00e1cil con nuestras apuestas 100% seguras. Sin riesgo.',
    estado='DUDOSO',
    motivo_revision='"apuestas": Publicidad de apuestas. "sin riesgo": Publicidad enga\u00f1osa.',
    fecha_creacion=now - timedelta(minutes=10),
    fecha_actualizacion=now - timedelta(minutes=10),
)

Contenido.objects.create(
    usuario_id=3, tipo='ANUNCIO', red_social='FACEBOOK', tono='ELEGANTE',
    oferta='Crema antiarrugas magia',
    texto='Elimina 20 a\u00f1os de arrugas en una semana. Resultados milagrosos.',
    estado='DUDOSO',
    motivo_revision='"elimina 20 a\u00f1os": Promesa exagerada. "milagrosos": Publicidad enga\u00f1osa.',
    fecha_creacion=now - timedelta(minutes=5),
    fecha_actualizacion=now - timedelta(minutes=5),
)

print("Datos insertados correctamente via ORM.")