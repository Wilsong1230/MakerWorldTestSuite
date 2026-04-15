package com.makerworld.base;

import com.twocaptcha.TwoCaptcha;
import com.twocaptcha.captcha.Turnstile;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
    enum Status {
        NOT_PRESENT,
        SOLVED,
        FAILED,

        
        TIMED_OUT
    }

    record Outcome(Status status, String detail) {
    }

    private record TurnstileRequest(
        String siteKey,
        String action,
        String cData,
        String pageData,
        String userAgent
    ) {
    }

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Supplier<String> currentUrlSupplier;
    private final Supplier<String> apiKeySupplier;
    private final Supplier<String> configuredSiteKeySupplier;
    private final IntSupplier timeoutSecondsSupplier;
    private final IntSupplier pollSecondsSupplier;
    private final Runnable briefPause;

    ChallengeSolver(
        WebDriver driver,
        WebDriverWait wait,
        Supplier<String> currentUrlSupplier,
        Supplier<String> apiKeySupplier,
        Supplier<String> configuredSiteKeySupplier,
        IntSupplier timeoutSecondsSupplier,
        IntSupplier pollSecondsSupplier,
        Runnable briefPause
    ) {
        this.driver = driver;
        this.wait = wait;
        this.currentUrlSupplier = currentUrlSupplier;
        this.apiKeySupplier = apiKeySupplier;
        this.configuredSiteKeySupplier = configuredSiteKeySupplier;
        this.timeoutSecondsSupplier = timeoutSecondsSupplier;
        this.pollSecondsSupplier = pollSecondsSupplier;
        this.briefPause = briefPause;
    }

    Outcome ensureChallengeCleared(String context) {
        if (!isSecurityVerificationPage()) {
            System.out.println("[ChallengeSolver] No challenge detected for: " + context);
            return new Outcome(Status.NOT_PRESENT, "No challenge detected for " + context);
        }
        System.out.println("[ChallengeSolver] Challenge detected for: " + context + " url=" + currentUrl());

        Outcome nativeClickOutcome = attemptNativeClick();
        System.out.println("[ChallengeSolver] Native attempt result: " + nativeClickOutcome.status() + " - " + nativeClickOutcome.detail());
        if (nativeClickOutcome.status() == Status.SOLVED) {
            return new Outcome(Status.SOLVED, "Challenge cleared with native click for " + context);
        }

        System.out.println("[ChallengeSolver] Falling back to 2Captcha for: " + context);
        Outcome captchaOutcome = attempt2Captcha();
        System.out.println("[ChallengeSolver] 2Captcha attempt result: " + captchaOutcome.status() + " - " + captchaOutcome.detail());
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
            List<WebElement> frames = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.cssSelector("iframe[src*='turnstile'], iframe[src*='challenges']")
            ));
            if (frames.isEmpty()) {
                return new Outcome(Status.FAILED, "No Turnstile iframe found for native click.");
            }

            // Cloudflare checkbox widgets are often best triggered by clicking the iframe center.
            WebElement frame = frames.get(0);
            int xOffset = Math.max(6, frame.getSize().getWidth() / 2);
            int yOffset = Math.max(6, frame.getSize().getHeight() / 2);
            new Actions(driver)
                .pause(Duration.ofSeconds(2))
                .moveToElement(frame, xOffset, yOffset)
                .pause(Duration.ofMillis(250))
                .click()
                .perform();

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
            System.out.println("[ChallengeSolver] 2Captcha key missing.");
            return new Outcome(Status.FAILED, "MW_2CAPTCHA_KEY is missing.");
        }

        try {
            TurnstileRequest request = extractTurnstileRequest();
            if (request.siteKey() == null || request.siteKey().isBlank()) {
                return new Outcome(Status.FAILED, "Unable to resolve Turnstile sitekey from page.");
            }
            boolean challengeUrl = currentUrl().toLowerCase(Locale.ROOT).contains("challenge");
            if (challengeUrl && (request.action().isBlank() || request.cData().isBlank() || request.pageData().isBlank())) {
                return new Outcome(
                    Status.FAILED,
                    "Challenge-mode Turnstile params missing (action/cData/pageData). "
                        + "Refusing blind solve to avoid hanging with unusable tokens."
                );
            }
            System.out.println(
                "[ChallengeSolver] 2Captcha request prepared:"
                    + " siteKey=" + mask(request.siteKey())
                    + " action=" + safe(request.action())
                    + " cData=" + (request.cData().isBlank() ? "<empty>" : "<present>")
                    + " pageData=" + (request.pageData().isBlank() ? "<empty>" : "<present>")
                    + " ua=" + (request.userAgent().isBlank() ? "<empty>" : "<present>")
            );

            TwoCaptcha solver = new TwoCaptcha(apiKey);
            Turnstile captcha = new Turnstile();
            captcha.setSiteKey(request.siteKey());
            captcha.setUrl(currentUrl());
            setCaptchaFieldIfPresent(captcha, request.action(), "setAction");
            setCaptchaFieldIfPresent(captcha, request.cData(), "setData", "setCData", "setCdata");
            setCaptchaFieldIfPresent(captcha, request.pageData(), "setPageData", "setPagedata");
            setCaptchaFieldIfPresent(captcha, request.userAgent(), "setUserAgent", "setUseragent");
            System.out.println("[ChallengeSolver] Calling 2Captcha solve...");
            solver.solve(captcha);
            System.out.println("[ChallengeSolver] 2Captcha solve returned.");
            String token = captcha.getCode();
            if (token == null || token.isBlank()) {
                return new Outcome(Status.FAILED, "2Captcha returned empty token.");
            }
            System.out.println("[ChallengeSolver] Received token from 2Captcha.");

            injectToken(token);
            System.out.println("[ChallengeSolver] Token injected; starting post-injection verification.");
            return verifyPostInjection();
        } catch (Exception ex) {
            return new Outcome(Status.FAILED, "2Captcha solve failed: " + ex.getMessage());
        }
    }

    private Outcome verifyPostInjection() {
        int timeoutSeconds = Math.max(15, timeoutSecondsSupplier.getAsInt());
        int pollMillis = Math.max(1, pollSecondsSupplier.getAsInt()) * 1000;
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        int polls = 0;
        while (System.currentTimeMillis() < deadline) {
            if (!isSecurityVerificationPage()) {
                return new Outcome(Status.SOLVED, "Challenge markers disappeared after token injection.");
            }
            polls++;
            if (polls % 5 == 0) {
                System.out.println("[ChallengeSolver] Post-injection wait still on challenge: " + currentUrl());
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

    private TurnstileRequest extractTurnstileRequest() {
        String siteKey = "";
        String action = "";
        String cData = "";
        String pageData = "";

        String explicitSiteKey = readConfiguredSiteKey();
        if (!explicitSiteKey.isBlank()) {
            siteKey = explicitSiteKey;
        }

        Object fromTurnstileDiv = javascript().executeScript(
            "var node = document.querySelector('.cf-turnstile[data-sitekey], [data-sitekey]');" +
                "return node ? node.getAttribute('data-sitekey') : null;"
        );
        if (fromTurnstileDiv instanceof String value && !value.isBlank()) {
            siteKey = value;
        }

        List<WebElement> iframes = driver.findElements(By.cssSelector("iframe"));
        for (WebElement iframe : iframes) {
            String src = iframe.getAttribute("src");
            if (src == null || src.isBlank()) {
                continue;
            }

            String fromQuery = queryParam(src, "k");
            if (siteKey.isBlank() && !fromQuery.isBlank()) {
                siteKey = fromQuery;
            }
            String fromSiteKey = queryParam(src, "sitekey");
            if (siteKey.isBlank() && !fromSiteKey.isBlank()) {
                siteKey = fromSiteKey;
            }
            if (siteKey.isBlank()) {
                siteKey = extractSiteKeyFromRawUrl(src);
            }
            if (action.isBlank()) {
                action = firstNonBlank(queryParam(src, "action"), queryParam(src, "sa"), "");
            }
            if (cData.isBlank()) {
                cData = firstNonBlank(queryParam(src, "data"), queryParam(src, "cData"), queryParam(src, "cdata"), "");
            }
            if (pageData.isBlank()) {
                pageData = firstNonBlank(queryParam(src, "pagedata"), queryParam(src, "pageData"), "");
            }
        }

        Object fromWindowEnv = javascript().executeScript(
            "return window.publicEnv ? window.publicEnv.NEXT_PUBLIC_CF_TURNSTILE_SITE_KEY : null;"
        );
        if (siteKey.isBlank() && fromWindowEnv instanceof String value && !value.isBlank()) {
            siteKey = value;
        }

        if (action.isBlank() || cData.isBlank() || pageData.isBlank()) {
            Map<String, String> domParams = extractDomChallengeParams();
            if (action.isBlank()) {
                action = domParams.getOrDefault("action", "");
            }
            if (cData.isBlank()) {
                cData = domParams.getOrDefault("cData", "");
            }
            if (pageData.isBlank()) {
                pageData = domParams.getOrDefault("pageData", "");
            }
            if (siteKey.isBlank()) {
                siteKey = domParams.getOrDefault("siteKey", "");
            }
        }
        if (action.isBlank() || cData.isBlank() || pageData.isBlank() || siteKey.isBlank()) {
            Map<String, String> scriptParams = extractScriptChallengeParams();
            if (action.isBlank()) {
                action = scriptParams.getOrDefault("action", "");
            }
            if (cData.isBlank()) {
                cData = scriptParams.getOrDefault("cData", "");
            }
            if (pageData.isBlank()) {
                pageData = scriptParams.getOrDefault("pageData", "");
            }
            if (siteKey.isBlank()) {
                siteKey = scriptParams.getOrDefault("siteKey", "");
            }
        }

        String userAgent = "";
        try {
            Object ua = javascript().executeScript("return navigator.userAgent || '';");
            if (ua instanceof String value) {
                userAgent = value;
            }
        } catch (Exception ignored) {
        }
        return new TurnstileRequest(siteKey, action, cData, pageData, userAgent);
    }

    private void injectToken(String token) {
        javascript().executeScript(
            "var solvedToken = arguments[0];" +
            "var input = document.getElementsByName('cf-turnstile-response')[0];" +
                "if (input) {" +
                "  input.value = solvedToken;" +
                "  input.dispatchEvent(new Event('input', { bubbles: true }));" +
                "  input.dispatchEvent(new Event('change', { bubbles: true }));" +
                "}" +
                "var altInput = document.querySelector('input[name=\"g-recaptcha-response\"]');" +
                "if (altInput) {" +
                "  altInput.value = solvedToken;" +
                "  altInput.dispatchEvent(new Event('input', { bubbles: true }));" +
                "  altInput.dispatchEvent(new Event('change', { bubbles: true }));" +
                "}" +
                "var form = document.getElementById('challenge-form') || (input ? input.closest('form') : null);" +
                "if (form) { form.submit(); }" +
                "if (window.turnstile && typeof window.turnstile.render === 'function') {" +
                "  try {" +
                "    var widgets = document.querySelectorAll('.cf-turnstile');" +
                "    widgets.forEach(function(w){" +
                "      var cb = w.getAttribute('data-callback');" +
                "      if (cb && typeof window[cb] === 'function') { window[cb](solvedToken); }" +
                "    });" +
                "  } catch(e) {}" +
                "}" +
                "var submitButton = document.querySelector('button[type=\"submit\"], input[type=\"submit\"]');" +
                "if (submitButton) { try { submitButton.click(); } catch(e) {} }",
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractDomChallengeParams() {
        try {
            Object raw = javascript().executeScript(
                "var out = { siteKey: '', action: '', cData: '', pageData: '' };" +
                    "var node = document.querySelector('.cf-turnstile, [data-sitekey], [data-action], [data-cdata], [data-pagedata]');" +
                    "if (node) {" +
                    "  out.siteKey = node.getAttribute('data-sitekey') || '';" +
                    "  out.action = node.getAttribute('data-action') || '';" +
                    "  out.cData = node.getAttribute('data-cdata') || node.getAttribute('data-cData') || '';" +
                    "  out.pageData = node.getAttribute('data-pagedata') || node.getAttribute('data-page-data') || node.getAttribute('data-pageData') || '';" +
                    "}" +
                    "if ((!out.action || !out.cData || !out.pageData) && window.__cf_chl_opt) {" +
                    "  var opt = window.__cf_chl_opt;" +
                    "  out.action = out.action || opt.action || '';" +
                    "  out.cData = out.cData || opt.cData || opt.data || '';" +
                    "  out.pageData = out.pageData || opt.chlPageData || opt.pageData || '';" +
                    "}" +
                    "return out;"
            );
            if (raw instanceof Map<?, ?> map) {
                Map<String, String> result = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                    }
                }
                return result;
            }
        } catch (Exception ignored) {
        }
        return new HashMap<>();
    }

    private void setCaptchaFieldIfPresent(Object captcha, String value, String... methodNames) {
        if (value == null || value.isBlank()) {
            return;
        }
        List<Method> methods = new ArrayList<>();
        for (Method method : captcha.getClass().getMethods()) {
            methods.add(method);
        }
        for (String methodName : methodNames) {
            for (Method method : methods) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && params[0].equals(String.class)) {
                    try {
                        method.invoke(captcha, value);
                        System.out.println("[ChallengeSolver] Applied optional field via " + methodName);
                        return;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        System.out.println("[ChallengeSolver] Optional field setter unavailable for " + String.join("/", methodNames));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractScriptChallengeParams() {
        try {
            Object raw = javascript().executeScript(
                "var out = { siteKey: '', action: '', cData: '', pageData: '' };" +
                    "var html = document.documentElement ? document.documentElement.innerHTML : '';" +
                    "function pick(re) { var m = html.match(re); return m && m[1] ? m[1] : ''; }" +
                    "out.siteKey = pick(/\\\"sitekey\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"/i) || " +
                    "  pick(/\\'sitekey\\'\\s*:\\s*\\'([^\\']+)\\'/i) || " +
                    "  pick(/sitekey\\s*:\\s*\\\"([^\\\"]+)\\\"/i) || " +
                    "  pick(/sitekey\\s*:\\s*\\'([^\\']+)\\'/i) || " +
                    "  pick(/data-sitekey=\\\"([^\\\"]+)\\\"/i) || " +
                    "  pick(/data-sitekey=\\'([^\\']+)\\'/i) || " +
                    "  pick(/[?&]k=([^&\\\"']+)/i) || " +
                    "  pick(/\\b0x4[0-9A-Za-z_-]{20,}\\b/i);" +
                    "out.action = pick(/\\\"action\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"/i);" +
                    "out.cData = pick(/\\\"cData\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"/i) || pick(/\\\"data\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"/i);" +
                    "out.pageData = pick(/\\\"chlPageData\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"/i) || pick(/\\\"pageData\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"/i);" +
                    "return out;"
            );
            if (raw instanceof Map<?, ?> map) {
                Map<String, String> result = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                    }
                }
                return result;
            }
        } catch (Exception ignored) {
        }
        return new HashMap<>();
    }

    private String extractSiteKeyFromRawUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[] patterns = new String[] {
            ".*[?&]k=([0-9A-Za-z_-]+).*",
            ".*[?&]sitekey=([0-9A-Za-z_-]+).*",
            ".*%3Fk%3D([0-9A-Za-z_-]+).*",
            ".*%26k%3D([0-9A-Za-z_-]+).*",
            ".*\\b(0x4[0-9A-Za-z_-]{20,})\\b.*"
        };
        for (String pattern : patterns) {
            if (raw.matches(pattern)) {
                return raw.replaceAll(pattern, "$1");
            }
        }
        return "";
    }

    private String readConfiguredSiteKey() {
        try {
            String fromConfig = configuredSiteKeySupplier.get();
            if (fromConfig != null && !fromConfig.isBlank()) {
                return fromConfig.trim();
            }
        } catch (Exception ignored) {
        }
        String[] candidates = new String[] {
            System.getenv("MW_TURNSTILE_SITEKEY"),
            System.getenv("MW_2CAPTCHA_SITEKEY"),
            System.getProperty("MW_TURNSTILE_SITEKEY"),
            System.getProperty("MW_2CAPTCHA_SITEKEY")
        };
        for (String value : candidates) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "<empty>" : value;
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return "<empty>";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
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
