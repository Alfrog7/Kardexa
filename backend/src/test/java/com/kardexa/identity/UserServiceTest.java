package com.kardexa.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.identity.application.port.in.LoginUseCase.LoginCommand;
import com.kardexa.identity.application.port.in.RegisterFirstAdminUseCase.RegisterFirstAdminCommand;
import com.kardexa.identity.application.port.out.PasswordHasher;
import com.kardexa.identity.application.port.out.TokenIssuer;
import com.kardexa.identity.application.port.out.UserRepository;
import com.kardexa.identity.application.service.UserService;
import com.kardexa.identity.domain.exception.InvalidCredentialsException;
import com.kardexa.identity.domain.exception.SetupAlreadyDoneException;
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
    private final TokenIssuer tokens = user -> new TokenIssuer.IssuedToken("token-" + user.id(), 3600);
    private final UserService service = new UserService(repository, hasher, tokens);

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

    @Test
    @DisplayName("login con credenciales correctas devuelve el token y el usuario")
    void logsIn() {
        User admin = service.register(command());

        var result = service.login(new LoginCommand("ANA@correo.com", "secreta123"));

        assertThat(result.user()).isEqualTo(admin);
        assertThat(result.token().value()).isEqualTo("token-" + admin.id());
    }

    @Test
    @DisplayName("login con contraseña o correo incorrectos lanza el mismo error genérico")
    void rejectsBadCredentials() {
        service.register(command());

        assertThatThrownBy(() -> service.login(new LoginCommand("ana@correo.com", "mala")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.login(new LoginCommand("otro@correo.com", "secreta123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos.");
    }

    @Test
    @DisplayName("un usuario desactivado no puede iniciar sesión")
    void rejectsInactiveUser() {
        User admin = service.register(command());
        stored.clear();
        stored.add(User.restore(admin.id(), admin.fullName(), admin.email(), admin.passwordHash(), admin.role(), false));

        assertThatThrownBy(() -> service.login(new LoginCommand("ana@correo.com", "secreta123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
