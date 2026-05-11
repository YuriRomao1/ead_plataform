package com.yuriromao.ead.authuser.application.exception;

import java.util.Objects;

/**
 * Business exception used when the auth-user-service email uniqueness invariant is violated before
 * persisting a new user.
 */
public class UserEmailAlreadyExistsException extends RuntimeException {

  public static final String CODE = "USER_EMAIL_ALREADY_EXISTS";

  private final String email;

  public UserEmailAlreadyExistsException(String email) {
    super("Email already exists.");
    this.email = Objects.requireNonNull(email, "email must not be null");
  }

  public String getEmail() {
    return email;
  }
}
