package com.adbrand.core.previsualizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.adbrand.core.previsualizacion.dto.FormatoAnuncio;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;

// HU 46: los tres formatos y cómo se acorta el texto que no entra.
class FormatoAnuncioTest {

    private static final String LARGO = "Este viernes el pan de yema viene doble en Panadería Doña Rosa: trae a quien quieras "
            + "y llévate dos panes por el precio de uno, recién salidos del horno, desde las siete de la mañana.";

    @Test
    void cadaFormatoTieneSuTamanoYSuLimiteDeTexto() {
        assertThat(FormatoAnuncio.CUADRADO.ancho()).isEqualTo(1080);
        assertThat(FormatoAnuncio.CUADRADO.alto()).isEqualTo(1080);
        assertThat(FormatoAnuncio.VERTICAL.alto()).isGreaterThan(FormatoAnuncio.VERTICAL.ancho());
        assertThat(FormatoAnuncio.HORIZONTAL.ancho()).isGreaterThan(FormatoAnuncio.HORIZONTAL.alto());
        assertThat(FormatoAnuncio.VERTICAL.maxCaracteres()).isLessThan(FormatoAnuncio.CUADRADO.maxCaracteres());
    }

    @Test
    void unTextoCortoQuedaIgual() {
        assertThat(FormatoAnuncio.VERTICAL.ajustar("  2x1 en pan de yema  ")).isEqualTo("2x1 en pan de yema");
    }

    @Test
    void unTextoLargoSeAcortaEnUnaPalabraCompletaConPuntosSuspensivos() {
        for (FormatoAnuncio formato : FormatoAnuncio.values()) {
            String ajustado = formato.ajustar(LARGO);

            assertThat(ajustado).endsWith("…");
            assertThat(ajustado.length()).isLessThanOrEqualTo(formato.maxCaracteres());
            String sinPuntos = ajustado.substring(0, ajustado.length() - 1);
            assertThat(LARGO).startsWith(sinPuntos);
            // Corta en el límite de una palabra, no en medio.
            assertThat(LARGO.charAt(sinPuntos.length())).isIn(' ', ',', ':', '.');
        }
    }

    @Test
    void elFormatoSeLeeSinImportarMayusculasYPorDefectoEsCuadrado() {
        assertThat(FormatoAnuncio.desde("vertical")).isEqualTo(FormatoAnuncio.VERTICAL);
        assertThat(FormatoAnuncio.desde(" Horizontal ")).isEqualTo(FormatoAnuncio.HORIZONTAL);
        assertThat(FormatoAnuncio.desde(null)).isEqualTo(FormatoAnuncio.CUADRADO);
        assertThatThrownBy(() -> FormatoAnuncio.desde("panoramico"))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("formato válido");
    }
}
