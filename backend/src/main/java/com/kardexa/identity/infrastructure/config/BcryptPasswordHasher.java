package com.kardexa.identity.infrastructure.config;

import com.kardexa.identity.application.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Adaptador del puerto {@link PasswordHasher} con BCrypt de Spring Security (ADR 0008). */
@Component
class BcryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    BcryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}
