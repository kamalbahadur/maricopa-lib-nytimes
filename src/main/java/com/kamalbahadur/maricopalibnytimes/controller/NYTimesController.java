package com.kamalbahadur.maricopalibnytimes.controller;

import com.kamalbahadur.maricopalibnytimes.service.SubscriptionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NYTimesController {

    private final SubscriptionService service;

    public NYTimesController(SubscriptionService service) {
        this.service = service;
    }

    @GetMapping("/renew/trigger")
    public ResponseEntity<Void> triggerRenewNow() {
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, service.buildRedeemUri().toString())
                .build();
    }

    @GetMapping("/renew")
    public ResponseEntity<Void> renew() {
        return triggerRenewNow();
    }

    @GetMapping("/renew/url")
    public String renewUrl() {
        return service.buildRedeemUrl();
    }
}