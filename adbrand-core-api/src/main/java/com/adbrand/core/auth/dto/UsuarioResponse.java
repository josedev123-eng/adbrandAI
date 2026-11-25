package com.adbrand.core.auth.dto;

import com.adbrand.core.auth.entity.Usuario;

// Lo que la API devuelve de un usuario. Nunca incluye el hash de la contraseña.
// tutorialVisto: la web muestra el tutorial guiado solo si es false.
public record UsuarioResponse(Long id, String nombre, String nombreEmpresa, String correo, String colorAvatar,
                              boolean tutorialVisto, String avatarImagen) {

    public UsuarioResponse(Long id, String nombre, String nombreEmpresa, String correo, String colorAvatar,
                           boolean tutorialVisto) {
        this(id, nombre, nombreEmpresa, correo, colorAvatar, tutorialVisto, null);
    }

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getNombreEmpresa(), usuario.getCorreo(),
                usuario.getColorAvatar(), usuario.isTutorialVisto(), usuario.getAvatarImagen());
    }
}
