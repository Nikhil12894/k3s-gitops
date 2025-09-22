package com.nk.observability;

import java.io.IOException;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {

    // --- MDC population ---
    String correlationId = request.getHeader("correlationId");
    String transactionId = request.getHeader("transactionId");
    String xyz = request.getHeader("xyz");

    if (correlationId != null)
      MDC.put("correlationId", correlationId);
    if (transactionId != null)
      MDC.put("transactionId", transactionId);

    // --- Log request details ---
    StringBuilder headers = new StringBuilder();
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String name = headerNames.nextElement();
      headers.append(name).append("=").append(request.getHeader(name)).append(", ");
    }

    log.info("Incoming request: method={}, uri={}, headers={}", request.getMethod(), request.getRequestURI(), headers);

    return true; // continue processing
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

    try {
      // --- Log response status ---
      log.info("Outgoing response: status={}", response.getStatus());

      // --- Log exception if any ---
      if (ex != null) {
        log.error("Exception occurred during request", ex);
      }
    } finally {
      // --- Clear MDC ---
      MDC.clear();
    }
  }
}
