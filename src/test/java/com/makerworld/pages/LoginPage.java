package com.makerworld.pages;

import com.makerworld.core.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LoginPage extends BasePage {
    public LoginPage(WebDriver driver, ConfigManager config) {
        super(driver, config);
    }

    public LoginPage open() {
        openPath("/en/login");
        return this;
    }

    public void fillCredentialsIfVisible(String email, String password) {
        maybeVisible(
            By.cssSelector("input[type='email']"),
            By.cssSelector("input[name='email']"),
            By.xpath("//input[contains(@placeholder,'Email') or contains(@placeholder,'email')]")
        ).ifPresent(element -> {
            element.clear();
            element.sendKeys(email);
        });

        maybeVisible(
            By.cssSelector("input[type='password']"),
            By.cssSelector("input[name='password']"),
            By.xpath("//input[contains(@placeholder,'Password') or contains(@placeholder,'password')]")
        ).ifPresent(element -> {
            element.clear();
            element.sendKeys(password);
        });
    }

    public void submitIfVisible() {
        maybeVisible(
            By.xpath("//button[@type='submit']"),
            By.xpath("//button[contains(.,'Sign In') or contains(.,'Log In') or contains(.,'Login')]"),
            By.xpath("//a[contains(.,'Sign In') or contains(.,'Log In') or contains(.,'Login')]")
        ).ifPresent(this::safeClick);
    }
}
