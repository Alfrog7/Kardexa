package com.kardexa.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kardexa.identity.domain.exception.InvalidUserException;
import com.kardexa.identity.domain.model.Role;
import com.kardexa.identity.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Usuario (dominio)")
class UserTest {

    @Test
    @DisplayName("normaliza el nombre y pasa el correo a minúsculas")
    void normalizes() {
        User user = User.create("  Ana   Pérez ", " Ana@Correo.COM ", "hash", Role.ADMIN);

        assertThat(user.fullName()).isEqualTo("Ana Pérez");
        assertThat(user.email()).isEqualTo("ana@correo.com");
        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("rechaza un nombre demasiado corto")
    void rejectsShortName() {
        assertThatThrownBy(() -> User.create("A", "ana@correo.com", "hash", Role.ADMIN))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    @DisplayName("rechaza un correo mal formado")
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> User.create("Ana", "sin-arroba", "hash", Role.ADMIN))
                .isInstanceOf(InvalidUserException.class);
    }
}
