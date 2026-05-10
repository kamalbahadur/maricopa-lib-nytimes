package com.kamalbahadur.maricopalibnytimes.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "nytimes.redeem-url=https://www.nytimes.com/subscription/redeem/all-access",
        "nytimes.campaign-id=87LH8",
        "nytimes.gift-code=gift-code"
})
class NYTimesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void renewEndpointRedirectsToNyTimesRedeemLink() throws Exception {
        mockMvc.perform(get("/renew"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://www.nytimes.com/subscription/redeem/all-access?campaignId=87LH8&gift_code=gift-code"));
    }

    @Test
    void renewTriggerEndpointRedirectsToNyTimesRedeemLink() throws Exception {
        mockMvc.perform(get("/renew/trigger"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://www.nytimes.com/subscription/redeem/all-access?campaignId=87LH8&gift_code=gift-code"));
    }

    @Test
    void renewUrlEndpointReturnsPlainRedeemLink() throws Exception {
        mockMvc.perform(get("/renew/url"))
                .andExpect(status().isOk())
                .andExpect(content().string("https://www.nytimes.com/subscription/redeem/all-access?campaignId=87LH8&gift_code=gift-code"));
    }
}
