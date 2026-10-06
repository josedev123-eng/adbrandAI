from django.utils import timezone
from rest_framework.test import APITestCase

from .models import Contenido, PerfilNegocio

URL_BANDEJA = "/api/moderacion/contenidos/dudosos/"


def crear_contenido(estado, oferta, motivo=None, minutos_atras=0, texto=None):
    fecha = timezone.now() - timezone.timedelta(minutes=minutos_atras)
    return Contenido.objects.create(
        usuario_id=1,
        tipo="ANUNCIO",
        red_social="INSTAGRAM",
        tono="CERCANO",
        oferta=oferta,
        texto=texto or f"¡Esto no te lo puedes perder! {oferta}.",
        estado=estado,
        motivo_revision=motivo,
        fecha_creacion=fecha,
        fecha_actualizacion=fecha,
    )


class BandejaRevisionTests(APITestCase):
    """HU-14: bandeja de revisión manual de contenidos dudosos."""

    @classmethod
    def setUpTestData(cls):
        PerfilNegocio.objects.create(usuario_id=1, nombre_comercial="Panadería Doña Rosa", rubro="Panadería")
        cls.aprobado = crear_contenido("APROBADO", "2 panetones por S/ 35")
        cls.rechazado = crear_contenido("RECHAZADO", "Apuestas deportivas", motivo='"apuestas": Publicidad de apuestas.')
        cls.dudoso_nuevo = crear_contenido(
            "DUDOSO", "Cigarrillos importados", motivo='"cigarrillos": Publicidad de tabaco.', minutos_atras=5
        )
        cls.dudoso_antiguo = crear_contenido(
            "DUDOSO", "Té que cura el cáncer", motivo='"cura el cáncer": Promete una cura médica.', minutos_atras=30
        )

    def test_la_bandeja_muestra_solo_los_dudosos(self):
        respuesta = self.client.get(URL_BANDEJA)

        self.assertEqual(respuesta.status_code, 200)
        ids = [fila["id"] for fila in respuesta.data]
        self.assertCountEqual(ids, [self.dudoso_nuevo.id, self.dudoso_antiguo.id])

    def test_el_mas_antiguo_aparece_primero(self):
        respuesta = self.client.get(URL_BANDEJA)

        self.assertEqual(respuesta.data[0]["id"], self.dudoso_antiguo.id)

    def test_cada_fila_trae_el_motivo_y_un_extracto(self):
        largo = crear_contenido("DUDOSO", "Promo", motivo="x", minutos_atras=60, texto="palabra " * 50)

        fila = self.client.get(URL_BANDEJA).data[0]

        self.assertEqual(fila["id"], largo.id)
        self.assertEqual(fila["motivo_revision"], "x")
        self.assertTrue(fila["extracto"].endswith("..."))
        self.assertLessEqual(len(fila["extracto"]), 123)

    def test_bandeja_vacia_devuelve_lista_vacia(self):
        Contenido.objects.filter(estado="DUDOSO").delete()

        respuesta = self.client.get(URL_BANDEJA)

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data, [])

    def test_el_detalle_trae_el_texto_completo_el_motivo_y_el_negocio(self):
        respuesta = self.client.get(f"/api/moderacion/contenidos/{self.dudoso_nuevo.id}/")

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["texto"], self.dudoso_nuevo.texto)
        self.assertEqual(respuesta.data["motivo_revision"], '"cigarrillos": Publicidad de tabaco.')
        self.assertEqual(respuesta.data["estado"], "DUDOSO")
        self.assertEqual(respuesta.data["negocio"]["nombre"], "Panadería Doña Rosa")

    def test_detalle_de_un_contenido_que_no_existe_da_404(self):
        respuesta = self.client.get("/api/moderacion/contenidos/99999/")

        self.assertEqual(respuesta.status_code, 404)


class ModerarContenidoTests(APITestCase):
    """HU-15: aprobar o rechazar contenido desde la bandeja de revisión."""

    @classmethod
    def setUpTestData(cls):
        PerfilNegocio.objects.create(usuario_id=1, nombre_comercial="Panadería Doña Rosa", rubro="Panadería")
        cls.dudoso = crear_contenido(
            "DUDOSO", "Cigarrillos importados", motivo='"cigarrillos": Publicidad de tabaco.'
        )
        cls.aprobado = crear_contenido("APROBADO", "Promo válida")
        cls.rechazado = crear_contenido("RECHAZADO", "Otra promo rechazada")

    def test_aprobar_contenido_dudoso_cambia_estado_a_aprobado_y_guarda_moderador(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.dudoso.id}/moderar/",
            {"accion": "APROBAR", "moderador_id": 42},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["estado"], "APROBADO")
        self.assertEqual(respuesta.data["moderador_id"], 42)

        # Verificar en BD
        self.dudoso.refresh_from_db()
        self.assertEqual(self.dudoso.estado, "APROBADO")
        self.assertEqual(self.dudoso.moderador_id, 42)

    def test_rechazar_contenido_dudoso_cambia_estado_a_rechazado_y_guarda_moderador(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.dudoso.id}/moderar/",
            {"accion": "RECHAZAR", "moderador_id": 99},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["estado"], "RECHAZADO")
        self.assertEqual(respuesta.data["moderador_id"], 99)

        self.dudoso.refresh_from_db()
        self.assertEqual(self.dudoso.estado, "RECHAZADO")
        self.assertEqual(self.dudoso.moderador_id, 99)

    def test_no_se_puede_moderar_contenido_ya_aprobado(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.aprobado.id}/moderar/",
            {"accion": "RECHAZAR", "moderador_id": 1},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("Solo se pueden moderar", respuesta.data["detail"])

    def test_no_se_puede_moderar_contenido_ya_rechazado(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.rechazado.id}/moderar/",
            {"accion": "APROBAR", "moderador_id": 1},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("Solo se pueden moderar", respuesta.data["detail"])

    def test_accion_invalida_devuelve_400(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.dudoso.id}/moderar/",
            {"accion": "INVALIDO", "moderador_id": 1},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 400)

    def test_moderador_id_faltante_devuelve_400(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.dudoso.id}/moderar/",
            {"accion": "APROBAR"},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 400)

    def test_moderar_contenido_inexistente_devuelve_404(self):
        respuesta = self.client.post(
            "/api/moderacion/contenidos/99999/moderar/",
            {"accion": "APROBAR", "moderador_id": 1},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 404)

    def test_rechazar_con_motivo_guarda_el_motivo_en_motivo_revision(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.dudoso.id}/moderar/",
            {"accion": "RECHAZAR", "moderador_id": 99, "motivo_rechazo": "Publicidad de apuestas"},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["motivo_revision"], "Publicidad de apuestas")

        self.dudoso.refresh_from_db()
        self.assertEqual(self.dudoso.estado, "RECHAZADO")
        self.assertEqual(self.dudoso.motivo_revision, "Publicidad de apuestas")

    def test_rechazar_sin_motivo_conserva_el_motivo_de_la_observacion(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.dudoso.id}/moderar/",
            {"accion": "RECHAZAR", "moderador_id": 1},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 200)

        self.dudoso.refresh_from_db()
        self.assertEqual(self.dudoso.estado, "RECHAZADO")
        self.assertEqual(self.dudoso.motivo_revision, '"cigarrillos": Publicidad de tabaco.')

