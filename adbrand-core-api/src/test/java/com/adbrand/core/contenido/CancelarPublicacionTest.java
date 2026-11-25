package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.adbrand.core.calendario.entity.EstadoPublicacion;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import com.adbrand.core.calendario.repository.PublicacionProgramadaRepository;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.DecisionDueno;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.contenido.service.RevisionDuenoService;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Mis anuncios: "Cancelar publicación" saca el anuncio de lo que se va a publicar y cancela sus fechas del calendario;
// "Volver a activar" lo deja otra vez listo para publicar.
class CancelarPublicacionTest {

    private ContenidoRepository contenidos;
    private PublicacionProgramadaRepository publicaciones;
    private RevisionDuenoService servicio;
    private Contenido contenido;

    @BeforeEach
    void preparar() {
        contenidos = mock(ContenidoRepository.class);
        publicaciones = mock(PublicacionProgramadaRepository.class);
        servicio = new RevisionDuenoService(contenidos, publicaciones);
        contenido = new Contenido();
        contenido.setUsuarioId(1L);
        when(contenidos.findById(9L)).thenReturn(Optional.of(contenido));
        when(contenidos.save(any(Contenido.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void cancelaElAnuncioYSusFechasProgramadas() {
        PublicacionProgramada uno = new PublicacionProgramada();
        uno.setEstado(EstadoPublicacion.PROGRAMADA);
        PublicacionProgramada dos = new PublicacionProgramada();
        dos.setEstado(EstadoPublicacion.PROGRAMADA);
        when(publicaciones.findByContenidoIdAndEstado(9L, EstadoPublicacion.PROGRAMADA)).thenReturn(List.of(uno, dos));

        int canceladas = servicio.cancelarPublicacion(1L, 9L);

        assertThat(canceladas).isEqualTo(2);
        assertThat(uno.getEstado()).isEqualTo(EstadoPublicacion.CANCELADA);
        assertThat(contenido.getDecisionDueno()).isEqualTo(DecisionDueno.RECHAZADO);
    }

    @Test
    void volverAActivarLoDejaListoOtraVez() {
        contenido.setDecisionDueno(DecisionDueno.RECHAZADO);

        servicio.reactivar(1L, 9L);

        assertThat(contenido.getDecisionDueno()).isEqualTo(DecisionDueno.APROBADO);
        assertThat(contenido.getMotivoDueno()).isNull();
    }

    @Test
    void noSePuedeCancelarElAnuncioDeOtraEmpresa() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> servicio.cancelarPublicacion(2L, 9L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void soloSeEliminanLosCancelados() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> servicio.eliminar(1L, 9L))
                .isInstanceOf(com.adbrand.core.shared.error.SolicitudInvalidaException.class)
                .hasMessageContaining("Primero cancélalo");

        contenido.setDecisionDueno(DecisionDueno.RECHAZADO);
        servicio.eliminar(1L, 9L);

        assertThat(contenido.isEliminado()).isTrue();
    }
}

