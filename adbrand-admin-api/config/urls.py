from django.contrib import admin
from django.urls import include, path

urlpatterns = [
    path("admin/", admin.site.urls),
    path("api/usuarios/", include("usuarios.urls")),
    path("api/moderacion/", include("moderacion.urls")),
    path("api/suscripciones/", include("suscripciones.urls")),
    path("api/auditoria/", include("auditoria.urls")),
    path("api/tokens/", include("tokens.urls")),
]