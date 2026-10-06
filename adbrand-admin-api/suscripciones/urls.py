from django.urls import path

from .views import SuscripcionesConDeudaView

urlpatterns = [
    path("con-deuda/", SuscripcionesConDeudaView.as_view()),
]
