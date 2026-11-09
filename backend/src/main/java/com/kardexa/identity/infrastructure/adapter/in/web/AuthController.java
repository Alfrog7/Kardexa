package com.kardexa.identity.infrastructure.adapter.in.web;

import com.kardexa.identity.application.port.in.RegisterFirstAdminUseCase;
import com.kardexa.identity.application.port.in.RegisterFirstAdminUseCase.RegisterFirstAdminCommand;
import com.kardexa.identity.domain.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/auth} según el contrato {@code docs/api/openapi.yaml} (HU-01). */
@RestController
@RequestMapping("/auth")
class AuthController {

    private final RegisterFirstAdminUseCase registerFirstAdmin;

    AuthController(RegisterFirstAdminUseCase registerFirstAdmin) {
        this.registerFirstAdmin = registerFirstAdmin;
    }

    @PostMapping("/setup")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse setup(@Valid @RequestBody RegisterAdminRequest request) {
        return UserResponse.from(registerFirstAdmin.register(
                new RegisterFirstAdminCommand(request.fullName(), request.email(), request.password())));
    }

    record RegisterAdminRequest(
            @NotBlank(message = "El nombre es obligatorio.")
            @Size(min = User.NAME_MIN_LENGTH, max = User.NAME_MAX_LENGTH,
                    message = "Debe tener entre {min} y {max} caracteres.")
            String fullName,
            @NotBlank(message = "El correo es obligatorio.")
            @Email(message = "El correo no es válido.")
            @Size(max = User.EMAIL_MAX_LENGTH, message = "Es demasiado largo.")
            String email,
            @NotBlank(message = "La contraseña es obligatoria.")
            @Size(min = 8, max = 72, message = "Debe tener entre {min} y {max} caracteres.")
            String password) {}

    record UserResponse(UUID id, String fullName, String email, String role, boolean active) {

        static UserResponse from(User user) {
            return new UserResponse(user.id(), user.fullName(), user.email(), user.role().name(), user.isActive());
        }
    }
}
