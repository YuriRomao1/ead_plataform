package com.yuriromao.ead.authuser.application.exception;

/** Business exception used when public registration requests a non-STUDENT role. */
public class UserRoleNotAllowedForPublicRegistrationException extends RuntimeException {

  public static final String CODE = "USER_ROLE_NOT_ALLOWED_FOR_PUBLIC_REGISTRATION";

  public UserRoleNotAllowedForPublicRegistrationException() {
    super("Role is not allowed for public registration.");
  }
}
