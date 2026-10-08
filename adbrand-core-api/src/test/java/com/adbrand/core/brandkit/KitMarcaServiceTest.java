package com.adbrand.core.brandkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.brandkit.dto.GenerarKitMarcaRequest;
import com.adbrand.core.brandkit.dto.KitMarcaGeneradoResponse;
import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.brandkit.service.KitMarcaService;
import com.adbrand.core.brandkit.service.PlantillaPromptKitMarca;
import com.adbrand.core.ia.service.IaNoDisponibleException;
import com.adbrand.core.ia.service.ServicioIa;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.entity.Tono;
import com.adbrand.core.negocio.service.PerfilNegocioService;
import com.adbrand.core.shared.error.ErrorResponse;
import com.adbrand.core.shared.error.ManejadorDeErrores;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Tarea de la HU 11: probar el servicio del kit de marca sin llamar a la IA real ni a la base de datos.
class KitMarcaServiceTest {

    private PerfilNegocioService perfilService;
    private ServicioIa ia;
    private KitMarcaRepository kits;
    private KitMarcaService servicio;

    private static final String JSON_IA = """
            {"logo_concepto": "Un sol sobre el horizonte", "tipografia_primaria": "Bricolage Grotesque",
             "tipografia_secundaria": "Plus Jakarta Sans", "paleta_colores": "#4f46e5 - Indigo - Confianza",
             "voz_marca": "Cercana y empática, usa tú"}""";

    @BeforeEach
    void preparar() {
        perfilService = mock(PerfilNegocioService.class);
        ia = mock(ServicioIa.class);
        kits = mock(KitMarcaRepository.class);
        servicio = new KitMarcaService(perfilService, new PlantillaPromptKitMarca(), ia, kits);

        // Por defecto "guardar" devuelve lo mismo que recibe.
        when(kits.save(any(KitMarca.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void perfilDelUsuario() {
        when(perfilService.obtener(1L)).thenReturn(new PerfilNegocioResponse(
                1L, "Panadería Doña Rosa", "Panadería y pastelería", "Familias del barrio", Tono.CERCANO, null, null));
    }

    private GenerarKitMarcaRequest datos() {
        return new GenerarKitMarcaRequest("Panadería Doña Rosa", "Panadería", "Verde y crema", "Hecho en casa", "Cálido");
    }

    // HU 11: el servicio arma el prompt, pide el kit a la IA y lo guarda completo.
    @Test
    void generaYGuardaElKitCorrectamente() {
        perfilDelUsuario();
        when(ia.generarTexto(anyString(), anyString())).thenReturn(JSON_IA);

        KitMarcaGeneradoResponse kit = servicio.generar(1L, datos());

        verify(ia).generarTexto(anyString(), anyString());

        ArgumentCaptor<KitMarca> captor = ArgumentCaptor.forClass(KitMarca.class);
        verify(kits).save(captor.capture());
        KitMarca guardado = captor.getValue();

        assertThat(kit.nombreNegocio()).isEqualTo("Panadería Doña Rosa");
        assertThat(kit.sector()).isEqualTo("Panadería");
        assertThat(kit.logoConcepto()).isEqualTo("Un sol sobre el horizonte");
        assertThat(kit.tipografiaPrimaria()).isEqualTo("Bricolage Grotesque");
        assertThat(kit.tipografiaSecundaria()).isEqualTo("Plus Jakarta Sans");
        assertThat(kit.paletaColores()).isEqualTo("#4f46e5 - Indigo - Confianza");
        assertThat(kit.vozMarca()).isEqualTo("Cercana y empática, usa tú");
        assertThat(kit.simulado()).isFalse();

        assertThat(guardado.getUsuarioId()).isEqualTo(1L);
        assertThat(guardado.getNombreNegocio()).isEqualTo("Panadería Doña Rosa");
        assertThat(guardado.getSector()).isEqualTo("Panadería");
        assertThat(guardado.getPaletaSugerida()).isEqualTo("Verde y crema");
        assertThat(guardado.getValoresEslogan()).isEqualTo("Hecho en casa");
        assertThat(guardado.getEstiloVisual()).isEqualTo("Cálido");
        assertThat(guardado.getLogoConcepto()).isEqualTo("Un sol sobre el horizonte");
        assertThat(guardado.getVozMarca()).isEqualTo("Cercana y empática, usa tú");
    }

    // HU 11: si la IA no responde el error sube tal cual y el manejador lo convierte en 503 IA_NO_DISPONIBLE.
    @Test
    void siLaIaFallaElErrorLlegaComoIaNoDisponible503() {
        perfilDelUsuario();
        when(ia.generarTexto(anyString(), anyString())).thenThrow(new IaNoDisponibleException("caída", null));

        assertThatThrownBy(() -> servicio.generar(1L, datos()))
                .isInstanceOf(IaNoDisponibleException.class);

        IaNoDisponibleException excepcion = new IaNoDisponibleException("caída", null);
        ErrorResponse error = new ManejadorDeErrores().iaNoDisponible(excepcion);
        assertThat(error.codigo()).isEqualTo("IA_NO_DISPONIBLE");

        Method manejo = null;
        try {
            manejo = ManejadorDeErrores.class.getMethod("iaNoDisponible", IaNoDisponibleException.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("El manejador ya no maneja IaNoDisponibleException", e);
        }
        ResponseStatus estado = manejo.getAnnotation(ResponseStatus.class);
        assertThat(estado.value()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
