package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CrowdfundingTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void crowdfundingPageLoadsWithProjects() {
        openCrowdfundingPageAndStabilize("crowdfunding page load");

        Assert.assertTrue(isCrowdfundingPageLoaded(), "Expected crowdfunding page to load.");
        Assert.assertTrue(firstCrowdfundingProjectCard().isPresent(), "Expected at least one crowdfunding project card.");
    }

    @Test(groups = {"regression", "content"})
    public void firstProjectContainsFundingMetadata() {
        openCrowdfundingPageAndStabilize("crowdfunding funding metadata");

        Assert.assertTrue(firstProjectHasFundingMetadata(), "Expected crowdfunding card to contain parseable funding or progress data.");
    }

    @Test(groups = {"regression", "content"})
    public void statusTabsAreVisible() {
        openCrowdfundingPageAndStabilize("crowdfunding status tabs");

        Assert.assertTrue(crowdfundingPageHasTabs(), "Expected crowdfunding status tabs.");
    }

    @Test(groups = {"regression", "content"})
    public void switchingStatusTabChangesVisibleState() {
        openCrowdfundingPageAndStabilize("crowdfunding tab switching");

        Assert.assertTrue(switchCrowdfundingTabChangesState(), "Expected switching crowdfunding tab to change the visible state.");
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void projectCardMatchesOpenedProjectPage() {
        openCrowdfundingPageAndStabilize("crowdfunding card-to-detail navigation");
        CardSnapshot card = firstCrowdfundingProjectCard()
            .orElseThrow(() -> new AssertionError("Expected a crowdfunding project card."));

        openFirstCrowdfundingProject();

        Assert.assertTrue(isCrowdfundingProjectPageLoaded(), "Expected a crowdfunding detail page to load.");
        Assert.assertTrue(detailMatchesCard(card, crowdfundingProjectHeading()), "Expected opened crowdfunding page heading to match the project card.");
    }
}
