// package com.nk.observability;

// import java.io.IOException;
// import java.util.UUID;

// import org.slf4j.MDC;
// import org.springframework.stereotype.Component;
// import org.springframework.web.filter.OncePerRequestFilter;

// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import lombok.extern.slf4j.Slf4j;

// @Component
// @Slf4j
// public class LoggingFilter extends OncePerRequestFilter {

// private static final String CORRELATION_ID = "correlationId";
// private static final String TRANSACTION_ID = "transactionId";

// @Override
// protected void doFilterInternal(HttpServletRequest request,
// HttpServletResponse response,
// FilterChain filterChain)
// throws ServletException, IOException {

// try {
// // Extract or generate correlationId
// String correlationId = request.getHeader(CORRELATION_ID);
// if (correlationId == null) {
// correlationId = UUID.randomUUID().toString();
// }

// // Generate new transactionId for each request
// String transactionId = UUID.randomUUID().toString();

// // Put into MDC so every log line includes them
// MDC.put(CORRELATION_ID, correlationId);
// MDC.put(TRANSACTION_ID, transactionId);

// log.info("➡️ Incoming Request: {} {} from {}",
// request.getMethod(), request.getRequestURI(), request.getRemoteAddr());

// filterChain.doFilter(request, response);

// log.info("⬅️ Response: {} {} - Status: {}",
// request.getMethod(), request.getRequestURI(), response.getStatus());

// } finally {
// // Clean up MDC
// MDC.clear();
// }
// }
// }
