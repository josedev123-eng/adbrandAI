package com.adbrand.core.contenido;

import static org.assertj.core.api.Assertions.assertThat;

import com.adbrand.core.contenido.dto.PromptAnuncio;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.service.PlantillaPromptAnuncio;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.entity.Tono;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// Prueba unitaria: no necesita base de datos ni servidor de IA.
class PlantillaPromptAnuncioTest {

    private final PlantillaPromptAnuncio plantilla = new PlantillaPromptAnuncio();

    private PerfilNegocioResponse perfil(Tono tono, String descripcion) {
        return new PerfilNegocioResponse(1L, "Panadería Doña Rosa", "Panadería y pastelería",
                "Familias del barrio", tono, descripcion, null);
    }

    @Test
    void incluyeLosDatosDelNegocioYLaOferta() {
        PromptAnuncio prompt = plantilla.construir(
                perfil(Tono.CERCANO, "Pan artesanal"), "  2 panetones por S/ 35  ", RedSocial.INSTAGRAM);

        assertThat(prompt.usuario())
                .contains("Instagram")
                .contains("Panadería Doña Rosa")
                .contains("Panadería y pastelería")
                .contains("Pan artesanal")
                .contains("Familias del barrio")
                .contains("Oferta a anunciar: 2 panetones por S/ 35\n");
        assertThat(prompt.sistema()).contains("No inventes precios");
    }

    @Test
    void sinDescripcionIndicaQueNoHayDato() {
        PromptAnuncio prompt = plantilla.construir(perfil(Tono.CERCANO, null), "Promo", RedSocial.FACEBOOK);

        assertThat(prompt.usuario()).contains("Qué vende: No indicado").doesNotContain("null");
    }

    @Test
    void cadaRedTieneSuLargoYSusHashtags() {
        String instagram = plantilla.construir(perfil(Tono.CERCANO, null), "Promo", RedSocial.INSTAGRAM).usuario();
        String facebook = plantilla.construir(perfil(Tono.CERCANO, null), "Promo", RedSocial.FACEBOOK).usuario();

        assertThat(instagram).contains("Máximo 80 palabras").contains("Agrega 4 hashtags");
        assertThat(facebook).contains("Facebook").contains("Máximo 120 palabras").contains("Agrega 2 hashtags");
    }

    @ParameterizedTest
    @EnumSource(Tono.class)
    void cadaTonoTieneSuPropiaInstruccion(Tono tono) {
        String usuario = plantilla.construir(perfil(tono, null), "Promo", RedSocial.INSTAGRAM).usuario();

        assertThat(usuario).contains("Tono: " + tono.name().toLowerCase().substring(0, 4));
    }
}
