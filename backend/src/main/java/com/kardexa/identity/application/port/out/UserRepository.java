package com.kardexa.identity.application.port.out;

import com.kardexa.identity.domain.model.User;

/** Puerto de salida: dónde se guardan los usuarios. */
public interface UserRepository {

    User save(User user);

    boolean existsAny();
}
