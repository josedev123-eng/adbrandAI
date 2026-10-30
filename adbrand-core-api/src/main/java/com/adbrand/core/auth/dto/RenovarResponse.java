package com.adbrand.core.auth.dto;

// HU 43: token de acceso nuevo y token de renovación rotado.
public record RenovarResponse(String token, String tokenRenovacion) {
}
