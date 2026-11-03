package com.adbrand.core.brandkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.brandkit.dto.EditarIdentidadRequest;
import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.entity.KitMarcaVersion;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.brandkit.repository.KitMarcaVersionRepository;
import com.adbrand.core.brandkit.service.EdicionKitMarcaService;
import com.adbrand.core.contenido.dto.IdentidadMarca;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.service.PlantillaPromptAnuncio;
import com.adbrand.core.negocio.dto.PerfilNegocioResponse;
import com.adbrand.core.negocio.entity.Tono;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// HU 31: reglas de la identidad visual y cómo llega al prompt del anuncio, sin base de datos (Mockito).
class IdentidadMarcaTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String VOZ = "Elegante y sobria, trata de usted al cliente.";

    private static Set<String> errores(EditarIdentidadRequest datos) {
        return VALIDADOR.validate(datos).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static PerfilNegocioResponse perfil() {
        return new PerfilNegocioResponse(1L, "Panadería Doña Rosa", "Panadería y pastelería",
                "Familias del barrio", Tono.CERCANO, null, null);
    }

    private KitMarcaVersionRepository versiones;
    private EdicionKitMarcaService servicio;
    private KitMarca kit;

    @BeforeEach
    void preparar() {
        KitMarcaRepository kits = mock(KitMarcaRepository.class);
        versiones = mock(KitMarcaVersionRepository.class);
        servicio = new EdicionKitMarcaService(kits, versiones);

        kit = new KitMarca();
        kit.setUsuarioId(1L);
        kit.setPaletaColores("#2E7D32 - Verde");
        kit.setTipografiaPrimaria("Bricolage Grotesque");
        kit.setVozMarca("Cercana, usa tú.");
        when(kits.findById(10L)).thenReturn(Optional.of(kit));
        when(kits.saveAndFlush(any(KitMarca.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void aceptaUnaIdentidadCompletaOSinTipografiaSecundaria() {
        assertThat(errores(new EditarIdentidadRequest("Georgia", "Arial", VOZ))).isEmpty();
        assertThat(errores(new EditarIdentidadRequest("Georgia", null, VOZ))).isEmpty();
    }

    @Test
    void exigeTipografiaPrincipalYVoz() {
        assertThat(errores(new EditarIdentidadRequest(null, "Arial", null)))
                .containsExactlyInAnyOrder("tipografiaPrimaria", "vozMarca");
        assertThat(errores(new EditarIdentidadRequest("   ", null, "   ")))
                .containsExactlyInAnyOrder("tipografiaPrimaria", "vozMarca");
    }

    @Test
    void respetaLosLargosMaximos() {
        assertThat(errores(new EditarIdentidadRequest("a".repeat(200), "b".repeat(200), "c".repeat(1000)))).isEmpty();
        assertThat(errores(new EditarIdentidadRequest("a".repeat(201), "b".repeat(201), "c".repeat(1001))))
                .containsExactlyInAnyOrder("tipografiaPrimaria", "tipografiaSecundaria", "vozMarca");
    }

    @Test
    void limpiaEspaciosYDejaVaciaLaSecundariaComoNull() {
        EditarIdentidadRequest datos = new EditarIdentidadRequest("  Georgia ", "   ", "  " + VOZ + "  ");

        assertThat(datos.tipografiaPrimaria()).isEqualTo("Georgia");
        assertThat(datos.tipografiaSecundaria()).isNull();
        assertThat(datos.vozMarca()).isEqualTo(VOZ);
    }

    @Test
    void guardarCambiaSoloLaIdentidadYDejaLaVozEnLaVersion() {
        servicio.guardarIdentidad(1L, 10L, new EditarIdentidadRequest("Georgia", "Arial", VOZ));

        assertThat(kit.getTipografiaPrimaria()).isEqualTo("Georgia");
        assertThat(kit.getTipografiaSecundaria()).isEqualTo("Arial");
        assertThat(kit.getVozMarca()).isEqualTo(VOZ);
        assertThat(kit.getPaletaColores()).isEqualTo("#2E7D32 - Verde");

        ArgumentCaptor<KitMarcaVersion> version = ArgumentCaptor.forClass(KitMarcaVersion.class);
        verify(versiones).save(version.capture());
        assertThat(version.getValue().getVozMarca()).isEqualTo(VOZ);
        assertThat(version.getValue().getTipografiaPrimaria()).isEqualTo("Georgia");
    }

    @Test
    void elPromptLlevaLaVozDeLaMarcaAdemasDelTono() {
        String usuario = new PlantillaPromptAnuncio().construir(perfil(), "Promo", RedSocial.INSTAGRAM,
                new IdentidadMarca("Georgia", null, "  " + VOZ + "  ")).usuario();

        assertThat(usuario)
                .contains("Tono: cercano")
                .contains("Voz de la marca: " + VOZ + "\n")
                .contains("- Escribe con la voz de la marca; si choca con el tono, manda la voz.");
    }

    @Test
    void sinKitOConVozVaciaElPromptQuedaComoAntes() {
        PlantillaPromptAnuncio plantilla = new PlantillaPromptAnuncio();
        String antes = plantilla.construir(perfil(), "Promo", RedSocial.INSTAGRAM).usuario();

        assertThat(plantilla.construir(perfil(), "Promo", RedSocial.INSTAGRAM, null).usuario()).isEqualTo(antes);
        assertThat(plantilla.construir(perfil(), "Promo", RedSocial.INSTAGRAM,
                new IdentidadMarca("Georgia", null, "  ")).usuario()).isEqualTo(antes);
        assertThat(antes).doesNotContain("Voz de la marca");
    }
}
