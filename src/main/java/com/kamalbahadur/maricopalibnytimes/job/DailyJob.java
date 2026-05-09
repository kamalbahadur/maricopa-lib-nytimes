package com.kamalbahadur.maricopalibnytimes.job;

import com.kamalbahadur.maricopalibnytimes.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyJob {

    private static final Logger log = LoggerFactory.getLogger(DailyJob.class);

    private final SubscriptionService service;

    public DailyJob(SubscriptionService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 0 * * ?") // Every day at midnight
    public void runDailyJob() {
        log.info("Running NYTimes daily subscription job");
        String result = service.redeemAllAccess();
        log.info("NYTimes daily subscription job result: {}", result);
    }
}