package com.kamalbahadur.maricopalibnytimes.service;

import com.kamalbahadur.maricopalibnytimes.config.NyTimesProperties;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Service
public class PlaywrightRenewalAutomationService implements RenewalAutomationService {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightRenewalAutomationService.class);

    private static final String CODE_INPUT_SELECTOR = "input[name='code']";
    private static final String REDEEM_BUTTON_SELECTOR = "button[data-testid='btn-redeem'], button[type='submit']";
    private static final List<String> SUCCESS_MARKERS = List.of(
            "you're all set",
            "you’re all set",
            "access has been activated",
            "enjoy all of the new york times",
            "manage your account",
            "your subscription",
            "welcome"
    );
    private static final List<String> ALREADY_ACTIVE_MARKERS = List.of(
            "already redeemed",
            "already have access",
            "already subscribed",
            "already a subscriber",
            "code has already been redeemed"
    );
    private static final List<String> LOGIN_MARKERS = List.of(
            "log in",
            "sign in",
            "continue with google",
            "register or logging in",
            "activate your access"
    );

    private final NyTimesProperties properties;

    public PlaywrightRenewalAutomationService(NyTimesProperties properties) {
        this.properties = properties;
    }

    @Override
    public BrowserRenewalResult renewOnce() {
        if (!hasStoredSession()) {
            return new BrowserRenewalResult(
                    BrowserRenewalResult.Status.BOOTSTRAP_REQUIRED,
                    "No saved NYTimes browser session was found. Run the interactive bootstrap first."
            );
        }

        try {
            Files.createDirectories(userDataDir());
        } catch (IOException ex) {
            return new BrowserRenewalResult(BrowserRenewalResult.Status.FAILURE, "Unable to prepare browser session directory: " + ex.getMessage());
        }

        try (Playwright playwright = Playwright.create();
             BrowserContext context = playwright.chromium().launchPersistentContext(userDataDir(), launchOptions(true))) {
            Page page = acquirePage(context);
            page.navigate(buildRedeemUrl(), new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(1500);

            if (requiresLogin(page)) {
                return new BrowserRenewalResult(
                        BrowserRenewalResult.Status.BOOTSTRAP_REQUIRED,
                        "Saved browser session is missing or expired. Run the interactive bootstrap again."
                );
            }

            submitRedeemFormIfPresent(page);
            page.waitForTimeout(3000);

            String text = pageText(page);
            if (containsAny(text, ALREADY_ACTIVE_MARKERS)) {
                return new BrowserRenewalResult(BrowserRenewalResult.Status.ALREADY_ACTIVE, "NYTimes account already appears to have access.");
            }
            if (containsAny(text, SUCCESS_MARKERS) && !requiresLogin(page)) {
                return new BrowserRenewalResult(BrowserRenewalResult.Status.SUCCESS, "Browser automation completed and NYTimes showed success indicators.");
            }
            if (requiresLogin(page)) {
                return new BrowserRenewalResult(
                        BrowserRenewalResult.Status.BOOTSTRAP_REQUIRED,
                        "NYTimes requested login during the automated run. Refresh the saved browser session with the bootstrap command."
                );
            }

            return new BrowserRenewalResult(
                    BrowserRenewalResult.Status.MANUAL_INTERVENTION_REQUIRED,
                    "Browser automation reached NYTimes, but activation could not be verified automatically. Check the page/session manually."
            );
        } catch (Exception ex) {
            log.error("Browser-based NYTimes renewal failed", ex);
            return new BrowserRenewalResult(BrowserRenewalResult.Status.FAILURE, "Browser automation failed: " + ex.getMessage());
        }
    }

    @Override
    public BrowserRenewalResult bootstrapSession() {
        try {
            Files.createDirectories(userDataDir());
        } catch (IOException ex) {
            return new BrowserRenewalResult(BrowserRenewalResult.Status.FAILURE, "Unable to prepare browser session directory: " + ex.getMessage());
        }

        log.info("Starting interactive NYTimes browser bootstrap using profile at {}", userDataDir());
        log.info("A Chromium window will open. Complete NYTimes login/redeem there, then close the browser window.");

        try (Playwright playwright = Playwright.create();
             BrowserContext context = playwright.chromium().launchPersistentContext(userDataDir(), launchOptions(false))) {
            Page page = acquirePage(context);
            page.navigate(buildRedeemUrl(), new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED));

            long deadline = System.currentTimeMillis() + Duration.ofMinutes(properties.getBrowser().getBootstrapTimeoutMinutes()).toMillis();
            while (System.currentTimeMillis() < deadline) {
                if (context.pages().stream().allMatch(Page::isClosed)) {
                    break;
                }
                page.waitForTimeout(1000);
            }

            return new BrowserRenewalResult(
                    BrowserRenewalResult.Status.SUCCESS,
                    "Interactive browser bootstrap finished. If you completed login/redeem successfully, scheduled auto-renew can now reuse that saved session."
            );
        } catch (Exception ex) {
            log.error("Interactive browser bootstrap failed", ex);
            return new BrowserRenewalResult(BrowserRenewalResult.Status.FAILURE, "Interactive browser bootstrap failed: " + ex.getMessage());
        }
    }

    @Override
    public boolean hasStoredSession() {
        Path path = userDataDir();
        if (!Files.isDirectory(path)) {
            return false;
        }

        try (var stream = Files.walk(path, 1)) {
            return stream.anyMatch(entry -> !entry.equals(path));
        } catch (IOException ex) {
            log.warn("Unable to inspect browser session directory {}: {}", path, ex.getMessage());
            return false;
        }
    }

    private BrowserType.LaunchPersistentContextOptions launchOptions(boolean headless) {
        return new BrowserType.LaunchPersistentContextOptions()
                .setHeadless(headless && properties.getBrowser().isHeadless())
                .setViewportSize(1440, 960)
                .setSlowMo((double) (headless ? 0 : properties.getBrowser().getSlowMoMillis()));
    }

    private Page acquirePage(BrowserContext context) {
        return context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
    }

    private void submitRedeemFormIfPresent(Page page) {
        Locator codeInput = page.locator(CODE_INPUT_SELECTOR).first();
        if (!isVisible(codeInput)) {
            return;
        }

        String currentValue = safeInputValue(codeInput);
        if (!properties.getGiftCode().equals(currentValue)) {
            codeInput.fill(properties.getGiftCode());
        }

        Locator redeemButton = page.locator(REDEEM_BUTTON_SELECTOR).first();
        if (isVisible(redeemButton)) {
            redeemButton.click();
            waitForPageSettled(page);
        }
    }

    private void waitForPageSettled(Page page) {
        try {
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE,
                    new Page.WaitForLoadStateOptions().setTimeout((double) properties.getBrowser().getTimeoutSeconds() * 1000));
        } catch (TimeoutError ex) {
            log.info("Timed out waiting for NYTimes page to go idle; continuing with best-effort inspection.");
        }
    }

    private boolean requiresLogin(Page page) {
        String text = pageText(page);
        String url = page.url().toLowerCase();
        return url.contains("login") || containsAny(text, LOGIN_MARKERS);
    }

    private String pageText(Page page) {
        try {
            return page.locator("body").innerText().toLowerCase();
        } catch (Exception ex) {
            return "";
        }
    }

    private boolean containsAny(String text, List<String> markers) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return markers.stream().anyMatch(text::contains);
    }

    private boolean isVisible(Locator locator) {
        try {
            return locator.isVisible(new Locator.IsVisibleOptions().setTimeout((double) properties.getBrowser().getTimeoutSeconds() * 1000));
        } catch (Exception ex) {
            return false;
        }
    }

    private String safeInputValue(Locator locator) {
        try {
            return locator.inputValue();
        } catch (Exception ex) {
            return "";
        }
    }

    private Path userDataDir() {
        return Path.of(properties.getBrowser().getUserDataDir());
    }

    private String buildRedeemUrl() {
        URI baseUri = URI.create(properties.getRedeemUrl());
        if (!"https".equalsIgnoreCase(baseUri.getScheme()) || !baseUri.getHost().endsWith("nytimes.com")) {
            throw new IllegalArgumentException("NYTIMES_REDEEM_URL must be an https nytimes.com URL");
        }

        return UriComponentsBuilder.fromUri(baseUri)
                .queryParam("campaignId", properties.getCampaignId())
                .queryParam("gift_code", properties.getGiftCode())
                .toUriString();
    }
}

