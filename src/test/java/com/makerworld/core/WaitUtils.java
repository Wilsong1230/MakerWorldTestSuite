package com.makerworld.core;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public final class WaitUtils {
    private WaitUtils() {
    }

    public static void waitForDocumentReady(WebDriver driver, Duration timeout) {
        new WebDriverWait(driver, timeout)
            .until(webDriver -> "complete".equals(
                String.valueOf(((JavascriptExecutor) webDriver).executeScript("return document.readyState"))));
    }

    public static boolean waitForUrlContains(WebDriver driver, String fragment, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ExpectedConditions.urlContains(fragment));
            return true;
        } catch (TimeoutException ex) {
            return false;
        }
    }

    public static boolean waitForTitleContains(WebDriver driver, String fragment, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ExpectedConditions.titleContains(fragment));
            return true;
        } catch (TimeoutException ex) {
            return false;
        }
    }

    public static boolean waitForValueChange(WebDriver driver, Supplier<String> supplier, String originalValue, Duration timeout) {
        Objects.requireNonNull(supplier, "supplier");
        try {
            new WebDriverWait(driver, timeout).until(webDriver -> {
                String current = supplier.get();
                return current != null && !current.equals(originalValue);
            });
            return true;
        } catch (TimeoutException ex) {
            return false;
        }
    }

    public static boolean waitForCondition(WebDriver driver, Duration timeout, Supplier<Boolean> condition) {
        try {
            new WebDriverWait(driver, timeout).until(webDriver -> Boolean.TRUE.equals(condition.get()));
            return true;
        } catch (TimeoutException ex) {
            return false;
        }
    }

    public static void waitForVisible(WebDriver driver, WebElement element, Duration timeout) {
        new WebDriverWait(driver, timeout).until(ExpectedConditions.visibilityOf(element));
    }
}
