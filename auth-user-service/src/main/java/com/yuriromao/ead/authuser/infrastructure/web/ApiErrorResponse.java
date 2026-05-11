package com.yuriromao.ead.authuser.infrastructure.web;

import java.util.List;
import java.util.Objects;

public record ApiErrorResponse(String code, String message, List<String> details) {

  public ApiErrorResponse {
    code = Objects.requireNonNull(code, "code must not be null");
    message = Objects.requireNonNull(message, "message must not be null");
    details = List.copyOf(Objects.requireNonNull(details, "details must not be null"));
  }

  static ApiErrorResponse of(String code, String message) {
    return new ApiErrorResponse(code, message, List.of());
  }
}
