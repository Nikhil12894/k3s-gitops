// package com.nk.observability;

// import java.io.IOException;

// import org.slf4j.MDC;
// import org.springframework.http.HttpRequest;
// import org.springframework.http.client.ClientHttpRequestExecution;
// import org.springframework.http.client.ClientHttpRequestInterceptor;
// import org.springframework.http.client.ClientHttpResponse;

// public class CorrelationRestTemplateInterceptor implements
// ClientHttpRequestInterceptor {
// @Override
// public ClientHttpResponse intercept(HttpRequest request, byte[] body,
// ClientHttpRequestExecution execution) throws IOException {
// String correlationId = MDC.get("correlationId");
// String transactionId = MDC.get("transactionId");

// if (correlationId != null) {
// request.getHeaders().add("correlationId", correlationId);
// }
// if (transactionId != null) {
// request.getHeaders().add("transactionId", transactionId);
// }

// return execution.execute(request, body);
// }
// }
