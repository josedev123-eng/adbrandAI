from django.urls import path

from .views import AdministradoresView, CambiarRolAdministradorView, DesactivarAdministradorView, RolesView

urlpatterns = [
    path("roles/", RolesView.as_view()),
    path("administradores/", AdministradoresView.as_view()),
    path("administradores/<int:admin_id>/desactivar/", DesactivarAdministradorView.as_view()),
    path("administradores/<int:admin_id>/rol/", CambiarRolAdministradorView.as_view()),
]