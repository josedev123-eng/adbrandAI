from rest_framework.test import APITestCase

from usuarios.models import Rol, UsuarioAdmin


class DesactivarAdminTests(APITestCase):
    @classmethod
    def setUpTestData(cls):
        cls.superadmin_rol = Rol.objects.create(nombre="SUPERADMIN")
        cls.moderador_rol = Rol.objects.create(nombre="MODERADOR")
        cls.superadmin = UsuarioAdmin.objects.create(
            nombres="Super", apellidos="Admin", correo="super@adbrand.pe",
            contrasena="password123", rol=cls.superadmin_rol, estado=UsuarioAdmin.ACTIVO
        )
        cls.admin = UsuarioAdmin.objects.create(
            nombres="Juan", apellidos="Perez", correo="juan@adbrand.pe",
            contrasena="password123", rol=cls.moderador_rol, estado=UsuarioAdmin.ACTIVO
        )

    def _auth_header(self, admin_id):
        return {"HTTP_X_ADMIN_ID": admin_id}

    def test_admin_inactivo_no_puede_autenticarse(self):
        self.client.credentials(**self._auth_header(self.admin.id))
        self.assertEqual(self.client.get("/api/usuarios/roles/").status_code, 200)

        self.admin.estado = UsuarioAdmin.INACTIVO
        self.admin.save()

        self.client.credentials(**self._auth_header(self.admin.id))
        respuesta = self.client.get("/api/usuarios/roles/")
        self.assertEqual(respuesta.status_code, 403)
        self.assertIn("desactivada", str(respuesta.data).lower())

    def test_desactivar_cuenta_cambia_estado_a_inactivo(self):
        url = f"/api/usuarios/administradores/{self.admin.id}/desactivar/"
        respuesta = self.client.patch(url, {"confirmar": True}, format="json", **self._auth_header(self.superadmin.id))
        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["estado"], "INACTIVO")
        self.admin.refresh_from_db()
        self.assertEqual(self.admin.estado, UsuarioAdmin.INACTIVO)

    def test_no_desactivar_sin_confirmacion(self):
        url = f"/api/usuarios/administradores/{self.admin.id}/desactivar/"
        respuesta = self.client.patch(url, {"confirmar": False}, format="json", **self._auth_header(self.superadmin.id))
        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("confirmar", respuesta.data["detail"].lower())
        self.admin.refresh_from_db()
        self.assertEqual(self.admin.estado, UsuarioAdmin.ACTIVO)

    def test_no_desactivar_admin_ya_inactivo(self):
        self.admin.estado = UsuarioAdmin.INACTIVO
        self.admin.save()
        url = f"/api/usuarios/administradores/{self.admin.id}/desactivar/"
        respuesta = self.client.patch(url, {"confirmar": True}, format="json", **self._auth_header(self.superadmin.id))
        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("ya esta desactivado", respuesta.data["detail"].lower())

    def test_desactivar_admin_inexistente_da_404(self):
        url = "/api/usuarios/administradores/99999/desactivar/"
        respuesta = self.client.patch(url, {"confirmar": True}, format="json", **self._auth_header(self.superadmin.id))
        self.assertEqual(respuesta.status_code, 404)