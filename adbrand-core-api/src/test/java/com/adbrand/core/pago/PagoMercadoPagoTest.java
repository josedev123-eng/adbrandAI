package com.adbrand.core.pago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.pago.entity.MetodoPago;
import com.adbrand.core.pago.entity.Pago;
import com.adbrand.core.pago.entity.Plan;
import com.adbrand.core.pago.repository.PagoRepository;
import com.adbrand.core.pago.repository.PlanRepository;
import com.adbrand.core.pago.service.ClienteMercadoPago;
import com.adbrand.core.pago.service.CobroMercadoPago;
import com.adbrand.core.pago.service.ResultadoCobro;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 26 con Mercado Pago: el cobro aprobado activa el plan y guarda el pago; el rechazado no guarda nada.
// Mercado Pago se reemplaza por un doble: estas pruebas no salen a internet.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PagoMercadoPagoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PlanRepository planes;

    @Autowired
    private PagoRepository pagos;

    @Autowired
    private ServicioJwt jwt;

    @MockitoBean
    private ClienteMercadoPago mercadoPago;

    private Usuario duenio;
    private Plan plan;

    @BeforeEach
    void preparar() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa("Bodega Mercado Pago");
        usuario.setCorreo("duenio.mercadopago@adbrand.pe");
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        duenio = usuarios.saveAndFlush(usuario);
        plan = planes.findAll().stream().filter(Plan::isActivo).findFirst().orElseThrow();
        when(mercadoPago.disponible()).thenReturn(true);
        when(mercadoPago.llavePublica()).thenReturn("TEST-publica");
    }

    private ResultActions pagar() throws Exception {
        return mvc.perform(post("/api/pagos/mercadopago")
                .header("Authorization", "Bearer " + jwt.generar(duenio))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"planId": %d, "token": "tok_123", "metodoPago": "visa", "emisorId": "",
                         "correo": "ana@correo.pe", "titular": "APRO", "ultimosDigitos": "3704"}"""
                        .formatted(plan.getId())));
    }

    private List<Pago> pagosDelDuenio() {
        return pagos.findAll().stream().filter(p -> p.getUsuarioId().equals(duenio.getId())).toList();
    }

    @Test
    void laWebRecibeLaLlavePublicaParaDibujarElFormulario() throws Exception {
        mvc.perform(get("/api/pagos/configuracion").header("Authorization", "Bearer " + jwt.generar(duenio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasarela").value("mercadopago"))
                .andExpect(jsonPath("$.publicKey").value("TEST-publica"));
    }

    @Test
    void unPagoAprobadoActivaElPlanYGuardaLaReferenciaDeMercadoPago() throws Exception {
        when(mercadoPago.cobrar(eq(plan.getPrecio()), anyString(), any(), anyString()))
                .thenReturn(new CobroMercadoPago(ResultadoCobro.aprobado("MP-999"), "APRO", "3704"));

        pagar().andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        assertThat(pagosDelDuenio()).hasSize(1);
        assertThat(pagosDelDuenio().get(0).getReferencia()).isEqualTo("MP-999");
        assertThat(pagosDelDuenio().get(0).getUltimosDigitos()).isEqualTo("3704");
    }

    @Test
    void unPagoRechazadoMuestraElMotivoYNoGuardaNada() throws Exception {
        when(mercadoPago.cobrar(any(), anyString(), any(), anyString()))
                .thenReturn(CobroMercadoPago.sinTarjeta(ResultadoCobro.rechazado("La tarjeta no tiene fondos suficientes.")));

        pagar().andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PAGO_RECHAZADO"))
                .andExpect(jsonPath("$.mensaje").value("La tarjeta no tiene fondos suficientes."));

        assertThat(pagosDelDuenio()).isEmpty();
    }

    @Test
    void sinLlavesDePruebaNoSeLlamaAMercadoPago() throws Exception {
        when(mercadoPago.disponible()).thenReturn(false);

        pagar().andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PASARELA_NO_CONFIGURADA"));

        verify(mercadoPago, never()).cobrar(any(), anyString(), any(), anyString());
    }

    private ResultActions pagarConYape(String celular) throws Exception {
        return mvc.perform(post("/api/pagos/mercadopago/yape")
                .header("Authorization", "Bearer " + jwt.generar(duenio))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"planId": %d, "token": "yape_tok", "celular": "%s", "correo": "comprador@testuser.com"}"""
                        .formatted(plan.getId(), celular)));
    }

    @Test
    void unYapeAprobadoActivaElPlanYSeGuardaComoYape() throws Exception {
        when(mercadoPago.cobrarConYape(eq(plan.getPrecio()), anyString(), any(), anyString()))
                .thenReturn(CobroMercadoPago.sinTarjeta(ResultadoCobro.aprobado("MP-ORDYAPE")));

        pagarConYape("111111111").andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        assertThat(pagosDelDuenio()).hasSize(1);
        assertThat(pagosDelDuenio().get(0).getMetodo()).isEqualTo(MetodoPago.YAPE);
        assertThat(pagosDelDuenio().get(0).getReferencia()).isEqualTo("MP-ORDYAPE");
        assertThat(pagosDelDuenio().get(0).getUltimosDigitos()).isEqualTo("1111");
    }

    @Test
    void unCelularInvalidoNoLlegaAMercadoPago() throws Exception {
        pagarConYape("12345").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));

        verify(mercadoPago, never()).cobrarConYape(any(), anyString(), any(), anyString());
    }
}
