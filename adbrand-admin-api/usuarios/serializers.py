import re

from rest_framework import serializers

from .models import Rol, UsuarioAdmin

# Solo letras (con tildes y ñ) separadas por un espacio, guion o apóstrofo: "María José", "De la Cruz".
NOMBRE_VALIDO = re.compile(r"^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+([ '-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$")


class RolSerializer(serializers.ModelSerializer):
    class Meta:
        model = Rol
        fields = ["id", "nombre", "descripcion"]


class CrearAdministradorSerializer(serializers.Serializer):
    """Datos que llegan del formulario para crear un administrador."""

    nombres = serializers.CharField(min_length=2, max_length=100)
    apellidos = serializers.CharField(min_length=2, max_length=100)
    correo = serializers.EmailField(max_length=150)
    contrasena = serializers.CharField(min_length=8, max_length=128, write_only=True, trim_whitespace=False)
    rol_id = serializers.PrimaryKeyRelatedField(
        queryset=Rol.objects.all(),
        source="rol",
        error_messages={"does_not_exist": "El rol seleccionado no existe."},
    )

    def _validar_nombre(self, valor):
        if not NOMBRE_VALIDO.match(valor):
            raise serializers.ValidationError("Usa solo letras y espacios, sin números ni símbolos.")
        return valor

    def validate_nombres(self, valor):
        return self._validar_nombre(valor)

    def validate_apellidos(self, valor):
        return self._validar_nombre(valor)

    def validate_contrasena(self, valor):
        """La contraseña debe tener mayúscula, minúscula y número, sin espacios."""
        if " " in valor:
            raise serializers.ValidationError("La contraseña no puede tener espacios.")
        if not re.search(r"[A-Z]", valor):
            raise serializers.ValidationError("La contraseña debe tener al menos una letra mayúscula.")
        if not re.search(r"[a-z]", valor):
            raise serializers.ValidationError("La contraseña debe tener al menos una letra minúscula.")
        if not re.search(r"[0-9]", valor):
            raise serializers.ValidationError("La contraseña debe tener al menos un número.")
        return valor

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
        fields = ["id", "nombres", "apellidos", "correo", "rol", "estado", "fecha_creacion"]


class DesactivarAdministradorSerializer(serializers.Serializer):
    """Serializador para desactivar un administrador (HU 03)."""

    confirmar = serializers.BooleanField(default=False)


class CambiarRolSerializer(serializers.Serializer):
    """Serializador para cambiar el rol de un administrador (HU 02)."""

    rol_id = serializers.PrimaryKeyRelatedField(
        queryset=Rol.objects.all(),
        source="rol",
        error_messages={"does_not_exist": "El rol seleccionado no existe."},
    )