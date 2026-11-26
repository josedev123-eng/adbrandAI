package com.adbrand.core.ia.service;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// Ayudas para leer el JSON que devuelven las IA de texto, que no siempre viene "limpio".
public final class JsonDeIa {

    private static final ObjectMapper LECTOR = new ObjectMapper();

    private JsonDeIa() {
    }

    // Algunas IA (por ejemplo Gemini) envuelven el JSON en un bloque ```json ... ```; se quita antes de leerlo.
    public static String sinBloqueMarkdown(String respuesta) {
        String texto = respuesta == null ? "" : respuesta.strip();
        if (texto.startsWith("```")) {
            texto = texto.replaceFirst("^```[a-zA-Z]*\\s*", "");
            texto = texto.replaceFirst("\\s*```$", "");
        }
        return texto;
    }

    // Un campo de texto de la raíz del JSON ({"usoFoto": "ESCENA", ...}); null si no vino o no es JSON.
    public static String campo(String respuesta, String clave) {
        try {
            JsonNode valor = LECTOR.readTree(sinBloqueMarkdown(respuesta)).path(clave);
            return valor.isTextual() && !valor.asText().isBlank() ? valor.asText().strip() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    // Un elemento de la lista: su texto y, si vino, un dato extra (por ejemplo la escena de la imagen).
    // HU 67: "titular" es el titular corto que la IA escribe aparte (null si no vino).
    public record TextoConExtra(String texto, String extra, String titular) {
        public TextoConExtra(String texto, String extra) {
            this(texto, extra, null);
        }
    }

    // Lee una lista cuyos elementos pueden ser textos ("…") u objetos ({"texto": "…", "escena": "…"}).
    // Si no es JSON válido, devuelve una lista vacía.
    public static List<TextoConExtra> listaDeObjetos(String respuesta, String clave, String campoTexto,
                                                     String campoExtra) {
        List<TextoConExtra> elementos = new ArrayList<>();
        try {
            JsonNode raiz = LECTOR.readTree(sinBloqueMarkdown(respuesta));
            JsonNode lista = raiz.isArray() ? raiz : raiz.get(clave);
            if (lista == null || !lista.isArray()) {
                return elementos;
            }
            for (JsonNode elemento : lista) {
                String texto = elemento.isObject() ? elemento.path(campoTexto).asText("") : elemento.asText("");
                String extra = elemento.isObject() ? elemento.path(campoExtra).asText("") : "";
                String titular = elemento.isObject() ? elemento.path("titular").asText("") : "";
                if (!texto.isBlank()) {
                    elementos.add(new TextoConExtra(texto.strip(), extra.isBlank() ? null : extra.strip(),
                            titular.isBlank() ? null : titular.strip()));
                }
            }
        } catch (RuntimeException ex) {
            // No era JSON: quien llama decide qué hacer.
        }
        return elementos;
    }

    // Lee una lista de textos: {"opciones": [...]} o directamente [...]. Si no es JSON válido, devuelve una lista vacía.
    public static List<String> listaDeTextos(String respuesta, String clave) {
        List<String> textos = new ArrayList<>();
        try {
            JsonNode raiz = LECTOR.readTree(sinBloqueMarkdown(respuesta));
            JsonNode lista = raiz.isArray() ? raiz : raiz.get(clave);
            if (lista == null || !lista.isArray()) {
                return textos;
            }
            for (JsonNode elemento : lista) {
                String texto = elemento.isTextual() ? elemento.asText().strip() : "";
                if (!texto.isEmpty()) {
                    textos.add(texto);
                }
            }
        } catch (RuntimeException ex) {
            // No era JSON: quien llama decide qué hacer (por ejemplo, pedir los textos de uno en uno).
        }
        return textos;
    }
}
