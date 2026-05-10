package com.kamalbahadur.maricopalibnytimes.service;

import com.kamalbahadur.maricopalibnytimes.config.NyTimesProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class SubscriptionService {

    private final NyTimesProperties properties;

    public SubscriptionService(NyTimesProperties properties) {
        this.properties = properties;
    }

    public String buildRedeemUrl() {
        URI baseUri = URI.create(properties.getRedeemUrl());
        if (!"https".equalsIgnoreCase(baseUri.getScheme()) || !baseUri.getHost().endsWith("nytimes.com")) {
            throw new IllegalArgumentException("NYTIMES_REDEEM_URL must be an https nytimes.com URL");
        }

        return UriComponentsBuilder.fromUri(baseUri)
                .queryParam("campaignId", properties.getCampaignId())
                .queryParam("gift_code", properties.getGiftCode())
                .toUriString();
    }

    public URI buildRedeemUri() {
        return URI.create(buildRedeemUrl());
    }

    public String buildReminderMessage() {
        return "NYTimes renewal reminder: open " + buildRedeemUrl() + " in a browser and complete the redeem flow manually.";
    }
}
