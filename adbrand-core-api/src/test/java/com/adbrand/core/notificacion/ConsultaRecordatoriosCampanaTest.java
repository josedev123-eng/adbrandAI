package com.adbrand.core.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.calendario.entity.Campana;
import com.adbrand.core.calendario.entity.TipoCampana;
import com.adbrand.core.calendario.repository.CampanaRepository;
import com.adbrand.core.notificacion.recordatorio.CampanaPorRecordar;
import com.adbrand.core.notificacion.recordatorio.ConsultaRecordatoriosCampana;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// HU 49: la consulta usa la fecha de mañana y distingue inicio de fin.
@ExtendWith(MockitoExtension.class)
class ConsultaRecordatoriosCampanaTest {

    @Mock
    private CampanaRepository campanas;

    @InjectMocks
    private ConsultaRecordatoriosCampana consulta;

    private Campana campana(String nombre, long usuarioId, LocalDate inicio, LocalDate fin) {
        Campana campana = new Campana();
        campana.setUsuarioId(usuarioId);
        campana.setNombre(nombre);
        campana.setColor("#1F3D33");
        campana.setTipo(TipoCampana.CAMPANA);
        campana.setFechaInicio(inicio);
        campana.setFechaFin(fin);
        return campana;
    }

    @Test
    void buscaLasCampanasDeMananaYNoLasDeHoy() {
        LocalDate hoy = LocalDate.of(2026, 11, 19);
        LocalDate manana = hoy.plusDays(1);
        when(campanas.buscarConFechaClave(manana)).thenReturn(List.of(
                campana("Feria", 7L, manana, manana.plusDays(3)),
                campana("Cierre de mes", 7L, hoy, manana)));

        List<CampanaPorRecordar> recordatorios = consulta.deManana(hoy);

        verify(campanas).buscarConFechaClave(manana);
        assertThat(recordatorios).hasSize(2);
        assertThat(recordatorios.get(0).empiezaManana()).isTrue();
        assertThat(recordatorios.get(0).terminaManana()).isFalse();
        assertThat(recordatorios.get(1).empiezaManana()).isFalse();
        assertThat(recordatorios.get(1).terminaManana()).isTrue();
    }

    @Test
    void unaCampanaDeUnSoloDiaSeAvisaComoInicioYFin() {
        LocalDate manana = LocalDate.of(2026, 11, 20);
        when(campanas.buscarConFechaClave(manana)).thenReturn(List.of(
                campana("Lanzamiento", 9L, manana, manana)));

        CampanaPorRecordar recordatorio = consulta.paraFecha(manana).get(0);

        assertThat(recordatorio.empiezaManana()).isTrue();
        assertThat(recordatorio.terminaManana()).isTrue();
        assertThat(recordatorio.mensaje()).contains("empieza y termina mañana");
    }

    @Test
    void agrupaLasCampanasPorEmpresaParaEnviarUnSoloAviso() {
        LocalDate hoy = LocalDate.of(2026, 11, 19);
        LocalDate manana = hoy.plusDays(1);
        when(campanas.buscarConFechaClave(manana)).thenReturn(List.of(
                campana("Feria", 7L, manana, manana.plusDays(2)),
                campana("Sorteo", 7L, manana, manana.plusDays(1)),
                campana("Aniversario", 8L, manana, manana)));

        var grupos = consulta.agrupadasPorUsuario(hoy);

        assertThat(grupos).containsOnlyKeys(7L, 8L);
        assertThat(grupos.get(7L)).hasSize(2);
        assertThat(grupos.get(8L)).hasSize(1);
    }
}
