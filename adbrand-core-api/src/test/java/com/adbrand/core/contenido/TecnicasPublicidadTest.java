package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.adbrand.core.contenido.dto.OpcionAnuncioResponse;
import com.adbrand.core.contenido.dto.PromptAnuncio;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.service.DisenoAnuncio;
import com.adbrand.core.contenido.service.PlantillaPromptAnuncio;
import com.adbrand.core.contenido.service.VideoAnuncioService;
import com.adbrand.core.ia.service.JsonDeIa;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.entity.Tono;
import org.junit.jupiter.api.Test;

// HU 67: anuncios con técnicas de publicidad. Titular aparte de 3 a 6 palabras, beneficio antes que característica,
// un ángulo por opción, urgencia y prueba social solo si son reales, una sola idea con un llamado claro.
class TecnicasPublicidadTest {

    private final PlantillaPromptAnuncio plantilla = new PlantillaPromptAnuncio();

    private PromptAnuncio opciones() {
        PerfilNegocioResponse perfil = new PerfilNegocioResponse(1L, "Café Aroma", "Cafetería", "Oficinistas",
                Tono.CERCANO, "Café de altura", null);
        return plantilla.construirOpciones(perfil, "20% en todos los cafés", RedSocial.INSTAGRAM, null, 4);
    }

    @Test
    void cadaOpcionTieneSuAnguloYPideUnTitularAparte() {
        String sistema = opciones().sistema();

        assertThat(sistema)
                .contains("Versión 1 → PRECIO")
                .contains("Versión 2 → ANTOJO")
                .contains("Versión 3 → CONFIANZA")
                .contains("Versión 4 → URGENCIA")
                .contains("\"titular\": un titular corto de 3 a 6 palabras")
                .contains("{\"opciones\": [{\"titular\"");
        assertThat(PlantillaPromptAnuncio.ANGULOS).containsExactly("Precio", "Antojo", "Confianza", "Urgencia");
    }

    @Test
    void lasReglasPidenBeneficioUnaIdeaFrasesCortasYNadaInventado() {
        String usuario = opciones().usuario();

        assertThat(usuario)
                .contains("Primero el beneficio para el cliente")
                .contains("Una sola idea por anuncio y un solo llamado a la acción")
                .contains("Frases cortas")
                .contains("Urgencia solo si es real")
                .contains("Nunca inventes fechas, \"últimas unidades\"")
                .contains("Prueba social (clientes, años en el barrio, premios) solo si aparece en \"Qué vende\"");
        // La guía de urgencia tampoco deja inventar plazos.
        assertThat(opciones().sistema()).contains("sin inventar fechas ni \"últimas unidades\"");
    }

    @Test
    void elTitularDeLaIaQuedaEnTresASeisPalabrasLimpias() {
        assertThat(VideoAnuncioService.titularCorto("“Tu café, más rico hoy” ☕ #CafeAroma"))
                .isEqualTo("Tu café, más rico hoy");
        assertThat(VideoAnuncioService.titularCorto("Recarga tu jornada con el mejor café del barrio"))
                .isEqualTo("Recarga tu jornada con el mejor");
        assertThat(VideoAnuncioService.titularCorto("Café")).isNull();
        assertThat(VideoAnuncioService.titularCorto(null)).isNull();
    }

    @Test
    void laImagenElAficheYElVideoUsanElTitularDeLaIaYSiNoHayLaPrimeraFrase() {
        Contenido conTitular = new Contenido();
        conTitular.setOferta("20% en cafés");
        conTitular.setTexto("¡Empieza tu día con energía! Ven por tu café.");
        conTitular.setTitular("Tu café, más rico hoy");
        Contenido anterior = new Contenido();
        anterior.setOferta("20% en cafés");
        anterior.setTexto("¡Empieza tu día con energía! Ven por tu café.");

        assertThat(VideoAnuncioService.titularDe(conTitular)).isEqualTo("Tu café, más rico hoy");
        assertThat(VideoAnuncioService.titularDe(anterior)).isEqualTo("¡Empieza tu día con energía!");
        assertThat(DisenoAnuncio.textosDe("Tu café, más rico hoy", "20% en cafés", "Texto.", false).titular())
                .isEqualTo("Tu café, más rico hoy");
    }

    @Test
    void elJsonDeLaIaTraeElTitularDeCadaOpcion() {
        var lista = JsonDeIa.listaDeObjetos(
                "{\"opciones\": [{\"titular\": \"Tu café, más rico hoy\", \"texto\": \"Uno\", \"escena\": \"a cup\"}]}",
                "opciones", "texto", "escena");

        assertThat(lista.get(0).titular()).isEqualTo("Tu café, más rico hoy");
        assertThat(lista.get(0).extra()).isEqualTo("a cup");
    }

    @Test
    void laOpcionMuestraSuAngulo() {
        var opcion = new OpcionAnuncioResponse(1L, 2, "Ilustración", "Texto", "data:,", "cloudflare", false, false, false);
        assertThat(opcion.angulo()).isEqualTo("Antojo");
    }
}
