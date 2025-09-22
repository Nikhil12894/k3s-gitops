package com.nk.observability;

import java.util.Random;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
public class HelloController {

  private final RestTemplate restTemplate;
  private final Random random = new Random();

  @GetMapping("/hello")
  public String hello() throws InterruptedException {
    // simulate variable latency
    Thread.sleep(100 + random.nextInt(400));
    log.info("Saying hello!");
    String quote = restTemplate.getForObject("https://api.animechan.io/v1/quotes/random", String.class);
    log.info("Fetched quote: {}", quote);
    return "Hello, OpenTelemetry!";
  }

  @GetMapping("/error")
  public String error() {
    String quote = restTemplate.getForObject("https://api.animechan.io/v1/quotes/random", String.class);
    log.info("Fetched quote: {}", quote);
    log.error("Simulating an error...");
    // simulate error endpoint
    throw new RuntimeException("Simulated error for tracing!");
  }
}
