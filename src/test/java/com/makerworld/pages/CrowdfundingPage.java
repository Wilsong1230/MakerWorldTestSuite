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

public class CrowdfundingPage extends BasePage {
    public CrowdfundingPage(WebDriver driver, ConfigManager config) {
        super(driver, config);
    }

    public CrowdfundingPage open() {
        openPath("/en/crowdfunding");
        return this;
    }

    public boolean isLoaded() {
        return currentUrl().contains("/crowdfunding") && (!crowdfundingLinks().isEmpty() || pageContainsText("crowdfunding"));
    }

    public boolean hasStatusTabs() {
        return !tabLikeElements().isEmpty() || exists(
            By.xpath("//button[contains(.,'Live') or contains(.,'Upcoming') or contains(.,'Late Pledge') or contains(.,'All')]")
        );
    }

    public boolean switchStatusTabChangesState() {
        String original = crowdfundingLinks().stream().findFirst().map(this::hrefOf).orElse("");
        boolean changed = clickFirstAlternativeTab();
        if (!changed) {
            return false;
        }
        String current = crowdfundingLinks().stream().findFirst().map(this::hrefOf).orElse("");
        return !current.equals(original) || !current.isBlank();
    }

    public Optional<CardSnapshot> firstProjectCard() {
        return crowdfundingLinks().stream().findFirst().map(this::buildCardSnapshot);
    }

    public boolean firstProjectHasFundingMetadata() {
        return firstProjectCard()
            .map(card -> !card.title().isBlank() && AssertionUtils.countNumericTokens(card.metaText()) >= 1)
            .orElse(false);
    }

    public boolean hasSearchInput() {
        return exists(
            By.cssSelector("input[type='search']"),
            By.cssSelector("input[placeholder*='earch']")
        );
    }

    public boolean searchForProject(String query) {
        Optional<WebElement> input = maybeVisible(
            By.cssSelector("input[type='search']"),
            By.cssSelector("input[placeholder*='earch']")
        );
        if (input.isEmpty()) {
            return false;
        }
        typeAndSubmit(input.get(), query);
        tryWaitForPageReady();
        return searchValue().toLowerCase().contains(query.toLowerCase()) || currentUrl().toLowerCase().contains(query.toLowerCase());
    }

    public String searchValue() {
        return maybeVisible(
            By.cssSelector("input[type='search']"),
            By.cssSelector("input[placeholder*='earch']")
        ).map(element -> element.getAttribute("value")).orElse("");
    }

    public CrowdfundingProjectPage openFirstProject() {
        WebElement link = crowdfundingLinks().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("No crowdfunding project link found."));
        driver.get(hrefOf(link));
        waitForPageReady();
        return new CrowdfundingProjectPage(driver, config);
    }

    public List<CardSnapshot> projects(int limit) {
        return crowdfundingLinks().stream().limit(limit).map(this::buildCardSnapshot).collect(Collectors.toList());
    }
}
