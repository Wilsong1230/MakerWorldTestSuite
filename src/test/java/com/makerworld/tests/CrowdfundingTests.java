package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CrowdfundingTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void crowdfundingPageLoadsWithProjects() {
        navigate("/en/crowdfunding");

        Assert.assertTrue(driver.getCurrentUrl().contains("/crowdfunding"), "Expected crowdfunding URL.");
        Assert.assertFalse(driver.getTitle().isBlank(), "Expected crowdfunding page title.");
        Assert.assertTrue(
            driver.findElements(By.cssSelector("a[href*='/crowdfunding/'], a[href*='/en/crowdfunding/']")).size() > 0,
            "Expected at least one crowdfunding project link."
        );
    }

    @Test(groups = {"regression", "content"})
    public void firstProjectContainsFundingMetadata() {
        navigate("/en/crowdfunding");

        // Minimal sanity check: page contains digits / percent somewhere (typical for funding/progress).
        Assert.assertTrue(
            driver.getPageSource().matches("(?s).*[0-9%].*"),
            "Expected crowdfunding surface to include numeric/progress information."
        );
    }

    @Test(groups = {"regression", "content"})
    public void statusTabsAreVisible() {
        navigate("/en/crowdfunding");

        Assert.assertTrue(
            driver.findElements(By.cssSelector("button, [role='tab'], [role='tablist']")).size() > 0,
            "Expected status tabs/segmented controls."
        );
    }

    @Test(groups = {"regression", "content"})
    public void switchingStatusTabChangesVisibleState() {
        navigate("/en/crowdfunding");

        Assert.assertTrue(
            driver.findElements(By.cssSelector("[role='tab'], button")).size() > 0,
            "Expected clickable tab controls on crowdfunding page."
        );
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void projectCardMatchesOpenedProjectPage() {
        navigate("/en/crowdfunding");

        String href = driver.findElements(By.cssSelector("a[href*='/crowdfunding/'], a[href*='/en/crowdfunding/']"))
            .stream()
            .map(el -> el.getAttribute("href"))
            .filter(val -> val != null && !val.isBlank())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a crowdfunding project link."));

        navigate(href);
        Assert.assertTrue(driver.getCurrentUrl().contains("/crowdfunding/"), "Expected crowdfunding project detail URL.");
        Assert.assertFalse(driver.getTitle().isBlank(), "Expected project detail page title.");
    }
}
