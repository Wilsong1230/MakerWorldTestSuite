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

   // Centralized Navigation with a slow-motion delay
    protected void navigate(String path) {
        String url = path.startsWith("http") ? path : "https://makerworld.com" + (path.startsWith("/") ? "" : "/") + path;
        
        System.out.println("[BaseTest] Navigating to: " + url);
        driver.get(url);
        
        // Let the page render so it's visible on recording
        slowDown(2000); 
        
        solver.solveIfPresent("Navigation to " + path);
        
        // Final pause to see the result
        slowDown(1000);
    }

    protected void slowDown(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }

    protected void typeAndSubmitSlowly(WebElement element, String value) {
        // Scroll into view so the recording shows where the action is
        driver.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
        slowDown(500);
        
        element.clear();
        // Type each character with a tiny delay to mimic a human
        for (char c : value.toCharArray()) {
            element.sendKeys(String.valueOf(c));
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        slowDown(500);
        element.sendKeys(Keys.ENTER);
    }

    // --- Core Logic Helpers (Copy from original BaseTest.java) ---
    // Make sure to update these to use navigate() or call solver.solveIfPresent() after clicks.

    protected void searchFromHomeAndStabilize(String term, String context) {
        navigate("/en");
        
        // Use a wait to ensure the search bar is ready
        WebElement searchInput = wait.until(ExpectedConditions.elementToBeClickable(
            By.cssSelector("input[type='search'], input[placeholder*='earch'], input[name*='search']")
        ));
        
        typeAndSubmitSlowly(searchInput, term);
        solver.solveIfPresent("Search Submission");
    }

    // Add all other missing methods like isHomePageLoaded(), modelLinks(), etc.
    // Ensure that low-level clicks (like openFirstLink) also trigger solver.solveIfPresent()

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    // --- Data Helpers ---
    protected String commonSearchTerm() {
        return testData.path("commonSearchTerm").asText("vase");
    }

    protected String pinnedModelPath() {
        return testData.path("pinnedModelPath").asText("/en/models/544229");
    }

    // --- Page State Helpers ---
    protected boolean isHomePageLoaded() {
        return driver.getTitle().contains("MakerWorld") && 
               driver.findElements(By.cssSelector("input[type='search']")).size() > 0;
    }
}