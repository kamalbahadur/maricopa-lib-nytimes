package com.kamalbahadur.maricopalibnytimes.controller;

import com.kamalbahadur.maricopalibnytimes.service.BrowserRenewalResult;
import com.kamalbahadur.maricopalibnytimes.service.RenewalAutomationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NYTimesController {

    private final RenewalAutomationService renewalAutomationService;

    public NYTimesController(RenewalAutomationService renewalAutomationService) {
        this.renewalAutomationService = renewalAutomationService;
    }

    @GetMapping("/renew/trigger")
    public String triggerRenewNow() {
        BrowserRenewalResult result = renewalAutomationService.renewOnce();
        return "Manual renewal triggered: " + result.message();
    }

    @GetMapping("/renew")
    public String renew() {
        // Backward-compatible alias for the explicit trigger endpoint.
        return triggerRenewNow();
    }
}