from io import StringIO
from unittest.mock import patch

from django.contrib.auth.hashers import check_password, make_password
from django.core.management import CommandError, call_command
from rest_framework.test import APITestCase

from auditoria.models import AuditoriaAdmin
from core.pruebas import con_sesion, crear_admin, iniciar_sesion

from .models import Rol, UsuarioAdmin
from .sesion import crear_token

URL = "/api/usuarios/administradores/"


class DesactivarAdminTests(APITestCase):
    """HU-03: solo un superadministrador activo desactiva cuentas, y no la suya."""

    @classmethod
    def setUpTestData(cls):
        cls.superadmin_rol = Rol.objects.create(nombre="SUPERADMIN")
        cls.moderador_rol = Rol.objects.create(nombre="MODERADOR")
        cls.superadmin = UsuarioAdmin.objects.create(
            nombres="Super", apellidos="Admin", correo="super@adbrand.pe",
            contrasena="password123", rol=cls.superadmin_rol, estado=UsuarioAdmin.ACTIVO,
        )
        cls.moderador = UsuarioAdmin.objects.create(
            nombres="Ana", apellidos="Moderadora", correo="ana@adbrand.pe",
            contrasena="password123", rol=cls.moderador_rol, estado=UsuarioAdmin.ACTIVO,
        )
        cls.objetivo = UsuarioAdmin.objects.create(
            nombres="Juan", apellidos="Perez", correo="juan@adbrand.pe",
            contrasena="password123", rol=cls.moderador_rol, estado=UsuarioAdmin.ACTIVO,
        )

    def _como(self, admin):
        return con_sesion(admin) if admin else {}

    def _desactivar(self, admin_objetivo, como=None, confirmar=True):
        url = f"/api/usuarios/administradores/{admin_objetivo.id}/desactivar/"
        return self.client.patch(url, {"confirmar": confirmar}, format="json", **(como or {}))

    def test_el_superadmin_desactiva_una_cuenta(self):
        respuesta = self._desactivar(self.objetivo, self._como(self.superadmin))

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["estado"], "INACTIVO")
        self.objetivo.refresh_from_db()
        self.assertEqual(self.objetivo.estado, UsuarioAdmin.INACTIVO)

    def test_un_moderador_recibe_403(self):
        respuesta = self._desactivar(self.objetivo, self._como(self.moderador))

        self.assertEqual(respuesta.status_code, 403)
        self.objetivo.refresh_from_db()
        self.assertEqual(self.objetivo.estado, UsuarioAdmin.ACTIVO)

    def test_sin_header_recibe_401_o_403(self):
        respuesta = self._desactivar(self.objetivo)

        self.assertIn(respuesta.status_code, [401, 403])
        self.objetivo.refresh_from_db()
        self.assertEqual(self.objetivo.estado, UsuarioAdmin.ACTIVO)

    def test_no_puede_desactivarse_a_si_mismo(self):
        respuesta = self._desactivar(self.superadmin, self._como(self.superadmin))

        self.assertEqual(respuesta.status_code, 403)
        self.superadmin.refresh_from_db()
        self.assertEqual(self.superadmin.estado, UsuarioAdmin.ACTIVO)

    def test_una_cuenta_inactiva_no_puede_desactivar(self):
        self.moderador.estado = UsuarioAdmin.INACTIVO
        self.moderador.save()

        respuesta = self._desactivar(self.objetivo, self._como(self.moderador))

        self.assertEqual(respuesta.status_code, 403)

    def test_roles_responde_200_al_superadmin(self):
        # Antes era público; desde el login solo lo usa el SUPERADMIN (formulario de crear).
        respuesta = self.client.get("/api/usuarios/roles/", **self._como(self.superadmin))

        self.assertEqual(respuesta.status_code, 200)

    def test_listado_de_administradores_no_trae_la_contrasena(self):
        respuesta = self.client.get("/api/usuarios/administradores/", **self._como(self.superadmin))

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(len(respuesta.data), 3)
        for fila in respuesta.data:
            self.assertNotIn("contrasena", fila)
            self.assertIn("estado", fila)

    def test_no_desactivar_sin_confirmacion(self):
        respuesta = self._desactivar(self.objetivo, self._como(self.superadmin), confirmar=False)

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("confirmar", respuesta.data["detail"].lower())
        self.objetivo.refresh_from_db()
        self.assertEqual(self.objetivo.estado, UsuarioAdmin.ACTIVO)

    def test_no_desactivar_admin_ya_inactivo(self):
        self.objetivo.estado = UsuarioAdmin.INACTIVO
        self.objetivo.save()

        respuesta = self._desactivar(self.objetivo, self._como(self.superadmin))

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("ya está desactivado", respuesta.data["detail"].lower())

    def test_desactivar_admin_inexistente_da_404(self):
        url = "/api/usuarios/administradores/99999/desactivar/"
        respuesta = self.client.patch(url, {"confirmar": True}, format="json", **self._como(self.superadmin))

        self.assertEqual(respuesta.status_code, 404)


class CrearAdministradorTests(APITestCase):
    """HU-01: crear cuentas de administrador con un rol asignado."""

    @classmethod
    def setUpTestData(cls):
        cls.moderador = Rol.objects.create(nombre="MODERADOR")
        cls.superadmin = crear_admin("SUPERADMIN")

    def setUp(self):
        iniciar_sesion(self.client, self.superadmin)

    def creados(self):
        """Las cuentas creadas en la prueba (sin contar al SUPERADMIN que las crea)."""
        return UsuarioAdmin.objects.exclude(pk=self.superadmin.pk)

    def datos_validos(self, **cambios):
        datos = {
            "nombres": "Karim",
            "apellidos": "Sovero",
            "correo": "karim@adbrand.pe",
            "contrasena": "Secreta123",
            "rol_id": self.moderador.id,
        }
        datos.update(cambios)
        return datos

    def test_crea_la_cuenta_con_su_rol(self):
        respuesta = self.client.post(URL, self.datos_validos(), format="json")

        self.assertEqual(respuesta.status_code, 201)
        self.assertEqual(respuesta.data["rol"]["nombre"], "MODERADOR")
        self.assertEqual(self.creados().count(), 1)

    def test_la_contrasena_se_guarda_cifrada_y_no_se_devuelve(self):
        respuesta = self.client.post(URL, self.datos_validos(), format="json")

        guardado = self.creados().get()
        self.assertNotEqual(guardado.contrasena, "Secreta123")
        self.assertTrue(check_password("Secreta123", guardado.contrasena))
        self.assertNotIn("contrasena", respuesta.data)

    def test_no_permite_correo_repetido(self):
        self.client.post(URL, self.datos_validos(), format="json")
        respuesta = self.client.post(URL, self.datos_validos(correo="KARIM@adbrand.pe"), format="json")

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("correo", respuesta.data)
        self.assertEqual(self.creados().count(), 1)

    def test_no_crea_la_cuenta_sin_rol(self):
        datos = self.datos_validos()
        del datos["rol_id"]
        respuesta = self.client.post(URL, datos, format="json")

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("rol_id", respuesta.data)

    def test_no_acepta_un_rol_que_no_existe(self):
        respuesta = self.client.post(URL, self.datos_validos(rol_id=999), format="json")

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("rol_id", respuesta.data)

    def test_no_crea_la_cuenta_si_faltan_datos(self):
        respuesta = self.client.post(URL, {}, format="json")

        self.assertEqual(respuesta.status_code, 400)
        for campo in ["nombres", "apellidos", "correo", "contrasena", "rol_id"]:
            self.assertIn(campo, respuesta.data)
        self.assertEqual(self.creados().count(), 0)

    def test_valida_formato_de_correo_y_largo_de_contrasena(self):
        respuesta = self.client.post(
            URL, self.datos_validos(correo="no-es-correo", contrasena="123"), format="json"
        )

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("correo", respuesta.data)
        self.assertIn("contrasena", respuesta.data)

    def test_rechaza_contrasenas_debiles(self):
        debiles = {
            "12345678": "mayúscula",
            "secreta123": "mayúscula",
            "SECRETA123": "minúscula",
            "Secretaaa": "número",
            "Secreta 123": "espacios",
        }
        for contrasena, motivo in debiles.items():
            with self.subTest(contrasena=contrasena):
                respuesta = self.client.post(URL, self.datos_validos(contrasena=contrasena), format="json")

                self.assertEqual(respuesta.status_code, 400)
                self.assertIn(motivo, str(respuesta.data["contrasena"][0]))
        self.assertFalse(self.creados().exists())

    def test_rechaza_nombres_con_numeros_o_simbolos(self):
        for nombre in ["Juan123", "@@@", "A", "Ana  María"]:
            with self.subTest(nombre=nombre):
                respuesta = self.client.post(
                    URL, self.datos_validos(nombres=nombre, apellidos=nombre), format="json"
                )

                self.assertEqual(respuesta.status_code, 400)
                self.assertIn("nombres", respuesta.data)
                self.assertIn("apellidos", respuesta.data)
        self.assertFalse(self.creados().exists())

    def test_acepta_nombres_con_tildes_y_espacios(self):
        respuesta = self.client.post(
            URL, self.datos_validos(nombres="María José", apellidos="De la Cruz Ñañez"), format="json"
        )

        self.assertEqual(respuesta.status_code, 201)


class CambiarRolTests(APITestCase):
    """HU-02: solo un superadministrador activo cambia el rol de otra cuenta."""

    @classmethod
    def setUpTestData(cls):
        cls.superadmin_rol = Rol.objects.create(nombre="SUPERADMIN")
        cls.moderador_rol = Rol.objects.create(nombre="MODERADOR")
        cls.finanzas_rol = Rol.objects.create(nombre="FINANZAS")
        cls.superadmin = UsuarioAdmin.objects.create(
            nombres="Super", apellidos="Admin", correo="super@adbrand.pe",
            contrasena="password123", rol=cls.superadmin_rol, estado=UsuarioAdmin.ACTIVO,
        )
        cls.moderador = UsuarioAdmin.objects.create(
            nombres="Ana", apellidos="Moderadora", correo="ana@adbrand.pe",
            contrasena="password123", rol=cls.moderador_rol, estado=UsuarioAdmin.ACTIVO,
        )
        cls.objetivo = UsuarioAdmin.objects.create(
            nombres="Juan", apellidos="Perez", correo="juan@adbrand.pe",
            contrasena="password123", rol=cls.moderador_rol, estado=UsuarioAdmin.ACTIVO,
        )

    def _como(self, admin):
        return con_sesion(admin) if admin else {}

    def _cambiar_rol(self, admin_objetivo, rol_id, como=None):
        url = f"/api/usuarios/administradores/{admin_objetivo.id}/rol/"
        return self.client.patch(url, {"rol_id": rol_id}, format="json", **(como or {}))

    def test_el_superadmin_cambia_el_rol_de_otra_cuenta(self):
        respuesta = self._cambiar_rol(self.objetivo, self.finanzas_rol.id, self._como(self.superadmin))

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["rol"]["nombre"], "FINANZAS")
        self.objetivo.refresh_from_db()
        self.assertEqual(self.objetivo.rol_id, self.finanzas_rol.id)

    def test_la_respuesta_no_trae_la_contrasena(self):
        respuesta = self._cambiar_rol(self.objetivo, self.finanzas_rol.id, self._como(self.superadmin))

        self.assertEqual(respuesta.data["id"], self.objetivo.id)
        self.assertNotIn("contrasena", respuesta.data)

    def test_un_moderador_no_puede_cambiar_roles(self):
        respuesta = self._cambiar_rol(self.objetivo, self.finanzas_rol.id, self._como(self.moderador))

        self.assertEqual(respuesta.status_code, 403)
        self.objetivo.refresh_from_db()
        self.assertEqual(self.objetivo.rol_id, self.moderador_rol.id)

    def test_sin_header_no_cambia_el_rol(self):
        respuesta = self._cambiar_rol(self.objetivo, self.finanzas_rol.id)

        self.assertIn(respuesta.status_code, [401, 403])
        self.objetivo.refresh_from_db()
        self.assertEqual(self.objetivo.rol_id, self.moderador_rol.id)

    def test_el_superadmin_no_cambia_su_propio_rol(self):
        respuesta = self._cambiar_rol(self.superadmin, self.moderador_rol.id, self._como(self.superadmin))

        self.assertEqual(respuesta.status_code, 403)
        self.superadmin.refresh_from_db()
        self.assertEqual(self.superadmin.rol_id, self.superadmin_rol.id)

    def test_un_superadmin_inactivo_no_cambia_roles(self):
        self.superadmin.estado = UsuarioAdmin.INACTIVO
        self.superadmin.save()

        respuesta = self._cambiar_rol(self.objetivo, self.finanzas_rol.id, self._como(self.superadmin))

        self.assertEqual(respuesta.status_code, 403)
        self.objetivo.refresh_from_db()
        self.assertEqual(self.objetivo.rol_id, self.moderador_rol.id)

    def test_un_rol_que_no_existe_responde_400(self):
        respuesta = self._cambiar_rol(self.objetivo, 999, self._como(self.superadmin))

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("rol_id", respuesta.data)
        self.objetivo.refresh_from_db()
        self.assertEqual(self.objetivo.rol_id, self.moderador_rol.id)

    def test_sin_rol_responde_400(self):
        url = f"/api/usuarios/administradores/{self.objetivo.id}/rol/"
        respuesta = self.client.patch(url, {}, format="json", **self._como(self.superadmin))

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("rol_id", respuesta.data)

    def test_un_administrador_que_no_existe_responde_404(self):
        url = "/api/usuarios/administradores/99999/rol/"
        respuesta = self.client.patch(
            url, {"rol_id": self.finanzas_rol.id}, format="json", **self._como(self.superadmin)
        )

        self.assertEqual(respuesta.status_code, 404)

class LoginTests(APITestCase):
    """Sprint 2: inicio de sesión de administradores con token."""

    URL = "/api/usuarios/login/"

    @classmethod
    def setUpTestData(cls):
        rol = Rol.objects.create(nombre="MODERADOR")
        cls.activo = UsuarioAdmin.objects.create(
            nombres="Ana", apellidos="Torres", correo="ana@adbrand.pe",
            contrasena=make_password("Secreta123"), rol=rol, estado=UsuarioAdmin.ACTIVO,
        )
        cls.inactivo = UsuarioAdmin.objects.create(
            nombres="Luis", apellidos="Paz", correo="luis@adbrand.pe",
            contrasena=make_password("Secreta123"), rol=rol, estado=UsuarioAdmin.INACTIVO,
        )

    def _login(self, correo, contrasena):
        return self.client.post(self.URL, {"correo": correo, "contrasena": contrasena}, format="json")

    def test_con_datos_correctos_devuelve_token_y_rol(self):
        respuesta = self._login("ana@adbrand.pe", "Secreta123")

        self.assertEqual(respuesta.status_code, 200)
        self.assertTrue(respuesta.data["token"])
        self.assertEqual(respuesta.data["admin"]["id"], self.activo.id)
        self.assertEqual(respuesta.data["admin"]["rol"]["nombre"], "MODERADOR")
        self.assertNotIn("contrasena", respuesta.data["admin"])

    def test_el_correo_no_distingue_mayusculas(self):
        respuesta = self._login("  ANA@adbrand.pe ", "Secreta123")
        self.assertEqual(respuesta.status_code, 200)

    def test_contrasena_incorrecta_o_correo_inexistente_dan_el_mismo_mensaje(self):
        mala = self._login("ana@adbrand.pe", "Otra1234")
        no_existe = self._login("nadie@adbrand.pe", "Secreta123")

        self.assertEqual(mala.status_code, 401)
        self.assertEqual(no_existe.status_code, 401)
        self.assertEqual(mala.data["detail"], no_existe.data["detail"])
        self.assertNotIn("token", mala.data)

    def test_cuenta_desactivada_no_puede_iniciar_sesion(self):
        # HU 03, criterio 1
        respuesta = self._login("luis@adbrand.pe", "Secreta123")

        self.assertEqual(respuesta.status_code, 403)
        self.assertIn("desactivada", respuesta.data["detail"])
        self.assertNotIn("token", respuesta.data)

    def test_faltan_datos(self):
        respuesta = self.client.post(self.URL, {"correo": "ana@adbrand.pe"}, format="json")
        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("contrasena", respuesta.data)


class SesionTests(APITestCase):
    """Sprint 2: el token identifica al administrador en los demás pedidos."""

    URL = "/api/usuarios/sesion/"

    @classmethod
    def setUpTestData(cls):
        cls.superadmin_rol = Rol.objects.create(nombre="SUPERADMIN")
        cls.moderador_rol = Rol.objects.create(nombre="MODERADOR")
        cls.superadmin = UsuarioAdmin.objects.create(
            nombres="Sofia", apellidos="Arias", correo="sofia@adbrand.pe",
            contrasena=make_password("Secreta123"), rol=cls.superadmin_rol,
        )
        cls.objetivo = UsuarioAdmin.objects.create(
            nombres="Juan", apellidos="Perez", correo="juan@adbrand.pe",
            contrasena=make_password("Secreta123"), rol=cls.moderador_rol,
        )

    def _bearer(self, token):
        return {"HTTP_AUTHORIZATION": f"Bearer {token}"}

    def test_con_token_valido_devuelve_al_admin_conectado(self):
        respuesta = self.client.get(self.URL, **self._bearer(crear_token(self.superadmin)))

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["correo"], "sofia@adbrand.pe")
        self.assertEqual(respuesta.data["rol"]["nombre"], "SUPERADMIN")

    def test_sin_token_no_hay_sesion(self):
        respuesta = self.client.get(self.URL)
        self.assertEqual(respuesta.status_code, 403)

    def test_token_alterado_es_rechazado(self):
        token = crear_token(self.superadmin)
        respuesta = self.client.get(self.URL, **self._bearer(token[:-3] + "abc"))

        self.assertEqual(respuesta.status_code, 403)
        self.assertIn("sesión", respuesta.data["detail"])

    def test_token_vencido_es_rechazado(self):
        token = crear_token(self.superadmin)
        with patch("usuarios.sesion.DURACION_SEGUNDOS", -1):
            respuesta = self.client.get(self.URL, **self._bearer(token))
        self.assertEqual(respuesta.status_code, 403)

    def test_si_desactivan_la_cuenta_el_token_deja_de_servir(self):
        token = crear_token(self.objetivo)
        UsuarioAdmin.objects.filter(pk=self.objetivo.pk).update(estado=UsuarioAdmin.INACTIVO)

        respuesta = self.client.get(self.URL, **self._bearer(token))
        self.assertEqual(respuesta.status_code, 403)

    def test_el_token_sirve_para_desactivar_cuentas(self):
        url = f"/api/usuarios/administradores/{self.objetivo.id}/desactivar/"
        respuesta = self.client.patch(
            url, {"confirmar": True}, format="json", **self._bearer(crear_token(self.superadmin))
        )
        self.assertEqual(respuesta.status_code, 200)


class CrearSuperadminComandoTests(APITestCase):
    """Sprint 2: comando para crear el primer SUPERADMIN desde la consola."""

    @classmethod
    def setUpTestData(cls):
        Rol.objects.create(nombre="SUPERADMIN")

    def test_crea_el_superadmin_con_contrasena_cifrada(self):
        call_command(
            "crear_superadmin", nombres="Jose", apellidos="Rojas", correo="Jose@AdBrand.pe",
            contrasena="Secreta123", stdout=StringIO(),
        )
        admin = UsuarioAdmin.objects.get(correo="jose@adbrand.pe")
        self.assertEqual(admin.rol.nombre, "SUPERADMIN")
        self.assertTrue(check_password("Secreta123", admin.contrasena))

    def test_rechaza_contrasena_debil(self):
        with self.assertRaises(CommandError):
            call_command(
                "crear_superadmin", nombres="Jose", apellidos="Rojas", correo="jose@adbrand.pe",
                contrasena="secreta", stdout=StringIO(),
            )
        self.assertFalse(UsuarioAdmin.objects.exists())


class AccesoPorRolTests(APITestCase):
    """Login de administradores (paso 3): Django revisa la sesión y el rol en cada endpoint."""

    MODERACION = "/api/moderacion/contenidos/dudosos/"
    SUSCRIPCIONES = "/api/suscripciones/activas/"
    AUDITORIA = "/api/auditoria/"
    ADMINISTRADORES = "/api/usuarios/administradores/"
    SESION = "/api/usuarios/sesion/"
    TODAS = [MODERACION, SUSCRIPCIONES, AUDITORIA, ADMINISTRADORES, "/api/usuarios/roles/", SESION]

    @classmethod
    def setUpTestData(cls):
        cls.superadmin = crear_admin("SUPERADMIN")
        cls.moderador = crear_admin("MODERADOR")
        cls.finanzas = crear_admin("FINANZAS")
        cls.tecnico = crear_admin("TECNICO")

    def _get(self, url, admin):
        return self.client.get(url, **con_sesion(admin))

    def _sin_acceso(self, respuesta):
        self.assertEqual(respuesta.status_code, 403)
        self.assertEqual(respuesta.data["detail"], "Tu rol no tiene acceso a esta sección.")

    def test_sin_token_cada_modulo_pide_iniciar_sesion(self):
        for url in self.TODAS:
            with self.subTest(url=url):
                respuesta = self.client.get(url)
                self.assertEqual(respuesta.status_code, 403)
                self.assertEqual(respuesta.data["detail"], "Inicia sesión para continuar.")

    def test_sin_token_no_se_puede_crear_un_administrador(self):
        datos = {"nombres": "Karim", "apellidos": "Sovero", "correo": "karim@adbrand.pe",
                 "contrasena": "Secreta123", "rol_id": self.moderador.rol_id}
        respuesta = self.client.post(self.ADMINISTRADORES, datos, format="json")

        self.assertEqual(respuesta.status_code, 403)
        self.assertFalse(UsuarioAdmin.objects.filter(correo="karim@adbrand.pe").exists())

    def test_un_token_falso_es_rechazado(self):
        respuesta = self.client.get(self.MODERACION, HTTP_AUTHORIZATION="Bearer token-inventado")

        self.assertIn(respuesta.status_code, [401, 403])
        self.assertEqual(respuesta.data["detail"], "Tu sesión venció o no es válida. Vuelve a iniciar sesión.")

    def test_solo_x_admin_id_ya_no_sirve(self):
        respuesta = self.client.get(self.AUDITORIA, HTTP_X_ADMIN_ID=self.superadmin.id)

        self.assertEqual(respuesta.status_code, 403)
        self.assertEqual(respuesta.data["detail"], "Inicia sesión para continuar.")

    def test_el_superadmin_entra_a_todo(self):
        for url in self.TODAS:
            with self.subTest(url=url):
                self.assertEqual(self._get(url, self.superadmin).status_code, 200)

    def test_el_moderador_entra_a_moderacion(self):
        self.assertEqual(self._get(self.MODERACION, self.moderador).status_code, 200)

    def test_el_moderador_no_entra_a_suscripciones(self):
        self._sin_acceso(self._get(self.SUSCRIPCIONES, self.moderador))

    def test_el_moderador_no_entra_a_auditoria(self):
        self._sin_acceso(self._get(self.AUDITORIA, self.moderador))

    def test_el_moderador_no_entra_a_administradores(self):
        self._sin_acceso(self._get(self.ADMINISTRADORES, self.moderador))

    def test_finanzas_entra_a_suscripciones_y_no_a_moderacion(self):
        self.assertEqual(self._get(self.SUSCRIPCIONES, self.finanzas).status_code, 200)
        self._sin_acceso(self._get(self.MODERACION, self.finanzas))

    def test_el_tecnico_solo_entra_a_su_sesion(self):
        self.assertEqual(self._get(self.SESION, self.tecnico).status_code, 200)
        for url in [self.MODERACION, self.SUSCRIPCIONES, self.AUDITORIA, self.ADMINISTRADORES]:
            with self.subTest(url=url):
                self._sin_acceso(self._get(url, self.tecnico))

    def test_crear_un_administrador_queda_en_la_auditoria(self):
        # HU 01 y HU 04: ahora se sabe quién crea la cuenta.
        datos = {"nombres": "Karim", "apellidos": "Sovero", "correo": "karim@adbrand.pe",
                 "contrasena": "Secreta123", "rol_id": self.moderador.rol_id}
        respuesta = self.client.post(self.ADMINISTRADORES, datos, format="json", **con_sesion(self.superadmin))

        self.assertEqual(respuesta.status_code, 201)
        registro = AuditoriaAdmin.objects.get()
        self.assertEqual(registro.admin_id, self.superadmin.id)
        self.assertEqual(registro.accion, "CREAR_ADMIN")
        self.assertEqual(registro.entidad_id, respuesta.data["id"])
        self.assertIn("karim@adbrand.pe", registro.detalle)
        self.assertIn("MODERADOR", registro.detalle)
