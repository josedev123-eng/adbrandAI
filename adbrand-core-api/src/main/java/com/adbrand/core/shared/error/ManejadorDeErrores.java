package com.adbrand.core.shared.error;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Convierte los errores de todos los controllers al mismo formato JSON.
@RestControllerAdvice
public class ManejadorDeErrores {

    // Falla una anotación como @NotBlank: 400 con el mensaje de cada campo.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse datosInvalidos(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return new ErrorResponse("DATOS_INVALIDOS", "Revisa los campos marcados.", campos);
    }

    // JSON mal formado o un valor que no existe, por ejemplo un tono que no está en la lista.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse jsonInvalido(HttpMessageNotReadableException ex) {
        return new ErrorResponse("DATOS_INVALIDOS", "Los datos enviados no tienen el formato correcto.", Map.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse noEncontrado(RecursoNoEncontradoException ex) {
        return new ErrorResponse(ex.getCodigo(), ex.getMessage(), Map.of());
    }
}