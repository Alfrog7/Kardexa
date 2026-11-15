package com.kardexa.identity.application.port.in;

import com.kardexa.identity.domain.model.Role;
import com.kardexa.identity.domain.model.User;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import java.util.UUID;

/** HU-03 · El administrador crea, lista y edita las cuentas de sus empleados (RF-03). */
public interface ManageUsersUseCase {

    User create(CreateUserCommand command);

    ResultPage<User> list(PageQuery page);

    /** Cambia solo lo que llega con valor; {@code null} deja el dato como está. */
    User update(UpdateUserCommand command);

    /** {@code siteId} nulo en un empleado: la «Sede principal». El administrador no lleva sede. */
    record CreateUserCommand(String fullName, String email, String password, Role role, UUID siteId) {

        public CreateUserCommand(String fullName, String email, String password, Role role) {
            this(fullName, email, password, role, null);
        }
    }

    /** {@code siteId} nulo deja la sede como está. */
    record UpdateUserCommand(UUID id, String fullName, Role role, Boolean active, UUID siteId) {

        public UpdateUserCommand(UUID id, String fullName, Role role, Boolean active) {
            this(id, fullName, role, active, null);
        }
    }
}
