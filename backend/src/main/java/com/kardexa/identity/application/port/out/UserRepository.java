package com.kardexa.identity.application.port.out;

import com.kardexa.identity.domain.model.User;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: dónde se guardan los usuarios. */
public interface UserRepository {

    User save(User user);

    boolean existsAny();

    Optional<User> findById(UUID id);

    /** Busca por correo sin distinguir mayúsculas. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countActiveAdmins();

    ResultPage<User> findAll(PageQuery page);
}
