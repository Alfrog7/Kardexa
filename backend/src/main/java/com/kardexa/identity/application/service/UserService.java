package com.kardexa.identity.application.service;

import com.kardexa.identity.application.port.in.RegisterFirstAdminUseCase;
import com.kardexa.identity.application.port.out.PasswordHasher;
import com.kardexa.identity.application.port.out.UserRepository;
import com.kardexa.identity.domain.exception.SetupAlreadyDoneException;
import com.kardexa.identity.domain.model.Role;
import com.kardexa.identity.domain.model.User;
import java.util.Objects;

/** Casos de uso de HU-01. Java puro: se registra como bean en {@code IdentityConfig}. */
public class UserService implements RegisterFirstAdminUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;

    public UserService(UserRepository users, PasswordHasher hasher) {
        this.users = Objects.requireNonNull(users);
        this.hasher = Objects.requireNonNull(hasher);
    }

    @Override
    public User register(RegisterFirstAdminCommand command) {
        if (users.existsAny()) {
            throw new SetupAlreadyDoneException();
        }
        User admin = User.create(
                command.fullName(), command.email(), hasher.hash(command.password()), Role.ADMIN);
        return users.save(admin);
    }
}
