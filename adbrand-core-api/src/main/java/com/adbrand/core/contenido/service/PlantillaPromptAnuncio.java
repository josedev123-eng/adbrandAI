package com.adbrand.core.contenido.service;

import com.adbrand.core.contenido.dto.IdentidadMarca;
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

    // Para "Generar 4 opciones": las mismas reglas, pero pide varias versiones distintas en un JSON.
    private static final String SISTEMA_OPCIONES = """
            Eres un redactor publicitario para pequeñas empresas del Perú.
            Escribes anuncios cortos para redes sociales, en español sencillo.
            No inventes precios, descuentos, direcciones ni datos que no estén en la información del negocio.
            No prometas curas, resultados de salud ni resultados garantizados.
            Esta vez escribe %d versiones DISTINTAS del mismo anuncio, cada una con un ángulo distinto:
            %s
            Para cada versión escribe también:
            - "titular": un titular corto de 3 a 6 palabras, con gancho, que resuma la versión. No repitas la
              primera frase del texto ni pongas hashtags, emojis ni comillas.
            - "escena": EN INGLÉS, una escena corta para la imagen del anuncio: qué producto se ve y dónde
              (por ejemplo "a golden sweet bread loaf on a rustic wooden table"). La escena no debe mencionar
              textos, letras, precios, números, carteles ni logos.
            Responde SOLO con un JSON válido, sin markdown ni explicaciones, con esta forma exacta:
            {"opciones": [{"titular": "titular corto", "texto": "versión 1", "escena": "scene in English"}, ...]}""";

    // HU 67: un ángulo por opción, en este orden (la opción 1 habla del precio, la 2 del antojo…).
    public static final java.util.List<String> ANGULOS = java.util.List.of("Precio", "Antojo", "Confianza", "Urgencia");
    private static final java.util.List<String> GUIA_ANGULOS = java.util.List.of(
            "PRECIO: el ahorro o lo conveniente que es, con la cifra exacta de la oferta.",
            "ANTOJO: los sentidos y la emoción (cómo se ve, huele, sabe o se siente).",
            "CONFIANZA: la calidad, lo hecho en el barrio, el buen trato.",
            "URGENCIA: por qué venir hoy. Solo menciona plazos o cantidades si la oferta los trae;"
                    + " si no, invita a no quedarse sin probarlo, sin inventar fechas ni \"últimas unidades\".");

    static String angulos(int cantidad) {
        StringBuilder lista = new StringBuilder();
        for (int i = 0; i < cantidad; i++) {
            lista.append(i == 0 ? "" : "\n").append("Versión ").append(i + 1).append(" → ")
                    .append(GUIA_ANGULOS.get(i % GUIA_ANGULOS.size()));
        }
        return lista.toString();
    }

    public PromptAnuncio construirOpciones(PerfilNegocioResponse perfil, String oferta, RedSocial red,
                                           IdentidadMarca identidad, int cantidad) {
        return construirOpciones(perfil, oferta, red, identidad, cantidad, null);
    }

    public PromptAnuncio construirOpciones(PerfilNegocioResponse perfil, String oferta, RedSocial red,
                                           IdentidadMarca identidad, int cantidad, String ocasion) {
        PromptAnuncio uno = construir(perfil, oferta, red, identidad, ocasion);
        return new PromptAnuncio(SISTEMA_OPCIONES.formatted(cantidad, angulos(cantidad)), uno.usuario());
    }

    public PromptAnuncio construir(PerfilNegocioResponse perfil, String oferta, RedSocial red) {
        return construir(perfil, oferta, red, null);
    }

    // HU 31, criterio 2: si el negocio guardó su identidad en el kit, el anuncio sigue esa voz de marca.
    // identidad puede ser null (el negocio aún no tiene kit) y entonces solo manda el tono del perfil.
    public PromptAnuncio construir(PerfilNegocioResponse perfil, String oferta, RedSocial red,
                                   IdentidadMarca identidad) {
        return construir(perfil, oferta, red, identidad, null);
    }

    // "ocasion": la festividad elegida en el calendario (Halloween, Navidad…). El texto y la escena se ambientan en ella.
    public PromptAnuncio construir(PerfilNegocioResponse perfil, String oferta, RedSocial red,
                                   IdentidadMarca identidad, String ocasion) {
        boolean conOcasion = ocasion != null && !ocasion.isBlank();
        boolean conVoz = identidad != null && identidad.tieneVoz();
        String usuario = """
                Escribe un anuncio para %s.

                Negocio: %s
                Rubro: %s
                Qué vende: %s
                Público objetivo: %s
                Tono: %s%s
                Oferta a anunciar: %s%s

                Reglas:
                - Máximo %d palabras.
                - Empieza con una frase corta (menos de 10 palabras) que llame la atención de ese público.
                - Primero el beneficio para el cliente (qué gana, cómo se siente), después cómo es el producto.
                - Una sola idea por anuncio y un solo llamado a la acción, claro, al final
                  (por ejemplo "Pide hoy por WhatsApp" o "Ven a visitarnos").
                - Frases cortas que se lean fácil en el celular.
                - Incluye la oferta tal como está escrita, sin cambiar precios ni fechas.
                - Urgencia solo si es real: menciona plazos ("solo este sábado") o cantidades ("20 unidades")
                  únicamente si están en la oferta. Nunca inventes fechas, "últimas unidades" ni "por tiempo limitado".
                - Prueba social (clientes, años en el barrio, premios) solo si aparece en "Qué vende"; nunca la inventes.
                - Agrega %d hashtags al final.%s%s"""
                .formatted(
                        nombreDeRed(red),
                        perfil.nombreComercial(),
                        perfil.rubro(),
                        perfil.descripcion() == null ? "No indicado" : perfil.descripcion(),
                        perfil.publicoObjetivo(),
                        instruccionDeTono(perfil.tono()),
                        conVoz ? "\nVoz de la marca: " + identidad.vozMarca().trim() : "",
                        oferta.trim(),
                        conOcasion ? "\nOcasión: " + ocasion.trim() : "",
                        red.getMaximoPalabras(),
                        red.getHashtags(),
                        conVoz ? "\n- Escribe con la voz de la marca; si choca con el tono, manda la voz." : "",
                        conOcasion ? "\n- Ambienta el anuncio en " + ocasion.trim() + ": menciónala y usa su ambiente"
                                + " (decoración, colores y símbolos típicos), también en la escena de la imagen." : "");
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
            case WHATSAPP -> "los Estados de WhatsApp";
        };
    }
}
