package com.nk.observability;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(HttpServletRequest request, Exception ex) {
    // Re-populate MDC from headers if needed
    // MDC.put("correlationId", request.getHeader("correlationId"));
    // MDC.put("transactionId", request.getHeader("transactionId"));

    log.error("Exception handled in controller advice", ex);

    // MDC.clear();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
  }
}
