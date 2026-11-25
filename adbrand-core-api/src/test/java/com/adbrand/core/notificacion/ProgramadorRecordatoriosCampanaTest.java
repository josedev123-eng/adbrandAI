package com.adbrand.core.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.adbrand.core.calendario.entity.Campana;
import com.adbrand.core.calendario.entity.TipoCampana;
import com.adbrand.core.notificacion.recordatorio.CampanaPorRecordar;
import com.adbrand.core.notificacion.recordatorio.ConsultaRecordatoriosCampana;
import com.adbrand.core.notificacion.recordatorio.ProgramadorRecordatoriosCampana;
import com.adbrand.core.notificacion.service.ServicioNotificaciones;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

// HU 49: un aviso por empresa, con el id de la campaña y el mensaje correcto.
@ExtendWith(MockitoExtension.class)
class ProgramadorRecordatoriosCampanaTest {

    @Mock
    private ConsultaRecordatoriosCampana consulta;

    @Mock
    private ServicioNotificaciones notificaciones;

    private ProgramadorRecordatoriosCampana programador;

    @BeforeEach
    void preparar() {
        programador = new ProgramadorRecordatoriosCampana(consulta, notificaciones, "America/Lima");
    }

    private CampanaPorRecordar recordatorio(long id, long usuarioId, String nombre, boolean empieza) {
        Campana campana = new Campana();
        ReflectionTestUtils.setField(campana, "id", id);
        campana.setUsuarioId(usuarioId);
        campana.setNombre(nombre);
        campana.setColor("#1F3D33");
        campana.setTipo(TipoCampana.CAMPANA);
        LocalDate manana = LocalDate.of(2026, 11, 19);
        campana.setFechaInicio(empieza ? manana : manana.minusDays(5));
        campana.setFechaFin(empieza ? manana.plusDays(5) : manana);
        return new CampanaPorRecordar(campana, empieza, !empieza);
    }

    @Test
    void enviaUnAvisoConElIdDeLaCampana() {
        LocalDate hoy = LocalDate.of(2026, 11, 18);
        Map<Long, List<CampanaPorRecordar>> grupos = new LinkedHashMap<>();
        grupos.put(7L, List.of(recordatorio(31L, 7L, "Feria de verano", true)));
        when(consulta.agrupadasPorUsuario(hoy)).thenReturn(grupos);
        when(notificaciones.enviarAUsuario(eq(7L), anyString(), anyString(), anyMap())).thenReturn(2);

        int enviados = programador.recordarCampanasDeManana(hoy);

        assertThat(enviados).isEqualTo(2);
        ArgumentCaptor<Map<String, String>> payload = ArgumentCaptor.forClass(Map.class);
        verify(notificaciones).enviarAUsuario(eq(7L), anyString(), anyString(), payload.capture());

        assertThat(payload.getValue())
                .containsEntry("type", "campaign_reminder")
                .containsEntry("campaign_id", "31")
                .containsEntry("campaign_name", "Feria de verano")
                .containsEntry("campaign_count", "1");
    }

    @Test
    void agrupaVariasCampanasEnUnSoloAvisoPorEmpresa() {
        LocalDate hoy = LocalDate.of(2026, 11, 18);
        Map<Long, List<CampanaPorRecordar>> grupos = new LinkedHashMap<>();
        grupos.put(7L, List.of(
                recordatorio(31L, 7L, "Feria de verano", true),
                recordatorio(32L, 7L, "Sorteo", false)));
        grupos.put(8L, List.of(recordatorio(40L, 8L, "Aniversario", true)));
        when(consulta.agrupadasPorUsuario(hoy)).thenReturn(grupos);
        when(notificaciones.enviarAUsuario(any(), anyString(), anyString(), anyMap())).thenReturn(1);

        programador.recordarCampanasDeManana(hoy);

        ArgumentCaptor<String> titulos = ArgumentCaptor.forClass(String.class);
        verify(notificaciones, org.mockito.Mockito.times(2))
                .enviarAUsuario(any(), titulos.capture(), anyString(), anyMap());
        assertThat(titulos.getAllValues())
                .contains("Mañana empiezan o terminan 2 campañas", "Mañana es clave para tu campaña");
    }

    @Test
    void sinCampanasNoEnviaNada() {
        LocalDate hoy = LocalDate.of(2026, 11, 18);
        when(consulta.agrupadasPorUsuario(hoy)).thenReturn(Map.of());

        assertThat(programador.recordarCampanasDeManana(hoy)).isZero();
        verifyNoInteractions(notificaciones);
        verify(notificaciones, never()).enviarAUsuario(any(), anyString(), anyString(), anyMap());
    }
}
