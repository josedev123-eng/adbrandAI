package com.adbrand.core.reporte.service;

import com.adbrand.core.reporte.dto.InformeCampanasResponse;
import com.adbrand.core.reporte.pdf.PlantillaInformePdf;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

// HU 38: genera el informe de campañas en PDF.
// Criterio 2: pide los datos a InformeService, el mismo que usa la pantalla de informes, así ambos coinciden.
@Service
public class InformePdfService {

    private final InformeService informes;
    private final PlantillaInformePdf plantilla;

    public InformePdfService(InformeService informes, PlantillaInformePdf plantilla) {
        this.informes = informes;
        this.plantilla = plantilla;
    }

    public byte[] campanas(Long usuarioId) {
        InformeCampanasResponse informe = informes.campanas(usuarioId);
        return plantilla.generar(informe, LocalDateTime.now());
    }

    // "informe-campanas-2026-11-03.pdf": el nombre con el que se guarda el archivo.
    public static String nombreArchivo(LocalDateTime fecha) {
        return "informe-campanas-" + fecha.toLocalDate() + ".pdf";
    }
}
