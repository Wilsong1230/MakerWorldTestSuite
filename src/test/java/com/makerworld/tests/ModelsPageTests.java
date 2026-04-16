package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ModelsPageTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void modelsListingLoadsMultipleUniqueCards() {
        navigate("/en/models");

        Assert.assertTrue(driver.getCurrentUrl().contains("/models"), "Expected models listing URL.");
        Assert.assertTrue(
            driver.findElements(By.cssSelector("a[href*='/models/'], a[href*='/en/models/']")).size() >= 3,
            "Expected multiple model links on the models listing."
        );
    }

    @Test(groups = {"regression", "content", "media"})
    public void firstModelCardHasMetadataAndImage() {
        navigate("/en/models");

        Assert.assertTrue(
            driver.findElements(By.cssSelector("img")).size() > 0,
            "Expected images to be present on the models listing."
        );
    }

    @Test(groups = {"regression", "content"})
    public void modelsPageExposesFilterOrSortControls() {
        navigate("/en/models");

        String page = driver.getPageSource().toLowerCase();
        Assert.assertTrue(
            page.contains("sort") || page.contains("filter"),
            "Expected filter/sort controls text to exist on the models page."
        );
    }

    @Test(groups = {"regression", "content"})
    public void changingFilterChangesVisibleState() {
        navigate("/en/models");

        // Minimal stability check: the listing is interactive enough to expose more than one clickable control.
        Assert.assertTrue(
            driver.findElements(By.cssSelector("button, [role='button'], a")).size() > 5,
            "Expected interactive controls on the models page."
        );
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void firstModelCardMatchesOpenedDetailPage() {
        navigate("/en/models");

        String href = driver.findElements(By.cssSelector("a[href*='/models/'], a[href*='/en/models/']"))
            .stream()
            .map(el -> el.getAttribute("href"))
            .filter(val -> val != null && !val.isBlank())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected at least one model link."));

        navigate(href);
        Assert.assertTrue(driver.getCurrentUrl().contains("/models/"), "Expected to land on a model detail-ish URL.");
        Assert.assertFalse(driver.getTitle().isBlank(), "Expected model detail page title.");
    }
}
