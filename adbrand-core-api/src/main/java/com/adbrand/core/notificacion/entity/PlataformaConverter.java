package com.adbrand.core.notificacion.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// HU 50: guarda la plataforma en minúsculas, como exige el CHECK de user_devices.
@Converter
public class PlataformaConverter implements AttributeConverter<Plataforma, String> {

    @Override
    public String convertToDatabaseColumn(Plataforma plataforma) {
        return plataforma == null ? null : plataforma.valor();
    }

    @Override
    public Plataforma convertToEntityAttribute(String valor) {
        return Plataforma.desde(valor);
    }
}
