package com.kamalbahadur.maricopalibnytimes.controller;

import com.kamalbahadur.maricopalibnytimes.service.BrowserRenewalResult;
import com.kamalbahadur.maricopalibnytimes.service.RenewalAutomationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "nytimes.oauth2.registration-id=google",
        "nytimes.oauth2.principal-name=test@example.com"
})
class NYTimesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RenewalAutomationService renewalAutomationService;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void renewEndpointReturnsAutomationMessageWhenAuthenticated() throws Exception {
        when(renewalAutomationService.renewOnce())
                .thenReturn(new BrowserRenewalResult(
                        BrowserRenewalResult.Status.SUCCESS,
                        "Browser automation completed and NYTimes showed success indicators."
                ));

        mockMvc.perform(get("/renew").with(user("integration-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Manual renewal triggered: Browser automation completed and NYTimes showed success indicators."
                ));
    }

    @Test
    void renewTriggerEndpointReturnsBootstrapMessageWhenSessionIsMissing() throws Exception {
        when(renewalAutomationService.renewOnce())
                .thenReturn(new BrowserRenewalResult(
                        BrowserRenewalResult.Status.BOOTSTRAP_REQUIRED,
                        "No saved NYTimes browser session was found. Run the interactive bootstrap first."
                ));

        mockMvc.perform(get("/renew/trigger").with(user("integration-user")))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Manual renewal triggered: No saved NYTimes browser session was found. Run the interactive bootstrap first."
                ));
    }
}
