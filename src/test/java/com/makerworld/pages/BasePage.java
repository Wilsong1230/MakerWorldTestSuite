package com.makerworld.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.makerworld.core.ConfigManager;
import com.makerworld.core.WaitUtils;
import com.makerworld.utils.AssertionUtils;
import com.makerworld.utils.CardSnapshot;
import com.twocaptcha.TwoCaptcha;
import com.twocaptcha.captcha.Turnstile;

public abstract class BasePage {
    protected final WebDriver driver;
    protected final ConfigManager config;
    protected final WebDriverWait wait;
    protected final JavascriptExecutor js;

    protected BasePage(WebDriver driver, ConfigManager config) {
        this.driver = driver;
        this.config = config;
        this.wait = new WebDriverWait(driver, timeout());
        this.js = (JavascriptExecutor) driver;
    }

    protected Duration timeout() {
        return Duration.ofSeconds(config.getTimeoutSeconds());
    }

    protected void openPath(String path) {
        driver.get(resolveUrl(path));
        // PATCH: Solve if Cloudflare block is detected
        if (isSecurityVerificationPage()) {
            solveCloudflareChallenge();
        }
        waitForPageReady();
        acceptCookiesIfPresent();
    }

    protected String resolveUrl(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (path.startsWith("/en")) {
            return config.joinUrl(config.getSiteRootUrl(), path);
        }
        if (path.startsWith("/")) {
            return config.joinUrl(config.getSiteRootUrl(), path);
        }
        return config.joinUrl(config.getLocaleBaseUrl(), path);
    }

    protected void waitForPageReady() {
        WaitUtils.waitForDocumentReady(driver, timeout());
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
    }

    protected void acceptCookiesIfPresent() {
        List<WebElement> buttons = driver.findElements(By.xpath(
            "//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'accept all')]"
        ));
        for (WebElement button : buttons) {
            try {
                if (button.isDisplayed()) {
                    safeClick(button);
                    pauseBriefly();
                    return;
                }
            } catch (StaleElementReferenceException ignored) {
                return;
            }
        }
    }

    protected WebElement firstVisible(By... locators) {
        for (By locator : locators) {
            List<WebElement> elements = visible(locator);
            if (!elements.isEmpty()) {
                return elements.get(0);
            }
        }
        throw new NoSuchElementException("Unable to find visible element using provided locators.");
    }

    protected Optional<WebElement> maybeVisible(By... locators) {
        for (By locator : locators) {
            List<WebElement> elements = visible(locator);
            if (!elements.isEmpty()) {
                return Optional.of(elements.get(0));
            }
        }
        return Optional.empty();
    }

    protected List<WebElement> firstVisibleList(By... locators) {
        for (By locator : locators) {
            List<WebElement> elements = visible(locator);
            if (!elements.isEmpty()) {
                return elements;
            }
        }
        return List.of();
    }

    protected List<WebElement> visible(By locator) {
        List<WebElement> results = new ArrayList<>();
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed()) {
                    results.add(element);
                }
            } catch (StaleElementReferenceException ignored) {
                // Ignore stale elements during dynamic page updates.
            }
        }
        return results;
    }

    protected boolean exists(By... locators) {
        return maybeVisible(locators).isPresent();
    }

    protected String textOf(WebElement element) {
        return AssertionUtils.normalizeWhitespace(element.getText());
    }

    protected String hrefOf(WebElement element) {
        return AssertionUtils.normalizeWhitespace(element.getAttribute("href"));
    }

    protected String titleOfPage() {
        return AssertionUtils.normalizeWhitespace(driver.getTitle());
    }

    protected String currentUrl() {
        return driver.getCurrentUrl();
    }

    public boolean isSecurityVerificationPage() {
        String url = currentUrl().toLowerCase();
        return pageContainsText("Performing security verification")
            || pageContainsText("security verification")
            || url.contains("challenge")
            || url.contains("turnstile");
    }

    protected void safeClick(WebElement element) {
        scrollIntoView(element);
        wait.until(ExpectedConditions.elementToBeClickable(element));
        try {
            element.click();
        } catch (Exception ex) {
            js.executeScript("arguments[0].click();", element);
        }
    }

    protected void typeAndSubmit(WebElement element, String value) {
        scrollIntoView(element);
        element.clear();
        element.sendKeys(Keys.chord(Keys.COMMAND, "a"));
        element.sendKeys(value);
        element.sendKeys(Keys.ENTER);
    }

    protected void scrollIntoView(WebElement element) {
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    protected List<WebElement> visibleAnchors() {
        return visible(By.cssSelector("a[href]"));
    }

    protected List<WebElement> linksMatching(Predicate<String> matcher) {
        return visibleAnchors().stream()
            .filter(link -> matcher.test(hrefOf(link)))
            .collect(Collectors.toList());
    }

    protected List<WebElement> modelLinks() {
        return linksMatching(href -> href.matches(".*/models/\\d+.*"));
    }

    protected List<WebElement> contestLinks() {
        return linksMatching(href -> href.matches(".*/contests/[^/?#]+.*") && !href.endsWith("/rules"));
    }

    protected List<WebElement> crowdfundingLinks() {
        return linksMatching(href -> href.matches(".*/crowdfunding/[^/?#]+.*"));
    }

    protected List<WebElement> makerLabToolLinks() {
        return linksMatching(href -> href.contains("/makerlab/") && !href.endsWith("/makerlab"));
    }

    protected List<WebElement> tabLikeElements() {
        List<WebElement> controls = new ArrayList<>();
        controls.addAll(firstVisibleList(By.cssSelector("[role='tab']")));
        if (controls.isEmpty()) {
            controls.addAll(visible(By.xpath("//button[@aria-selected or @role='tab'][normalize-space()]")));
        }
        return controls.stream()
            .filter(element -> !textOf(element).isBlank())
            .collect(Collectors.toList());
    }

    protected boolean clickFirstAlternativeTab() {
        List<WebElement> tabs = tabLikeElements();
        if (tabs.size() < 2) {
            return false;
        }

        String originalUrl = currentUrl();
        String originalText = textOf(tabs.get(0));

        for (WebElement tab : tabs) {
            String ariaSelected = tab.getAttribute("aria-selected");
            if (!"true".equalsIgnoreCase(ariaSelected) && !textOf(tab).equalsIgnoreCase(originalText)) {
                safeClick(tab);
                waitForPageReady();
                boolean urlChanged = WaitUtils.waitForValueChange(driver, this::currentUrl, originalUrl, timeout());
                boolean selectionChanged = "true".equalsIgnoreCase(tab.getAttribute("aria-selected"));
                return urlChanged || selectionChanged;
            }
        }

        return false;
    }

    protected boolean imageLoaded(WebElement image) {
        Object result = js.executeScript(
            "return arguments[0] && arguments[0].tagName === 'IMG' && arguments[0].complete && arguments[0].naturalWidth > 0 && arguments[0].naturalHeight > 0;",
            image
        );
        return Boolean.TRUE.equals(result);
    }

    protected Optional<WebElement> firstImageInside(WebElement container) {
        List<WebElement> images = container.findElements(By.cssSelector("img"));
        return images.stream().filter(WebElement::isDisplayed).findFirst();
    }

    protected WebElement nearestCard(WebElement node) {
        Object result = js.executeScript("return arguments[0].closest('article, li, section, div, a');", node);
        if (result instanceof WebElement element) {
            return element;
        }
        return node;
    }

    protected CardSnapshot buildCardSnapshot(WebElement link) {
        String href = hrefOf(link);
        String title = extractTitle(link);
        WebElement card = nearestCard(link);
        String cardText = textOf(card);
        String subtitle = extractSubtitle(cardText, title);
        return new CardSnapshot(title, subtitle, href, cardText);
    }

    private String extractTitle(WebElement link) {
        String directText = textOf(link);
        if (!directText.isBlank()) {
            return directText;
        }

        String attributeTitle = AssertionUtils.normalizeWhitespace(link.getAttribute("title"));
        if (!attributeTitle.isBlank()) {
            return attributeTitle;
        }

        for (By locator : List.of(
            By.cssSelector("h1,h2,h3,h4,strong,span,p"),
            By.cssSelector("img[alt]"))) {
            List<WebElement> matches = link.findElements(locator);
            for (WebElement match : matches) {
                String candidate = "img".equalsIgnoreCase(match.getTagName())
                    ? AssertionUtils.normalizeWhitespace(match.getAttribute("alt"))
                    : textOf(match);
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        }

        return hrefOf(link);
    }

    private String extractSubtitle(String cardText, String title) {
        if (cardText.isBlank()) {
            return "";
        }

        for (String line : cardText.split("\\R")) {
            String normalizedLine = AssertionUtils.normalizeWhitespace(line);
            if (!normalizedLine.isBlank() && !AssertionUtils.normalizeForComparison(normalizedLine)
                .equals(AssertionUtils.normalizeForComparison(title))) {
                return normalizedLine;
            }
        }
        return "";
    }

    protected boolean pageContainsText(String needle) {
        return AssertionUtils.normalizedContains(driver.findElement(By.tagName("body")).getText(), needle);
    }

    protected List<String> distinctTexts(List<WebElement> elements) {
        Set<String> values = new LinkedHashSet<>();
        for (WebElement element : elements) {
            String text = textOf(element);
            if (!text.isBlank()) {
                values.add(text);
            }
        }
        return new ArrayList<>(values);
    }

    protected List<WebElement> images() {
        return visible(By.cssSelector("img"));
    }

    protected void hover(WebElement element) {
        new Actions(driver).moveToElement(element).perform();
    }

    protected boolean clickLinkByHrefFragment(String hrefFragment) {
        Optional<WebElement> link = visibleAnchors().stream()
            .filter(anchor -> hrefOf(anchor).contains(hrefFragment))
            .findFirst();

        if (link.isPresent()) {
            safeClick(link.get());
            waitForPageReady();
            return true;
        }
        return false;
    }

    protected boolean waitForUrlChange(String originalUrl) {
        return WaitUtils.waitForValueChange(driver, this::currentUrl, originalUrl, timeout());
    }

    protected void pauseBriefly() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected boolean tryWaitForPageReady() {
        try {
            waitForPageReady();
            return true;
        } catch (TimeoutException ex) {
            return false;
        }
    }
    private void solveCloudflareChallenge() {
    String apiKey = config.getTwoCaptchaKey();
    if (apiKey.isBlank()) {
        System.err.println("Cloudflare detected but MW_2CAPTCHA_KEY is not configured.");
        return;
    }

    try {
        System.out.println("Cloudflare Challenge detected. Solving via 2Captcha...");
        TwoCaptcha solver = new TwoCaptcha(apiKey);
        
        Turnstile captcha = new Turnstile();
        captcha.setSiteKey("0x4AAAAAAAPGX7kh4AO_iqCW"); // Sitekey found in MakerWorld scripts
        captcha.setUrl(currentUrl());
        
        // Pass the browser User-Agent to match fingerprints
        String ua = (String) js.executeScript("return navigator.userAgent;");
        captcha.setUserAgent(ua);

        solver.solve(captcha);
        String token = captcha.getCode();

        // Inject the token and trigger the callback
        js.executeScript("document.getElementsByName('cf-turnstile-response')[0].value='" + token + "';");
        js.executeScript("if (window.turnstile) { turnstile.callback('" + token + "'); }");
        
        // Wait for the challenge to be cleared from the UI
        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("challenge")));
        System.out.println("Cloudflare Challenge solved successfully.");
    } catch (Exception e) {
        System.err.println("Failed to solve Cloudflare: " + e.getMessage());
    }
}
}
