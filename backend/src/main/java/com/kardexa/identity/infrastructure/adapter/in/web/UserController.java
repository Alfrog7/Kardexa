package com.kardexa.identity.infrastructure.adapter.in.web;

import com.kardexa.identity.application.port.in.ManageUsersUseCase;
import com.kardexa.identity.application.port.in.ManageUsersUseCase.CreateUserCommand;
import com.kardexa.identity.application.port.in.ManageUsersUseCase.UpdateUserCommand;
import com.kardexa.identity.domain.model.Role;
import com.kardexa.identity.domain.model.User;
import com.kardexa.shared.application.PageQuery;
import com.kardexa.shared.application.ResultPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** {@code /api/v1/users} según el contrato {@code docs/api/openapi.yaml} (HU-03). Solo {@code ADMIN}. */
@RestController
@RequestMapping("/users")
class UserController {

    private final ManageUsersUseCase manageUsers;

    UserController(ManageUsersUseCase manageUsers) {
        this.manageUsers = manageUsers;
    }

    @GetMapping
    UserPageResponse list(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return UserPageResponse.from(manageUsers.list(PageQuery.of(page, size)));
    }

    @PostMapping
    ResponseEntity<AuthController.UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User created = manageUsers.create(
                new CreateUserCommand(request.fullName(), request.email(), request.password(), request.role()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(AuthController.UserResponse.from(created));
    }

    @PatchMapping("/{id}")
    AuthController.UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return AuthController.UserResponse.from(
                manageUsers.update(new UpdateUserCommand(id, request.fullName(), request.role(), request.active())));
    }

    record CreateUserRequest(
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
            String password,
            @NotNull(message = "El rol es obligatorio.") Role role) {}

    record UpdateUserRequest(
            @Size(min = User.NAME_MIN_LENGTH, max = User.NAME_MAX_LENGTH,
                    message = "Debe tener entre {min} y {max} caracteres.")
            String fullName,
            Role role,
            Boolean active) {}

    record UserPageResponse(
            List<AuthController.UserResponse> content, int page, int size, long totalElements, int totalPages) {

        static UserPageResponse from(ResultPage<User> page) {
            return new UserPageResponse(page.content().stream().map(AuthController.UserResponse::from).toList(),
                    page.page(), page.size(), page.totalElements(), page.totalPages());
        }
    }
}
