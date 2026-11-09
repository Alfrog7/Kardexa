package com.kardexa.identity.infrastructure.adapter.out.persistence;

import static com.kardexa.shared.infrastructure.persistence.ConstraintViolations.violates;

import com.kardexa.identity.application.port.out.UserRepository;
import com.kardexa.identity.domain.exception.DuplicateEmailException;
import com.kardexa.identity.domain.model.Role;
import com.kardexa.identity.domain.model.User;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** Implementa el puerto {@link UserRepository} con Spring Data JPA sobre PostgreSQL. */
@Component
class UserPersistenceAdapter implements UserRepository {

    /** Índice único de la migración V6 sobre {@code lower(email)}. */
    static final String UNIQUE_EMAIL_INDEX = "uk_app_user_email";

    private final UserJpaRepository jpa;

    UserPersistenceAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        try {
            return jpa.saveAndFlush(UserJpaEntity.fromDomain(user)).toDomain();
        } catch (DataIntegrityViolationException ex) {
            if (violates(ex, UNIQUE_EMAIL_INDEX)) {
                throw new DuplicateEmailException(user.email());
            }
            throw ex;
        }
    }

    @Override
    public boolean existsAny() {
        return jpa.count() > 0;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmailIgnoreCase(email).map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmailIgnoreCase(email);
    }

    @Override
    public long countActiveAdmins() {
        return jpa.countByRoleAndActiveTrue(Role.ADMIN);
    }

    @Override
    public ResultPage<User> findAll(PageQuery page) {
        Page<UserJpaEntity> result = jpa.findAll(
                PageRequest.of(page.page(), page.size(), Sort.by("fullName").ascending().and(Sort.by("id"))));
        return ResultPage.of(result.getContent().stream().map(UserJpaEntity::toDomain).toList(), page,
                result.getTotalElements());
    }
}
