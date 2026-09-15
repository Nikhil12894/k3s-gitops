package com.explorewithnk.springapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/secure")
public class SecuredController {

    private static final Logger log = LoggerFactory.getLogger(SecuredController.class);

    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getSecuredData(@AuthenticationPrincipal Jwt jwt) {
        log.info("HTTP GET /api/secure/data: Authenticated request for subject '{}'", jwt != null ? jwt.getSubject() : "anonymous");
        
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "SUCCESS");
        res.put("message", "Access granted to Keycloak protected resource");
        res.put("timestamp", Instant.now().toString());

        if (jwt != null) {
            res.put("subject", jwt.getSubject());
            res.put("claims", jwt.getClaims());
            res.put("issuer", jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
        }

        return ResponseEntity.ok(res);
    }
}
