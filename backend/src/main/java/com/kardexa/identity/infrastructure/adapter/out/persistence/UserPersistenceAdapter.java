package com.kardexa.identity.infrastructure.adapter.out.persistence;

import com.kardexa.identity.application.port.out.UserRepository;
import com.kardexa.identity.domain.model.User;
import org.springframework.stereotype.Component;

/** Implementa el puerto {@link UserRepository} con Spring Data JPA sobre PostgreSQL. */
@Component
class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    UserPersistenceAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        return jpa.saveAndFlush(UserJpaEntity.fromDomain(user)).toDomain();
    }

    @Override
    public boolean existsAny() {
        return jpa.count() > 0;
    }
}
