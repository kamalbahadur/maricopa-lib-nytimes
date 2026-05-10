package com.kamalbahadur.maricopalibnytimes.job;

import com.kamalbahadur.maricopalibnytimes.service.BrowserRenewalResult;
import com.kamalbahadur.maricopalibnytimes.service.RenewalAutomationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyJobTest {

    @Mock
    private RenewalAutomationService renewalAutomationService;

    @Test
    void scheduledJobInvokesBrowserAutomationRenewal() {
        when(renewalAutomationService.renewOnce())
                .thenReturn(new BrowserRenewalResult(BrowserRenewalResult.Status.SUCCESS, "ok"));

        DailyJob dailyJob = new DailyJob(renewalAutomationService);
        dailyJob.runDailyJob();

        verify(renewalAutomationService).renewOnce();
    }
}

