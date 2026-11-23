package com.adbrand.core.brandkit.controller;

import com.adbrand.core.brandkit.dto.IdentidadVisualDtos.ElegirEsloganRequest;
import com.adbrand.core.brandkit.dto.IdentidadVisualDtos.GuardarLogoRequest;
import com.adbrand.core.brandkit.dto.IdentidadVisualDtos.OpcionesLogoResponse;
import com.adbrand.core.brandkit.dto.KitMarcaGeneradoResponse;
import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.service.IdentidadVisualService;
import com.adbrand.core.brandkit.service.ManualMarcaPdf;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Identidad de marca (V70): logo (subido o de la IA), eslogan elegido y manual de marca en PDF.
// Siempre sobre un kit de la empresa que inició sesión ("usuarioId" de FiltroJwt).
@RestController
@RequestMapping("/api/kit-marca/{id}")
public class IdentidadVisualController {

    private final IdentidadVisualService identidad;
    private final ManualMarcaPdf manual;

    public IdentidadVisualController(IdentidadVisualService identidad, ManualMarcaPdf manual) {
        this.identidad = identidad;
        this.manual = manual;
    }

    // 3 propuestas de logo hechas por la IA (no se guardan hasta elegir una).
    @PostMapping("/logo/opciones")
    public OpcionesLogoResponse opcionesLogo(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id) {
        return identidad.opcionesLogo(usuarioId, id);
    }

    // Guarda el logo: el que ya tenía la empresa (SUBIDO) o el elegido de la IA (IA). 400 si no es PNG/JPG o pesa más de 2 MB.
    @PutMapping("/logo-imagen")
    public KitMarcaGeneradoResponse guardarLogo(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id,
                                                @Valid @RequestBody GuardarLogoRequest datos) {
        return identidad.guardarLogo(usuarioId, id, datos.imagen(), datos.origen());
    }

    @PutMapping("/eslogan")
    public KitMarcaGeneradoResponse elegirEslogan(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id,
                                                  @Valid @RequestBody ElegirEsloganRequest datos) {
        return identidad.elegirEslogan(usuarioId, id, datos.eslogan());
    }

    // Manual de marca en PDF, para descargar o llevar a la imprenta.
    @GetMapping("/manual")
    public ResponseEntity<byte[]> manualPdf(@RequestAttribute("usuarioId") Long usuarioId, @PathVariable Long id) {
        KitMarca kit = identidad.buscar(usuarioId, id);
        String nombre = "manual-de-marca-" + kit.getNombreNegocio().toLowerCase()
                .replaceAll("[^a-z0-9áéíóúñ]+", "-").replaceAll("(^-|-$)", "") + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre, StandardCharsets.UTF_8).build().toString())
                .body(manual.generar(kit));
    }
}
