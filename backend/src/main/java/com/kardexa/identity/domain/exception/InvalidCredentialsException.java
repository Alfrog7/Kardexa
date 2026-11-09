package com.kardexa.identity.domain.exception;

import com.kardexa.shared.domain.UnauthorizedException;

/**
 * Correo o contraseña incorrectos, o usuario desactivado. El mensaje es el mismo en los tres casos
 * para no revelar cuál falló.
 */
public class InvalidCredentialsException extends UnauthorizedException {

    public InvalidCredentialsException() {
        super("RF-02", "Correo o contraseña incorrectos.");
    }

    @Override
    public String title() {
        return "Credenciales inválidas";
    }
}
