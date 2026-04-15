package com.makerworld.base;

import org.openqa.selenium.WebDriver;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import com.makerworld.auth.AuthConfig;
import com.makerworld.auth.AuthStateProvider;
import com.makerworld.auth.AuthVerifier;
import com.makerworld.auth.ChromeProfileSessionProvider;
import com.makerworld.auth.CookieSessionProvider;
import com.makerworld.auth.ManualLoginCheckpointProvider;
import com.makerworld.core.ConfigManager;
import com.makerworld.core.DriverFactory;
import com.makerworld.listeners.TestListener;
import com.makerworld.pages.BasePage;
import com.makerworld.utils.TestDataLoader;
import com.twocaptcha.TwoCaptcha;




@Listeners(TestListener.class)
public abstract class BaseTest {
    private static final long HUMAN_VERIFICATION_RECHECK_MILLIS = 10_000L;
    private static final ThreadLocal<WebDriver> THREAD_DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<ConfigManager> THREAD_CONFIG = new ThreadLocal<>();

    protected ConfigManager config;
    protected TestDataLoader testData;
    protected AuthConfig authConfig;
    protected WebDriver driver;
    protected TwoCaptcha solver;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        config = new ConfigManager();
        testData = new TestDataLoader();
        authConfig = AuthConfig.from(config);
        String apiKey = config.getTwoCaptchaKey();
        if (!apiKey.isBlank()) {
            solver = new TwoCaptcha(apiKey);
        }
        driver = DriverFactory.createDriver(config);
        THREAD_DRIVER.set(driver);
        THREAD_CONFIG.set(config);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
        THREAD_DRIVER.remove();
        THREAD_CONFIG.remove();
    }

    protected void bootstrapAuthenticatedSessionOrSkip() {
        if (!authConfig.authSuiteEnabled()) {
            throw new SkipException("Auth suite is disabled. Set MW_ENABLE_AUTH_SUITE=true to run authenticated scenarios.");
        }

        AuthVerifier verifier = new AuthVerifier(driver, config);
        AuthStateProvider provider = switch (authConfig.mode()) {
            case COOKIE_SESSION -> new CookieSessionProvider();
            case CHROME_PROFILE -> new ChromeProfileSessionProvider();
            case MANUAL_CHECKPOINT -> new ManualLoginCheckpointProvider();
        };

        provider.establishAuthenticatedSession(driver, config, authConfig, verifier);
    }

    protected AuthVerifier authVerifier() {
        return new AuthVerifier(driver, config);
    }

    protected void skipIfHumanVerificationPersists(BasePage page, String context) {
        if (!page.isSecurityVerificationPage()) {
            return;
        }

        System.out.println("Human verification detected for " + context + ". Waiting 10 seconds before rechecking...");
        try {
            Thread.sleep(HUMAN_VERIFICATION_RECHECK_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (page.isSecurityVerificationPage()) {
            throw new SkipException("MakerWorld human verification still present after 10 seconds: " + context);
        }
    }

    public static WebDriver getThreadDriver() {
        return THREAD_DRIVER.get();
    }

    public static ConfigManager getThreadConfig() {
        return THREAD_CONFIG.get();
    }
}
