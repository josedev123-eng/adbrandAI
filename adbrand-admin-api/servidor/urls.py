from django.urls import path

from .views import EstadoServidorView

urlpatterns = [
    path("estado/", EstadoServidorView.as_view()),
]
