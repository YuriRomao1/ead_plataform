package com.yuriromao.ead.authuser.infrastructure.web;

import com.yuriromao.ead.authuser.application.usecase.CreateUserCommand;
import com.yuriromao.ead.authuser.domain.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request body for public user registration.")
public record CreateUserRequest(
    @Schema(example = "User Name") @NotBlank(message = "USER_NAME_REQUIRED") String name,
    @Schema(example = "user@email.com")
        @NotBlank(message = "USER_EMAIL_REQUIRED")
        @Email(message = "USER_EMAIL_INVALID")
        String email,
    @Schema(example = "plainPassword") @NotBlank(message = "USER_PASSWORD_REQUIRED")
        String password,
    @NotEmpty(message = "USER_ROLE_REQUIRED")
        @Schema(description = "Public registration currently accepts only STUDENT.")
        Set<@NotNull(message = "USER_ROLE_INVALID") UserRole> roles) {

  /** Converts the HTTP request shape into the application-layer command. */
  CreateUserCommand toCommand() {
    return new CreateUserCommand(name, email, password, roles);
  }
}
