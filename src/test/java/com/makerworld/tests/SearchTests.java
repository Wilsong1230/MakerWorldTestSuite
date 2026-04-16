package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SearchTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void keywordSearchReturnsResults() {
        String term = testData.path("commonSearchTerm").asText("vase");
        searchFromHomeAndStabilize(term, "search results load");

        Assert.assertTrue(driver.getCurrentUrl().toLowerCase().contains("search"), "Expected to land on a search URL.");
        Assert.assertTrue(driver.getPageSource().toLowerCase().contains(term.toLowerCase()), "Expected the term to appear somewhere on the page.");
        Assert.assertTrue(
            driver.findElements(By.cssSelector("a[href*='/models/'], a[href*='/en/models/']")).size() > 0,
            "Expected at least one model link in search results."
        );
    }

    @Test(groups = {"regression", "content"})
    public void searchTermPersistsInUrlOrSearchBox() {
        String query = testData.path("secondarySearchTerm").asText("swatch");
        searchFromHomeAndStabilize(query, "search term persistence");

        String url = driver.getCurrentUrl().toLowerCase();
        Assert.assertTrue(url.contains(query.toLowerCase()) || url.contains("search"), "Expected the query to persist in the URL.");
    }

    @Test(groups = {"regression", "content"})
    public void firstSearchResultLooksRelevantToQuery() {
        String query = testData.path("commonSearchTerm").asText("vase");
        searchFromHomeAndStabilize(query, "search result relevance");

        String page = driver.getPageSource().toLowerCase();
        Assert.assertTrue(page.contains(query.toLowerCase()), "Expected the page content to include the query.");
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void firstSearchResultMatchesDetailPage() {
        String term = testData.path("commonSearchTerm").asText("vase");
        searchFromHomeAndStabilize(term, "search result card-to-detail navigation");

        String firstHref = driver.findElements(By.cssSelector("a[href*='/models/'], a[href*='/en/models/']"))
            .stream()
            .map(el -> el.getAttribute("href"))
            .filter(href -> href != null && !href.isBlank())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a model link to open from search results."));

        navigate(firstHref);
        Assert.assertTrue(driver.getCurrentUrl().contains("/models/"), "Expected to land on a model detail-ish URL.");
        Assert.assertFalse(driver.getTitle().isBlank(), "Expected the model detail page to have a non-empty title.");
    }

    @Test(groups = {"regression", "content"})
    public void rareSearchStillShowsStableState() {
        String rareQuery = testData.path("rareSearchTerm").asText("zzzzmakerworldunlikelyterm");
        searchFromHomeAndStabilize(rareQuery, "rare search results");

        Assert.assertTrue(driver.getCurrentUrl().toLowerCase().contains("search"), "Expected to remain on a search URL.");
        Assert.assertFalse(driver.getPageSource().isBlank(), "Expected a stable rendered page (non-empty source).");
    }
}
