from rest_framework import serializers

from .models import Rol, UsuarioAdmin


class RolSerializer(serializers.ModelSerializer):
    class Meta:
        model = Rol
        fields = ["id", "nombre", "descripcion"]


class CrearAdministradorSerializer(serializers.Serializer):
    """Datos que llegan del formulario para crear un administrador."""

    nombres = serializers.CharField(max_length=100)
    apellidos = serializers.CharField(max_length=100)
    correo = serializers.EmailField(max_length=150)
    contrasena = serializers.CharField(min_length=8, max_length=128, write_only=True)
    rol_id = serializers.PrimaryKeyRelatedField(
        queryset=Rol.objects.all(),
        source="rol",
        error_messages={"does_not_exist": "El rol seleccionado no existe."},
    )

    def validate_correo(self, valor):
        correo = valor.strip().lower()
        if UsuarioAdmin.objects.filter(correo__iexact=correo).exists():
            raise serializers.ValidationError("Ya existe un administrador con este correo.")
        return correo


class AdministradorSerializer(serializers.ModelSerializer):
    """Lo que la API devuelve. Nunca incluye la contraseña."""

    rol = RolSerializer()

    class Meta:
        model = UsuarioAdmin
        fields = ["id", "nombres", "apellidos", "correo", "rol", "fecha_creacion"]