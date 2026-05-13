package com.yuriromao.ead.authuser.infrastructure.web;

import com.yuriromao.ead.authuser.application.usecase.CreateUserCommand;
import com.yuriromao.ead.authuser.domain.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * HTTP request body for POST /users.
 *
 * <p>Bean Validation annotations enforce the public contract before the request is translated into
 * the application command.
 */
public record CreateUserRequest(
    @NotBlank(message = "USER_NAME_REQUIRED") String name,
    @NotBlank(message = "USER_EMAIL_REQUIRED") @Email(message = "USER_EMAIL_INVALID") String email,
    @NotBlank(message = "USER_PASSWORD_REQUIRED") String password,
    @NotEmpty(message = "USER_ROLE_REQUIRED")
        Set<@NotNull(message = "USER_ROLE_INVALID") UserRole> roles) {

  /** Converts the HTTP request shape into the application-layer command. */
  CreateUserCommand toCommand() {
    return new CreateUserCommand(name, email, password, roles);
  }
}
