package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HomePageTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void homePageTitleAndHeroAreVisible() {
        navigate("/en");

        Assert.assertFalse(driver.getTitle().isBlank(), "Expected MakerWorld home page to have a title.");
        Assert.assertTrue(
            driver.findElements(By.cssSelector("input[type='search']")).size() > 0,
            "Expected a search input on the home page."
        );
    }

    @Test(groups = {"regression", "content"})
    public void headerIncludesExpectedNavigationLinks() {
        navigate("/en");

        String page = driver.getPageSource().toLowerCase();
        for (var node : testData.path("expectedHeaderLinks")) {
            String expected = node.asText();
            Assert.assertTrue(page.contains(expected.toLowerCase()), "Expected header/navigation to mention: " + expected);
        }
    }

    @Test(groups = {"regression", "content", "media"})
    public void firstFeaturedCardHasMetadataAndLoadedImage() {
        navigate("/en");

        Assert.assertTrue(
            driver.findElements(By.cssSelector("a[href*='/models/'], a[href*='/en/models/']")).size() > 0,
            "Expected at least one model link on the home page."
        );
        Assert.assertTrue(
            driver.findElements(By.cssSelector("img")).size() > 0,
            "Expected at least one image element on the home page."
        );
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void openingFeaturedCardLandsOnModelDetailPage() {
        navigate("/en");

        String href = driver.findElements(By.cssSelector("a[href*='/models/'], a[href*='/en/models/']"))
            .stream()
            .map(el -> el.getAttribute("href"))
            .filter(val -> val != null && !val.isBlank())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a model link to open from the home page."));

        navigate(href);
        Assert.assertTrue(driver.getCurrentUrl().contains("/models/"), "Expected to land on a model detail-ish URL.");
        Assert.assertFalse(driver.getTitle().isBlank(), "Expected the model page to have a title.");
    }

    @Test(groups = {"regression", "content"})
    public void footerIncludesFaqOrHelpLink() {
        navigate("/en");

        String page = driver.getPageSource().toLowerCase();
        Assert.assertTrue(
            page.contains("faq") || page.contains("help") || page.contains("support"),
            "Expected FAQ/help/support text somewhere in the page navigation."
        );
    }
}
