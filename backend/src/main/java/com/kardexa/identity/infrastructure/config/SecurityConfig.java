package com.kardexa.identity.infrastructure.config;

import com.kardexa.shared.infrastructure.config.WebConfig;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de la API (ADR 0008): sin sesión ni CSRF, JWT HS256 propio y BCrypt. Por ahora solo
 * {@code /auth/me} exige token; las rutas del catálogo y el inventario se protegen en HU-03.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    private static final String API = WebConfig.API_PREFIX;
    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey jwtSecretKey(@Value("${kardexa.security.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos %d bytes.".formatted(MIN_SECRET_BYTES));
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.POST, API + "/auth/setup", API + "/auth/login").permitAll();
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                    // Las imágenes de producto se cargan con una etiqueta img, que no envía el token.
                    auth.requestMatchers(HttpMethod.GET, API + "/products/*/image").permitAll();
                    // Solo ADMIN gestiona usuarios (RNF-03).
                    auth.requestMatchers(API + "/users", API + "/users/**").hasRole("ADMIN");
                    // Las compras muestran costos: consultarlas también es solo de ADMIN (RN-05, ADR 0009).
                    auth.requestMatchers(API + "/purchases", API + "/purchases/**").hasRole("ADMIN");
                    // RN-05: registrar y consultar ventas es de todos; anularlas, solo del ADMIN (ADR 0010).
                    auth.requestMatchers(HttpMethod.POST, API + "/sales/*/void").hasRole("ADMIN");
                    // RN-05: crear, editar y ajustar el stock es solo de ADMIN; consultar, de cualquiera con sesión.
                    for (HttpMethod method : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
                        auth.requestMatchers(method,
                                API + "/categories", API + "/categories/**",
                                API + "/products", API + "/products/**",
                                API + "/stock-adjustments", API + "/stock-adjustments/**",
                                API + "/suppliers", API + "/suppliers/**").hasRole("ADMIN");
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter()))
                        .authenticationEntryPoint(unauthorizedEntryPoint()))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(forbiddenHandler()))
                .build();
    }

    /** Convierte el claim {@code role} del token en la autoridad {@code ROLE_<rol>}. */
    private static JwtAuthenticationConverter roleConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** Responde 401 en Problem Details, con el mismo formato que el resto de errores. */
    private static AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, ex) -> writeProblem(response, HttpStatus.UNAUTHORIZED, "no-autenticado",
                "No autenticado", "Falta el token de acceso, o es inválido o está vencido.");
    }

    /** Responde 403 en Problem Details cuando el rol no alcanza. */
    private static AccessDeniedHandler forbiddenHandler() {
        return (request, response, ex) -> writeProblem(response, HttpStatus.FORBIDDEN, "operacion-no-permitida",
                "Acceso denegado", "Tu rol no permite esta operación.");
    }

    private static void writeProblem(
            HttpServletResponse response, HttpStatus status, String type, String title, String detail)
            throws IOException {
        response.setStatus(status.value());
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"type":"https://kardexa.dev/problemas/%s","title":"%s","status":%d,"detail":"%s"}"""
                .formatted(type, title, status.value(), detail));
    }
}
