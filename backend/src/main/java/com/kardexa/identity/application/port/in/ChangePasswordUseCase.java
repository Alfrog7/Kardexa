package com.kardexa.identity.application.port.in;

import java.util.UUID;

/** HU-04 · Cada usuario cambia su propia contraseña indicando la actual (RF-05). */
public interface ChangePasswordUseCase {

    void changePassword(ChangePasswordCommand command);

    record ChangePasswordCommand(UUID userId, String currentPassword, String newPassword) {}
}
