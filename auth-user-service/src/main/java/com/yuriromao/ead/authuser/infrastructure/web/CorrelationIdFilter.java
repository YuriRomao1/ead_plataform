package com.yuriromao.ead.authuser.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Adds a correlation id to each HTTP request, response, and log MDC. */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = resolveCorrelationId(request);

    response.setHeader(CorrelationId.HEADER_NAME, correlationId);
    MDC.put(CorrelationId.MDC_KEY, correlationId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(CorrelationId.MDC_KEY);
    }
  }

  private static String resolveCorrelationId(HttpServletRequest request) {
    String correlationId = request.getHeader(CorrelationId.HEADER_NAME);

    if (correlationId == null || correlationId.isBlank()) {
      return UUID.randomUUID().toString();
    }

    return correlationId;
  }
}
