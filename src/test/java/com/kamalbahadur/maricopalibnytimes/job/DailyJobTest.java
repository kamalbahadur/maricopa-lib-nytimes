package com.kamalbahadur.maricopalibnytimes.job;

import com.kamalbahadur.maricopalibnytimes.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyJobTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Test
    void scheduledJobBuildsReminderMessage() {
        when(subscriptionService.buildReminderMessage()).thenReturn("reminder");

        DailyJob dailyJob = new DailyJob(subscriptionService);
        dailyJob.runDailyJob();

        verify(subscriptionService).buildReminderMessage();
    }
}
