package com.makerworld.auth;

import com.makerworld.core.ConfigManager;
import java.time.Duration;
import org.openqa.selenium.WebDriver;

public class ChromeProfileSessionProvider implements AuthStateProvider {
    @Override
    public String name() {
        return "chrome_profile";
    }

    @Override
    public void establishAuthenticatedSession(WebDriver driver, ConfigManager config, AuthConfig authConfig, AuthVerifier verifier) {
        verifier.goHome();
        if (!verifier.waitForAuthenticatedState(Duration.ofSeconds(config.getTimeoutSeconds()))) {
            throw new IllegalStateException("Chrome profile did not appear authenticated. Check MW_CHROME_USER_DATA_DIR and MW_CHROME_PROFILE_DIR.");
        }
    }
}
