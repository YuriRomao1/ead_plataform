package com.yuriromao.ead.authuser.infrastructure.web;

import com.yuriromao.ead.authuser.application.usecase.CreateUserResult;
import com.yuriromao.ead.authuser.application.usecase.CreateUserUseCase;
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
public class UserController {

  private final CreateUserUseCase createUserUseCase;

  public UserController(CreateUserUseCase createUserUseCase) {
    this.createUserUseCase =
        Objects.requireNonNull(createUserUseCase, "createUserUseCase must not be null");
  }

  @PostMapping
  public ResponseEntity<UserResponse> createUser(
      @Valid @RequestBody CreateUserRequest request, UriComponentsBuilder uriBuilder) {
    CreateUserResult result = createUserUseCase.execute(request.toCommand());
    URI location = uriBuilder.path("/users/{id}").buildAndExpand(result.id()).toUri();

    return ResponseEntity.created(location).body(UserResponse.from(result));
  }
}
