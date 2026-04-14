package com.makerworld.pages;

import com.makerworld.core.ConfigManager;
import com.makerworld.utils.CardSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ModelsPage extends BasePage {
    public ModelsPage(WebDriver driver, ConfigManager config) {
        super(driver, config);
    }

    public ModelsPage open() {
        HomePage homePage = new HomePage(driver, config).open();
        if (!homePage.openNavigationItem("All Models")) {
            openPath("/en/models");
        }
        return this;
    }

    public boolean isLoaded() {
        return !modelLinks().isEmpty()
            && (currentUrl().contains("/models") || pageContainsText("All Models") || pageContainsText("For You"));
    }

    public List<CardSnapshot> visibleModelCards(int limit) {
        return modelLinks().stream().limit(limit).map(this::buildCardSnapshot).collect(Collectors.toList());
    }

    public boolean hasMultipleUniqueCards(int minimumCount) {
        return visibleModelCards(minimumCount + 2).stream()
            .map(CardSnapshot::href)
            .distinct()
            .count() >= minimumCount;
    }

    public boolean firstCardHasMetadataAndLoadedImage() {
        Optional<WebElement> link = modelLinks().stream().findFirst();
        if (link.isEmpty()) {
            return false;
        }

        CardSnapshot snapshot = buildCardSnapshot(link.get());
        boolean hasTitle = !snapshot.title().isBlank();
        boolean hasMeta = !snapshot.metaText().isBlank();
        boolean hasImage = firstImageInside(nearestCard(link.get())).map(this::imageLoaded).orElse(false);
        return hasTitle && hasMeta && hasImage;
    }

    public boolean hasFilterOrSortControls() {
        return !tabLikeElements().isEmpty()
            || exists(By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sort')]"))
            || exists(By.tagName("select"));
    }

    public boolean switchFirstAlternativeFilter() {
        String originalFirstHref = modelLinks().stream().findFirst().map(this::hrefOf).orElse("");
        boolean clicked = clickFirstAlternativeTab();
        if (!clicked) {
            return false;
        }
        String newFirstHref = modelLinks().stream().findFirst().map(this::hrefOf).orElse("");
        return !newFirstHref.equals(originalFirstHref) || !newFirstHref.isBlank();
    }

    public Optional<CardSnapshot> firstModelCard() {
        return modelLinks().stream().findFirst().map(this::buildCardSnapshot);
    }

    public ModelDetailPage openFirstModel() {
        WebElement link = modelLinks().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("No model link found on models page."));
        driver.get(hrefOf(link));
        waitForPageReady();
        return new ModelDetailPage(driver, config);
    }
}
