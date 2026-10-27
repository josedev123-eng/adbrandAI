package com.adbrand.core.calendario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.calendario.dto.CrearPublicacionRequest;
import com.adbrand.core.calendario.dto.PublicacionResponse;
import com.adbrand.core.calendario.entity.EstadoPublicacion;
import com.adbrand.core.calendario.entity.PublicacionProgramada;
import com.adbrand.core.calendario.entity.RedSocialPublicacion;
import com.adbrand.core.calendario.repository.PublicacionProgramadaRepository;
import com.adbrand.core.calendario.service.CalendarioService;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

// HU 33: el calendario pide solo el mes elegido y las publicaciones nuevas quedan PROGRAMADAS.
@ExtendWith(MockitoExtension.class)
class CalendarioServiceTest {

    @Mock
    private PublicacionProgramadaRepository repositorio;

    private CalendarioService servicio;

    @BeforeEach
    void setUp() {
        servicio = new CalendarioService(repositorio);
    }

    private PublicacionProgramada publicacion() {
        PublicacionProgramada publicacion = new PublicacionProgramada();
        ReflectionTestUtils.setField(publicacion, "id", 3L);
        publicacion.setUsuarioId(1L);
        publicacion.setTitulo("Promoción de fin de semana");
        publicacion.setRedSocial(RedSocialPublicacion.INSTAGRAM);
        publicacion.setFecha(LocalDate.of(2026, 11, 14));
        publicacion.setHora(LocalTime.of(18, 30));
        publicacion.setEstado(EstadoPublicacion.PROGRAMADA);
        return publicacion;
    }

    @Test
    void listarPideElMesCompletoYDevuelveLasPublicaciones() {
        when(repositorio.findByUsuarioIdAndFechaBetweenOrderByFechaAscHoraAsc(
                eq(1L), eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 11, 30))))
                .thenReturn(List.of(publicacion()));

        List<PublicacionResponse> respuesta = servicio.listar(1L, "2026-11");

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.get(0).id()).isEqualTo(3L);
        assertThat(respuesta.get(0).redSocial()).isEqualTo("INSTAGRAM");
        assertThat(respuesta.get(0).fecha()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(respuesta.get(0).hora()).isEqualTo(LocalTime.of(18, 30));
        assertThat(respuesta.get(0).estado()).isEqualTo("PROGRAMADA");
    }

    @Test
    void sinMesUsaElMesActual() {
        YearMonth ahora = YearMonth.now();
        when(repositorio.findByUsuarioIdAndFechaBetweenOrderByFechaAscHoraAsc(
                eq(1L), eq(ahora.atDay(1)), eq(ahora.atEndOfMonth())))
                .thenReturn(List.of());

        assertThat(servicio.listar(1L, null)).isEmpty();
    }

    @Test
    void crearGuardaLaPublicacionProgramada() {
        when(repositorio.saveAndFlush(any(PublicacionProgramada.class))).thenAnswer(invocacion -> {
            PublicacionProgramada guardada = invocacion.getArgument(0);
            ReflectionTestUtils.setField(guardada, "id", 3L);
            return guardada;
        });

        PublicacionResponse respuesta = servicio.crear(1L, new CrearPublicacionRequest(
                "  Promoción de fin de semana  ", RedSocialPublicacion.FACEBOOK,
                LocalDate.of(2026, 11, 14), LocalTime.of(18, 30)));

        assertThat(respuesta.id()).isEqualTo(3L);
        assertThat(respuesta.titulo()).isEqualTo("Promoción de fin de semana");
        assertThat(respuesta.redSocial()).isEqualTo("FACEBOOK");
        assertThat(respuesta.estado()).isEqualTo("PROGRAMADA");

        org.mockito.ArgumentCaptor<PublicacionProgramada> captor =
                org.mockito.ArgumentCaptor.forClass(PublicacionProgramada.class);
        verify(repositorio).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUsuarioId()).isEqualTo(1L);
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoPublicacion.PROGRAMADA);
    }

    @Test
    void unMesConFormatoIncorrectoResponde400() {
        assertThatThrownBy(() -> servicio.listar(1L, "noviembre"))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("AAAA-MM");
    }
}
