package com.kamalbahadur.maricopalibnytimes.service;

public record BrowserRenewalResult(Status status, String message) {

    public enum Status {
        SUCCESS,
        ALREADY_ACTIVE,
        BOOTSTRAP_REQUIRED,
        MANUAL_INTERVENTION_REQUIRED,
        FAILURE
    }

    public boolean isSuccessful() {
        return status == Status.SUCCESS || status == Status.ALREADY_ACTIVE;
    }
}

