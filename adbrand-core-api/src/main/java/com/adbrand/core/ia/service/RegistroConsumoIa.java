package com.adbrand.core.ia.service;

import com.adbrand.core.config.IaProperties;
import com.adbrand.core.ia.dto.RespuestaChatIa;
import com.adbrand.core.ia.entity.ConsumoToken;
import com.adbrand.core.ia.entity.OrigenConsumo;
import com.adbrand.core.ia.repository.ConsumoTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// HU 17: guarda cuántos tokens gastó una empresa en cada generación con IA.
@Service
public class RegistroConsumoIa {

    // Regla común para estimar: un token son unos 4 caracteres.
    static final int CARACTERES_POR_TOKEN = 4;

    private final ConsumoTokenRepository consumos;
    private final IaProperties ia;

    public RegistroConsumoIa(ConsumoTokenRepository consumos, IaProperties ia) {
        this.consumos = consumos;
        this.ia = ia;
    }

    // Transacción propia: si guardar el consumo falla, no se deshace el anuncio o el kit que se estaba guardando.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConsumoToken registrar(Long usuarioId, OrigenConsumo origen, String enviado, String recibido,
            RespuestaChatIa.Uso uso) {
        ConsumoToken consumo = new ConsumoToken();
        consumo.setUsuarioId(usuarioId);
        consumo.setOrigen(origen);
        consumo.setModelo(ia.model());

        if (uso != null && uso.totalTokens() != null) {
            consumo.setTokensEntrada(valor(uso.promptTokens()));
            consumo.setTokensSalida(valor(uso.completionTokens()));
            consumo.setTokensTotal(uso.totalTokens());
            consumo.setSimulado(false);
        } else {
            // Modo simulado (AI_MOCK=true) o servidor sin usage: se estima para que el panel funcione igual.
            int entrada = estimar(enviado);
            int salida = estimar(recibido);
            consumo.setTokensEntrada(entrada);
            consumo.setTokensSalida(salida);
            consumo.setTokensTotal(entrada + salida);
            consumo.setSimulado(true);
        }
        return consumos.save(consumo);
    }

    private static int estimar(String texto) {
        return texto == null ? 0 : texto.length() / CARACTERES_POR_TOKEN;
    }

    private static int valor(Integer tokens) {
        return tokens == null ? 0 : tokens;
    }
}
