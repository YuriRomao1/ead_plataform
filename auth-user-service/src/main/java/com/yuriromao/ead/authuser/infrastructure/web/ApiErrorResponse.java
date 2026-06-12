package com.yuriromao.ead.authuser.infrastructure.web;

import java.util.List;
import java.util.Objects;

/**
 * Public HTTP error response contract.
 *
 * <p>The response exposes stable machine-readable codes and client-safe messages, never stack
 * traces or internal persistence/messaging details.
 */
public record ApiErrorResponse(
    String code, String message, String correlationId, List<String> details) {

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
