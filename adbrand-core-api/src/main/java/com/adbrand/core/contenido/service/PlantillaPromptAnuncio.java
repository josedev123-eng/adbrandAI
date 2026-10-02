package com.adbrand.core.contenido.service;

import com.adbrand.core.contenido.dto.PromptAnuncio;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.entity.Tono;
import org.springframework.stereotype.Component;

// Arma el texto que se le manda a la IA juntando el perfil del negocio (HU 9) y la oferta del momento.
@Component
public class PlantillaPromptAnuncio {

    private static final String SISTEMA = """
            Eres un redactor publicitario para pequeñas empresas del Perú.
            Escribes anuncios cortos para redes sociales, en español sencillo.
            No inventes precios, descuentos, direcciones ni datos que no estén en la información del negocio.
            No prometas curas, resultados de salud ni resultados garantizados.
            Responde solo con el texto del anuncio, sin títulos ni explicaciones.""";

    public PromptAnuncio construir(PerfilNegocioResponse perfil, String oferta, RedSocial red) {
        String usuario = """
                Escribe un anuncio para %s.

                Negocio: %s
                Rubro: %s
                Qué vende: %s
                Público objetivo: %s
                Tono: %s
                Oferta a anunciar: %s

                Reglas:
                - Máximo %d palabras.
                - Empieza con una frase que llame la atención de ese público.
                - Incluye la oferta tal como está escrita, sin cambiar precios ni fechas.
                - Termina invitando a comprar o a visitar el negocio.
                - Agrega %d hashtags al final."""
                .formatted(
                        nombreDeRed(red),
                        perfil.nombreComercial(),
                        perfil.rubro(),
                        perfil.descripcion() == null ? "No indicado" : perfil.descripcion(),
                        perfil.publicoObjetivo(),
                        instruccionDeTono(perfil.tono()),
                        oferta.trim(),
                        red.getMaximoPalabras(),
                        red.getHashtags());
        return new PromptAnuncio(SISTEMA, usuario);
    }

    // Criterio 2 de la HU 10: el anuncio debe respetar el tono elegido en el perfil.
    static String instruccionDeTono(Tono tono) {
        return switch (tono) {
            case CERCANO -> "cercano y cálido, como un vecino que recomienda algo; tutea al cliente";
            case PROFESIONAL -> "profesional y confiable, claro y sin exageraciones; trata de usted al cliente";
            case DIVERTIDO -> "divertido y con energía, con humor ligero y uno o dos emojis";
            case ELEGANTE -> "elegante y sobrio, con palabras cuidadas y sin emojis";
        };
    }

    private static String nombreDeRed(RedSocial red) {
        return switch (red) {
            case INSTAGRAM -> "Instagram";
            case FACEBOOK -> "Facebook";
        };
    }
}
