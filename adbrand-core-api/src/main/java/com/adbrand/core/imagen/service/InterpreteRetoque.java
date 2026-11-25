package com.adbrand.core.imagen.service;

import com.adbrand.core.ia.entity.OrigenConsumo;
import com.adbrand.core.ia.service.ServicioIa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Lo que el dueño escribe para retocar ("pon el fondo más claro", "que se vea navideño") no se manda tal cual a la IA
// de imágenes: esa IA a veces lo dibujaba como letras dentro de la foto. Primero la IA de texto lo interpreta y lo
// convierte en una instrucción visual corta en inglés (el idioma que mejor entiende la IA de imágenes).
// Si la IA de texto no responde o está en modo de prueba, se usa lo que escribió el dueño.
@Service
public class InterpreteRetoque {

    private static final Logger log = LoggerFactory.getLogger(InterpreteRetoque.class);

    static final String SISTEMA = """
            Eres un director de arte. Recibes un pedido en español de un dueño de negocio para cambiar una imagen.
            Conviértelo en UNA instrucción visual concreta en inglés, de máximo 30 palabras, para una IA que edita imágenes.
            Describe solo cambios visuales (colores, luz, fondo, objetos, ambiente, encuadre).
            Si el pedido trae frases, precios o palabras para el anuncio, NO las incluyas: la imagen nunca lleva texto.
            Responde solo con la instrucción, sin comillas ni explicaciones.""";

    private final ServicioIa ia;

    public InterpreteRetoque(ServicioIa ia) {
        this.ia = ia;
    }

    public String interpretar(String pedido, Long usuarioId, OrigenConsumo origen) {
        String limpio = pedido == null ? "" : pedido.trim();
        if (limpio.isEmpty() || ia.esSimulado()) {
            return limpio;
        }
        try {
            String respuesta = ia.generarTexto(SISTEMA, limpio, usuarioId, origen);
            String instruccion = respuesta == null ? "" : respuesta.trim().replaceAll("^[\"'“”]+|[\"'“”]+$", "");
            return instruccion.isBlank() || instruccion.length() > 400 ? limpio : instruccion;
        } catch (RuntimeException ex) {
            log.warn("No se pudo interpretar el retoque, se usa el pedido tal cual: {}", ex.getMessage());
            return limpio;
        }
    }
}
