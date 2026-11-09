package com.kardexa.identity.application.port.out;

import com.kardexa.identity.domain.model.User;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: dónde se guardan los usuarios. */
public interface UserRepository {

    User save(User user);

    boolean existsAny();

    Optional<User> findById(UUID id);

    /** Busca por correo sin distinguir mayúsculas. */
    Optional<User> findByEmail(String email);
}
