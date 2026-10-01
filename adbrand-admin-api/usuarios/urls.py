from django.urls import path

from .views import AdministradoresView, RolesView

urlpatterns = [
    path("roles/", RolesView.as_view()),
    path("administradores/", AdministradoresView.as_view()),
]