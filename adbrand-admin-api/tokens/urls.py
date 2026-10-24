from django.urls import path

from .views import ConsumoActualView, HistorialView

urlpatterns = [
    path("consumo-actual/", ConsumoActualView.as_view()),
    path("historial/", HistorialView.as_view()),
]
