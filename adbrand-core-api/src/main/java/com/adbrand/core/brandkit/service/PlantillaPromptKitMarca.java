package com.adbrand.core.brandkit.service;

import com.adbrand.core.brandkit.dto.GenerarKitMarcaRequest;
import com.adbrand.core.brandkit.dto.KitMarcaPrompt;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import org.springframework.stereotype.Component;

// Arma el prompt que se le manda a la IA para generar el Kit de Marca.
@Component
public class PlantillaPromptKitMarca {

    private static final String SISTEMA = """
            Eres un diseñador de marca experto para pequeñas empresas del Perú.
            Generas Kits de Marca básicos: concepto de logo, tipografías, paleta de colores y voz de marca.
            Respondes SOLO en formato JSON válido, sin texto adicional, sin markdown.
            Usa español sencillo, adaptado al rubro y estilo solicitado.""";

    public KitMarcaPrompt construir(PerfilNegocioResponse perfil, GenerarKitMarcaRequest datos) {
        String usuario = """
                Genera un Kit de Marca básico para:

                Negocio: %s
                Sector: %s
                %s
                %s
                Estilo visual: %s

                Reglas:
                - Responde SOLO con un JSON válido con estas claves exactas:
                  "logo_concepto", "tipografia_primaria", "tipografia_secundaria",
                  "paleta_colores", "voz_marca"
                - "logo_concepto": descripción conceptual del logo (qué elementos, formas, símbolo principal).
                - "tipografia_primaria": nombre de fuente principal + por qué funciona para este rubro.
                - "tipografia_secundaria": nombre de fuente secundaria + uso sugerido.
                - "paleta_colores": 3-5 colores en hexadecimal + nombre/uso (ej: "#2E7D32 - Verde bosque - Confianza, naturaleza").
                - "voz_marca": descripción del tono de comunicación (ej: "Cercana, empática, usa tú, evita tecnicismos").
                - No uses markdown, no añadas explicaciones extra.
                """.formatted(
                        datos.nombreNegocio(),
                        datos.sector(),
                        datos.paletaSugerida() == null || datos.paletaSugerida().isBlank() ? "" : "Paleta sugerida por el cliente: " + datos.paletaSugerida(),
                        datos.valoresEslogan() == null || datos.valoresEslogan().isBlank() ? "" : "Valores/eslogan: " + datos.valoresEslogan(),
                        datos.estiloVisual()
                );

        return new KitMarcaPrompt(SISTEMA, usuario);
    }
}