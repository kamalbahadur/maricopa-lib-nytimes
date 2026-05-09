package com.kamalbahadur.maricopalibnytimes.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "nytimes.oauth2.registration-id=google",
        "nytimes.oauth2.principal-name=test@example.com",
        "nytimes.redeem-url=https://www.nytimes.com/subscription/redeem/all-access",
        "nytimes.campaign-id=87LH8",
        "nytimes.gift-code=gift-code"
})
class NYTimesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @MockBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("ok"));
    }

    @Test
    void renewRedirectsAuthenticatedUserToNyTimesRedeemPage() throws Exception {
        mockMvc.perform(get("/renew").with(user("integration-user")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://www.nytimes.com/subscription/redeem/all-access?campaignId=87LH8&gift_code=gift-code"));
    }

    @Test
    void renewTriggerEndpointRedirectsAuthenticatedUserToNyTimesRedeemPage() throws Exception {
        mockMvc.perform(get("/renew/trigger").with(user("integration-user")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://www.nytimes.com/subscription/redeem/all-access?campaignId=87LH8&gift_code=gift-code"));
    }

    @Test
    void serverSideRedeemStillReturnsDownstreamErrorMessageWhenNyTimesFails() {
        when(authorizedClientService.loadAuthorizedClient("google", "integration-user"))
                .thenReturn(authorizedClient("integration-user"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "NYTimes error"));

        String result = new com.kamalbahadur.maricopalibnytimes.service.SubscriptionService(
                restTemplate,
                authorizedClientManager,
                authorizedClientService,
                nyTimesProperties()
        ).redeemAllAccess(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated("integration-user", "N/A", java.util.List.of()));

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo("Subscription failed with status: 500");
    }

    private com.kamalbahadur.maricopalibnytimes.config.NyTimesProperties nyTimesProperties() {
        com.kamalbahadur.maricopalibnytimes.config.NyTimesProperties properties = new com.kamalbahadur.maricopalibnytimes.config.NyTimesProperties();
        properties.setRedeemUrl("https://www.nytimes.com/subscription/redeem/all-access");
        properties.setCampaignId("87LH8");
        properties.setGiftCode("gift-code");

        com.kamalbahadur.maricopalibnytimes.config.NyTimesProperties.OAuth2 oauth2 = new com.kamalbahadur.maricopalibnytimes.config.NyTimesProperties.OAuth2();
        oauth2.setRegistrationId("google");
        oauth2.setPrincipalName("test@example.com");
        properties.setOauth2(oauth2);
        return properties;
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

