package com.adbrand.core.cuenta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.adbrand.core.auth.dto.UsuarioResponse;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.contenido.service.MarcaDelNegocio;
import com.adbrand.core.cuenta.service.AvatarService;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Avatar de Mi Cuenta: el logo del kit o una foto, guardados en 256 x 256.
class AvatarTest {

    private UsuarioRepository usuarios;
    private KitMarcaRepository kits;
    private AvatarService servicio;
    private Usuario usuario;

    private static String base64Png(int ancho, int alto) throws Exception {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        var g = imagen.createGraphics();
        g.setColor(new Color(0x8B, 0x45, 0x13));
        g.fillRect(0, 0, ancho, alto);
        g.dispose();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return Base64.getEncoder().encodeToString(salida.toByteArray());
    }

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        kits = mock(KitMarcaRepository.class);
        servicio = new AvatarService(usuarios, kits);
        usuario = new Usuario();
        when(usuarios.findById(5L)).thenReturn(Optional.of(usuario));
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void unaFotoSeGuardaRecortadaEn256() throws Exception {
        UsuarioResponse respuesta = servicio.cambiar(5L, "data:image/png;base64," + base64Png(800, 600), false);

        assertThat(respuesta.avatarImagen()).startsWith("data:image/png;base64,");
        BufferedImage guardada = MarcaDelNegocio.leerDataUrl(respuesta.avatarImagen());
        assertThat(guardada.getWidth()).isEqualTo(256);
        assertThat(guardada.getHeight()).isEqualTo(256);
    }

    @Test
    void puedeUsarElLogoDelKit() throws Exception {
        KitMarca kit = new KitMarca();
        kit.setLogoImagen(base64Png(512, 512));
        when(kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(5L)).thenReturn(Optional.of(kit));

        assertThat(servicio.cambiar(5L, null, true).avatarImagen()).startsWith("data:image/png;base64,");
    }

    @Test
    void sinLogoEnElKitLoDiceClaro() {
        when(kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(5L)).thenReturn(Optional.of(new KitMarca()));

        assertThatThrownBy(() -> servicio.cambiar(5L, null, true))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("todavía no tiene logo");
    }

    @Test
    void rechazaLoQueNoEsImagenYQuitarVuelveALasIniciales() {
        assertThatThrownBy(() -> servicio.cambiar(5L, "data:image/png;base64,bm8gZXMgdW5hIGltYWdlbg==", false))
                .isInstanceOf(SolicitudInvalidaException.class);

        usuario.setAvatarImagen("data:image/png;base64,AAA");
        assertThat(servicio.quitar(5L).avatarImagen()).isNull();
    }
}
