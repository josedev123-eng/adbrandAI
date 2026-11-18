package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.contenido.dto.VersionesAnuncioResponse;
import com.adbrand.core.contenido.entity.OpcionAnuncio;
import com.adbrand.core.contenido.entity.VersionAnuncio;
import com.adbrand.core.contenido.repository.OpcionAnuncioRepository;
import com.adbrand.core.contenido.repository.VersionAnuncioRepository;
import com.adbrand.core.contenido.service.RetoqueAnuncioService;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.imagen.service.ServicioImagenes;
import com.adbrand.core.imagen.service.ControlGastoImagenes;
import com.adbrand.core.imagen.entity.ConsumoImagen;
import com.adbrand.core.contenido.entity.GeneracionAnuncio;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// "Retocar con prompts cortos": versiones v1 (original), v2, v3… y deshacer volviendo a una anterior.
class RetoqueAnuncioServiceTest {

    private static final ImagenGenerada ORIGINAL = new ImagenGenerada("image/jpeg", "ORIGINAL", "cloudflare", false);

    private OpcionAnuncioRepository opciones;
    private VersionAnuncioRepository versiones;
    private ServicioImagenes imagenes;
    private ControlGastoImagenes gasto;
    private RetoqueAnuncioService servicio;
    private OpcionAnuncio opcion;
    private final List<VersionAnuncio> historial = new ArrayList<>();
    private int retoques = 0;

    @BeforeEach
    void preparar() {
        opciones = mock(OpcionAnuncioRepository.class);
        versiones = mock(VersionAnuncioRepository.class);
        imagenes = mock(ServicioImagenes.class);
        gasto = mock(ControlGastoImagenes.class);
        when(gasto.retoquesRestantes(any())).thenReturn(19);
        servicio = new RetoqueAnuncioService(opciones, versiones, imagenes, gasto);

        opcion = new OpcionAnuncio();
        ReflectionTestUtils.setField(opcion, "id", 102L);
        opcion.setNumero(2);
        GeneracionAnuncio generacion = new GeneracionAnuncio();
        generacion.setUsuarioId(1L);
        opcion.setGeneracion(generacion);
        opcion.setElegida(true);
        opcion.ponerImagen(ORIGINAL);
        when(opciones.findByIdAndGeneracionUsuarioId(102L, 1L)).thenReturn(Optional.of(opcion));
        when(opciones.save(any(OpcionAnuncio.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        // Un historial "en memoria" en lugar de la base de datos.
        when(versiones.save(any(VersionAnuncio.class))).thenAnswer(invocacion -> {
            historial.add(invocacion.getArgument(0));
            return invocacion.getArgument(0);
        });
        when(versiones.findByOpcionIdOrderByNumero(102L)).thenAnswer(invocacion -> List.copyOf(historial));
        when(versiones.findByOpcionIdAndNumero(eq(102L), any())).thenAnswer(invocacion -> historial.stream()
                .filter(version -> version.getNumero().equals(invocacion.getArgument(1)))
                .findFirst());
        when(imagenes.finalORetoque(anyString(), any())).thenAnswer(invocacion ->
                new ImagenGenerada("image/png", "RETOQUE" + (++retoques), "openrouter", false));
    }

    @Test
    void elPrimerRetoqueGuardaLaOriginalComoV1YLaNuevaComoV2() {
        VersionesAnuncioResponse respuesta = servicio.retocar(1L, 102L, "pon el fondo más claro");

        assertThat(respuesta.versionActual()).isEqualTo(2);
        assertThat(respuesta.imagenActual()).isEqualTo("data:image/png;base64,RETOQUE1");
        assertThat(respuesta.versiones()).extracting("numero").containsExactly(1, 2);
        assertThat(respuesta.versiones().get(0).instruccion()).isNull();
        assertThat(respuesta.versiones().get(0).imagen()).isEqualTo("data:image/jpeg;base64,ORIGINAL");
        assertThat(respuesta.versiones().get(1).instruccion()).isEqualTo("pon el fondo más claro");
        assertThat(respuesta.versiones().get(1).actual()).isTrue();
        // La IA recibe la imagen actual para editarla, no empieza de cero.
        verify(imagenes).finalORetoque(anyString(), eq(ORIGINAL));
    }

    @Test
    void cadaRetoqueSeHaceSobreLaUltimaImagen() {
        servicio.retocar(1L, 102L, "fondo más claro");
        VersionesAnuncioResponse respuesta = servicio.retocar(1L, 102L, "logo arriba a la derecha");

        assertThat(respuesta.versiones()).extracting("numero").containsExactly(1, 2, 3);
        assertThat(respuesta.versionActual()).isEqualTo(3);
        verify(imagenes).finalORetoque(anyString(), eq(new ImagenGenerada("image/png", "RETOQUE1", "openrouter", false)));
    }

    @Test
    void deshacerVuelveAUsarUnaVersionAnterior() {
        servicio.retocar(1L, 102L, "fondo más claro");

        VersionesAnuncioResponse respuesta = servicio.usarVersion(1L, 102L, 1);

        assertThat(respuesta.versionActual()).isEqualTo(1);
        assertThat(respuesta.imagenActual()).isEqualTo("data:image/jpeg;base64,ORIGINAL");
        assertThat(respuesta.versiones().get(0).actual()).isTrue();
        assertThat(opcion.imagen()).isEqualTo(ORIGINAL);
    }

    @Test
    void laInstruccionLlegaALaIaYPideNoAgregarTexto() {
        servicio.retocar(1L, 102L, "  pon el fondo más claro  ");

        verify(imagenes).finalORetoque(
                org.mockito.ArgumentMatchers.argThat(descripcion -> descripcion.contains("Change only this: pon el fondo más claro.")
                        && descripcion.contains("Do not add text")),
                any());
    }

    @Test
    void noSeRetocaUnaOpcionQueNoFueElegida() {
        opcion.setElegida(false);

        assertThatThrownBy(() -> servicio.retocar(1L, 102L, "fondo"))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("Primero elige");
        verify(imagenes, never()).finalORetoque(anyString(), any());
    }

    @Test
    void laOpcionDeOtraEmpresaNoSeEncuentra() {
        when(opciones.findByIdAndGeneracionUsuarioId(anyLong(), eq(2L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.retocar(2L, 102L, "fondo"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void usarUnaVersionQueNoExisteRespondeNoEncontrada() {
        assertThatThrownBy(() -> servicio.usarVersion(1L, 102L, 9))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("versión");
    }

    @Test
    void cadaRetoqueQuedaAnotadoYDiceCuantosQuedan() {
        VersionesAnuncioResponse respuesta = servicio.retocar(1L, 102L, "fondo más claro");

        verify(gasto).verificarRetoqueDisponible(1L);
        verify(gasto).registrar(eq(1L), eq(ConsumoImagen.Tipo.RETOQUE), any());
        assertThat(respuesta.retoquesRestantes()).isEqualTo(19);
    }

    @Test
    void siLlegoAlLimiteDelMesNoSeLlamaALaIaDePago() {
        org.mockito.Mockito.doThrow(new SolicitudInvalidaException("LIMITE_RETOQUES", "Llegaste a tu límite de 20 retoques"))
                .when(gasto).verificarRetoqueDisponible(1L);

        assertThatThrownBy(() -> servicio.retocar(1L, 102L, "fondo"))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("límite");
        verify(imagenes, never()).finalORetoque(anyString(), any());
    }

    @Test
    void enModoDePruebaNoSeLimitaNiSeMuestranRestantes() {
        when(imagenes.esSimulado()).thenReturn(true);

        VersionesAnuncioResponse respuesta = servicio.retocar(1L, 102L, "fondo");

        verify(gasto, never()).verificarRetoqueDisponible(any());
        assertThat(respuesta.retoquesRestantes()).isNull();
    }
}
