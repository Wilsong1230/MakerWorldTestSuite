package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import com.makerworld.pages.HomePage;
import com.makerworld.pages.ModelDetailPage;
import com.makerworld.utils.AssertionUtils;
import com.makerworld.utils.CardSnapshot;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

public class HomePageTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void homePageTitleAndHeroAreVisible() {
        HomePage homePage = new HomePage(driver, config).open();

        Assert.assertTrue(homePage.isLoaded(), "Expected MakerWorld home page to load.");
        Assert.assertTrue(homePage.heroText().length() >= 3, "Expected hero text to be visible.");
    }

    @Test(groups = {"regression", "content"})
    public void headerIncludesExpectedNavigationLinks() {
        HomePage homePage = new HomePage(driver, config).open();

        for (String expectedLink : testData.expectedHeaderLinks()) {
            Assert.assertTrue(
                homePage.headerLinkTexts().stream().anyMatch(text -> AssertionUtils.normalizedContains(text, expectedLink)),
                "Expected header link for " + expectedLink
            );
        }
    }

    @Test(groups = {"regression", "content", "media"})
    public void firstFeaturedCardHasMetadataAndLoadedImage() {
        HomePage homePage = new HomePage(driver, config).open();
        CardSnapshot snapshot = homePage.firstFeaturedModelCard()
            .orElseThrow(() -> new AssertionError("Expected at least one featured model card."));

        Assert.assertFalse(snapshot.title().isBlank(), "Expected featured model card title.");
        Assert.assertFalse(snapshot.href().isBlank(), "Expected featured model card href.");
        Assert.assertTrue(homePage.firstFeaturedCardImageLoads(), "Expected featured model image to load.");
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void openingFeaturedCardLandsOnModelDetailPage() {
        HomePage homePage = new HomePage(driver, config).open();
        CardSnapshot snapshot = homePage.firstFeaturedModelCard()
            .orElseThrow(() -> new AssertionError("Expected a featured model card to open."));

        ModelDetailPage detailPage = homePage.openFirstFeaturedModel();

        if (detailPage.isSecurityVerificationPage()) {
            throw new SkipException("MakerWorld security verification blocked the featured-card detail navigation.");
        }

        Assert.assertTrue(detailPage.isLoaded(), "Expected to land on a model detail page.");
        Assert.assertTrue(
            AssertionUtils.normalizedContains(detailPage.modelTitle(), AssertionUtils.slugToken(snapshot.title()))
                || AssertionUtils.normalizedContains(snapshot.metaText(), detailPage.modelTitle()),
            "Expected detail title to match the featured card title."
        );
    }

    @Test(groups = {"regression", "content"})
    public void footerIncludesFaqOrHelpLink() {
        HomePage homePage = new HomePage(driver, config).open();

        Assert.assertTrue(homePage.hasFooterHelpOrFaqLink(), "Expected FAQ or help links in visible page navigation.");
    }
}
