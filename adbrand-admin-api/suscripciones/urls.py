from django.urls import path

from .views import SuscripcionesActivasView, SuscripcionesConDeudaView

urlpatterns = [
    path("con-deuda/", SuscripcionesConDeudaView.as_view()),
    path("activas/", SuscripcionesActivasView.as_view()),
]
