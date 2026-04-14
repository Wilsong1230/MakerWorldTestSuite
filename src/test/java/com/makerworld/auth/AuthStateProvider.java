package com.makerworld.auth;

import com.makerworld.core.ConfigManager;
import org.openqa.selenium.WebDriver;

public interface AuthStateProvider {
    String name();

    void establishAuthenticatedSession(WebDriver driver, ConfigManager config, AuthConfig authConfig, AuthVerifier verifier);
}
