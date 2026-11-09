package com.kardexa.identity.infrastructure.config;

import com.kardexa.identity.application.port.out.TokenIssuer;
import com.kardexa.identity.domain.model.User;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Emite el JWT de acceso HS256 de la API (ADR 0008): {@code sub} = id del usuario, más {@code role}. */
@Component
class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder encoder;
    private final Duration lifetime;

    JwtTokenIssuer(JwtEncoder encoder, @Value("${kardexa.security.jwt.expiration-minutes}") long expirationMinutes) {
        this.encoder = encoder;
        this.lifetime = Duration.ofMinutes(expirationMinutes);
    }

    @Override
    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .claim("role", user.role().name())
                .issuedAt(now)
                .expiresAt(now.plus(lifetime))
                .build();
        String value = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new IssuedToken(value, lifetime.toSeconds());
    }
}
