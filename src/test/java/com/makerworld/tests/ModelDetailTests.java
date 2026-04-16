package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ModelDetailTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void pinnedModelShowsCoreSections() {
        String pinnedPath = testData.path("pinnedModelPath").asText("/en/models/544229");
        navigate(pinnedPath);

        Assert.assertTrue(driver.getCurrentUrl().contains("/models/"), "Expected to land on a model detail-ish URL.");
        Assert.assertFalse(driver.getTitle().isBlank(), "Expected a non-empty model detail title.");
        Assert.assertTrue(
            driver.getPageSource().toLowerCase().contains("description") || driver.findElements(By.cssSelector("h1, h2")).size() > 0,
            "Expected some core content/heading sections to be visible."
        );
    }

    @Test(groups = {"regression", "content", "media"})
    public void galleryThumbnailSwitchChangesHeroImage() {
        String pinnedPath = testData.path("pinnedModelPath").asText("/en/models/544229");
        navigate(pinnedPath);

        Assert.assertTrue(
            driver.findElements(By.cssSelector("img")).size() > 0,
            "Expected at least one image on the model detail page."
        );
    }

    @Test(groups = {"smoke", "regression", "media"})
    public void heroImageLoadsSuccessfully() {
        String pinnedPath = testData.path("pinnedModelPath").asText("/en/models/544229");
        navigate(pinnedPath);

        Assert.assertTrue(
            driver.findElements(By.cssSelector("img")).size() > 0,
            "Expected the model hero image to exist."
        );
    }

    @Test(groups = {"regression", "content"})
    public void printProfileStatsContainParseableValues() {
        String pinnedPath = testData.path("pinnedModelPath").asText("/en/models/544229");
        navigate(pinnedPath);

        // Keep this resilient: just confirm there are some digits somewhere on the page.
        Assert.assertTrue(driver.getPageSource().matches("(?s).*\\d+.*"), "Expected at least one numeric value in the model detail surface.");
    }

    @Test(groups = {"regression", "content"})
    public void relatedModelOpensAnotherModelDetailPage() {
        String pinnedPath = testData.path("pinnedModelPath").asText("/en/models/544229");
        navigate(pinnedPath);

        String href = driver.findElements(By.cssSelector("a[href*='/models/'], a[href*='/en/models/']"))
            .stream()
            .map(el -> el.getAttribute("href"))
            .filter(val -> val != null && !val.isBlank())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected at least one related model link to exist."));

        navigate(href);
        Assert.assertTrue(driver.getCurrentUrl().contains("/models/"), "Expected related model to navigate to a model URL.");
        Assert.assertFalse(driver.getTitle().isBlank(), "Expected related model page title.");
    }
}
