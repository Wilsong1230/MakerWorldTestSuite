package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ContestsPageTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void contestsPageLoadsWithHeadingAndCards() {
        openContestsPageAndStabilize("contests page load");

        Assert.assertTrue(isContestsPageLoaded(), "Expected contests page to load.");
        Assert.assertFalse(contestsHeading().isBlank(), "Expected a contests page heading.");
        Assert.assertTrue(firstContestCard().isPresent(), "Expected at least one contest card.");
    }

    @Test(groups = {"regression", "content"})
    public void contestTabsAreVisible() {
        openContestsPageAndStabilize("contest tabs");

        Assert.assertTrue(contestsPageHasTabs(), "Expected contest tabs or segmented controls.");
    }

    @Test(groups = {"regression", "content"})
    public void switchingContestTabChangesVisibleState() {
        openContestsPageAndStabilize("contest tab switching");

        Assert.assertTrue(switchContestTabChangesState(), "Expected switching contest tab to change visible state.");
    }

    @Test(groups = {"regression", "content"})
    public void firstContestCardIncludesMetadata() {
        openContestsPageAndStabilize("contest card metadata");

        Assert.assertTrue(firstContestCardHasMetadata(), "Expected the first contest card to contain title and metadata.");
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void firstContestCardOpensMatchingDetailPage() {
        openContestsPageAndStabilize("contest card-to-detail navigation");
        CardSnapshot contestCard = firstContestCard()
            .orElseThrow(() -> new AssertionError("Expected a contest card to open."));

        openFirstContest();

        Assert.assertTrue(isContestDetailPageLoaded(), "Expected contest detail page to load.");
        Assert.assertTrue(detailMatchesCard(contestCard, contestHeading()), "Expected the contest detail heading to match the contest card title.");
    }
}
