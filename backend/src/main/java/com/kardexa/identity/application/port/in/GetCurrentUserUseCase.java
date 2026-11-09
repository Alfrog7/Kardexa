package com.kardexa.identity.application.port.in;

import com.kardexa.identity.domain.model.User;
import java.util.UUID;

/** HU-02 · Devolver el usuario autenticado (RF-04). */
public interface GetCurrentUserUseCase {

    User getCurrent(UUID userId);
}
