from django.urls import path

from .views import EmpresasView, ReactivarEmpresaView, SuspenderEmpresaView

urlpatterns = [
    path("", EmpresasView.as_view()),
    path("<int:empresa_id>/suspender/", SuspenderEmpresaView.as_view()),
    path("<int:empresa_id>/reactivar/", ReactivarEmpresaView.as_view()),
]
