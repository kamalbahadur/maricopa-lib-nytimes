package com.kamalbahadur.maricopalibnytimes.service;

import com.kamalbahadur.maricopalibnytimes.config.NyTimesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final RestTemplate restTemplate;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final NyTimesProperties properties;

    public SubscriptionService(
            RestTemplate restTemplate,
            OAuth2AuthorizedClientManager authorizedClientManager,
            NyTimesProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.authorizedClientManager = authorizedClientManager;
        this.properties = properties;
    }

    public String redeemAllAccess() {
        String accessToken = getAccessToken();
        if (!StringUtils.hasText(accessToken)) {
            return "Failed to retrieve access token. Login once with Google OAuth2 to cache an authorized client.";
        }

        String url = UriComponentsBuilder.fromUriString(properties.getRedeemUrl())
                .queryParam("campaignId", properties.getCampaignId())
                .queryParam("gift_code", properties.getGiftCode())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            HttpStatusCode statusCode = response.getStatusCode();
            if (statusCode.is2xxSuccessful()) {
                log.info("NYTimes subscription redemption succeeded. status={}", statusCode.value());
                return "Subscription successful";
            }

            log.warn("NYTimes redemption returned non-success status={}", statusCode.value());
            return "Subscription failed with status: " + statusCode.value();
        } catch (RestClientResponseException ex) {
            // Downstream HTTP failures are expected operational outcomes; log concise details without stacktrace noise.
            log.warn("NYTimes redemption failed. status={} body={}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            return "Subscription failed with status: " + ex.getRawStatusCode();
        } catch (Exception ex) {
            log.error("Unexpected error during NYTimes redemption", ex);
            return "Subscription failed due to an unexpected error";
        }
    }

    private String getAccessToken() {
        if (!StringUtils.hasText(properties.getOauth2().getRegistrationId())
                || !StringUtils.hasText(properties.getOauth2().getPrincipalName())) {
            log.warn("Missing nytimes.oauth2.registration-id or nytimes.oauth2.principal-name configuration.");
            return null;
        }

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(properties.getOauth2().getRegistrationId())
                .principal(properties.getOauth2().getPrincipalName())
                .build();

        OAuth2AuthorizedClient client = authorizedClientManager.authorize(authorizeRequest);
        if (client == null || client.getAccessToken() == null) {
            log.warn("OAuth2 authorized client not available for principal={}", properties.getOauth2().getPrincipalName());
            return null;
        }

        return client.getAccessToken().getTokenValue();
    }
}
