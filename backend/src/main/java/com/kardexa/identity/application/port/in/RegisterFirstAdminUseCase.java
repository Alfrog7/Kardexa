package com.kardexa.identity.application.port.in;

import com.kardexa.identity.domain.model.User;

/** HU-01 · Registrar al primer administrador, solo mientras no exista ningún usuario. */
public interface RegisterFirstAdminUseCase {

    User register(RegisterFirstAdminCommand command);

    record RegisterFirstAdminCommand(String fullName, String email, String password) {}
}
