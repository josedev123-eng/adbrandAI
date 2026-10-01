from django.contrib.auth.hashers import check_password
from rest_framework.test import APITestCase

from .models import Rol, UsuarioAdmin

URL = "/api/usuarios/administradores/"


class CrearAdministradorTests(APITestCase):
    """HU-01: crear cuentas de administrador con un rol asignado."""

    @classmethod
    def setUpTestData(cls):
        cls.moderador = Rol.objects.create(nombre="MODERADOR")

    def datos_validos(self, **cambios):
        datos = {
            "nombres": "Karim",
            "apellidos": "Sovero",
            "correo": "karim@adbrand.pe",
            "contrasena": "secreta123",
            "rol_id": self.moderador.id,
        }
        datos.update(cambios)
        return datos

    def test_crea_la_cuenta_con_su_rol(self):
        respuesta = self.client.post(URL, self.datos_validos(), format="json")

        self.assertEqual(respuesta.status_code, 201)
        self.assertEqual(respuesta.data["rol"]["nombre"], "MODERADOR")
        self.assertEqual(UsuarioAdmin.objects.count(), 1)

    def test_la_contrasena_se_guarda_cifrada_y_no_se_devuelve(self):
        respuesta = self.client.post(URL, self.datos_validos(), format="json")

        guardado = UsuarioAdmin.objects.get()
        self.assertNotEqual(guardado.contrasena, "secreta123")
        self.assertTrue(check_password("secreta123", guardado.contrasena))
        self.assertNotIn("contrasena", respuesta.data)

    def test_no_permite_correo_repetido(self):
        self.client.post(URL, self.datos_validos(), format="json")
        respuesta = self.client.post(URL, self.datos_validos(correo="KARIM@adbrand.pe"), format="json")

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("correo", respuesta.data)
        self.assertEqual(UsuarioAdmin.objects.count(), 1)

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
        self.assertEqual(UsuarioAdmin.objects.count(), 0)

    def test_valida_formato_de_correo_y_largo_de_contrasena(self):
        respuesta = self.client.post(
            URL, self.datos_validos(correo="no-es-correo", contrasena="123"), format="json"
        )

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("correo", respuesta.data)
        self.assertIn("contrasena", respuesta.data)