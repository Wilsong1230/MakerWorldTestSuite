package com.makerworld.auth;

import com.makerworld.core.ConfigManager;
import com.makerworld.pages.LoginPage;
import java.time.Duration;
import org.openqa.selenium.WebDriver;

public class ManualLoginCheckpointProvider implements AuthStateProvider {
    @Override
    public String name() {
        return "manual_checkpoint";
    }

    @Override
    public void establishAuthenticatedSession(WebDriver driver, ConfigManager config, AuthConfig authConfig, AuthVerifier verifier) {
        LoginPage loginPage = new LoginPage(driver, config).open();
        loginPage.fillCredentialsIfVisible(authConfig.email(), authConfig.password());
        loginPage.submitIfVisible();

        System.out.println("Manual login checkpoint active. Complete any remaining login steps in Chrome. Waiting for authenticated markers...");
        if (!verifier.waitForAuthenticatedState(Duration.ofMinutes(3))) {
            throw new IllegalStateException("Manual login checkpoint timed out before authenticated UI was detected.");
        }
    }
}
