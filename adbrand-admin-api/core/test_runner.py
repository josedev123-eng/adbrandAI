from django.apps import apps
from django.conf import settings
from django.test.runner import DiscoverRunner

APPS_DEL_PROYECTO = ["core", "usuarios", "auditoria", "suscripciones", "moderacion", "tokens", "servidor"]


class PruebasConTablasFlyway(DiscoverRunner):
    """
    Nuestras tablas las crea Flyway (managed = False), así que Django no las crea.
    Para las pruebas, Django usa una base de datos temporal (test_adbrand);
    este runner le pide que cree ahí esas tablas a partir de los modelos.
    """

    def setup_databases(self, **kwargs):
        settings.MIGRATION_MODULES = {app: None for app in APPS_DEL_PROYECTO}
        for modelo in apps.get_models():
            if modelo._meta.app_label in APPS_DEL_PROYECTO:
                modelo._meta.managed = True
        return super().setup_databases(**kwargs)