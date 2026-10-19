from django.utils import timezone
from rest_framework.test import APITestCase

from core.pruebas import crear_admin, iniciar_sesion, usar_vista_de_flyway

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
        cls.moderador = crear_admin("MODERADOR")

    def setUp(self):
        iniciar_sesion(self.client, self.moderador)

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
        cls.moderador = crear_admin("MODERADOR")

    def setUp(self):
        # El moderador ya no va en el cuerpo: Django lo saca del token de sesión.
        iniciar_sesion(self.client, self.moderador)

    def test_aprobar_contenido_dudoso_cambia_estado_a_aprobado_y_guarda_moderador(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.dudoso.id}/moderar/",
            {"accion": "APROBAR"},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["estado"], "APROBADO")
        self.assertEqual(respuesta.data["moderador_id"], self.moderador.id)

        # Verificar en BD
        self.dudoso.refresh_from_db()
        self.assertEqual(self.dudoso.estado, "APROBADO")
        self.assertEqual(self.dudoso.moderador_id, self.moderador.id)

    def test_rechazar_contenido_dudoso_cambia_estado_a_rechazado_y_guarda_moderador(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.dudoso.id}/moderar/",
            {"accion": "RECHAZAR"},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 200)
        self.assertEqual(respuesta.data["estado"], "RECHAZADO")
        self.assertEqual(respuesta.data["moderador_id"], self.moderador.id)

        self.dudoso.refresh_from_db()
        self.assertEqual(self.dudoso.estado, "RECHAZADO")
        self.assertEqual(self.dudoso.moderador_id, self.moderador.id)

    def test_no_se_puede_moderar_contenido_ya_aprobado(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.aprobado.id}/moderar/",
            {"accion": "RECHAZAR"},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("Solo se pueden moderar", respuesta.data["detail"])

    def test_no_se_puede_moderar_contenido_ya_rechazado(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.rechazado.id}/moderar/",
            {"accion": "APROBAR"},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("Solo se pueden moderar", respuesta.data["detail"])

    def test_accion_invalida_devuelve_400(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.dudoso.id}/moderar/",
            {"accion": "INVALIDO"},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 400)

    def test_accion_faltante_devuelve_400(self):
        # Antes faltaba moderador_id; ahora sale del token y lo obligatorio es la acción.
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.dudoso.id}/moderar/",
            {},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 400)
        self.assertIn("accion", respuesta.data)
        self.dudoso.refresh_from_db()
        self.assertEqual(self.dudoso.estado, "DUDOSO")

    def test_moderar_contenido_inexistente_devuelve_404(self):
        respuesta = self.client.post(
            "/api/moderacion/contenidos/99999/moderar/",
            {"accion": "APROBAR"},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 404)

    def test_rechazar_con_motivo_guarda_el_motivo_en_motivo_revision(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.dudoso.id}/moderar/",
            {"accion": "RECHAZAR", "motivo_rechazo": "Publicidad de apuestas"},
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
            {"accion": "RECHAZAR"},
            format="json",
        )

        self.assertEqual(respuesta.status_code, 200)

        self.dudoso.refresh_from_db()
        self.assertEqual(self.dudoso.estado, "RECHAZADO")
        self.assertEqual(self.dudoso.motivo_revision, '"cigarrillos": Publicidad de tabaco.')


URL_HISTORIAL = "/api/moderacion/historial/"


class HistorialContenidoTests(APITestCase):
    """HU 16: historial de contenidos aprobados y rechazados con moderador y motivo.

    Usa la vista real de Flyway (V34), así se prueba también el nombre del moderador.
    """

    @classmethod
    def setUpTestData(cls):
        usar_vista_de_flyway("V34__crear_vista_historial_contenido.sql")
        PerfilNegocio.objects.create(usuario_id=1, nombre_comercial="Panadería Doña Rosa", rubro="Panadería")
        cls.moderador = crear_admin("MODERADOR")
        cls.aprobado_por_filtro = crear_contenido("APROBADO", "2 panetones por S/ 35", minutos_atras=30)
        cls.dudoso = crear_contenido("DUDOSO", "Cigarrillos importados", motivo='"cigarrillos": Publicidad de tabaco.')

    def setUp(self):
        iniciar_sesion(self.client, self.moderador)

    def test_muestra_aprobados_y_rechazados_pero_no_los_dudosos(self):
        rechazado = crear_contenido("RECHAZADO", "Apuestas", motivo="Publicidad de apuestas")

        respuesta = self.client.get(URL_HISTORIAL)

        self.assertEqual(respuesta.status_code, 200)
        ids = [fila["id"] for fila in respuesta.data["resultados"]]
        self.assertEqual(ids, [rechazado.id, self.aprobado_por_filtro.id])  # el más reciente primero
        self.assertEqual(respuesta.data["total"], 2)

    def test_cada_fila_trae_fecha_moderador_y_motivo(self):
        # Criterio 1: lo aprobó el filtro automático (sin moderador).
        fila = self.client.get(URL_HISTORIAL).data["resultados"][0]

        self.assertEqual(fila["empresa"], "Panadería Doña Rosa")
        self.assertEqual(fila["moderador"], "Filtro automático")
        self.assertIsNone(fila["motivo"])
        self.assertIsNotNone(fila["fecha_decision"])

    def test_inicio_posterior_al_fin_responde_400(self):
        respuesta = self.client.get(f"{URL_HISTORIAL}?desde=2026-10-20&hasta=2026-10-01")

        self.assertEqual(respuesta.status_code, 400)
        self.assertEqual(respuesta.data["desde"][0], "La fecha de inicio no puede ser posterior a la fecha de fin")

    def test_finanzas_no_entra(self):
        iniciar_sesion(self.client, crear_admin("FINANZAS"))

        self.assertEqual(self.client.get(URL_HISTORIAL).status_code, 403)

    # --- Tarea 4 de la HU 16: el historial con contenidos de distintos estados ---

    def decidido_hace(self, estado, oferta, dias, **campos):
        """Un contenido decidido hace `dias` días, al mediodía de Lima."""
        contenido = crear_contenido(estado, oferta, **campos)
        fecha = timezone.localtime().replace(hour=12, minute=0, second=0, microsecond=0) - timezone.timedelta(days=dias)
        Contenido.objects.filter(pk=contenido.pk).update(fecha_actualizacion=fecha)
        return contenido

    def dia(self, dias_atras):
        return (timezone.localdate() - timezone.timedelta(days=dias_atras)).isoformat()

    def ids(self, respuesta):
        return [fila["id"] for fila in respuesta.data["resultados"]]

    def test_filtra_por_estado(self):
        rechazado = crear_contenido("RECHAZADO", "Apuestas", motivo="Publicidad de apuestas")

        rechazados = self.client.get(f"{URL_HISTORIAL}?estado=RECHAZADO")
        aprobados = self.client.get(f"{URL_HISTORIAL}?estado=APROBADO")

        self.assertEqual(self.ids(rechazados), [rechazado.id])
        self.assertEqual(self.ids(aprobados), [self.aprobado_por_filtro.id])

    def test_filtra_por_fechas_con_los_dos_dias_incluidos(self):
        hace_10 = self.decidido_hace("APROBADO", "Hace 10 días", 10)
        hace_5 = self.decidido_hace("RECHAZADO", "Hace 5 días", 5)
        hace_3 = self.decidido_hace("APROBADO", "Hace 3 días", 3)

        respuesta = self.client.get(f"{URL_HISTORIAL}?desde={self.dia(5)}&hasta={self.dia(3)}")

        self.assertEqual(self.ids(respuesta), [hace_3.id, hace_5.id])
        self.assertNotIn(hace_10.id, self.ids(respuesta))

    def test_un_solo_dia_y_estado_juntos(self):
        hace_5 = self.decidido_hace("RECHAZADO", "Hace 5 días", 5)
        self.decidido_hace("APROBADO", "También hace 5 días", 5)

        respuesta = self.client.get(f"{URL_HISTORIAL}?estado=RECHAZADO&desde={self.dia(5)}&hasta={self.dia(5)}")

        self.assertEqual(self.ids(respuesta), [hace_5.id])

    def test_un_estado_invalido_responde_400(self):
        for estado in ["DUDOSO", "otro"]:
            with self.subTest(estado=estado):
                respuesta = self.client.get(f"{URL_HISTORIAL}?estado={estado}")
                self.assertEqual(respuesta.status_code, 400)
                self.assertIn("estado", respuesta.data)

    def test_una_fecha_con_formato_invalido_responde_400(self):
        respuesta = self.client.get(f"{URL_HISTORIAL}?desde=27/10/2026")

        self.assertEqual(respuesta.status_code, 400)
        self.assertEqual(respuesta.data["desde"][0], "Usa el formato AAAA-MM-DD.")

    def test_muestra_el_nombre_del_moderador_cuando_lo_hay(self):
        crear_contenido("RECHAZADO", "Apuestas", motivo="Publicidad de apuestas")
        Contenido.objects.filter(oferta="Apuestas").update(moderador_id=self.moderador.id)

        fila = self.client.get(f"{URL_HISTORIAL}?estado=RECHAZADO").data["resultados"][0]

        self.assertEqual(fila["moderador"], f"{self.moderador.nombres} {self.moderador.apellidos}")

    def test_lo_que_rechaza_un_moderador_en_la_hu_15_aparece_con_su_motivo(self):
        respuesta = self.client.post(
            f"/api/moderacion/contenidos/{self.dudoso.id}/moderar/",
            {"accion": "RECHAZAR", "motivo_rechazo": "Publicidad de tabaco"},
            format="json",
        )
        self.assertEqual(respuesta.status_code, 200)

        fila = self.client.get(URL_HISTORIAL).data["resultados"][0]

        self.assertEqual(fila["id"], self.dudoso.id)
        self.assertEqual(fila["estado"], "RECHAZADO")
        self.assertEqual(fila["motivo"], "Publicidad de tabaco")
        self.assertEqual(fila["moderador"], f"{self.moderador.nombres} {self.moderador.apellidos}")
        self.assertEqual(fila["texto"], self.dudoso.texto)

    def test_una_empresa_sin_perfil_sale_con_su_id(self):
        contenido = crear_contenido("APROBADO", "Promo")
        Contenido.objects.filter(pk=contenido.pk).update(usuario_id=9)

        fila = self.client.get(URL_HISTORIAL).data["resultados"][0]

        self.assertEqual(fila["empresa"], "Empresa #9")

    def test_pagina_de_20_en_20(self):
        for numero in range(24):
            crear_contenido("APROBADO", f"Promo {numero}")  # 24 más el aprobado del setUp = 25

        primera = self.client.get(URL_HISTORIAL).data
        segunda = self.client.get(f"{URL_HISTORIAL}?pagina=2").data

        self.assertEqual((primera["total"], primera["pagina"], primera["paginas"]), (25, 1, 2))
        self.assertEqual(len(primera["resultados"]), 20)
        self.assertEqual(len(segunda["resultados"]), 5)
        self.assertFalse(set(self.ids_de(primera)) & set(self.ids_de(segunda)))

    def ids_de(self, datos):
        return [fila["id"] for fila in datos["resultados"]]

    def test_una_pagina_invalida_responde_400(self):
        self.assertEqual(self.client.get(f"{URL_HISTORIAL}?pagina=0").status_code, 400)

    def test_el_superadmin_tambien_entra(self):
        iniciar_sesion(self.client, crear_admin("SUPERADMIN"))

        self.assertEqual(self.client.get(URL_HISTORIAL).status_code, 200)

