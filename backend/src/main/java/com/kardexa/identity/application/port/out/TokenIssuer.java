package com.kardexa.identity.application.port.out;

import com.kardexa.identity.domain.model.User;

/** Puerto de salida: emite el token de acceso de un usuario (ADR 0008). */
public interface TokenIssuer {

    IssuedToken issue(User user);

    /** Token firmado y segundos que le quedan de vida. */
    record IssuedToken(String value, long expiresInSeconds) {}
}
