package com.makerworld.pages;

import com.makerworld.core.ConfigManager;
import com.makerworld.utils.CardSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class MakerLabPage extends BasePage {
    public MakerLabPage(WebDriver driver, ConfigManager config) {
        super(driver, config);
    }

    public MakerLabPage open() {
        openPath("/makerlab");
        return this;
    }

    public boolean isLoaded() {
        return currentUrl().contains("/makerlab") && (!makerLabToolLinks().isEmpty() || pageContainsText("MakerLab"));
    }

    public boolean hasCategoryFilters() {
        return !tabLikeElements().isEmpty();
    }

    public boolean hasCommunitySection() {
        return pageContainsText("Community Gallery");
    }

    public Optional<CardSnapshot> firstToolCard() {
        return makerLabToolLinks().stream().findFirst().map(this::buildCardSnapshot);
    }

    public List<CardSnapshot> tools(int limit) {
        return makerLabToolLinks().stream().limit(limit).map(this::buildCardSnapshot).collect(Collectors.toList());
    }

    public boolean firstToolImageLoads() {
        Optional<WebElement> link = makerLabToolLinks().stream().findFirst();
        if (link.isEmpty()) {
            return false;
        }
        return firstImageInside(nearestCard(link.get())).map(this::imageLoaded).orElse(false);
    }
}
