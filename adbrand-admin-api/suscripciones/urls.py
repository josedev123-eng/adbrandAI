from django.urls import path

from .views import IngresosMensualesView, SuscripcionesActivasView, SuscripcionesConDeudaView

urlpatterns = [
    path("con-deuda/", SuscripcionesConDeudaView.as_view()),
    path("activas/", SuscripcionesActivasView.as_view()),
    path("ingresos-mensuales/", IngresosMensualesView.as_view()),
]
