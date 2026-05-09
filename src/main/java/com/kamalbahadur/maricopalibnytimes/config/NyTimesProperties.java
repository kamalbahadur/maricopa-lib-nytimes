package com.kamalbahadur.maricopalibnytimes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nytimes")
public class NyTimesProperties {

    private String redeemUrl;
    private String campaignId;
    private String giftCode;
    private OAuth2 oauth2 = new OAuth2();

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
}

