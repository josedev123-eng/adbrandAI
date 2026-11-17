from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.permissions import AllowAny

from auditoria.services import registrar_accion

from . import services
from .models import Rol, UsuarioAdmin
from .serializers import (
    AdministradorSerializer,
    CambiarRolSerializer,
    CrearAdministradorSerializer,
    DesactivarAdministradorSerializer,
    CambiarConCodigoSerializer,
    CambiarMiContrasenaSerializer,
    LoginSerializer,
    RolSerializer,
    SolicitarCodigoSerializer,
)
from .sesion import crear_token
from .vistas import VistaAdmin


class RolesView(VistaAdmin):
    """GET /api/usuarios/roles/ : lista de roles para el formulario."""
    roles_permitidos = ("SUPERADMIN",)

    def get(self, request):
        roles = Rol.objects.order_by("id")
        return Response(RolSerializer(roles, many=True).data)


class AdministradoresView(VistaAdmin):
    """GET /api/usuarios/administradores/ : lista las cuentas, nunca la contraseña (HU 03).
    POST /api/usuarios/administradores/ : crea una cuenta de administrador con su rol (HU 01)."""
    roles_permitidos = ("SUPERADMIN",)

    def get(self, request):
        administradores = UsuarioAdmin.objects.select_related("rol").order_by("id")
        return Response(AdministradorSerializer(administradores, many=True).data)

    def post(self, request):
        serializer = CrearAdministradorSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        # La contraseña que escribe el superadmin es temporal: el nuevo administrador la cambia al entrar.
        administrador = services.crear_administrador(serializer.validated_data, contrasena_temporal=True)
        registrar_accion(
            request.user.id, "CREAR_ADMIN", "usuario_admin", administrador.id,
            f"Creó la cuenta de {administrador.correo} con el rol {administrador.rol.nombre}",
        )
        return Response(AdministradorSerializer(administrador).data, status=status.HTTP_201_CREATED)


class DesactivarAdministradorView(VistaAdmin):
    """PATCH /api/usuarios/administradores/<id>/desactivar/ : desactiva un administrador (HU 03).

    Solo un superadministrador activo puede usarlo, y nunca sobre su propia cuenta.
    """

    def patch(self, request, admin_id):
        if getattr(request.user.rol, "nombre", None) != "SUPERADMIN":
            return Response(
                {"detail": "Solo un superadministrador puede desactivar cuentas."},
                status=status.HTTP_403_FORBIDDEN,
            )

        if request.user.id == admin_id:
            return Response(
                {"detail": "No puedes desactivar tu propia cuenta."},
                status=status.HTTP_403_FORBIDDEN,
            )

        serializer = DesactivarAdministradorSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        if not serializer.validated_data["confirmar"]:
            return Response(
                {"detail": "Debe confirmar la desactivación."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            admin = services.desactivar_administrador(admin_id)
        except ValueError as e:
            return Response({"detail": str(e)}, status=status.HTTP_400_BAD_REQUEST)

        registrar_accion(request.user.id, "DESACTIVAR_ADMIN", "usuario_admin", admin.id, f"Desactivó la cuenta de {admin.correo}")

        return Response(AdministradorSerializer(admin).data)


class CambiarRolAdministradorView(VistaAdmin):
    """PATCH /api/usuarios/administradores/<id>/rol/ : cambia el rol de un administrador (HU 02).

    Solo un superadministrador activo puede usarlo, y nunca sobre su propia cuenta.
    """

    def patch(self, request, admin_id):
        if getattr(request.user.rol, "nombre", None) != "SUPERADMIN":
            return Response(
                {"detail": "Solo un superadministrador puede modificar permisos."},
                status=status.HTTP_403_FORBIDDEN,
            )

        if request.user.id == admin_id:
            return Response(
                {"detail": "No puedes modificar tus propios permisos."},
                status=status.HTTP_403_FORBIDDEN,
            )

        serializer = CambiarRolSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        admin = services.cambiar_rol(admin_id, serializer.validated_data["rol"])

        registrar_accion(request.user.id, "CAMBIAR_ROL", "usuario_admin", admin.id, f"Cambió el rol de {admin.correo} a {admin.rol.nombre}")

        return Response(AdministradorSerializer(admin).data)


class LoginView(APIView):
    """POST /api/usuarios/login/ : inicio de sesión de administradores (Sprint 2).

    Si el correo y la contraseña son correctos devuelve el token y los datos del administrador.
    """
    authentication_classes = []
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        datos = serializer.validated_data

        try:
            admin = services.iniciar_sesion(datos["correo"], datos["contrasena"])
        except services.CuentaBloqueada as bloqueo:
            minutos = "minuto" if bloqueo.minutos == 1 else "minutos"
            return Response(
                {
                    "detail": (
                        f"Escribiste mal la contraseña {services.INTENTOS_LOGIN_MAXIMOS} veces seguidas. "
                        f"Por seguridad, la cuenta se bloqueó; intenta de nuevo en {bloqueo.minutos} {minutos} "
                        "o cambia tu contraseña con \"¿Olvidaste tu contraseña?\"."
                    ),
                    "codigo": "CUENTA_BLOQUEADA",
                },
                status=status.HTTP_429_TOO_MANY_REQUESTS,
            )
        except services.CuentaDesactivada:
            # HU 03, criterio 1: un administrador desactivado ya no puede iniciar sesión.
            return Response(
                {"detail": "Tu cuenta está desactivada. Contacta al superadministrador."},
                status=status.HTTP_403_FORBIDDEN,
            )

        if admin is None:
            return Response({"detail": "Correo o contraseña incorrectos."}, status=status.HTTP_401_UNAUTHORIZED)

        return Response({"token": crear_token(admin), "admin": AdministradorSerializer(admin).data})


class SesionView(VistaAdmin):
    """GET /api/usuarios/sesion/ : devuelve quién está conectado según su token (Sprint 2).

    La web lo usa al recargar la página para saber si la sesión sigue activa y qué rol tiene.
    """

    permite_contrasena_temporal = True

    def get(self, request):
        return Response(AdministradorSerializer(request.user).data)


class MiContrasenaView(VistaAdmin):
    """POST /api/usuarios/mi-contrasena/ : el administrador conectado cambia su contraseña (cualquier rol).

    También es el paso obligatorio cuando entra por primera vez con la contraseña temporal.
    """

    permite_contrasena_temporal = True

    def post(self, request):
        serializer = CambiarMiContrasenaSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        try:
            services.cambiar_mi_contrasena(request.user, serializer.validated_data["actual"], serializer.validated_data["nueva"])
        except services.ContrasenaActualIncorrecta:
            return Response({"actual": ["La contraseña actual no es correcta."]}, status=status.HTTP_400_BAD_REQUEST)

        registrar_accion(request.user.id, "CAMBIAR_CONTRASENA", "usuario_admin", request.user.id, "Cambió su propia contraseña")
        return Response({"detail": "Tu contraseña se cambió."})


MENSAJE_CODIGO_ENVIADO = "Si el correo es de un administrador activo, te enviamos un código de 6 números. Revisa tu bandeja."


class SolicitarCodigoView(APIView):
    """POST /api/usuarios/recuperar/solicitar/ : manda el código al correo. Pública.

    Responde siempre lo mismo, exista o no el correo, para no revelar quién es administrador.
    """

    authentication_classes = []
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = SolicitarCodigoSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        admin = services.solicitar_codigo(serializer.validated_data["correo"])
        if admin is not None:
            registrar_accion(admin.id, "SOLICITAR_CODIGO", "usuario_admin", admin.id, "Pidió un código para recuperar su contraseña")
        return Response({"detail": MENSAJE_CODIGO_ENVIADO})


class CambiarConCodigoView(APIView):
    """POST /api/usuarios/recuperar/cambiar/ : con el código del correo pone una contraseña nueva. Pública."""

    authentication_classes = []
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = CambiarConCodigoSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        datos = serializer.validated_data
        try:
            admin = services.cambiar_con_codigo(datos["correo"], datos["codigo"], datos["nueva"])
        except services.CodigoInvalido:
            return Response(
                {"codigo": ["El código no es correcto o ya venció. Pide uno nuevo."]},
                status=status.HTTP_400_BAD_REQUEST,
            )

        registrar_accion(admin.id, "RECUPERAR_CONTRASENA", "usuario_admin", admin.id, "Cambió su contraseña con un código del correo")
        return Response({"detail": "Listo, tu contraseña se cambió. Ya puedes iniciar sesión."})
