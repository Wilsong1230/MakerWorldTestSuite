package com.makerworld.base;

import com.twocaptcha.TwoCaptcha;
import com.twocaptcha.captcha.Turnstile;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

final class ChallengeSolver {
    private static final String DEFAULT_SITE_KEY = "0x4AAAAAAAPGX7kh4AO_iqCW";

    enum Status {
        NOT_PRESENT,
        SOLVED,
        FAILED,
        TIMED_OUT
    }

    record Outcome(Status status, String detail) {
    }

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Supplier<String> currentUrlSupplier;
    private final Supplier<String> apiKeySupplier;
    private final IntSupplier timeoutSecondsSupplier;
    private final IntSupplier pollSecondsSupplier;
    private final Runnable briefPause;

    ChallengeSolver(
        WebDriver driver,
        WebDriverWait wait,
        Supplier<String> currentUrlSupplier,
        Supplier<String> apiKeySupplier,
        IntSupplier timeoutSecondsSupplier,
        IntSupplier pollSecondsSupplier,
        Runnable briefPause
    ) {
        this.driver = driver;
        this.wait = wait;
        this.currentUrlSupplier = currentUrlSupplier;
        this.apiKeySupplier = apiKeySupplier;
        this.timeoutSecondsSupplier = timeoutSecondsSupplier;
        this.pollSecondsSupplier = pollSecondsSupplier;
        this.briefPause = briefPause;
    }

    Outcome ensureChallengeCleared(String context) {
        if (!isSecurityVerificationPage()) {
            return new Outcome(Status.NOT_PRESENT, "No challenge detected for " + context);
        }

        Outcome nativeClickOutcome = attemptNativeClick();
        if (nativeClickOutcome.status() == Status.SOLVED) {
            return new Outcome(Status.SOLVED, "Challenge cleared with native click for " + context);
        }

        Outcome captchaOutcome = attempt2Captcha();
        if (captchaOutcome.status() != Status.SOLVED) {
            return captchaOutcome;
        }

        return verifyChallengeClearance(context);
    }

    boolean isSecurityVerificationPage() {
        String url = currentUrl().toLowerCase(Locale.ROOT);
        if (!driver.findElements(By.className("cf-turnstile")).isEmpty()) {
            return true;
        }

        try {
            String pageText = driver.findElement(By.tagName("body")).getText().toLowerCase(Locale.ROOT);
            return pageText.contains("security verification")
                || pageText.contains("just a moment")
                || pageText.contains("verify you are human")
                || url.contains("challenge")
                || url.contains("turnstile");
        } catch (Exception ignored) {
            return false;
        }
    }

    private Outcome attemptNativeClick() {
        try {
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                By.cssSelector("iframe[src*='turnstile'], iframe[src*='challenges']")
            ));
            WebElement challengeBody = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            new Actions(driver)
                .pause(Duration.ofSeconds(4))
                .moveToElement(challengeBody, 12, 8)
                .pause(Duration.ofMillis(600))
                .click()
                .perform();
            driver.switchTo().defaultContent();

            if (waitUntilNotVerification(Duration.ofSeconds(10))) {
                briefPause.run();
                return new Outcome(Status.SOLVED, "Native Turnstile click solved challenge.");
            }
            return new Outcome(Status.FAILED, "Native click did not clear challenge.");
        } catch (Exception ex) {
            try {
                driver.switchTo().defaultContent();
            } catch (Exception ignored) {
            }
            return new Outcome(Status.FAILED, "Native click failed: " + ex.getMessage());
        }
    }

    private Outcome attempt2Captcha() {
        String apiKey = apiKeySupplier.get();
        if (apiKey == null || apiKey.isBlank()) {
            return new Outcome(Status.FAILED, "MW_2CAPTCHA_KEY is missing.");
        }

        try {
            String siteKey = extractSiteKey();
            if (siteKey == null || siteKey.isBlank()) {
                return new Outcome(Status.FAILED, "Unable to resolve Turnstile sitekey from page.");
            }

            TwoCaptcha solver = new TwoCaptcha(apiKey);
            Turnstile captcha = new Turnstile();
            captcha.setSiteKey(siteKey);
            captcha.setUrl(currentUrl());
            solver.solve(captcha);
            String token = captcha.getCode();
            if (token == null || token.isBlank()) {
                return new Outcome(Status.FAILED, "2Captcha returned empty token.");
            }

            injectToken(token);
            return verifyPostInjection();
        } catch (Exception ex) {
            return new Outcome(Status.FAILED, "2Captcha solve failed: " + ex.getMessage());
        }
    }

    private Outcome verifyPostInjection() {
        int timeoutSeconds = Math.max(15, timeoutSecondsSupplier.getAsInt());
        int pollMillis = Math.max(1, pollSecondsSupplier.getAsInt()) * 1000;
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        while (System.currentTimeMillis() < deadline) {
            if (!isSecurityVerificationPage()) {
                return new Outcome(Status.SOLVED, "Challenge markers disappeared after token injection.");
            }
            try {
                Thread.sleep(pollMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new Outcome(Status.TIMED_OUT, "Interrupted while polling post-injection challenge status.");
            }
        }
        return new Outcome(Status.TIMED_OUT, "Challenge persisted after token injection timeout.");
    }

    private Outcome verifyChallengeClearance(String context) {
        if (isSecurityVerificationPage()) {
            return new Outcome(Status.TIMED_OUT, "Challenge still present after solve attempts for " + context);
        }

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
            wait.until(webDriver -> !currentUrl().toLowerCase(Locale.ROOT).contains("challenge"));
            return new Outcome(Status.SOLVED, "Challenge cleared and page became interactive for " + context);
        } catch (TimeoutException ex) {
            return new Outcome(Status.TIMED_OUT, "Challenge cleared but page readiness checks timed out for " + context);
        }
    }

    private String extractSiteKey() {
        Object fromTurnstileDiv = javascript().executeScript(
            "var node = document.querySelector('.cf-turnstile[data-sitekey], [data-sitekey]');" +
                "return node ? node.getAttribute('data-sitekey') : null;"
        );
        if (fromTurnstileDiv instanceof String value && !value.isBlank()) {
            return value;
        }

        List<WebElement> iframes = driver.findElements(By.cssSelector("iframe[src*='turnstile'], iframe[src*='challenges']"));
        for (WebElement iframe : iframes) {
            String src = iframe.getAttribute("src");
            if (src == null || src.isBlank()) {
                continue;
            }

            String fromQuery = queryParam(src, "k");
            if (!fromQuery.isBlank()) {
                return fromQuery;
            }
            String fromSiteKey = queryParam(src, "sitekey");
            if (!fromSiteKey.isBlank()) {
                return fromSiteKey;
            }
        }

        Object fromWindowEnv = javascript().executeScript(
            "return window.publicEnv ? window.publicEnv.NEXT_PUBLIC_CF_TURNSTILE_SITE_KEY : null;"
        );
        if (fromWindowEnv instanceof String value && !value.isBlank()) {
            return value;
        }

        return DEFAULT_SITE_KEY;
    }

    private void injectToken(String token) {
        javascript().executeScript(
            "var input = document.getElementsByName('cf-turnstile-response')[0];" +
                "if (input) { input.value = arguments[0]; }" +
                "var altInput = document.querySelector('input[name=\"g-recaptcha-response\"]');" +
                "if (altInput) { altInput.value = arguments[0]; }" +
                "var form = document.getElementById('challenge-form') || (input ? input.closest('form') : null);" +
                "if (form) { form.submit(); }" +
                "if (window.turnstile && typeof window.turnstile.render === 'function') {" +
                "  try {" +
                "    var widgets = document.querySelectorAll('.cf-turnstile');" +
                "    widgets.forEach(function(w){ var cb = w.getAttribute('data-callback'); if (cb && window[cb]) { window[cb](arguments[0]); }});" +
                "  } catch(e) {}" +
                "}",
            token
        );
    }

    private boolean waitUntilNotVerification(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(webDriver -> !isSecurityVerificationPage());
            return true;
        } catch (TimeoutException ex) {
            return false;
        }
    }

    private JavascriptExecutor javascript() {
        return (JavascriptExecutor) driver;
    }

    private String currentUrl() {
        return currentUrlSupplier.get();
    }

    private String queryParam(String rawUrl, String key) {
        try {
            URI uri = URI.create(rawUrl);
            String query = uri.getQuery();
            if (query == null || query.isBlank()) {
                return "";
            }
            Map<String, String> params = parseQuery(query);
            return params.getOrDefault(key, "");
        } catch (Exception ignored) {
            return "";
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> values = new HashMap<>();
        String[] parts = query.split("&");
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            String[] kv = part.split("=", 2);
            String mapKey = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String mapValue = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            values.put(mapKey, mapValue);
        }
        return values;
    }
}
