package com.kardexa.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.identity.application.port.in.RegisterFirstAdminUseCase.RegisterFirstAdminCommand;
import com.kardexa.identity.application.port.out.PasswordHasher;
import com.kardexa.identity.application.port.out.UserRepository;
import com.kardexa.identity.application.service.UserService;
import com.kardexa.identity.domain.exception.SetupAlreadyDoneException;
import com.kardexa.identity.domain.model.Role;
import com.kardexa.identity.domain.model.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Registro del primer administrador (servicio, HU-01)")
class UserServiceTest {

    private final List<User> stored = new ArrayList<>();
    private final UserRepository repository = new UserRepository() {
        @Override
        public User save(User user) {
            stored.add(user);
            return user;
        }

        @Override
        public boolean existsAny() {
            return !stored.isEmpty();
        }
    };
    private final PasswordHasher hasher = raw -> "hash:" + raw;
    private final UserService service = new UserService(repository, hasher);

    private static RegisterFirstAdminCommand command() {
        return new RegisterFirstAdminCommand("Ana Pérez", "ana@correo.com", "secreta123");
    }

    @Test
    @DisplayName("sin usuarios crea un ADMIN con la contraseña hasheada")
    void createsAdmin() {
        User admin = service.register(command());

        assertThat(admin.role()).isEqualTo(Role.ADMIN);
        assertThat(admin.passwordHash()).isEqualTo("hash:secreta123");
        assertThat(stored).containsExactly(admin);
    }

    @Test
    @DisplayName("con un usuario existente responde con SetupAlreadyDoneException")
    void closedAfterFirstUser() {
        service.register(command());

        assertThatThrownBy(() -> service.register(command())).isInstanceOf(SetupAlreadyDoneException.class);
        assertThat(stored).hasSize(1);
    }
}
