package com.kamalbahadur.maricopalibnytimes.config;

import com.kamalbahadur.maricopalibnytimes.service.BrowserRenewalResult;
import com.kamalbahadur.maricopalibnytimes.service.RenewalAutomationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BrowserAutomationCommandRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BrowserAutomationCommandRunner.class);

    private final RenewalAutomationService renewalAutomationService;
    private final NyTimesProperties properties;
    private final ApplicationContext applicationContext;

    public BrowserAutomationCommandRunner(
            RenewalAutomationService renewalAutomationService,
            NyTimesProperties properties,
            ApplicationContext applicationContext
    ) {
        this.renewalAutomationService = renewalAutomationService;
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        String command = properties.getBrowser().getCommand();
        if (!StringUtils.hasText(command)) {
            return;
        }

        BrowserRenewalResult result;
        switch (command.trim().toLowerCase()) {
            case "bootstrap" -> result = renewalAutomationService.bootstrapSession();
            case "renew-once" -> result = renewalAutomationService.renewOnce();
            default -> {
                log.error("Unknown nytimes.browser.command value: {}", command);
                exit(1);
                return;
            }
        }

        if (result.isSuccessful()) {
            log.info(result.message());
            exit(0);
            return;
        }

        log.warn(result.message());
        exit(1);
    }

    private void exit(int code) {
        Thread shutdownThread = new Thread(() -> {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            System.exit(org.springframework.boot.SpringApplication.exit(applicationContext, () -> code));
        });
        shutdownThread.setDaemon(false);
        shutdownThread.start();
    }
}

