from django.urls import path

from .views import AtenderMensajeView, MensajesContactoView

urlpatterns = [
    path("", MensajesContactoView.as_view()),
    path("<int:mensaje_id>/", AtenderMensajeView.as_view()),
]
