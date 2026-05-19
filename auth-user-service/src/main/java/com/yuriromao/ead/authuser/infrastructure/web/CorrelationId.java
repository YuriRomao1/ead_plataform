package com.yuriromao.ead.authuser.infrastructure.web;

import org.slf4j.MDC;

/** Shared correlation-id names used by HTTP responses and logging MDC. */
public final class CorrelationId {

  public static final String HEADER_NAME = "X-Correlation-Id";
  public static final String MDC_KEY = "correlationId";

  private CorrelationId() {}

  /** Returns the current request correlation id from MDC, when available. */
  static String current() {
    return MDC.get(MDC_KEY);
  }
}
