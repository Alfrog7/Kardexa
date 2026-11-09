package com.kardexa.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.identity.application.port.in.ManageUsersUseCase.CreateUserCommand;
import com.kardexa.identity.application.port.in.ManageUsersUseCase.UpdateUserCommand;
import com.kardexa.identity.application.port.out.PasswordHasher;
import com.kardexa.identity.application.port.out.UserRepository;
import com.kardexa.identity.application.service.UserManagementService;
import com.kardexa.identity.domain.exception.DuplicateEmailException;
import com.kardexa.identity.domain.exception.LastAdminException;
import com.kardexa.identity.domain.exception.UserNotFoundException;
import com.kardexa.identity.domain.model.Role;
import com.kardexa.identity.domain.model.User;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Gestión de usuarios (servicio, HU-03)")
class UserManagementServiceTest {

    private final List<User> stored = new ArrayList<>();
    private final UserRepository repository = new UserRepository() {
        @Override
        public User save(User user) {
            stored.removeIf(u -> u.id().equals(user.id()));
            stored.add(user);
            return user;
        }

        @Override
        public boolean existsAny() {
            return !stored.isEmpty();
        }

        @Override
        public Optional<User> findById(UUID id) {
            return stored.stream().filter(u -> u.id().equals(id)).findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return stored.stream().filter(u -> u.email().equalsIgnoreCase(email)).findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public long countActiveAdmins() {
            return stored.stream().filter(User::isActiveAdmin).count();
        }

        @Override
        public ResultPage<User> findAll(PageQuery page) {
            return ResultPage.of(List.copyOf(stored), page, stored.size());
        }
    };
    private final PasswordHasher hasher = new PasswordHasher() {
        @Override
        public String hash(String raw) {
            return "hash:" + raw;
        }

        @Override
        public boolean matches(String raw, String hash) {
            return hash.equals("hash:" + raw);
        }
    };
    private final UserManagementService service = new UserManagementService(repository, hasher);

    private User admin() {
        return repository.save(User.create("Ana Pérez", "ana@correo.com", "hash:x", Role.ADMIN));
    }

    @Test
    @DisplayName("crea un empleado con la contraseña hasheada")
    void createsEmployee() {
        User employee = service.create(new CreateUserCommand("Luis Gómez", "Luis@Correo.com", "clave12345", Role.EMPLOYEE));

        assertThat(employee.role()).isEqualTo(Role.EMPLOYEE);
        assertThat(employee.email()).isEqualTo("luis@correo.com");
        assertThat(employee.passwordHash()).isEqualTo("hash:clave12345");
    }

    @Test
    @DisplayName("rechaza un correo repetido sin distinguir mayúsculas")
    void rejectsDuplicateEmail() {
        admin();

        assertThatThrownBy(() -> service.create(new CreateUserCommand("Otra Ana", "ANA@correo.com", "clave12345", Role.EMPLOYEE)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("RN-11: el último administrador activo no puede desactivarse")
    void lastAdminCannotBeDeactivated() {
        User ana = admin();

        assertThatThrownBy(() -> service.update(new UpdateUserCommand(ana.id(), null, null, false)))
                .isInstanceOf(LastAdminException.class);
    }

    @Test
    @DisplayName("RN-11: el último administrador activo no puede cambiar de rol")
    void lastAdminCannotChangeRole() {
        User ana = admin();

        assertThatThrownBy(() -> service.update(new UpdateUserCommand(ana.id(), null, Role.EMPLOYEE, null)))
                .isInstanceOf(LastAdminException.class);
    }

    @Test
    @DisplayName("con otro administrador activo, un administrador sí puede desactivarse")
    void adminCanBeDeactivatedWhenAnotherExists() {
        User ana = admin();
        repository.save(User.create("Bruno Ríos", "bruno@correo.com", "hash:y", Role.ADMIN));

        User updated = service.update(new UpdateUserCommand(ana.id(), null, null, false));

        assertThat(updated.isActive()).isFalse();
    }

    @Test
    @DisplayName("actualizar un usuario inexistente lanza UserNotFoundException")
    void updateUnknownUser() {
        assertThatThrownBy(() -> service.update(new UpdateUserCommand(UUID.randomUUID(), "Nuevo Nombre", null, null)))
                .isInstanceOf(UserNotFoundException.class);
    }
}
