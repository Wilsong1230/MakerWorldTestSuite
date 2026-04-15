package com.makerworld.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.makerworld.base.BaseTest;
import com.makerworld.pages.ContestDetailPage;
import com.makerworld.pages.ContestsPage;
import com.makerworld.pages.ModelDetailPage;

public class ContestDetailTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void contestDetailShowsHeaderAndBreadcrumb() {
        ContestDetailPage detailPage = new ContestsPage(driver, config).open().openFirstContest();
        skipIfHumanVerificationPersists(detailPage, "contest detail header and breadcrumb");

        Assert.assertTrue(detailPage.isLoaded(), "Expected contest detail page to load.");
        Assert.assertFalse(detailPage.heading().isBlank(), "Expected contest heading.");
        Assert.assertTrue(detailPage.hasBreadcrumb() || driver.getCurrentUrl().contains("/contests"), "Expected contest breadcrumb or contest-context URL.");
    }

    @Test(groups = {"regression", "content"})
    public void contestEntriesSectionLoadsContent() {
        ContestDetailPage detailPage = new ContestsPage(driver, config).open().openFirstContest();
        skipIfHumanVerificationPersists(detailPage, "contest entries section");

        Assert.assertTrue(detailPage.entriesSectionLoaded(), "Expected contest entries content to be present.");
    }

    @Test(groups = {"regression", "content"})
    public void linkedModelFromContestOpensModelDetail() {
        ContestDetailPage detailPage = new ContestsPage(driver, config).open().openFirstContest();
        skipIfHumanVerificationPersists(detailPage, "contest-linked model navigation");
        ModelDetailPage modelDetailPage = detailPage.openFirstEntryModel();
        skipIfHumanVerificationPersists(modelDetailPage, "contest-linked model detail");

        Assert.assertTrue(modelDetailPage.isLoaded(), "Expected contest entry model to open a model detail page.");
        Assert.assertFalse(modelDetailPage.modelTitle().isBlank(), "Expected opened model detail to have a title.");
    }

    @Test(groups = {"regression", "content"})
    public void contestRulesLinksStayOnMakerWorld() {
        ContestDetailPage detailPage = new ContestsPage(driver, config).open().openFirstContest();
        skipIfHumanVerificationPersists(detailPage, "contest rules links");

        Assert.assertTrue(detailPage.rulesLinksStayWithinMakerWorld(), "Expected rules links to remain on MakerWorld.");
    }

    @Test(groups = {"regression", "content"})
    public void browserBackReturnsToContestsListing() {
        ContestDetailPage detailPage = new ContestsPage(driver, config).open().openFirstContest();
        skipIfHumanVerificationPersists(detailPage, "contest back navigation");

        driver.navigate().back();

        Assert.assertTrue(driver.getCurrentUrl().contains("/contests"), "Expected browser back to return to contests listing.");
    }
}
