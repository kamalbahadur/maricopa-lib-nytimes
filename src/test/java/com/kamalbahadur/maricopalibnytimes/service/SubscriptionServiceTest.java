package com.kamalbahadur.maricopalibnytimes.service;
import com.kamalbahadur.maricopalibnytimes.config.NyTimesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class SubscriptionServiceTest {
    private SubscriptionService subscriptionService;
    @BeforeEach
    void setUp() {
        NyTimesProperties properties = new NyTimesProperties();
        properties.setRedeemUrl("https://www.nytimes.com/subscription/redeem/all-access");
        properties.setCampaignId("87LH8");
        properties.setGiftCode("gift-code");
        subscriptionService = new SubscriptionService(properties);
    }
    @Test
    void buildRedeemUrlIncludesCampaignAndGiftCode() {
        String url = subscriptionService.buildRedeemUrl();
        assertThat(url).isEqualTo("https://www.nytimes.com/subscription/redeem/all-access?campaignId=87LH8&gift_code=gift-code");
    }
    @Test
    void buildReminderMessageContainsRedeemUrl() {
        String message = subscriptionService.buildReminderMessage();
        assertThat(message).contains("NYTimes renewal reminder")
                .contains("https://www.nytimes.com/subscription/redeem/all-access?campaignId=87LH8&gift_code=gift-code");
    }
    @Test
    void buildRedeemUrlRejectsNonNyTimesHosts() {
        NyTimesProperties properties = new NyTimesProperties();
        properties.setRedeemUrl("https://example.com/redeem");
        properties.setCampaignId("87LH8");
        properties.setGiftCode("gift-code");
        SubscriptionService service = new SubscriptionService(properties);
        assertThatThrownBy(service::buildRedeemUrl)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nytimes.com");
    }
}
