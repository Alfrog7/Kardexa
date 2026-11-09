package com.kardexa.identity.infrastructure.adapter.out.persistence;

import com.kardexa.identity.domain.model.Role;
import com.kardexa.identity.domain.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Fila de la tabla {@code app_user} (migración V6). Solo existe en esta capa. */
@Entity
@Table(name = "app_user")
class UserJpaEntity {

    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false, length = User.NAME_MAX_LENGTH)
    private String fullName;

    @Column(nullable = false, length = User.EMAIL_MAX_LENGTH)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    protected UserJpaEntity() {
        // Requerido por JPA.
    }

    static UserJpaEntity fromDomain(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = user.id();
        entity.fullName = user.fullName();
        entity.email = user.email();
        entity.passwordHash = user.passwordHash();
        entity.role = user.role();
        entity.active = user.isActive();
        return entity;
    }

    User toDomain() {
        return User.restore(id, fullName, email, passwordHash, role, active);
    }
}
