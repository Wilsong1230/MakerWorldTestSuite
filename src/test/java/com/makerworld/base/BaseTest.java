package com.makerworld.base;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.util.Map;
import java.util.List;

public abstract class BaseTest {
    protected ChromeDriver driver;
    protected ChallengeSolver solver;

    protected void initDriver() {
        ChromeOptions options = new ChromeOptions();
        // Remove automation markers
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        driver = new ChromeDriver(options);

        // THE STEALTH & INTERCEPT HOOK
        // Injected before the page loads to capture Turnstile params and hide Selenium
        String script = """
            Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
            window.__mw_cf_params = null;
            
            const hook = () => {
                if (window.turnstile && !window.turnstile._hooked) {
                    const originalRender = window.turnstile.render;
                    window.turnstile.render = (container, params) => {
                        window.__mw_cf_params = {
                            sitekey: params.sitekey,
                            action: params.action || '',
                            cData: params.cData || '',
                            pageData: params.chlPageData || params.pageData || '',
                            callback: params.callback
                        };
                        return originalRender(container, params);
                    };
                    window.turnstile._hooked = true;
                }
            };
            setInterval(hook, 10); // Aggressive polling for the turnstile object
        """;

        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", Map.of("source", script));
        solver = new ChallengeSolver(driver);
    }

    // Centralized navigation that ALWAYS checks for challenges
    protected void navigate(String url) {
        driver.get(url);
        solver.solveIfPresent(url);
    }
}