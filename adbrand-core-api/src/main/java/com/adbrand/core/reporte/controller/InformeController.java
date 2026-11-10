package com.adbrand.core.reporte.controller;

import com.adbrand.core.reporte.dto.InformeCampanasResponse;
import com.adbrand.core.reporte.service.InformePdfService;
import com.adbrand.core.reporte.service.InformeService;
import com.adbrand.core.reporte.service.InformeWordService;
import java.time.LocalDateTime;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Controller: solo recibe la petición y llama al service. Sin lógica.
// HU 37: no hay id en la URL; el informe es de la empresa que inició sesión ("usuarioId" de FiltroJwt).
@RestController
@RequestMapping("/api/reportes")
public class InformeController {

    // El tipo de un archivo .docx, para que el navegador lo abra con Word.
    private static final MediaType DOCX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final InformeService servicio;
    private final InformePdfService pdf;
    private final InformeWordService word;

    public InformeController(InformeService servicio, InformePdfService pdf, InformeWordService word) {
        this.servicio = servicio;
        this.pdf = pdf;
        this.word = word;
    }

    // HU 40: desde, hasta (AAAA-MM-DD) y campanaId son opcionales; sin ellos sale el informe completo.
    @GetMapping("/campanas")
    public InformeCampanasResponse campanas(@RequestAttribute("usuarioId") Long usuarioId,
                                            @RequestParam(required = false) String desde,
                                            @RequestParam(required = false) String hasta,
                                            @RequestParam(required = false) Long campanaId) {
        return servicio.campanas(usuarioId, desde, hasta, campanaId);
    }

    // HU 38: el mismo informe como archivo PDF para descargar. HU 40: con los mismos filtros.
    @GetMapping("/campanas/pdf")
    public ResponseEntity<byte[]> campanasPdf(@RequestAttribute("usuarioId") Long usuarioId,
                                              @RequestParam(required = false) String desde,
                                              @RequestParam(required = false) String hasta,
                                              @RequestParam(required = false) Long campanaId) {
        byte[] archivo = pdf.campanas(usuarioId, desde, hasta, campanaId);
        ContentDisposition descarga = ContentDisposition.attachment()
                .filename(InformePdfService.nombreArchivo(LocalDateTime.now()))
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, descarga.toString())
                .body(archivo);
    }

    // HU 39: el mismo informe como Word (.docx) editable, con los mismos filtros de la HU 40.
    @GetMapping("/campanas/docx")
    public ResponseEntity<byte[]> campanasWord(@RequestAttribute("usuarioId") Long usuarioId,
                                               @RequestParam(required = false) String desde,
                                               @RequestParam(required = false) String hasta,
                                               @RequestParam(required = false) Long campanaId) {
        byte[] archivo = word.campanas(usuarioId, desde, hasta, campanaId);
        ContentDisposition descarga = ContentDisposition.attachment()
                .filename(InformeWordService.nombreArchivo(LocalDateTime.now()))
                .build();
        return ResponseEntity.ok()
                .contentType(DOCX)
                .header(HttpHeaders.CONTENT_DISPOSITION, descarga.toString())
                .body(archivo);
    }
}
