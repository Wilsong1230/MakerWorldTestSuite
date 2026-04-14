package com.makerworld.core;

import java.io.File;
import java.time.Duration;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.safari.SafariDriver;

import com.makerworld.auth.AuthConfig;

public final class DriverFactory {
    private DriverFactory() {
    }

    public static WebDriver createDriver(ConfigManager config) {
        return switch (config.getBrowser()) {
            case "safari" -> createSafariDriver(config);
            case "chrome" -> createChromeDriver(config, AuthConfig.from(config));
            default -> throw new IllegalArgumentException("Unsupported browser: " + config.getBrowser());
        };
    }

    private static WebDriver createChromeDriver(ConfigManager config, AuthConfig authConfig) {
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.addArguments("--window-size=1600,1200");
        options.addArguments("--disable-notifications");
        options.addArguments("--lang=en-US");

        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", java.util.List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        if (config.isHeadless()) {
            options.addArguments("--headless=new");
        }

        String chromeBinary = config.getChromeBinary();
        if (chromeBinary != null && !chromeBinary.isBlank() && new File(chromeBinary).exists()) {
            options.setBinary(chromeBinary);
        }

        if (authConfig.mode() == AuthConfig.AuthMode.CHROME_PROFILE && authConfig.chromeUserDataDir() != null) {
            options.addArguments("--user-data-dir=" + authConfig.chromeUserDataDir().toString());
            if (authConfig.chromeProfileDir() != null && !authConfig.chromeProfileDir().isBlank()) {
                options.addArguments("--profile-directory=" + authConfig.chromeProfileDir());
            }
        }

        String chromeDriverPath = config.getChromeDriverPath();
        if (chromeDriverPath != null && !chromeDriverPath.isBlank()) {
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        }

        ChromeDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(Math.max(config.getTimeoutSeconds(), 30)));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
        return driver;
    }

    private static WebDriver createSafariDriver(ConfigManager config) {
        SafariDriver driver = new SafariDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(Math.max(config.getTimeoutSeconds(), 30)));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
        return driver;
    }
}
