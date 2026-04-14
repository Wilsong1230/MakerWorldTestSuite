package com.makerworld.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

public class ConfigManager {
    private static final String DEFAULT_BASE_URL = "https://makerworld.com";
    private static final String DEFAULT_CHROME_BINARY = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
    private final Properties properties = new Properties();

    public ConfigManager() {
        loadClasspathProperties("config.properties");
    }

    private void loadClasspathProperties(String name) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(name)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + name, e);
        }
    }

    public String getString(String key, String defaultValue) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = properties.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getString(key, String.valueOf(defaultValue)));
    }

    public int getInt(String key, int defaultValue) {
        String raw = getString(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public String getSiteRootUrl() {
        String base = getString("MW_BASE_URL", DEFAULT_BASE_URL);
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }

    public String getLocaleBaseUrl() {
        String base = getSiteRootUrl();
        return base.endsWith("/en") ? base : base + "/en";
    }

    public String getBrowser() {
        return getString("MW_BROWSER", "chrome").toLowerCase();
    }

    public boolean isHeadless() {
        return getBoolean("MW_HEADLESS", false);
    }

    public int getTimeoutSeconds() {
        return getInt("MW_TIMEOUT_SECONDS", 20);
    }

    public String getChromeBinary() {
        return getString("MW_CHROME_BINARY", DEFAULT_CHROME_BINARY);
    }

    public String getChromeDriverPath() {
        return getString("MW_CHROMEDRIVER_PATH", "");
    }

    public String getEmail() {
        return getString("MW_EMAIL", "");
    }

    public String getPassword() {
        return getString("MW_PASSWORD", "");
    }

    public boolean isScreenshotOnFailure() {
        return getBoolean("MW_SCREENSHOT_ON_FAILURE", true);
    }

    public Path getProjectRoot() {
        return Paths.get("").toAbsolutePath().normalize();
    }

    public Path resolveProjectPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }

        Path path = Paths.get(rawPath.trim());
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return getProjectRoot().resolve(path).normalize();
    }

    public boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    public String joinUrl(String root, String path) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(path, "path");

        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }

        if (root.endsWith("/") && path.startsWith("/")) {
            return root + path.substring(1);
        }

        if (!root.endsWith("/") && !path.startsWith("/")) {
            return root + "/" + path;
        }

        return root + path;
    }
}
