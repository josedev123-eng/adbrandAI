from datetime import timedelta

from django.utils import timezone
from rest_framework.test import APITestCase

from core.pruebas import con_sesion, crear_admin, iniciar_sesion
from moderacion.models import PerfilNegocio

from .models import ConsumoToken
from .services import inicio_del_mes_actual

URL = "/api/tokens/consumo-actual/"


def consumir(usuario_id, tokens, fecha=None, origen="ANUNCIO"):
    return ConsumoToken.objects.create(
        usuario_id=usuario_id, origen=origen, modelo="Qwen/Qwen3", tokens_entrada=tokens // 2,
        tokens_salida=tokens - tokens // 2, tokens_total=tokens, fecha=fecha or timezone.now(),
    )


class ConsumoActualTests(APITestCase):
    """HU 17: tokens de IA consumidos por cada empresa en el mes actual."""

    @classmethod
    def setUpTestData(cls):
        cls.tecnico = crear_admin("TECNICO")
        PerfilNegocio.objects.create(usuario_id=1, nombre_comercial="Panadería Doña Rosa", rubro="Panadería")
        PerfilNegocio.objects.create(usuario_id=2, nombre_comercial="Café Central", rubro="Cafetería")

    def setUp(self):
        iniciar_sesion(self.client, self.tecnico)

    def consultar(self):
        respuesta = self.client.get(URL)
        self.assertEqual(respuesta.status_code, 200)
        return respuesta.data

    def test_sin_consumo_el_total_es_cero(self):
        datos = self.consultar()

        self.assertEqual(datos["mes"], timezone.localdate().strftime("%Y-%m"))
        self.assertEqual(datos["total"], 0)
        self.assertEqual(datos["empresas"], [])

    def test_suma_los_tokens_de_cada_empresa(self):
        consumir(1, 100)
        consumir(1, 50, origen="REGENERAR_ANUNCIO")
        consumir(2, 30, origen="KIT_MARCA")

        datos = self.consultar()

        self.assertEqual(datos["total"], 180)
        rosa = datos["empresas"][0]
        self.assertEqual(rosa["usuario_id"], 1)
        self.assertEqual(rosa["tokens"], 150)
        self.assertEqual(rosa["generaciones"], 2)

    def test_ordena_de_la_empresa_que_mas_gasta_a_la_que_menos(self):
        consumir(1, 20)
        consumir(2, 500)

        empresas = self.consultar()["empresas"]

        self.assertEqual([fila["usuario_id"] for fila in empresas], [2, 1])

    def test_no_cuenta_los_meses_anteriores(self):
        consumir(1, 100)
        consumir(1, 999, fecha=inicio_del_mes_actual() - timedelta(minutes=1))

        datos = self.consultar()

        self.assertEqual(datos["total"], 100)
        self.assertEqual(datos["empresas"][0]["generaciones"], 1)

    def test_el_nombre_sale_del_perfil_o_es_empresa_con_su_id(self):
        consumir(2, 10)
        consumir(7, 10)

        nombres = {fila["usuario_id"]: fila["empresa"] for fila in self.consultar()["empresas"]}

        self.assertEqual(nombres[2], "Café Central")
        self.assertEqual(nombres[7], "Empresa #7")

    def test_trae_la_fecha_de_la_ultima_generacion(self):
        consumir(1, 10, fecha=timezone.now() - timedelta(seconds=30))
        reciente = consumir(1, 10)

        fila = self.consultar()["empresas"][0]

        self.assertEqual(fila["ultima_generacion"], timezone.localtime(reciente.fecha).isoformat())

    def test_cada_generacion_nueva_se_ve_al_volver_a_consultar(self):
        # HU 17, criterio 2: el panel se actualiza sin volver a iniciar sesión.
        consumir(1, 100)
        self.assertEqual(self.consultar()["total"], 100)

        consumir(1, 40)

        self.assertEqual(self.consultar()["total"], 140)


class ConsumoActualPermisosTests(APITestCase):
    """HU 17: solo el administrador técnico y el superadministrador ven el consumo."""

    def test_tecnico_y_superadmin_entran(self):
        for rol in ["TECNICO", "SUPERADMIN"]:
            with self.subTest(rol=rol):
                respuesta = self.client.get(URL, **con_sesion(crear_admin(rol)))
                self.assertEqual(respuesta.status_code, 200)

    def test_moderador_y_finanzas_reciben_403(self):
        for rol in ["MODERADOR", "FINANZAS"]:
            with self.subTest(rol=rol):
                respuesta = self.client.get(URL, **con_sesion(crear_admin(rol)))
                self.assertEqual(respuesta.status_code, 403)
                self.assertEqual(respuesta.data["detail"], "Tu rol no tiene acceso a esta sección.")

    def test_sin_sesion_pide_iniciar_sesion(self):
        respuesta = self.client.get(URL)

        self.assertEqual(respuesta.status_code, 403)
        self.assertEqual(respuesta.data["detail"], "Inicia sesión para continuar.")
