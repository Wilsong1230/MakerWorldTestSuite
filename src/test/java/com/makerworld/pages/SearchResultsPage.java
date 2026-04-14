package com.makerworld.pages;

import com.makerworld.core.ConfigManager;
import com.makerworld.utils.AssertionUtils;
import com.makerworld.utils.CardSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SearchResultsPage extends BasePage {
    public SearchResultsPage(WebDriver driver, ConfigManager config) {
        super(driver, config);
    }

    public boolean isLoaded() {
        return currentUrl().contains("search") || hasResults() || hasEmptyState();
    }

    public boolean hasResults() {
        return !resultLinks().isEmpty();
    }

    public boolean hasEmptyState() {
        return pageContainsText("no results")
            || pageContainsText("0 results")
            || pageContainsText("no models");
    }

    public String searchBoxValue() {
        return maybeVisible(
            By.cssSelector("input[type='search']"),
            By.cssSelector("input[placeholder*='earch']"),
            By.cssSelector("input[name*='search']")
        ).map(element -> element.getAttribute("value")).orElse("");
    }

    public Optional<CardSnapshot> firstResultCard() {
        return resultLinks().stream().findFirst().map(this::buildCardSnapshot);
    }

    public boolean firstResultLooksRelevant(String term) {
        return firstResultCard()
            .map(card -> AssertionUtils.normalizedContains(card.title() + " " + card.metaText(), term))
            .orElse(false);
    }

    public ModelDetailPage openFirstResult() {
        WebElement link = resultLinks().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("No search result link found."));
        driver.get(hrefOf(link));
        waitForPageReady();
        return new ModelDetailPage(driver, config);
    }

    public List<CardSnapshot> topResults(int limit) {
        return resultLinks().stream().limit(limit).map(this::buildCardSnapshot).collect(Collectors.toList());
    }

    private List<WebElement> resultLinks() {
        return modelLinks();
    }
}
