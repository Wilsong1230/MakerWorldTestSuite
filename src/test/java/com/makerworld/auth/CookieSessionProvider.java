package com.makerworld.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.makerworld.core.ConfigManager;
import com.makerworld.core.WaitUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Date;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

public class CookieSessionProvider implements AuthStateProvider {
    @Override
    public String name() {
        return "cookie_session";
    }

    @Override
    public void establishAuthenticatedSession(WebDriver driver, ConfigManager config, AuthConfig authConfig, AuthVerifier verifier) {
        if (authConfig.cookieFile() == null || !Files.exists(authConfig.cookieFile())) {
            throw new IllegalStateException("Cookie file not found: " + authConfig.cookieFile());
        }

        driver.get(config.getLocaleBaseUrl());
        WaitUtils.waitForDocumentReady(driver, Duration.ofSeconds(config.getTimeoutSeconds()));

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode cookies = mapper.readTree(Files.readString(authConfig.cookieFile()));
            for (JsonNode node : cookies) {
                Cookie.Builder builder = new Cookie.Builder(node.path("name").asText(), node.path("value").asText());
                if (node.hasNonNull("domain")) {
                    builder.domain(node.path("domain").asText());
                }
                if (node.hasNonNull("path")) {
                    builder.path(node.path("path").asText("/"));
                }
                if (node.path("secure").asBoolean(false)) {
                    builder.isSecure(true);
                }
                if (node.path("httpOnly").asBoolean(false)) {
                    builder.isHttpOnly(true);
                }
                if (node.hasNonNull("expiry")) {
                    builder.expiresOn(new Date(node.path("expiry").asLong() * 1000));
                }
                driver.manage().addCookie(builder.build());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse cookies from " + authConfig.cookieFile(), e);
        }

        driver.navigate().refresh();
        WaitUtils.waitForDocumentReady(driver, Duration.ofSeconds(config.getTimeoutSeconds()));
        if (!verifier.waitForAuthenticatedState(Duration.ofSeconds(config.getTimeoutSeconds()))) {
            throw new IllegalStateException("Cookie session bootstrap did not produce an authenticated state.");
        }
    }
}
