package com.kardexa.identity.application.service;

import com.kardexa.identity.application.port.in.ManageUsersUseCase;
import com.kardexa.identity.application.port.out.PasswordHasher;
import com.kardexa.identity.application.port.out.UserRepository;
import com.kardexa.identity.domain.exception.DuplicateEmailException;
import com.kardexa.identity.domain.exception.LastAdminException;
import com.kardexa.identity.domain.exception.UserNotFoundException;
import com.kardexa.identity.domain.model.User;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.util.Objects;

/** Casos de uso de HU-03. Java puro: se registra como bean en {@code IdentityConfig}. */
public class UserManagementService implements ManageUsersUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;

    public UserManagementService(UserRepository users, PasswordHasher hasher) {
        this.users = Objects.requireNonNull(users);
        this.hasher = Objects.requireNonNull(hasher);
    }

    @Override
    public User create(CreateUserCommand command) {
        String email = User.normalizeEmail(command.email());
        if (users.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        return users.save(User.create(command.fullName(), email, hasher.hash(command.password()), command.role()));
    }

    @Override
    public ResultPage<User> list(PageQuery page) {
        return users.findAll(page);
    }

    @Override
    public User update(UpdateUserCommand command) {
        User user = users.findById(command.id()).orElseThrow(() -> new UserNotFoundException(command.id()));
        boolean wasActiveAdmin = user.isActiveAdmin();
        boolean wasLastAdmin = wasActiveAdmin && users.countActiveAdmins() <= 1;

        if (command.fullName() != null) {
            user.rename(command.fullName());
        }
        if (command.role() != null) {
            user.changeRole(command.role());
        }
        if (command.active() != null) {
            if (command.active()) {
                user.activate();
            } else {
                user.deactivate();
            }
        }

        // RN-11: si dejó de ser administrador activo, no puede ser el último.
        if (wasLastAdmin && !user.isActiveAdmin()) {
            throw new LastAdminException();
        }
        return users.save(user);
    }
}
