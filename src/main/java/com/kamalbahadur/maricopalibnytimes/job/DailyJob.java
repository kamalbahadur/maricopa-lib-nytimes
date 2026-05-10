package com.kamalbahadur.maricopalibnytimes.job;

import com.kamalbahadur.maricopalibnytimes.service.BrowserRenewalResult;
import com.kamalbahadur.maricopalibnytimes.service.RenewalAutomationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyJob {

    private static final Logger log = LoggerFactory.getLogger(DailyJob.class);

    private final RenewalAutomationService renewalAutomationService;

    public DailyJob(RenewalAutomationService renewalAutomationService) {
        this.renewalAutomationService = renewalAutomationService;
    }

    @Scheduled(cron = "0 0 0 * * ?") // Every day at midnight
    public void runDailyJob() {
        log.info("Running NYTimes daily subscription job");
        BrowserRenewalResult result = renewalAutomationService.renewOnce();
        log.info("NYTimes daily subscription job result: status={} message={}", result.status(), result.message());
    }
}