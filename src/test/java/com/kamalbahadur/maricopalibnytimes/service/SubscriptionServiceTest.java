package com.kamalbahadur.maricopalibnytimes.service;

import com.kamalbahadur.maricopalibnytimes.config.NyTimesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    private SubscriptionService subscriptionService;

    @BeforeEach
    void setUp() {
        NyTimesProperties properties = new NyTimesProperties();
        properties.setRedeemUrl("https://api.nytimes.com/svc/subscription/redeem");
        properties.setCampaignId("87LH8");
        properties.setGiftCode("gift-code");

        NyTimesProperties.OAuth2 oauth2 = new NyTimesProperties.OAuth2();
        oauth2.setRegistrationId("google");
        oauth2.setPrincipalName("test@example.com");
        properties.setOauth2(oauth2);

        subscriptionService = new SubscriptionService(restTemplate, authorizedClientManager, authorizedClientService, properties);
    }

    @Test
    void redeemAllAccessReturnsHelpfulMessageWhenTokenIsMissing() {
        when(authorizedClientService.loadAuthorizedClient("google", "test@example.com")).thenReturn(null);
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

        String response = subscriptionService.redeemAllAccess();

        assertThat(response).contains("Failed to retrieve access token");
    }

    @Test
    void redeemAllAccessCallsRedeemEndpointWhenTokenExists() {
        OAuth2AuthorizedClient client = authorizedClient();
        when(authorizedClientService.loadAuthorizedClient("google", "test@example.com")).thenReturn(client);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK).body("ok"));

        String response = subscriptionService.redeemAllAccess();

        assertThat(response).contains("Subscription successful");
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
        verify(authorizedClientManager, never()).authorize(any(OAuth2AuthorizeRequest.class));
    }

    @Test
    void redeemAllAccessUsesAuthenticatedPrincipalFromRequest() {
        OAuth2AuthorizedClient client = authorizedClient("integration-user");
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated("integration-user", "N/A", java.util.List.of());

        when(authorizedClientService.loadAuthorizedClient("google", "integration-user")).thenReturn(client);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK).body("ok"));

        String response = subscriptionService.redeemAllAccess(authentication);

        assertThat(response).contains("Subscription successful");
        verify(authorizedClientService).loadAuthorizedClient("google", "integration-user");
        verify(authorizedClientManager, never()).authorize(any(OAuth2AuthorizeRequest.class));
    }

    private OAuth2AuthorizedClient authorizedClient() {
        return authorizedClient("test@example.com");
    }

    private OAuth2AuthorizedClient authorizedClient(String principalName) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("client-id")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .redirectUri("http://localhost/login/oauth2/code/google")
                .scope("openid")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300)
        );

        return new OAuth2AuthorizedClient(registration, principalName, accessToken);
    }
}

