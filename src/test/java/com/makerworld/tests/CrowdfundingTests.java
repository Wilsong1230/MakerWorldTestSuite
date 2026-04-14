package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import com.makerworld.pages.CrowdfundingPage;
import com.makerworld.pages.CrowdfundingProjectPage;
import com.makerworld.utils.AssertionUtils;
import com.makerworld.utils.CardSnapshot;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

public class CrowdfundingTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void crowdfundingPageLoadsWithProjects() {
        CrowdfundingPage crowdfundingPage = new CrowdfundingPage(driver, config).open();

        Assert.assertTrue(crowdfundingPage.isLoaded(), "Expected crowdfunding page to load.");
        Assert.assertTrue(crowdfundingPage.firstProjectCard().isPresent(), "Expected at least one crowdfunding project card.");
    }

    @Test(groups = {"regression", "content"})
    public void firstProjectContainsFundingMetadata() {
        CrowdfundingPage crowdfundingPage = new CrowdfundingPage(driver, config).open();

        Assert.assertTrue(crowdfundingPage.firstProjectHasFundingMetadata(), "Expected crowdfunding card to contain parseable funding or progress data.");
    }

    @Test(groups = {"regression", "content"})
    public void statusTabsAreVisible() {
        CrowdfundingPage crowdfundingPage = new CrowdfundingPage(driver, config).open();

        Assert.assertTrue(crowdfundingPage.hasStatusTabs(), "Expected crowdfunding status tabs.");
    }

    @Test(groups = {"regression", "content"})
    public void switchingStatusTabChangesVisibleState() {
        CrowdfundingPage crowdfundingPage = new CrowdfundingPage(driver, config).open();

        Assert.assertTrue(crowdfundingPage.switchStatusTabChangesState(), "Expected switching crowdfunding tab to change the visible state.");
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void projectCardMatchesOpenedProjectPage() {
        CrowdfundingPage crowdfundingPage = new CrowdfundingPage(driver, config).open();
        CardSnapshot card = crowdfundingPage.firstProjectCard()
            .orElseThrow(() -> new AssertionError("Expected a crowdfunding project card."));

        CrowdfundingProjectPage detailPage = crowdfundingPage.openFirstProject();

        if (detailPage.isSecurityVerificationPage()) {
            throw new SkipException("MakerWorld security verification blocked crowdfunding detail navigation.");
        }

        Assert.assertTrue(detailPage.isLoaded(), "Expected a crowdfunding detail page to load.");
        Assert.assertTrue(
            AssertionUtils.normalizedContains(detailPage.heading(), AssertionUtils.slugToken(card.title()))
                || AssertionUtils.normalizedContains(card.metaText(), detailPage.heading()),
            "Expected opened crowdfunding page heading to match the project card."
        );
    }
}
