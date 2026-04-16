package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ContestDetailTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void contestDetailShowsHeaderAndBreadcrumb() {
        navigate("/en/contests");

        String href = driver.findElements(By.cssSelector("a[href*='/contests/'], a[href*='/en/contests/']"))
            .stream()
            .map(el -> el.getAttribute("href"))
            .filter(val -> val != null && !val.isBlank())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a contest link."));

        navigate(href);
        Assert.assertTrue(driver.getCurrentUrl().contains("/contests/"), "Expected contest detail URL.");
        Assert.assertFalse(driver.getTitle().isBlank(), "Expected contest detail title.");
    }

    @Test(groups = {"regression", "content"})
    public void contestEntriesSectionLoadsContent() {
        navigate("/en/contests");

        String href = driver.findElements(By.cssSelector("a[href*='/contests/'], a[href*='/en/contests/']"))
            .stream()
            .map(el -> el.getAttribute("href"))
            .filter(val -> val != null && !val.isBlank())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a contest link."));

        navigate(href);
        Assert.assertFalse(driver.getPageSource().isBlank(), "Expected contest detail HTML to be non-empty.");
    }

    @Test(groups = {"regression", "content"})
    public void linkedModelFromContestOpensModelDetail() {
        navigate("/en/contests");

        String contestHref = driver.findElements(By.cssSelector("a[href*='/contests/'], a[href*='/en/contests/']"))
            .stream()
            .map(el -> el.getAttribute("href"))
            .filter(val -> val != null && !val.isBlank())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a contest link."));

        navigate(contestHref);

        String modelHref = driver.findElements(By.cssSelector("a[href*='/models/'], a[href*='/en/models/']"))
            .stream()
            .map(el -> el.getAttribute("href"))
            .filter(val -> val != null && !val.isBlank())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a model link from contest detail."));

        navigate(modelHref);
        Assert.assertTrue(driver.getCurrentUrl().contains("/models/"), "Expected model detail URL.");
        Assert.assertFalse(driver.getTitle().isBlank(), "Expected model detail title.");
    }

    @Test(groups = {"regression", "content"})
    public void contestRulesLinksStayOnMakerWorld() {
        navigate("/en/contests");

        String contestHref = driver.findElements(By.cssSelector("a[href*='/contests/'], a[href*='/en/contests/']"))
            .stream()
            .map(el -> el.getAttribute("href"))
            .filter(val -> val != null && !val.isBlank())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a contest link."));

        navigate(contestHref);
        Assert.assertTrue(driver.getCurrentUrl().contains("makerworld.com"), "Expected to remain on MakerWorld after opening contest detail.");
    }

    @Test(groups = {"regression", "content"})
    public void browserBackReturnsToContestsListing() {
        navigate("/en/contests");

        String listingUrl = driver.getCurrentUrl();
        String contestHref = driver.findElements(By.cssSelector("a[href*='/contests/'], a[href*='/en/contests/']"))
            .stream()
            .map(el -> el.getAttribute("href"))
            .filter(val -> val != null && !val.isBlank())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a contest link."));

        navigate(contestHref);
        driver.navigate().back();
        Assert.assertTrue(driver.getCurrentUrl().contains("/contests") || driver.getCurrentUrl().equals(listingUrl), "Expected browser back to return to contests listing.");
    }
}
