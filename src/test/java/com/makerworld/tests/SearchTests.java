package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SearchTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void keywordSearchReturnsResults() {
        searchFromHomeAndStabilize(commonSearchTerm(), "search results load");

        Assert.assertTrue(isSearchResultsLoaded(), "Expected search results page to load.");
        Assert.assertTrue(hasSearchResults(), "Expected at least one search result.");
    }

    @Test(groups = {"regression", "content"})
    public void searchTermPersistsInUrlOrSearchBox() {
        String query = commonSearchTerm();
        searchFromHomeAndStabilize(query, "search term persistence");

        Assert.assertTrue(
            normalizedContains(searchBoxValue(), query) || normalizedContains(currentUrl(), query),
            "Expected the search term to persist in the UI or URL."
        );
    }

    @Test(groups = {"regression", "content"})
    public void firstSearchResultLooksRelevantToQuery() {
        searchFromHomeAndStabilize(commonSearchTerm(), "search result relevance");

        Assert.assertTrue(firstSearchResultLooksRelevant(commonSearchTerm()), "Expected the first result to look relevant to the query.");
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void firstSearchResultMatchesDetailPage() {
        searchFromHomeAndStabilize(commonSearchTerm(), "search result card-to-detail navigation");
        CardSnapshot firstResult = firstSearchResultCard()
            .orElseThrow(() -> new AssertionError("Expected a search result card."));

        openFirstSearchResult();

        Assert.assertTrue(isModelDetailPageLoaded(), "Expected a model detail page from the search result.");
        Assert.assertTrue(detailMatchesCard(firstResult, modelTitle()), "Expected the opened detail page title to match the search card.");
    }

    @Test(groups = {"regression", "content"})
    public void rareSearchStillShowsStableState() {
        String rareQuery = rareSearchTerm();
        searchFromHomeAndStabilize(rareQuery, "rare search results");

        Assert.assertTrue(isSearchResultsLoaded(), "Expected the search results surface to remain stable.");
        Assert.assertTrue(hasSearchEmptyState() || hasSearchResults(), "Expected either an empty state or a valid results layout for a rare search.");
        Assert.assertTrue(
            normalizedContains(searchBoxValue(), rareQuery) || normalizedContains(currentUrl(), rareQuery),
            "Expected the rare search term to persist in the page state."
        );
    }
}
