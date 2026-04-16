package com.makerworld.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;
import java.time.Duration;
import java.util.Map;
import java.util.List;

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
        String url = path.startsWith("http") ? path : "https://makerworld.com" + path;
        driver.get(url);
        solver.solveIfPresent("Navigation to " + path);
    }

    // --- Restored Helper Methods for Test Classes ---
    protected void openHomePageAndStabilize(String context) { navigate("/en"); }
    protected void openModelsPageAndStabilize(String context) { navigate("/en/models"); }
    protected void openModelDetailPageAndStabilize(String path, String context) { navigate(path); }

    protected String commonSearchTerm() { return testData.path("commonSearchTerm").asText(); }
    protected String pinnedModelPath() { return testData.path("pinnedModelPath").asText(); }
    
    protected void searchFromHomeAndStabilize(String term, String context) {
        navigate("/en");
        // Add search logic here using the driver...
        solver.solveIfPresent("Search Submission");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
    }
}