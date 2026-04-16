package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ContestDetailTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void contestDetailShowsHeaderAndBreadcrumb() {
        openContestsPageAndStabilize("contest detail header and breadcrumb");
        openFirstContest();

        Assert.assertTrue(isContestDetailPageLoaded(), "Expected contest detail page to load.");
        Assert.assertFalse(contestHeading().isBlank(), "Expected contest heading.");
        Assert.assertTrue(contestDetailHasBreadcrumb() || currentUrl().contains("/contests"), "Expected contest breadcrumb or contest-context URL.");
    }

    @Test(groups = {"regression", "content"})
    public void contestEntriesSectionLoadsContent() {
        openContestsPageAndStabilize("contest entries section");
        openFirstContest();

        Assert.assertTrue(contestEntriesSectionLoaded(), "Expected contest entries content to be present.");
    }

    @Test(groups = {"regression", "content"})
    public void linkedModelFromContestOpensModelDetail() {
        openContestsPageAndStabilize("contest-linked model navigation");
        openFirstContest();
        openFirstContestEntryModel();

        Assert.assertTrue(isModelDetailPageLoaded(), "Expected contest entry model to open a model detail page.");
        Assert.assertFalse(modelTitle().isBlank(), "Expected opened model detail to have a title.");
    }

    @Test(groups = {"regression", "content"})
    public void contestRulesLinksStayOnMakerWorld() {
        openContestsPageAndStabilize("contest rules links");
        openFirstContest();

        Assert.assertTrue(contestRulesLinksStayWithinMakerWorld(), "Expected rules links to remain on MakerWorld.");
    }

    @Test(groups = {"regression", "content"})
    public void browserBackReturnsToContestsListing() {
        openContestsPageAndStabilize("contest back navigation");
        openFirstContest();

        driver.navigate().back();

        Assert.assertTrue(currentUrl().contains("/contests"), "Expected browser back to return to contests listing.");
    }
}
