package com.explorewithnk.appcore.controller;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/secure")
public class SecuredController {

    @Inject
    SecurityIdentity securityIdentity;

    @GetMapping("/data")
    @Authenticated
    public ResponseEntity<Map<String, Object>> getSecuredData() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("message", "Access granted to secured resource via Keycloak OIDC");
        res.put("timestamp", Instant.now().toString());
        res.put("principal", securityIdentity.getPrincipal().getName());
        res.put("roles", securityIdentity.getRoles());
        res.put("isAnonymous", securityIdentity.isAnonymous());
        res.put("attributes", securityIdentity.getAttributes());

        return ResponseEntity.ok(res);
    }
}
