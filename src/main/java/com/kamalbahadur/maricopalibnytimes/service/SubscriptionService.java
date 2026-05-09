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
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
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
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final NyTimesProperties properties;

    public SubscriptionService(
            RestTemplate restTemplate,
            OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2AuthorizedClientService authorizedClientService,
            NyTimesProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.authorizedClientManager = authorizedClientManager;
        this.authorizedClientService = authorizedClientService;
        this.properties = properties;
    }

    public String redeemAllAccess() {
        return redeemAllAccess(null);
    }

    public String redeemAllAccess(Authentication authentication) {
        String accessToken = getAccessToken(authentication);
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
            log.warn("NYTimes redemption failed. status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
            return "Subscription failed with status: " + ex.getStatusCode().value();
        } catch (Exception ex) {
            log.error("Unexpected error during NYTimes redemption", ex);
            return "Subscription failed due to an unexpected error";
        }
    }

    private String getAccessToken(Authentication authentication) {
        if (!StringUtils.hasText(properties.getOauth2().getRegistrationId())) {
            log.warn("Missing nytimes.oauth2.registration-id configuration.");
            return null;
        }

        Authentication effectivePrincipal = resolvePrincipal(authentication);
        if (effectivePrincipal == null) {
            return null;
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                properties.getOauth2().getRegistrationId(),
                effectivePrincipal.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            client = authorizeClient(effectivePrincipal);
        }

        if (client == null || client.getAccessToken() == null) {
            log.warn("OAuth2 authorized client not available for principal={}", effectivePrincipal.getName());
            return null;
        }

        return client.getAccessToken().getTokenValue();
    }

    private Authentication resolvePrincipal(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication;
        }

        if (!StringUtils.hasText(properties.getOauth2().getPrincipalName())) {
            log.warn("Missing nytimes.oauth2.principal-name configuration.");
            return null;
        }

        return UsernamePasswordAuthenticationToken.authenticated(
                properties.getOauth2().getPrincipalName(),
                "N/A",
                AuthorityUtils.createAuthorityList("ROLE_USER")
        );
    }

    private OAuth2AuthorizedClient authorizeClient(Authentication principal) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(properties.getOauth2().getRegistrationId())
                .principal(principal)
                .build();

        try {
            return authorizedClientManager.authorize(authorizeRequest);
        } catch (Exception ex) {
            log.warn("OAuth2 authorization attempt failed for principal={}: {}", principal.getName(), ex.getMessage());
            return null;
        }
    }
}
