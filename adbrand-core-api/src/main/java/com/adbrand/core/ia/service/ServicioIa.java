package com.adbrand.core.ia.service;

import com.adbrand.core.config.IaProperties;
import com.adbrand.core.ia.client.ClienteIa;
import com.adbrand.core.ia.dto.RespuestaChatIa;
import com.adbrand.core.ia.entity.OrigenConsumo;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

// Único punto de entrada a la IA para todo el proyecto (ver CONTEXTO.md).
// Con AI_MOCK=true devuelve un texto de prueba para poder trabajar fuera de Tecsup.
@Service
public class ServicioIa {

    private static final Logger log = LoggerFactory.getLogger(ServicioIa.class);

    // Qwen3 a veces escribe su "razonamiento" entre <think> y </think> antes de la respuesta.
    // Ese texto no es el anuncio, así que se quita.
    private static final Pattern BLOQUE_THINK = Pattern.compile("(?s)<think>.*?</think>");
    private static final String FIN_THINK = "</think>";

    private final ClienteIa cliente;
    private final IaProperties ia;
    private final RegistroConsumoIa registroConsumo;

    public ServicioIa(ClienteIa cliente, IaProperties ia, RegistroConsumoIa registroConsumo) {
        this.cliente = cliente;
        this.ia = ia;
        this.registroConsumo = registroConsumo;
    }

    public boolean esSimulado() {
        return ia.mock();
    }

    public String generarTexto(String sistema, String usuario) {
        if (ia.mock()) {
            return respuestaSimulada(usuario);
        }
        return limpiar(llamar(() -> cliente.completar(sistema, usuario)));
    }

    // HU 17: genera igual que generarTexto y además suma los tokens usados a la empresa que lo pidió.
    public String generarTexto(String sistema, String usuario, Long usuarioId, OrigenConsumo origen) {
        if (ia.mock()) {
            String texto = respuestaSimulada(usuario);
            registrarConsumo(usuarioId, origen, sistema + usuario, texto, null);
            return texto;
        }
        RespuestaChatIa respuesta = llamar(() -> cliente.completarConUso(sistema, usuario));
        String texto = limpiar(respuesta == null ? null : respuesta.textoGenerado());
        registrarConsumo(usuarioId, origen, sistema + usuario, texto, respuesta.usage());
        return texto;
    }

    private <T> T llamar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (RestClientException ex) {
            log.warn("Falló la llamada al servidor de IA: {}", ex.getMessage());
            throw new IaNoDisponibleException("No se pudo conectar con la IA", ex);
        }
    }

    private String limpiar(String respuesta) {
        String texto = quitarRazonamiento(respuesta);
        if (texto == null || texto.isBlank()) {
            throw new IaNoDisponibleException("La IA respondió sin texto", null);
        }
        return texto.trim();
    }

    // Si no se puede guardar el consumo, solo se avisa en el log: el usuario igual recibe su texto.
    private void registrarConsumo(Long usuarioId, OrigenConsumo origen, String enviado, String recibido,
            RespuestaChatIa.Uso uso) {
        try {
            registroConsumo.registrar(usuarioId, origen, enviado, recibido, uso);
        } catch (RuntimeException ex) {
            log.warn("No se pudo registrar el consumo de tokens del usuario {}: {}", usuarioId, ex.getMessage());
        }
    }

    // Quita los bloques <think>...</think>. Si solo llegó el cierre (el servidor cortó la apertura),
    // se queda con lo que viene después de </think>.
    private String quitarRazonamiento(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = BLOQUE_THINK.matcher(texto).replaceAll("");
        int fin = limpio.lastIndexOf(FIN_THINK);
        if (fin >= 0) {
            limpio = limpio.substring(fin + FIN_THINK.length());
        }
        return limpio;
    }

    // Arma un anuncio de ejemplo usando la oferta que vino en el prompt.
    private String respuestaSimulada(String usuario) {
        String oferta = usuario.lines()
                .filter(linea -> linea.startsWith("Oferta a anunciar: "))
                .map(linea -> linea.substring("Oferta a anunciar: ".length()))
                .findFirst()
                .orElse("nuestra promoción de hoy");
        return """
                ¡Esto no te lo puedes perder! %s.
                Ven a visitarnos y aprovecha antes de que se acabe.
                #Promo #ComproLocal #Peru""".formatted(oferta);
    }
}
