package com.adbrand.core.cuenta.service;

import com.adbrand.core.auth.dto.UsuarioResponse;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.contenido.service.MarcaDelNegocio;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import com.adbrand.core.shared.error.SolicitudInvalidaException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Avatar de Mi Cuenta: el logo del kit de marca o una foto que sube la empresa. Se guarda recortada al centro y
// reducida a 256 x 256 (PNG), así pesa poco aunque el logo original sea grande. Quitarlo vuelve a las iniciales.
@Service
public class AvatarService {

    static final int LADO = 256;
    static final int TAMANO_MAXIMO = 3 * 1024 * 1024;

    private final UsuarioRepository usuarios;
    private final KitMarcaRepository kits;

    public AvatarService(UsuarioRepository usuarios, KitMarcaRepository kits) {
        this.usuarios = usuarios;
        this.kits = kits;
    }

    // "imagen": data URL de la foto subida. Si "usarLogoDelKit" es true, se toma el logo del kit más reciente.
    @Transactional
    public UsuarioResponse cambiar(Long usuarioId, String imagen, boolean usarLogoDelKit) {
        BufferedImage original;
        if (usarLogoDelKit) {
            String logo = kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(usuarioId)
                    .map(kit -> kit.getLogoImagen())
                    .filter(base64 -> base64 != null && !base64.isBlank())
                    .orElseThrow(() -> new SolicitudInvalidaException("SIN_LOGO",
                            "Tu kit de marca todavía no tiene logo. Súbelo o créalo en Kit de Marca."));
            original = MarcaDelNegocio.leerBase64(logo);
        } else {
            if (imagen == null || !imagen.startsWith("data:image/") || !imagen.contains(",")) {
                throw new SolicitudInvalidaException("AVATAR_INVALIDO", "Elige una imagen PNG o JPG.");
            }
            if (imagen.length() > TAMANO_MAXIMO * 4 / 3 + 100) {
                throw new SolicitudInvalidaException("AVATAR_MUY_GRANDE", "La imagen puede pesar hasta 3 MB.");
            }
            original = MarcaDelNegocio.leerDataUrl(imagen);
        }
        if (original == null) {
            throw new SolicitudInvalidaException("AVATAR_INVALIDO", "No pudimos leer la imagen. Prueba con otra PNG o JPG.");
        }
        Usuario usuario = buscar(usuarioId);
        usuario.setAvatarImagen("data:image/png;base64," + Base64.getEncoder().encodeToString(png(reducir(original))));
        return UsuarioResponse.desde(usuarios.save(usuario));
    }

    @Transactional
    public UsuarioResponse quitar(Long usuarioId) {
        Usuario usuario = buscar(usuarioId);
        usuario.setAvatarImagen(null);
        return UsuarioResponse.desde(usuarios.save(usuario));
    }

    // Recorta el cuadrado central y lo lleva a 256 x 256 sobre blanco (los logos con fondo transparente se ven bien).
    static BufferedImage reducir(BufferedImage original) {
        int lado = Math.min(original.getWidth(), original.getHeight());
        int x = (original.getWidth() - lado) / 2;
        int y = (original.getHeight() - lado) / 2;
        BufferedImage salida = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = salida.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, LADO, LADO);
        g.drawImage(original, 0, 0, LADO, LADO, x, y, x + lado, y + lado, null);
        g.dispose();
        return salida;
    }

    private static byte[] png(BufferedImage imagen) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el avatar.", ex);
        }
    }

    private Usuario buscar(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("USUARIO_NO_ENCONTRADO", "La cuenta no existe."));
    }
}
