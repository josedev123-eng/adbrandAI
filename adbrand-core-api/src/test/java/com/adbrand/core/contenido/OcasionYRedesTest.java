package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;

import com.adbrand.core.calendario.service.FechasPeruanas;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.service.PlantillaPromptAnuncio;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.entity.Tono;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

// Ajustes de la revisión de la web: el anuncio creado desde una fecha del calendario se ambienta en esa festividad,
// WhatsApp (Estados) es una red más, y el calendario trae Halloween y el Día de los Muertos.
class OcasionYRedesTest {

    private static PerfilNegocioResponse perfil() {
        return new PerfilNegocioResponse(1L, "Cafetería Don Pepe", "Cafetería", "Vecinos", Tono.CERCANO, null, null);
    }

    @Test
    void conOcasionElPromptPideAmbientarElTextoYLaImagen() {
        String usuario = new PlantillaPromptAnuncio()
                .construir(perfil(), "2x1 en cupcakes", RedSocial.INSTAGRAM, null, "Halloween").usuario();

        assertThat(usuario).contains("Ocasión: Halloween").contains("Ambienta el anuncio en Halloween")
                .contains("escena de la imagen");
    }

    @Test
    void sinOcasionElPromptQuedaComoAntes() {
        PlantillaPromptAnuncio plantilla = new PlantillaPromptAnuncio();

        assertThat(plantilla.construir(perfil(), "Promo", RedSocial.FACEBOOK, null, " ").usuario())
                .isEqualTo(plantilla.construir(perfil(), "Promo", RedSocial.FACEBOOK).usuario())
                .doesNotContain("Ocasión");
    }

    @Test
    void whatsappEsUnaRedConTextoCorto() {
        String usuario = new PlantillaPromptAnuncio().construir(perfil(), "Promo", RedSocial.WHATSAPP).usuario();

        assertThat(usuario).contains("los Estados de WhatsApp").contains("Máximo 45 palabras");
    }

    @Test
    void elCalendarioTraeHalloweenYElDiaDeLosMuertos() {
        var fechas = FechasPeruanas.delAnio(2026);

        assertThat(fechas).anyMatch(f -> f.nombre().equals("Halloween") && f.fecha().equals(LocalDate.of(2026, 10, 31)));
        assertThat(fechas).anyMatch(f -> f.nombre().equals("Día de los Muertos") && f.fecha().equals(LocalDate.of(2026, 11, 2)));
        assertThat(fechas.stream().filter(f -> f.nombre().equals("Halloween")).findFirst().orElseThrow()
                .ideaPara("Panadería")).contains("calabaza");
    }

    @Test
    void laImagenSeAmbientaEnLaFechaEspecial() {
        assertThat(com.adbrand.core.contenido.service.OpcionesAnuncioService.ambienteDeOcasion("Black Friday"))
                .contains("Black Friday").contains("no text");
        assertThat(com.adbrand.core.contenido.service.OpcionesAnuncioService.ambienteDeOcasion("Navidad"))
                .contains("Christmas");
        assertThat(com.adbrand.core.contenido.service.OpcionesAnuncioService.ambienteDeOcasion("Día de la Primavera"))
                .contains("Día de la Primavera in Peru");
        assertThat(com.adbrand.core.contenido.service.OpcionesAnuncioService.ambienteDeOcasion(null)).isEmpty();
    }
}

