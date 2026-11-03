package com.adbrand.core.cuenta.dto;

import com.adbrand.core.auth.entity.Usuario;

// Datos de la empresa que ve el usuario en "Mi cuenta". Nombre de empresa y correo solo se muestran.
public record EmpresaResponse(
        String nombreEmpresa,
        String correo,
        String ruc,
        String razonSocial,
        String telefono,
        String direccion) {

    public static EmpresaResponse desde(Usuario usuario) {
        return new EmpresaResponse(usuario.getNombreEmpresa(), usuario.getCorreo(), usuario.getRuc(),
                usuario.getRazonSocial(), usuario.getTelefono(), usuario.getDireccion());
    }
}
