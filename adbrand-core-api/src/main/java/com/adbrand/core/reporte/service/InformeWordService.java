package com.adbrand.core.reporte.service;

import com.adbrand.core.reporte.dto.InformeCampanasResponse;
import com.adbrand.core.reporte.word.PlantillaInformeWord;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

// HU 39: genera el informe de campañas en Word.
// Criterio 2: pide los datos a InformeService (con los filtros de la HU 40), el mismo que usan la pantalla y el PDF.
@Service
public class InformeWordService {

    private final InformeService informes;
    private final PlantillaInformeWord plantilla;

    public InformeWordService(InformeService informes, PlantillaInformeWord plantilla) {
        this.informes = informes;
        this.plantilla = plantilla;
    }

    public byte[] campanas(Long usuarioId, String desde, String hasta, Long campanaId) {
        InformeCampanasResponse informe = informes.campanas(usuarioId, desde, hasta, campanaId);
        return plantilla.generar(informe, LocalDateTime.now());
    }

    // "informe-campanas-2026-11-10.docx": el nombre con el que se guarda el archivo.
    public static String nombreArchivo(LocalDateTime fecha) {
        return "informe-campanas-" + fecha.toLocalDate() + ".docx";
    }
}
