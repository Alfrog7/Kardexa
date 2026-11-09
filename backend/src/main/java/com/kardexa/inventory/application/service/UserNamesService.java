package com.kardexa.inventory.application.service;

import com.kardexa.inventory.application.port.in.ResolveUserNamesUseCase;
import com.kardexa.inventory.application.port.out.UserNames;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Java puro: se registra como bean en {@code InventoryConfig}. */
public class UserNamesService implements ResolveUserNamesUseCase {

    private final UserNames userNames;

    public UserNamesService(UserNames userNames) {
        this.userNames = Objects.requireNonNull(userNames);
    }

    @Override
    public Map<UUID, String> namesOf(Collection<UUID> userIds) {
        Collection<UUID> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        return ids.isEmpty() ? Map.of() : userNames.namesOf(ids);
    }
}
