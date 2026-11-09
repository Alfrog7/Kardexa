package com.kardexa.inventory.infrastructure.adapter.out.persistence;

import com.kardexa.inventory.application.port.out.UserNames;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Lectura de {@code app_user.full_name} entre módulos, permitida en adaptadores por el ADR 0007. */
@Component
class UserNamesAdapter implements UserNames {

    private final JdbcClient jdbc;

    UserNamesAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<UUID, String> namesOf(Collection<UUID> userIds) {
        Map<UUID, String> names = new HashMap<>();
        jdbc.sql("SELECT id, full_name FROM app_user WHERE id IN (:ids)")
                .param("ids", userIds)
                .query((row, n) -> {
                    names.put(row.getObject("id", UUID.class), row.getString("full_name"));
                    return null;
                })
                .list();
        return names;
    }
}
