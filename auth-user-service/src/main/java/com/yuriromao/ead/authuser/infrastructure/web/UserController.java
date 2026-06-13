package com.yuriromao.ead.authuser.infrastructure.web;

import com.yuriromao.ead.authuser.application.usecase.CreateUserResult;
import com.yuriromao.ead.authuser.application.usecase.CreateUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HTTP adapter for user resources.
 *
 * <p>The controller only translates the HTTP contract to the application use case and keeps user
 * creation rules outside the web layer.
 */
@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Public user registration API.")
public class UserController {

  private final CreateUserUseCase createUserUseCase;

  public UserController(CreateUserUseCase createUserUseCase) {
    this.createUserUseCase =
        Objects.requireNonNull(createUserUseCase, "createUserUseCase must not be null");
  }

  /** Handles POST /users by delegating creation to the application use case. */
  @Operation(
      summary = "Create a public user account",
      description = "Creates an ACTIVE user and records UserCreated in the transactional outbox.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "User created.",
        content = @Content(schema = @Schema(implementation = UserResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request data.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Email already exists.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "Unexpected internal failure.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  @PostMapping
  public ResponseEntity<UserResponse> createUser(
      @Valid @RequestBody CreateUserRequest request, UriComponentsBuilder uriBuilder) {
    CreateUserResult result = createUserUseCase.execute(request.toCommand());
    URI location = uriBuilder.path("/users/{id}").buildAndExpand(result.id()).toUri();

    return ResponseEntity.created(location).body(UserResponse.from(result));
  }
}
