from django.urls import path

from .views import ConsumoActualView

urlpatterns = [
    path("consumo-actual/", ConsumoActualView.as_view()),
]
