package com.adbrand.core.reporte.service;

import com.adbrand.core.calendario.entity.Campana;
import com.adbrand.core.calendario.repository.CampanaRepository;
import com.adbrand.core.reporte.dto.InformeCampanasResponse;
import com.adbrand.core.reporte.word.PlantillaInformeWord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

// HU 39: genera el informe de campañas en Word.
// Criterio 2: pide los datos a InformeService (con los filtros de la HU 40), el mismo que usan la pantalla y el PDF.
@Service
public class InformeWordService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InformeService informes;
    private final PlantillaInformeWord plantilla;
    private final CampanaRepository campanas;

    public InformeWordService(InformeService informes, PlantillaInformeWord plantilla, CampanaRepository campanas) {
        this.informes = informes;
        this.plantilla = plantilla;
        this.campanas = campanas;
    }

    public byte[] campanas(Long usuarioId, String desde, String hasta, Long campanaId) {
        // InformeService ya revisó las fechas y que la campaña sea de la empresa.
        InformeCampanasResponse informe = informes.campanas(usuarioId, desde, hasta, campanaId);
        return plantilla.generar(informe, LocalDateTime.now(), descripcionFiltro(usuarioId, desde, hasta, campanaId));
    }

    // "Periodo: 01/11/2026 al 30/11/2026 · Campaña: Navidad 2026". Sin filtros devuelve null.
    private String descripcionFiltro(Long usuarioId, String desde, String hasta, Long campanaId) {
        List<String> partes = new ArrayList<>();
        boolean conDesde = desde != null && !desde.isBlank();
        boolean conHasta = hasta != null && !hasta.isBlank();
        if (conDesde && conHasta) {
            partes.add("Periodo: " + dia(desde) + " al " + dia(hasta));
        } else if (conDesde) {
            partes.add("Periodo: desde el " + dia(desde));
        } else if (conHasta) {
            partes.add("Periodo: hasta el " + dia(hasta));
        }
        if (campanaId != null) {
            campanas.findByIdAndUsuarioId(campanaId, usuarioId)
                    .map(Campana::getNombre)
                    .ifPresent(nombre -> partes.add("Campaña: " + nombre));
        }
        return partes.isEmpty() ? null : String.join(" · ", partes);
    }

    private static String dia(String texto) {
        return LocalDate.parse(texto.trim()).format(DIA);
    }

    // "informe-campanas-2026-11-10.docx": el nombre con el que se guarda el archivo.
    public static String nombreArchivo(LocalDateTime fecha) {
        return "informe-campanas-" + fecha.toLocalDate() + ".docx";
    }
}
