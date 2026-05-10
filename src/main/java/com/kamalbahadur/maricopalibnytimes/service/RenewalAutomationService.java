package com.kamalbahadur.maricopalibnytimes.service;

public interface RenewalAutomationService {

    BrowserRenewalResult renewOnce();

    BrowserRenewalResult bootstrapSession();

    boolean hasStoredSession();
}

