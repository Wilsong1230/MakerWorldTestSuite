package com.makerworld.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;
import java.time.Duration;
import java.util.Map;
import java.util.List;
import java.util.Optional;

public abstract class BaseTest {
    protected ChromeDriver driver;
    protected ChallengeSolver solver;
    protected WebDriverWait wait;
    protected JsonNode testData;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        testData = new ObjectMapper().readTree(getClass().getClassLoader().getResourceAsStream("testdata/search-terms.json"));
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.addArguments("--window-size=1600,1200");
        
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // THE STEALTH & INTERCEPT HOOK
        String script = """
            Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
            window.__mw_cf_params = null;
            const hook = () => {
                if (window.turnstile && !window.turnstile._hooked) {
                    const org = window.turnstile.render;
                    window.turnstile.render = (container, params) => {
                        window.__mw_cf_params = {
                            sitekey: params.sitekey,
                            action: params.action || '',
                            cData: params.cData || '',
                            pageData: params.chlPageData || params.pageData || '',
                            callback: params.callback
                        };
                        return org(container, params);
                    };
                    window.turnstile._hooked = true;
                }
            };
            setInterval(hook, 10);
        """;
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", Map.of("source", script));
        
        solver = new ChallengeSolver(driver);
    }

    // Centralized Navigation
    protected void navigate(String path) {
        String url = path.startsWith("http") ? "https://makerworld.com" + path : path;
        if (!path.startsWith("http")) url = "https://makerworld.com" + path;
        driver.get(url);
        solver.solveIfPresent("Navigation to " + path);
    }

    // --- Core Logic Helpers (Copy from original BaseTest.java) ---
    // Make sure to update these to use navigate() or call solver.solveIfPresent() after clicks.

    protected void searchFromHomeAndStabilize(String term, String context) {
        navigate("/en");
        // Re-implement your original search logic here
        WebElement searchInput = driver.findElement(By.cssSelector("input[type='search']"));
        searchInput.sendKeys(term);
        searchInput.sendKeys(Keys.ENTER);
        solver.solveIfPresent("Search Submission");
    }

    // Add all other missing methods like isHomePageLoaded(), modelLinks(), etc.
    // Ensure that low-level clicks (like openFirstLink) also trigger solver.solveIfPresent()

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
    }
}