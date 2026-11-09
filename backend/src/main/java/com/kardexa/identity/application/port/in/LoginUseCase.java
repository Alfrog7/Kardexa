package com.kardexa.identity.application.port.in;

import com.kardexa.identity.application.port.out.TokenIssuer.IssuedToken;
import com.kardexa.identity.domain.model.User;

/** HU-02 · Iniciar sesión con correo y contraseña (RF-02). */
public interface LoginUseCase {

    LoginResult login(LoginCommand command);

    record LoginCommand(String email, String password) {}

    record LoginResult(IssuedToken token, User user) {}
}
