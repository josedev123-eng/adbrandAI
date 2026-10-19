from unittest.mock import MagicMock, patch
from urllib.error import URLError

from django.db import DatabaseError
from rest_framework.test import APITestCase

from core.pruebas import crear_admin, iniciar_sesion

from .models import VerificacionServidor
from .services import IA_SIN_BACKEND

URL = "/api/servidor/estado/"
SPRING = "servidor.services.pedir_estado_a_spring"

SPRING_TODO_ACTIVO = {"backend": "ACTIVO", "ia": {"estado": "ACTIVO", "tiempo_ms": 40, "detalle": None}}


class EstadoServidorTests(APITestCase):
    """HU 18: estado del backend, la base de datos y la IA. Spring se simula, no hace falta tenerlo encendido."""

    @classmethod
    def setUpTestData(cls):
        cls.tecnico = crear_admin("TECNICO")

    def setUp(self):
        iniciar_sesion(self.client, self.tecnico)

    def consultar(self):
        respuesta = self.client.get(URL)
        self.assertEqual(respuesta.status_code, 200)
        return respuesta.data

    def por_servicio(self, datos):
        return {servicio["servicio"]: servicio for servicio in datos["servicios"]}

    @patch(SPRING, return_value=SPRING_TODO_ACTIVO)
    def test_todo_activo_con_la_hora_de_verificacion(self, _spring):
        # Criterio 1
        datos = self.consultar()

        servicios = self.por_servicio(datos)
        self.assertEqual(list(servicios), ["BACKEND", "BASE_DATOS", "IA"])
        for servicio in servicios.values():
            self.assertEqual(servicio["estado"], "ACTIVO")
        self.assertEqual(servicios["BACKEND"]["nombre"], "Backend")
        self.assertEqual(servicios["BASE_DATOS"]["nombre"], "Base de datos")
        self.assertEqual(servicios["IA"]["nombre"], "Servicio de IA")
        self.assertEqual(servicios["IA"]["tiempo_ms"], 40)
        self.assertIsNotNone(servicios["BASE_DATOS"]["tiempo_ms"])
        self.assertTrue(datos["verificado_en"].endswith("-05:00"))  # hora de Lima

    @patch(SPRING, side_effect=URLError(TimeoutError("timed out")))
    def test_si_spring_no_responde_backend_e_ia_quedan_caidos(self, _spring):
        # Criterio 2
        servicios = self.por_servicio(self.consultar())

        self.assertEqual(servicios["BACKEND"]["estado"], "CAIDO")
        self.assertEqual(servicios["BACKEND"]["detalle"], "El backend no responde")
        self.assertIsNone(servicios["BACKEND"]["tiempo_ms"])
        self.assertEqual(servicios["IA"]["estado"], "CAIDO")
        self.assertEqual(servicios["IA"]["detalle"], IA_SIN_BACKEND)
        self.assertEqual(servicios["BASE_DATOS"]["estado"], "ACTIVO")

    @patch(SPRING, side_effect=ValueError("respuesta que no es JSON"))
    def test_si_spring_responde_algo_roto_el_backend_queda_caido(self, _spring):
        servicios = self.por_servicio(self.consultar())

        self.assertEqual(servicios["BACKEND"]["estado"], "CAIDO")

    @patch(SPRING, return_value={"backend": "ACTIVO", "ia": {"estado": "CAIDO", "tiempo_ms": None,
                                                            "detalle": "La IA no respondió en 5 segundos"}})
    def test_si_spring_responde_pero_la_ia_esta_caida(self, _spring):
        servicios = self.por_servicio(self.consultar())

        self.assertEqual(servicios["BACKEND"]["estado"], "ACTIVO")
        self.assertEqual(servicios["IA"]["estado"], "CAIDO")
        self.assertEqual(servicios["IA"]["detalle"], "La IA no respondió en 5 segundos")

    @patch(SPRING, return_value={"backend": "ACTIVO"})
    def test_si_spring_no_dice_nada_de_la_ia_se_marca_caida(self, _spring):
        servicios = self.por_servicio(self.consultar())

        self.assertEqual(servicios["IA"]["estado"], "CAIDO")
        self.assertEqual(servicios["IA"]["detalle"], "La IA no responde")

    @patch(SPRING, return_value=SPRING_TODO_ACTIVO)
    def test_un_error_de_la_base_la_marca_caida_sin_romper_la_respuesta(self, _spring):
        conexion_rota = MagicMock()
        conexion_rota.cursor.side_effect = DatabaseError("sin conexión")
        with patch("servidor.services.connection", conexion_rota):
            with self.assertLogs("servidor.services", level="WARNING"):
                servicios = self.por_servicio(self.consultar())

        self.assertEqual(servicios["BASE_DATOS"]["estado"], "CAIDO")
        self.assertEqual(servicios["BASE_DATOS"]["detalle"], "La base de datos no responde")
        self.assertEqual(servicios["BACKEND"]["estado"], "ACTIVO")

    @patch(SPRING, return_value=SPRING_TODO_ACTIVO)
    def test_si_no_se_puede_guardar_igual_responde(self, _spring):
        with patch.object(VerificacionServidor.objects, "bulk_create", side_effect=DatabaseError("sin tabla")):
            with self.assertLogs("servidor.services", level="WARNING"):
                datos = self.consultar()

        self.assertEqual(len(datos["servicios"]), 3)

    @patch(SPRING, side_effect=URLError("connection refused"))
    def test_se_guardan_las_tres_verificaciones(self, _spring):
        self.consultar()

        guardadas = {fila.servicio: fila for fila in VerificacionServidor.objects.all()}
        self.assertEqual(set(guardadas), {"BACKEND", "BASE_DATOS", "IA"})
        self.assertEqual(guardadas["BACKEND"].estado, "CAIDO")
        self.assertEqual(guardadas["BACKEND"].detalle, "El backend no responde")
        self.assertEqual(guardadas["BASE_DATOS"].estado, "ACTIVO")
        self.assertEqual(len({fila.fecha for fila in guardadas.values()}), 1)  # las tres con la misma hora

    @patch(SPRING, return_value=SPRING_TODO_ACTIVO)
    def test_el_superadmin_tambien_entra(self, _spring):
        iniciar_sesion(self.client, crear_admin("SUPERADMIN"))

        self.assertEqual(self.client.get(URL).status_code, 200)

    @patch(SPRING, return_value=SPRING_TODO_ACTIVO)
    def test_el_moderador_recibe_403_y_no_se_verifica_nada(self, spring):
        iniciar_sesion(self.client, crear_admin("MODERADOR"))

        respuesta = self.client.get(URL)

        self.assertEqual(respuesta.status_code, 403)
        spring.assert_not_called()
        self.assertFalse(VerificacionServidor.objects.exists())
