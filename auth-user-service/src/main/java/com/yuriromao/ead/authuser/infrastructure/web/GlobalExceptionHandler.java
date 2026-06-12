package com.yuriromao.ead.authuser.infrastructure.web;

import com.yuriromao.ead.authuser.application.exception.UserEmailAlreadyExistsException;
import com.yuriromao.ead.authuser.application.exception.UserRoleNotAllowedForPublicRegistrationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps expected application and request errors to the public HTTP error format.
 *
 * <p>This keeps controllers focused on request orchestration and prevents internal exception
 * details from leaking to API clients.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String INTERNAL_ERROR = "INTERNAL_ERROR";

  private static final Map<String, String> ERROR_MESSAGES =
      Map.of(
          "USER_NAME_REQUIRED",
          "Name is required.",
          "USER_EMAIL_REQUIRED",
          "Email is required.",
          "USER_EMAIL_INVALID",
          "Email must be valid.",
          "USER_PASSWORD_REQUIRED",
          "Password is required.",
          "USER_ROLE_REQUIRED",
          "At least one role is required.",
          "USER_ROLE_INVALID",
          "Role is invalid.",
          UserRoleNotAllowedForPublicRegistrationException.CODE,
          "Role is not allowed for public registration.",
          UserEmailAlreadyExistsException.CODE,
          "Email already exists.",
          INTERNAL_ERROR,
          "Internal error.");

  /** Converts Bean Validation failures into the first matching public validation error code. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
    String code =
        exception.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getDefaultMessage())
            .filter(ERROR_MESSAGES::containsKey)
            .findFirst()
            .orElse(INTERNAL_ERROR);

    return error(HttpStatus.BAD_REQUEST, code);
  }

  /** Handles malformed JSON or enum conversion failures in the request body. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiErrorResponse> handleUnreadableRequest() {
    return error(HttpStatus.BAD_REQUEST, "USER_ROLE_INVALID");
  }

  /** Maps the duplicate-email business exception to HTTP 409 Conflict. */
  @ExceptionHandler(UserEmailAlreadyExistsException.class)
  ResponseEntity<ApiErrorResponse> handleDuplicateEmail() {
    return error(HttpStatus.CONFLICT, UserEmailAlreadyExistsException.CODE);
  }

  /** Maps non-STUDENT public registration requests to HTTP 400 Bad Request. */
  @ExceptionHandler(UserRoleNotAllowedForPublicRegistrationException.class)
  ResponseEntity<ApiErrorResponse> handleRoleNotAllowedForPublicRegistration() {
    return error(HttpStatus.BAD_REQUEST, UserRoleNotAllowedForPublicRegistrationException.CODE);
  }

  /** Provides a safe fallback response for unexpected failures. */
  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> handleUnexpected() {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR);
  }

  private static ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code) {
    return ResponseEntity.status(status)
        .body(ApiErrorResponse.of(code, ERROR_MESSAGES.get(code), CorrelationId.current()));
  }
}
