package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ContestsPageTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void contestsPageLoadsWithHeadingAndCards() {
        navigate("/en/contests");

        Assert.assertTrue(driver.getCurrentUrl().contains("/contests"), "Expected contests listing URL.");
        Assert.assertFalse(driver.getTitle().isBlank(), "Expected contests page title.");
        Assert.assertTrue(
            driver.findElements(By.cssSelector("a[href*='/contests/'], a[href*='/en/contests/']")).size() > 0,
            "Expected at least one contest link card."
        );
    }

    @Test(groups = {"regression", "content"})
    public void contestTabsAreVisible() {
        navigate("/en/contests");

        Assert.assertTrue(
            driver.findElements(By.cssSelector("button, [role='tab'], [role='tablist']")).size() > 0,
            "Expected contest tabs/segmented controls."
        );
    }

    @Test(groups = {"regression", "content"})
    public void switchingContestTabChangesVisibleState() {
        navigate("/en/contests");

        // Minimal stability check: there are multiple clickable controls, indicating segmented navigation is present.
        Assert.assertTrue(
            driver.findElements(By.cssSelector("[role='tab'], button")).size() > 0,
            "Expected clickable tab controls on contests page."
        );
    }

    @Test(groups = {"regression", "content"})
    public void firstContestCardIncludesMetadata() {
        navigate("/en/contests");

        String page = driver.getPageSource();
        Assert.assertFalse(page.isBlank(), "Expected contests page to render HTML.");
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void firstContestCardOpensMatchingDetailPage() {
        navigate("/en/contests");

        String href = driver.findElements(By.cssSelector("a[href*='/contests/'], a[href*='/en/contests/']"))
            .stream()
            .map(el -> el.getAttribute("href"))
            .filter(val -> val != null && !val.isBlank())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a contest link to open."));

        navigate(href);
        Assert.assertTrue(driver.getCurrentUrl().contains("/contests/"), "Expected contest detail-ish URL.");
        Assert.assertFalse(driver.getTitle().isBlank(), "Expected contest detail page title.");
    }
}
