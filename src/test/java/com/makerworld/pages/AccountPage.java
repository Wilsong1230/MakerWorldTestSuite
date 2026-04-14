package com.makerworld.pages;

import com.makerworld.auth.AuthVerifier;
import com.makerworld.core.ConfigManager;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AccountPage extends BasePage {
    public AccountPage(WebDriver driver, ConfigManager config) {
        super(driver, config);
    }

    public boolean hasAuthenticatedMarkers() {
        return new AuthVerifier(driver, config).isAuthenticated();
    }

    public boolean openAccountSurfaceIfAvailable() {
        List<By> locators = List.of(
            By.xpath("//a[contains(@href,'/profile') or contains(@href,'/user') or contains(@href,'/account')][normalize-space()]"),
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'profile') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'account')]")
        );
        for (By locator : locators) {
            List<WebElement> elements = visible(locator);
            if (!elements.isEmpty()) {
                safeClick(elements.get(0));
                waitForPageReady();
                return true;
            }
        }
        return false;
    }

    public boolean hasLogoutControl() {
        return exists(
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'logout') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log out')]"),
            By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'logout') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log out')]")
        );
    }

    public boolean logoutIfPossible() {
        List<By> locators = List.of(
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'logout') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log out')]"),
            By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'logout') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log out')]")
        );
        for (By locator : locators) {
            List<WebElement> elements = visible(locator);
            if (!elements.isEmpty()) {
                safeClick(elements.get(0));
                tryWaitForPageReady();
                return true;
            }
        }
        return false;
    }
}
