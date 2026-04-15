package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HomePageTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void homePageTitleAndHeroAreVisible() {
        openHomePageAndStabilize("home page load");

        Assert.assertTrue(isHomePageLoaded(), "Expected MakerWorld home page to load.");
        Assert.assertTrue(homeHeroText().length() >= 3, "Expected hero text to be visible.");
    }

    @Test(groups = {"regression", "content"})
    public void headerIncludesExpectedNavigationLinks() {
        openHomePageAndStabilize("home page header navigation");

        for (String expectedLink : expectedHeaderLinks()) {
            Assert.assertTrue(
                headerLinkTexts().stream().anyMatch(text -> normalizedContains(text, expectedLink)),
                "Expected header link for " + expectedLink
            );
        }
    }

    @Test(groups = {"regression", "content", "media"})
    public void firstFeaturedCardHasMetadataAndLoadedImage() {
        openHomePageAndStabilize("home page featured cards");
        CardSnapshot snapshot = firstFeaturedModelCard()
            .orElseThrow(() -> new AssertionError("Expected at least one featured model card."));

        Assert.assertFalse(snapshot.title().isBlank(), "Expected featured model card title.");
        Assert.assertFalse(snapshot.href().isBlank(), "Expected featured model card href.");
        Assert.assertTrue(firstFeaturedCardImageLoads(), "Expected featured model image to load.");
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void openingFeaturedCardLandsOnModelDetailPage() {
        openHomePageAndStabilize("home page featured-card navigation");
        CardSnapshot snapshot = firstFeaturedModelCard()
            .orElseThrow(() -> new AssertionError("Expected a featured model card to open."));

        openFirstFeaturedModel();
        skipIfHumanVerificationPersists("featured-card detail navigation");

        Assert.assertTrue(isModelDetailPageLoaded(), "Expected to land on a model detail page.");
        Assert.assertTrue(detailMatchesCard(snapshot, modelTitle()), "Expected detail title to match the featured card title.");
    }

    @Test(groups = {"regression", "content"})
    public void footerIncludesFaqOrHelpLink() {
        openHomePageAndStabilize("home page footer links");

        Assert.assertTrue(hasFooterHelpOrFaqLink(), "Expected FAQ or help links in visible page navigation.");
    }
}
