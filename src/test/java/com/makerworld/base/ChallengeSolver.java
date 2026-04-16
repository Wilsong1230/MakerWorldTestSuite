package com.makerworld.base;

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

        // 1. Extract params from our CDP hook
        Map<String, Object> params = (Map<String, Object>) driver.executeScript("return window.__mw_cf_params;");
        
        if (params == null) {
            System.out.println("Challenge active but params not yet captured. Retrying...");
            // Add a small wait/retry loop here if needed
            return;
        }

        // 2. Send to 2Captcha (Implement your existing solver logic here)
        String token = call2CaptchaAPI(params); 

        // 3. The "Nuclear" Injection
        // Fills the inputs AND triggers the site's internal logic
        String inject = """
            const token = arguments[0];
            const params = window.__mw_cf_params;
            
            // Fill hidden fields
            document.getElementsByName('cf-turnstile-response')[0].value = token;
            
            // Trigger the callback directly
            if (params && params.callback) {
                if (typeof window[params.callback] === 'function') {
                    window[params.callback](token);
                }
            }
        """;
        driver.executeScript(inject, token);
    }

    private boolean isChallengeActive() {
        return driver.getCurrentUrl().contains("challenge") || 
               (boolean) driver.executeScript("return document.querySelectorAll('.cf-turnstile').length > 0;");
    }
}