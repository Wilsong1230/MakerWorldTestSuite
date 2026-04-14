package com.makerworld.pages;

import com.makerworld.core.ConfigManager;
import com.makerworld.utils.CardSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ContestsPage extends BasePage {
    public ContestsPage(WebDriver driver, ConfigManager config) {
        super(driver, config);
    }

    public ContestsPage open() {
        openPath("/en/contests");
        return this;
    }

    public boolean isLoaded() {
        return currentUrl().contains("/contests") && (!contestLinks().isEmpty() || pageContainsText("contest"));
    }

    public String heading() {
        return maybeVisible(By.cssSelector("main h1"), By.xpath("//h1[normalize-space()]"))
            .map(this::textOf)
            .orElse("");
    }

    public boolean hasContestTabs() {
        return !tabLikeElements().isEmpty() || exists(
            By.xpath("//button[contains(.,'Active') or contains(.,'Finished') or contains(.,'Upcoming')]")
        );
    }

    public boolean switchContestTabChangesState() {
        String original = contestLinks().stream().findFirst().map(this::hrefOf).orElse("");
        boolean changed = clickFirstAlternativeTab();
        if (!changed) {
            return false;
        }
        String current = contestLinks().stream().findFirst().map(this::hrefOf).orElse("");
        return !current.equals(original) || !current.isBlank();
    }

    public Optional<CardSnapshot> firstContestCard() {
        return contestLinks().stream().findFirst().map(this::buildCardSnapshot);
    }

    public boolean firstContestCardHasMetadata() {
        return firstContestCard()
            .map(card -> !card.title().isBlank() && !card.metaText().isBlank())
            .orElse(false);
    }

    public boolean hasRulesOrThemeLink() {
        return visibleAnchors().stream()
            .anyMatch(anchor -> hrefOf(anchor).contains("rules") || textOf(anchor).toLowerCase().contains("theme"));
    }

    public ContestDetailPage openFirstContest() {
        WebElement link = contestLinks().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("No contest link found on contests page."));
        driver.get(hrefOf(link));
        waitForPageReady();
        return new ContestDetailPage(driver, config);
    }

    public List<CardSnapshot> contests(int limit) {
        return contestLinks().stream().limit(limit).map(this::buildCardSnapshot).collect(Collectors.toList());
    }
}
