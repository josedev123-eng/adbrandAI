package com.adbrand.core.revision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.adbrand.core.revision.dto.ResultadoRevision;
import com.adbrand.core.revision.entity.CategoriaRegla;
import com.adbrand.core.revision.entity.ReglaRevision;
import com.adbrand.core.revision.repository.ReglaRevisionRepository;
import com.adbrand.core.revision.service.FiltroContenido;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// HU 13, tarea 4: casos permitidos y casos observados del filtro automático.
// Las reglas son las mismas que carga la V3, pero sin base de datos (Mockito las devuelve).
class FiltroContenidoTest {

    private FiltroContenido filtro;

    @BeforeEach
    void preparar() {
        ReglaRevisionRepository repositorio = mock(ReglaRevisionRepository.class);
        when(repositorio.findByActivaTrue()).thenReturn(List.of(
                new ReglaRevision("imbécil", CategoriaRegla.LENGUAJE_OFENSIVO, "Contiene un insulto."),
                new ReglaRevision("cura el cáncer", CategoriaRegla.PUBLICIDAD_ENGANOSA, "Promete una cura médica."),
                new ReglaRevision("100% garantizado", CategoriaRegla.PUBLICIDAD_ENGANOSA, "Promete un resultado garantizado."),
                new ReglaRevision("cigarrillos", CategoriaRegla.PRODUCTO_PROHIBIDO, "Publicidad de tabaco."),
                new ReglaRevision("apuestas", CategoriaRegla.PRODUCTO_PROHIBIDO, "Publicidad de apuestas.")));
        filtro = new FiltroContenido(repositorio);
    }

    // Criterio 1: el contenido normal pasa la revisión y se puede publicar.
    @ParameterizedTest
    @ValueSource(strings = {
            "¡2 panetones por S/ 35 hasta el 24 de diciembre! Ven a Doña Rosa.",
            "Martes de pan con chicharrón a S/ 6. #ComproLocal",
            "Tortas por encargo con 10% de descuento, calidad garantizada.",
            "Nueva línea de postres franceses desde S/ 18."
    })
    void casosPermitidos(String texto) {
        ResultadoRevision resultado = filtro.revisar(texto);

        assertThat(resultado.aprobado()).isTrue();
        assertThat(resultado.motivos()).isEmpty();
        assertThat(resultado.motivoTexto()).isNull();
    }

    // Criterio 2: el contenido que incumple queda observado con su motivo.
    @ParameterizedTest
    @ValueSource(strings = {
            "Promo de cigarrillos importados a S/ 10",
            "No seas imbécil, aprovecha la oferta",
            "Este té cura el cáncer",
            "Resultados 100% garantizado en una semana",
            "Gana en nuestras apuestas deportivas"
    })
    void casosObservados(String texto) {
        ResultadoRevision resultado = filtro.revisar(texto);

        assertThat(resultado.aprobado()).isFalse();
        assertThat(resultado.motivos()).hasSize(1);
        assertThat(resultado.motivoTexto()).isNotBlank();
    }

    @Test
    void noLoEnganaConMayusculasNiSinTildes() {
        ResultadoRevision resultado = filtro.revisar("NO SEAS IMBECIL, este jarabe CURA EL CANCER");

        assertThat(resultado.aprobado()).isFalse();
        assertThat(resultado.motivos()).containsExactly(
                "\"imbécil\": Contiene un insulto.",
                "\"cura el cáncer\": Promete una cura médica.");
    }

    @Test
    void soloBuscaPalabrasCompletas() {
        // "apuestas" está dentro de "apuestasdepan", pero no es la palabra completa.
        assertThat(filtro.revisar("Prueba nuestras apuestasdepan").aprobado()).isTrue();
    }

    @Test
    void juntaTodosLosMotivosEnUnaLinea() {
        ResultadoRevision resultado = filtro.revisar("Cigarrillos y apuestas, 100% garantizado");

        assertThat(resultado.motivos()).hasSize(3);
        assertThat(resultado.motivoTexto())
                .contains("Publicidad de tabaco.")
                .contains("Publicidad de apuestas.")
                .contains("Promete un resultado garantizado.");
    }

    @Test
    void textoVacioPasaSinObservaciones() {
        assertThat(filtro.revisar(null).aprobado()).isTrue();
        assertThat(filtro.revisar("").aprobado()).isTrue();
    }
}

