package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import com.makerworld.pages.HomePage;
import com.makerworld.pages.ModelDetailPage;
import com.makerworld.pages.SearchResultsPage;
import com.makerworld.utils.AssertionUtils;
import com.makerworld.utils.CardSnapshot;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SearchTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void keywordSearchReturnsResults() {
        SearchResultsPage resultsPage = new HomePage(driver, config).open().searchFor(testData.commonSearchTerm());
        skipIfHumanVerificationPersists(resultsPage, "search results load");

        Assert.assertTrue(resultsPage.isLoaded(), "Expected search results page to load.");
        Assert.assertTrue(resultsPage.hasResults(), "Expected at least one search result.");
    }

    @Test(groups = {"regression", "content"})
    public void searchTermPersistsInUrlOrSearchBox() {
        String query = testData.commonSearchTerm();
        SearchResultsPage resultsPage = new HomePage(driver, config).open().searchFor(query);
        skipIfHumanVerificationPersists(resultsPage, "search term persistence");

        Assert.assertTrue(
            resultsPage.searchBoxValue().toLowerCase().contains(query.toLowerCase())
                || driver.getCurrentUrl().toLowerCase().contains(query.toLowerCase()),
            "Expected the search term to persist in the UI or URL."
        );
    }

    @Test(groups = {"regression", "content"})
    public void firstSearchResultLooksRelevantToQuery() {
        SearchResultsPage resultsPage = new HomePage(driver, config).open().searchFor(testData.commonSearchTerm());
        skipIfHumanVerificationPersists(resultsPage, "search result relevance");

        Assert.assertTrue(resultsPage.firstResultLooksRelevant(testData.commonSearchTerm()), "Expected the first result to look relevant to the query.");
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void firstSearchResultMatchesDetailPage() {
        SearchResultsPage resultsPage = new HomePage(driver, config).open().searchFor(testData.commonSearchTerm());
        skipIfHumanVerificationPersists(resultsPage, "search result card-to-detail navigation");
        CardSnapshot firstResult = resultsPage.firstResultCard()
            .orElseThrow(() -> new AssertionError("Expected a search result card."));

        ModelDetailPage detailPage = resultsPage.openFirstResult();
        skipIfHumanVerificationPersists(detailPage, "search-result detail navigation");

        Assert.assertTrue(detailPage.isLoaded(), "Expected a model detail page from the search result.");
        Assert.assertTrue(
            AssertionUtils.normalizedContains(detailPage.modelTitle(), AssertionUtils.slugToken(firstResult.title()))
                || AssertionUtils.normalizedContains(firstResult.metaText(), detailPage.modelTitle()),
            "Expected the opened detail page title to match the search card."
        );
    }

    @Test(groups = {"regression", "content"})
    public void rareSearchStillShowsStableState() {
        String rareQuery = testData.rareSearchTerm();
        SearchResultsPage resultsPage = new HomePage(driver, config).open().searchFor(rareQuery);
        skipIfHumanVerificationPersists(resultsPage, "rare search results");

        Assert.assertTrue(resultsPage.isLoaded(), "Expected the search results surface to remain stable.");
        Assert.assertTrue(
            resultsPage.hasEmptyState() || resultsPage.hasResults(),
            "Expected either an empty state or a valid results layout for a rare search."
        );
        Assert.assertTrue(
            resultsPage.searchBoxValue().toLowerCase().contains(rareQuery.toLowerCase())
                || driver.getCurrentUrl().toLowerCase().contains(rareQuery.toLowerCase()),
            "Expected the rare search term to persist in the page state."
        );
    }
}
