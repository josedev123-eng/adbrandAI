package com.adbrand.core.suscripcion;

import com.adbrand.core.suscripcion.entity.EstadoSuscripcion;
import com.adbrand.core.suscripcion.entity.Suscripcion;
import com.adbrand.core.suscripcion.repository.SuscripcionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class SuscripcionRepositoryTest {

    @Autowired
    SuscripcionRepository repo;

    @Test
    void findByEstado_filtraCorrectamente() {
        Suscripcion activa = new Suscripcion();
        activa.setEstado(EstadoSuscripcion.ACTIVA);
        activa.setFechaInicio(LocalDate.now());

        Suscripcion vencida = new Suscripcion();
        vencida.setEstado(EstadoSuscripcion.VENCIDA);
        vencida.setFechaInicio(LocalDate.now());

        repo.saveAll(List.of(activa, vencida));

        List<Suscripcion> resultado = repo.findByEstado(EstadoSuscripcion.ACTIVA);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoSuscripcion.ACTIVA);
    }

    @Test
    void findVencidas_detectaAutomaticamente() {
        Suscripcion vigente = new Suscripcion();
        vigente.setEstado(EstadoSuscripcion.ACTIVA);
        vigente.setFechaVencimiento(LocalDate.now().plusDays(10));
        vigente.setFechaInicio(LocalDate.now());

        Suscripcion vencida = new Suscripcion();
        vencida.setEstado(EstadoSuscripcion.ACTIVA);
        vencida.setFechaVencimiento(LocalDate.now().minusDays(5));
        vencida.setFechaInicio(LocalDate.now());

        repo.saveAll(List.of(vigente, vencida));

        List<Suscripcion> vencidas = repo.findVencidas(LocalDate.now());

        assertThat(vencidas).hasSize(1);
        assertThat(vencidas.get(0).getFechaVencimiento()).isBefore(LocalDate.now());
    }

    @Test
    void findPendientesPago_filtraCorrectamente() {
        Suscripcion pendiente = new Suscripcion();
        pendiente.setEstado(EstadoSuscripcion.PENDIENTE_PAGO);

        Suscripcion activa = new Suscripcion();
        activa.setEstado(EstadoSuscripcion.ACTIVA);

        repo.saveAll(List.of(pendiente, activa));

        List<Suscripcion> pendientes = repo.findByEstado(EstadoSuscripcion.PENDIENTE_PAGO);

        assertThat(pendientes).hasSize(1);
        assertThat(pendientes.get(0).getEstado()).isEqualTo(EstadoSuscripcion.PENDIENTE_PAGO);
    }
}