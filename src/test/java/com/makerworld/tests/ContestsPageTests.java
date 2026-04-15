package com.makerworld.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.makerworld.base.BaseTest;
import com.makerworld.pages.ContestDetailPage;
import com.makerworld.pages.ContestsPage;
import com.makerworld.utils.AssertionUtils;
import com.makerworld.utils.CardSnapshot;

public class ContestsPageTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void contestsPageLoadsWithHeadingAndCards() {
        ContestsPage contestsPage = new ContestsPage(driver, config).open();
        skipIfHumanVerificationPersists(contestsPage, "contests page load");

        Assert.assertTrue(contestsPage.isLoaded(), "Expected contests page to load.");
        Assert.assertFalse(contestsPage.heading().isBlank(), "Expected a contests page heading.");
        Assert.assertTrue(contestsPage.firstContestCard().isPresent(), "Expected at least one contest card.");
    }

    @Test(groups = {"regression", "content"})
    public void contestTabsAreVisible() {
        ContestsPage contestsPage = new ContestsPage(driver, config).open();
        skipIfHumanVerificationPersists(contestsPage, "contest tabs");

        Assert.assertTrue(contestsPage.hasContestTabs(), "Expected contest tabs or segmented controls.");
    }

    @Test(groups = {"regression", "content"})
    public void switchingContestTabChangesVisibleState() {
        ContestsPage contestsPage = new ContestsPage(driver, config).open();
        skipIfHumanVerificationPersists(contestsPage, "contest tab switching");

        Assert.assertTrue(contestsPage.switchContestTabChangesState(), "Expected switching contest tab to change visible state.");
    }

    @Test(groups = {"regression", "content"})
    public void firstContestCardHasMetadata() {
        ContestsPage contestsPage = new ContestsPage(driver, config).open();
        skipIfHumanVerificationPersists(contestsPage, "contest card metadata");

        Assert.assertTrue(contestsPage.firstContestCardHasMetadata(), "Expected the first contest card to contain title and metadata.");
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void firstContestCardOpensMatchingDetailPage() {
        ContestsPage contestsPage = new ContestsPage(driver, config).open();
        skipIfHumanVerificationPersists(contestsPage, "contest card-to-detail navigation");
        CardSnapshot contestCard = contestsPage.firstContestCard()
            .orElseThrow(() -> new AssertionError("Expected a contest card to open."));

        ContestDetailPage detailPage = contestsPage.openFirstContest();
        skipIfHumanVerificationPersists(detailPage, "contest detail navigation");

        Assert.assertTrue(detailPage.isLoaded(), "Expected contest detail page to load.");
        Assert.assertTrue(
            AssertionUtils.normalizedContains(detailPage.heading(), AssertionUtils.slugToken(contestCard.title()))
                || AssertionUtils.normalizedContains(contestCard.metaText(), detailPage.heading()),
            "Expected the contest detail heading to match the contest card title."
        );
    }
}
