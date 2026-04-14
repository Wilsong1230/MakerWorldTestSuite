package com.makerworld.pages;

import com.makerworld.core.ConfigManager;
import com.makerworld.core.WaitUtils;
import com.makerworld.utils.AssertionUtils;
import com.makerworld.utils.CardSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class ModelDetailPage extends BasePage {
    public ModelDetailPage(WebDriver driver, ConfigManager config) {
        super(driver, config);
    }

    public ModelDetailPage open(String path) {
        openPath(path);
        return this;
    }

    public boolean isLoaded() {
        return AssertionUtils.looksLikeModelDetailUrl(currentUrl()) && !modelTitle().isBlank();
    }

    public String modelTitle() {
        return maybeVisible(
            By.cssSelector("main h1"),
            By.xpath("//h1[normalize-space()]")
        ).map(this::textOf).orElse("");
    }

    public String creatorText() {
        return maybeVisible(
            By.xpath("//main//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'designer')]"),
            By.xpath("//a[contains(@href,'/profile') or contains(@href,'/user')]"),
            By.xpath("//main//a[normalize-space()]")
        ).map(this::textOf).orElse("");
    }

    public boolean hasCoreSections() {
        return !modelTitle().isBlank()
            && pageContainsText("Description")
            && pageContainsText("Print Profile");
    }

    public boolean heroImageLoads() {
        return heroImage().map(this::imageLoaded).orElse(false);
    }

    public boolean switchToDifferentThumbnailChangesHeroImage() {
        Optional<WebElement> hero = heroImage();
        if (hero.isEmpty()) {
            return false;
        }

        String originalSrc = hero.get().getAttribute("src");
        for (WebElement thumbnail : galleryThumbnails()) {
            String thumbnailSrc = thumbnail.getAttribute("src");
            if (thumbnailSrc != null && !thumbnailSrc.equals(originalSrc)) {
                safeClick(thumbnail);
                boolean changed = WaitUtils.waitForCondition(driver, timeout(), () -> {
                    String newSrc = heroImage().map(element -> element.getAttribute("src")).orElse("");
                    return !newSrc.equals(originalSrc) && !newSrc.isBlank();
                });
                if (changed) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean printProfileStatsContainNumbers() {
        String pageText = driver.findElement(By.tagName("body")).getText();
        return pageContainsText("Print Profile") && AssertionUtils.countNumericTokens(pageText) >= 3;
    }

    public Optional<CardSnapshot> firstRelatedModelCard() {
        String current = currentUrl();
        return modelLinks().stream()
            .filter(link -> !hrefOf(link).equals(current))
            .findFirst()
            .map(this::buildCardSnapshot);
    }

    public ModelDetailPage openFirstRelatedModel() {
        WebElement link = modelLinks().stream()
            .filter(candidate -> !hrefOf(candidate).equals(currentUrl()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No related model link found."));
        driver.get(hrefOf(link));
        waitForPageReady();
        return new ModelDetailPage(driver, config);
    }

    public boolean hasEngagementActions() {
        return exists(
            By.xpath("//button[contains(.,'Boost') or contains(.,'Like') or contains(.,'Save') or contains(.,'Follow')]"),
            By.xpath("//a[contains(.,'Boost') or contains(.,'Like') or contains(.,'Save') or contains(.,'Follow')]")
        );
    }

    public boolean tryTriggerEngagementAction() {
        List<By> locators = List.of(
            By.xpath("//button[contains(.,'Save') or contains(.,'Like') or contains(.,'Follow')]"),
            By.xpath("//a[contains(.,'Save') or contains(.,'Like') or contains(.,'Follow')]")
        );
        for (By locator : locators) {
            List<WebElement> elements = visible(locator);
            if (!elements.isEmpty()) {
                safeClick(elements.get(0));
                pauseBriefly();
                return true;
            }
        }
        return false;
    }

    private Optional<WebElement> heroImage() {
        return maybeVisible(
            By.cssSelector("main img"),
            By.xpath("//main//img[@src]")
        );
    }

    private List<WebElement> galleryThumbnails() {
        List<WebElement> candidates = new ArrayList<>();
        for (WebElement image : images()) {
            String src = image.getAttribute("src");
            if (src != null && !src.isBlank()) {
                candidates.add(image);
            }
        }
        return candidates;
    }
}
