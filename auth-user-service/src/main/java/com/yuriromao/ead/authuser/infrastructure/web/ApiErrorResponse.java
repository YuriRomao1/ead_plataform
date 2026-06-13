package com.yuriromao.ead.authuser.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * Public HTTP error response contract.
 *
 * <p>The response exposes stable machine-readable codes and client-safe messages, never stack
 * traces or internal persistence/messaging details.
 */
@Schema(description = "Standard error response for Auth/User HTTP API failures.")
public record ApiErrorResponse(
    @Schema(example = "USER_EMAIL_ALREADY_EXISTS") String code,
    @Schema(example = "Email already exists.") String message,
    @Schema(example = "request-correlation-id") String correlationId,
    List<String> details) {

  public ApiErrorResponse {
    code = Objects.requireNonNull(code, "code must not be null");
    message = Objects.requireNonNull(message, "message must not be null");
    details = List.copyOf(Objects.requireNonNull(details, "details must not be null"));
  }

  /** Creates an error response without field-level details. */
  static ApiErrorResponse of(String code, String message, String correlationId) {
    return new ApiErrorResponse(code, message, correlationId, List.of());
  }
}
