package com.kamalbahadur.maricopalibnytimes.controller;

import com.kamalbahadur.maricopalibnytimes.service.SubscriptionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NYTimesController {

    private final SubscriptionService service;

    public NYTimesController(SubscriptionService service) {
        this.service = service;
    }

    @GetMapping("/renew/trigger")
    public String triggerRenewNow(Authentication authentication) {
        return "Manual renewal triggered: " + service.redeemAllAccess(authentication);
    }

    @GetMapping("/renew")
    public String renew(Authentication authentication) {
        // Backward-compatible alias for the explicit trigger endpoint.
        return triggerRenewNow(authentication);
    }
}