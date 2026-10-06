from django.urls import path

from .views import BandejaRevisionView, DetalleContenidoView, ModerarContenidoView

urlpatterns = [
    path("contenidos/dudosos/", BandejaRevisionView.as_view()),
    path("contenidos/<int:contenido_id>/", DetalleContenidoView.as_view()),
    path("contenidos/<int:contenido_id>/moderar/", ModerarContenidoView.as_view()),
]