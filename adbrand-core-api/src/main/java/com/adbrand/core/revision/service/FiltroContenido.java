package com.adbrand.core.revision.service;

import com.adbrand.core.revision.dto.ResultadoRevision;
import com.adbrand.core.revision.entity.ReglaRevision;
import com.adbrand.core.revision.repository.ReglaRevisionRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

// HU 13, tarea 2: revisa el texto generado contra las reglas activas de la base de datos.
@Service
public class FiltroContenido {

    private final ReglaRevisionRepository reglas;

    public FiltroContenido(ReglaRevisionRepository reglas) {
        this.reglas = reglas;
    }

    public ResultadoRevision revisar(String texto) {
        String textoNormalizado = normalizar(texto);
        List<String> motivos = new ArrayList<>();

        for (ReglaRevision regla : reglas.findByActivaTrue()) {
            if (contienePalabra(textoNormalizado, normalizar(regla.getTermino()))) {
                motivos.add("\"" + regla.getTermino() + "\": " + regla.getMotivo());
            }
        }

        return motivos.isEmpty() ? ResultadoRevision.sinObservaciones() : new ResultadoRevision(false, motivos);
    }

    // Busca el término como palabra completa: "apuestas" no debe saltar dentro de otra palabra.
    static boolean contienePalabra(String texto, String termino) {
        String patron = "(?<![\\p{L}\\p{N}])" + Pattern.quote(termino) + "(?![\\p{L}\\p{N}])";
        return Pattern.compile(patron).matcher(texto).find();
    }

    // Minúsculas y sin tildes, para que "IMBÉCIL" e "imbecil" cuenten igual que "imbécil".
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }
}
