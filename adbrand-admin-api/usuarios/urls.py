from django.urls import path

from .views import (
    AdministradoresView,
    CambiarRolAdministradorView,
    DesactivarAdministradorView,
    CambiarConCodigoView,
    LoginView,
    MiContrasenaView,
    RolesView,
    SesionView,
    SolicitarCodigoView,
)

urlpatterns = [
    path("login/", LoginView.as_view()),
    path("sesion/", SesionView.as_view()),
    path("mi-contrasena/", MiContrasenaView.as_view()),
    path("recuperar/solicitar/", SolicitarCodigoView.as_view()),
    path("recuperar/cambiar/", CambiarConCodigoView.as_view()),
    path("roles/", RolesView.as_view()),
    path("administradores/", AdministradoresView.as_view()),
    path("administradores/<int:admin_id>/desactivar/", DesactivarAdministradorView.as_view()),
    path("administradores/<int:admin_id>/rol/", CambiarRolAdministradorView.as_view()),
]