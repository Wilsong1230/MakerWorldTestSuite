package com.makerworld.base;

import com.twocaptcha.TwoCaptcha;
import com.twocaptcha.captcha.Turnstile;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import java.util.Map;

public class ChallengeSolver {
    private final ChromeDriver driver;

    public ChallengeSolver(ChromeDriver driver) {
        this.driver = driver;
    }

    public void solveIfPresent(String context) {
        if (!isChallengeActive()) return;

        System.out.println("[ChallengeSolver] Cloudflare detected for: " + context);

        // 1. Extract params from the CDP-injected hook
        Map<String, Object> params = (Map<String, Object>) driver.executeScript("return window.__mw_cf_params;");
        
        if (params == null) {
            System.out.println("[ChallengeSolver] Challenge active but params not yet captured. Waiting...");
            return; 
        }

        // 2. Solve via 2Captcha SDK
        String token = call2CaptchaAPI(params); 

        // 3. The "Nuclear" Injection
        // Fills the inputs AND triggers the site's internal callback to bypass the checkmark
        String inject = """
            const token = arguments[0];
            const params = window.__mw_cf_params;
            
            // Fill hidden fields
            ['cf-turnstile-response', 'g-recaptcha-response'].forEach(name => {
                const el = document.getElementsByName(name)[0];
                if (el) {
                    el.value = token;
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                }
            });
            
            // Trigger the site's internal callback directly (Clears the spinner)
            if (params && params.callback) {
                if (typeof window[params.callback] === 'function') {
                    window[params.callback](token);
                }
            } else if (typeof window.cfCallback === 'function') {
                window.cfCallback(token);
            }
        """;
        driver.executeScript(inject, token);
        System.out.println("[ChallengeSolver] Token injected and callback executed.");
    }

    private String call2CaptchaAPI(Map<String, Object> params) {
        String apiKey = System.getProperty("MW_2CAPTCHA_KEY", System.getenv("MW_2CAPTCHA_KEY"));
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("MW_2CAPTCHA_KEY is not set.");
        }
    
        TwoCaptcha solver = new TwoCaptcha(apiKey);
        Turnstile captcha = new Turnstile();
    
        captcha.setSiteKey((String) params.get("sitekey"));
        captcha.setUrl(driver.getCurrentUrl());
        
        // Correct setters for 2Captcha Java SDK 1.3.2
        if (params.get("action") != null) captcha.setAction((String) params.get("action"));
        if (params.get("cData") != null) captcha.setData((String) params.get("cData"));
        if (params.get("pageData") != null) captcha.setPageData((String) params.get("pageData"));
    
        String userAgent = (String) driver.executeScript("return navigator.userAgent;");
        captcha.setUserAgent(userAgent);
    
        try {
            solver.solve(captcha);
            return captcha.getCode();
        } catch (Exception e) {
            throw new RuntimeException("2Captcha failed: " + e.getMessage());
        }
    }

    public boolean isChallengeActive() {
        return driver.getCurrentUrl().contains("challenge") || 
               (boolean) driver.executeScript("return document.querySelectorAll('.cf-turnstile').length > 0;");
    }
}