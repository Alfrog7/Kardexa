package com.kardexa.identity.infrastructure.config;

import com.kardexa.identity.application.port.out.PasswordHasher;
import com.kardexa.identity.application.port.out.UserRepository;
import com.kardexa.identity.application.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra los casos de uso de identidad como beans (los servicios no llevan anotaciones de Spring). */
@Configuration(proxyBeanMethods = false)
class IdentityConfig {

    @Bean
    UserService userService(UserRepository users, PasswordHasher hasher) {
        return new UserService(users, hasher);
    }
}
