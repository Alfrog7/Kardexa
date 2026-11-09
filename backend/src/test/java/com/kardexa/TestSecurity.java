package com.kardexa;

import com.kardexa.identity.application.port.out.TokenIssuer;
import com.kardexa.identity.domain.model.Role;
import com.kardexa.identity.domain.model.User;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Autentica por defecto todas las peticiones de {@code MockMvc} como un ADMIN, para que los tests de
 * negocio no repitan el token. Un test que necesite otro usuario manda su propio {@code Authorization}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestSecurity {

    /** Usuario con el que se firma el token por defecto; {@link #insertAdmin} lo crea en la base. */
    public static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-0000000000ad");

    @Bean
    MockMvcBuilderCustomizer defaultAdminToken(TokenIssuer tokens) {
        String token = token(tokens, Role.ADMIN);
        return builder -> builder.defaultRequest(
                MockMvcRequestBuilders.get("/").header("Authorization", "Bearer " + token));
    }

    /** Token firmado de un usuario cualquiera con ese rol (no hace falta que exista en la base). */
    public static String token(TokenIssuer tokens, Role role) {
        User user = User.restore(ADMIN_ID, "Administrador de pruebas", "admin@pruebas.test", "x", role, true);
        return tokens.issue(user).value();
    }

    /** Crea la fila del usuario por defecto, para los tests cuyos registros la referencian por clave foránea. */
    public static void insertAdmin(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT INTO app_user (id, full_name, email, password_hash, role)
                VALUES (?, 'Administrador de pruebas', 'admin@pruebas.test', 'x', 'ADMIN')
                ON CONFLICT DO NOTHING""", ADMIN_ID);
    }
}
