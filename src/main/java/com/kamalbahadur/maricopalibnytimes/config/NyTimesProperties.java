package com.kamalbahadur.maricopalibnytimes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nytimes")
public class NyTimesProperties {

    private String redeemUrl;
    private String campaignId;
    private String giftCode;
    private OAuth2 oauth2 = new OAuth2();
    private Browser browser = new Browser();

    public String getRedeemUrl() {
        return redeemUrl;
    }

    public void setRedeemUrl(String redeemUrl) {
        this.redeemUrl = redeemUrl;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getGiftCode() {
        return giftCode;
    }

    public void setGiftCode(String giftCode) {
        this.giftCode = giftCode;
    }

    public OAuth2 getOauth2() {
        return oauth2;
    }

    public void setOauth2(OAuth2 oauth2) {
        this.oauth2 = oauth2;
    }

    public Browser getBrowser() {
        return browser;
    }

    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    public static class OAuth2 {
        private String registrationId;
        private String principalName;

        public String getRegistrationId() {
            return registrationId;
        }

        public void setRegistrationId(String registrationId) {
            this.registrationId = registrationId;
        }

        public String getPrincipalName() {
            return principalName;
        }

        public void setPrincipalName(String principalName) {
            this.principalName = principalName;
        }
    }

    public static class Browser {
        private String userDataDir;
        private boolean headless = true;
        private int timeoutSeconds = 60;
        private int bootstrapTimeoutMinutes = 15;
        private int slowMoMillis = 75;
        private String command;

        public String getUserDataDir() {
            return userDataDir;
        }

        public void setUserDataDir(String userDataDir) {
            this.userDataDir = userDataDir;
        }

        public boolean isHeadless() {
            return headless;
        }

        public void setHeadless(boolean headless) {
            this.headless = headless;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getBootstrapTimeoutMinutes() {
            return bootstrapTimeoutMinutes;
        }

        public void setBootstrapTimeoutMinutes(int bootstrapTimeoutMinutes) {
            this.bootstrapTimeoutMinutes = bootstrapTimeoutMinutes;
        }

        public int getSlowMoMillis() {
            return slowMoMillis;
        }

        public void setSlowMoMillis(int slowMoMillis) {
            this.slowMoMillis = slowMoMillis;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }
    }
}

