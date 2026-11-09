package com.kardexa.identity.application.service;

import com.kardexa.identity.application.port.in.GetCurrentUserUseCase;
import com.kardexa.identity.application.port.in.LoginUseCase;
import com.kardexa.identity.application.port.in.RegisterFirstAdminUseCase;
import com.kardexa.identity.application.port.out.PasswordHasher;
import com.kardexa.identity.application.port.out.TokenIssuer;
import com.kardexa.identity.application.port.out.UserRepository;
import com.kardexa.identity.domain.exception.InvalidCredentialsException;
import com.kardexa.identity.domain.exception.SetupAlreadyDoneException;
import com.kardexa.identity.domain.model.Role;
import com.kardexa.identity.domain.model.User;
import java.util.Objects;
import java.util.UUID;

/** Casos de uso de HU-01 y HU-02. Java puro: se registra como bean en {@code IdentityConfig}. */
public class UserService implements RegisterFirstAdminUseCase, LoginUseCase, GetCurrentUserUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final TokenIssuer tokens;

    public UserService(UserRepository users, PasswordHasher hasher, TokenIssuer tokens) {
        this.users = Objects.requireNonNull(users);
        this.hasher = Objects.requireNonNull(hasher);
        this.tokens = Objects.requireNonNull(tokens);
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

    @Override
    public LoginResult login(LoginCommand command) {
        String email = command.email() == null ? "" : command.email().strip();
        User user = users.findByEmail(email)
                .filter(User::isActive)
                .filter(found -> command.password() != null && hasher.matches(command.password(), found.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return new LoginResult(tokens.issue(user), user);
    }

    @Override
    public User getCurrent(UUID userId) {
        return users.findById(userId)
                .filter(User::isActive)
                .orElseThrow(InvalidCredentialsException::new);
    }
}
