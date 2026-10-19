from django.urls import path

from .views import AuditoriaView

urlpatterns = [
    path("", AuditoriaView.as_view()),
]
