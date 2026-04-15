package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ContestDetailTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void contestDetailShowsHeaderAndBreadcrumb() {
        openContestsPageAndStabilize("contest detail header and breadcrumb");
        openFirstContest();
        skipIfHumanVerificationPersists("contest detail header and breadcrumb");

        Assert.assertTrue(isContestDetailPageLoaded(), "Expected contest detail page to load.");
        Assert.assertFalse(contestHeading().isBlank(), "Expected contest heading.");
        Assert.assertTrue(contestDetailHasBreadcrumb() || currentUrl().contains("/contests"), "Expected contest breadcrumb or contest-context URL.");
    }

    @Test(groups = {"regression", "content"})
    public void contestEntriesSectionLoadsContent() {
        openContestsPageAndStabilize("contest entries section");
        openFirstContest();
        skipIfHumanVerificationPersists("contest entries section");

        Assert.assertTrue(contestEntriesSectionLoaded(), "Expected contest entries content to be present.");
    }

    @Test(groups = {"regression", "content"})
    public void linkedModelFromContestOpensModelDetail() {
        openContestsPageAndStabilize("contest-linked model navigation");
        openFirstContest();
        skipIfHumanVerificationPersists("contest-linked model navigation");
        openFirstContestEntryModel();
        skipIfHumanVerificationPersists("contest-linked model detail");

        Assert.assertTrue(isModelDetailPageLoaded(), "Expected contest entry model to open a model detail page.");
        Assert.assertFalse(modelTitle().isBlank(), "Expected opened model detail to have a title.");
    }

    @Test(groups = {"regression", "content"})
    public void contestRulesLinksStayOnMakerWorld() {
        openContestsPageAndStabilize("contest rules links");
        openFirstContest();
        skipIfHumanVerificationPersists("contest rules links");

        Assert.assertTrue(contestRulesLinksStayWithinMakerWorld(), "Expected rules links to remain on MakerWorld.");
    }

    @Test(groups = {"regression", "content"})
    public void browserBackReturnsToContestsListing() {
        openContestsPageAndStabilize("contest back navigation");
        openFirstContest();
        skipIfHumanVerificationPersists("contest back navigation");

        driver.navigate().back();

        Assert.assertTrue(currentUrl().contains("/contests"), "Expected browser back to return to contests listing.");
    }
}
