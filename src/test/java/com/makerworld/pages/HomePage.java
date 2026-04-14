package com.makerworld.pages;

import com.makerworld.core.ConfigManager;
import com.makerworld.utils.CardSnapshot;
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class HomePage extends BasePage {
    public HomePage(WebDriver driver, ConfigManager config) {
        super(driver, config);
    }

    public HomePage open() {
        openPath("/en");
        return this;
    }

    public boolean isLoaded() {
        return titleOfPage().contains("MakerWorld")
            && exists(By.cssSelector("header"), By.cssSelector("main"), By.tagName("h1"));
    }

    public String heroText() {
        String heading = maybeVisible(
            By.cssSelector("main h1"),
            By.xpath("//main//*[self::h1 or self::h2][normalize-space()]"),
            By.xpath("//h1[normalize-space()]")
        ).map(this::textOf).orElse("");

        if (!heading.isBlank()) {
            return heading;
        }

        return firstFeaturedModelCard().map(CardSnapshot::title).orElse("");
    }

    public List<String> headerLinkTexts() {
        return distinctTexts(firstVisibleList(
            By.cssSelector("header a[href]"),
            By.cssSelector("nav a[href]")
        ));
    }

    public Optional<CardSnapshot> firstFeaturedModelCard() {
        return modelLinks().stream().findFirst().map(this::buildCardSnapshot);
    }

    public boolean firstFeaturedCardImageLoads() {
        Optional<WebElement> firstModelLink = modelLinks().stream().findFirst();
        if (firstModelLink.isEmpty()) {
            return false;
        }
        return firstImageInside(nearestCard(firstModelLink.get())).map(this::imageLoaded).orElse(false);
    }

    public ModelDetailPage openFirstFeaturedModel() {
        WebElement link = modelLinks().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("No featured model link found on home page."));
        driver.get(hrefOf(link));
        waitForPageReady();
        acceptCookiesIfPresent();
        return new ModelDetailPage(driver, config);
    }

    public boolean hasFooterHelpOrFaqLink() {
        return visibleAnchors().stream()
            .anyMatch(anchor -> hrefOf(anchor).contains("/faq") || hrefOf(anchor).contains("/help"));
    }

    public SearchResultsPage searchFor(String term) {
        Optional<WebElement> directInput = maybeVisible(
            By.cssSelector("input[type='search']"),
            By.cssSelector("input[placeholder*='earch']"),
            By.cssSelector("input[name*='search']"),
            By.xpath("//input[contains(@placeholder,'Search') or contains(@placeholder,'search')]"),
            By.xpath("//*[@role='searchbox']"),
            By.xpath("//*[contains(normalize-space(.),'Search models, users, collections, and posts')]")
        );

        if (directInput.isPresent()) {
            WebElement searchInput = directInput.get();
            String tagName = searchInput.getTagName().toLowerCase();
            if ("input".equals(tagName) || "textarea".equals(tagName)) {
                typeAndSubmit(searchInput, term);
            } else {
                safeClick(searchInput);
                pauseBriefly();
                driver.switchTo().activeElement().sendKeys(term);
                driver.switchTo().activeElement().sendKeys(org.openqa.selenium.Keys.ENTER);
            }
        } else {
            throw new NoSuchElementException("Search bar is not currently visible on the MakerWorld home page.");
        }

        tryWaitForPageReady();
        return new SearchResultsPage(driver, config);
    }

    public boolean openNavigationItem(String label) {
        Optional<WebElement> match = firstVisibleList(
            By.xpath("//*[self::a or self::button or @role='button'][contains(normalize-space(.),'" + label + "')]"),
            By.xpath("//*[contains(@class,'nav') or contains(@class,'side')]//*[contains(normalize-space(.),'" + label + "')]")
        ).stream().findFirst();
        if (match.isPresent()) {
            String originalUrl = currentUrl();
            safeClick(match.get());
            pauseBriefly();
            waitForPageReady();
            acceptCookiesIfPresent();
            return !currentUrl().equals(originalUrl) || pageContainsText(label);
        }
        return false;
    }
}
