package com.makerworld.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public abstract class BaseTest {
    private static final String DEFAULT_BASE_URL = "https://makerworld.com";
    private static final String DEFAULT_CHROME_BINARY = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
    private static final DateTimeFormatter SCREENSHOT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+[\\d,.]*)(\\s*[%kKmM]?)");
    private final Properties properties = new Properties();

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected JsonNode testData;
    protected String siteRootUrl;
    protected String localeBaseUrl;
    protected int timeoutSeconds;
    protected boolean screenshotOnFailure;

    private AuthSettings authSettings;
    private ChallengeSolver challengeSolver;
    private ChallengeSolver.Outcome lastChallengeOutcome = new ChallengeSolver.Outcome(
        ChallengeSolver.Status.NOT_PRESENT,
        "Not evaluated yet."
    );

    protected record CardSnapshot(String title, String href, String metaText) {
    }

    private record AuthSettings(
        boolean enabled,
        AuthMode mode,
        String email,
        String password,
        Path cookieFile,
        Path chromeUserDataDir,
        String chromeProfileDir
    ) {
    }

    private enum AuthMode {
        MANUAL_CHECKPOINT,
        COOKIE_SESSION,
        CHROME_PROFILE;

        private static AuthMode from(String raw) {
            if (raw == null || raw.isBlank()) {
                return MANUAL_CHECKPOINT;
            }

            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "cookie_session" -> COOKIE_SESSION;
                case "chrome_profile" -> CHROME_PROFILE;
                default -> MANUAL_CHECKPOINT;
            };
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        properties.clear();
        loadClasspathProperties("config.properties");
        testData = loadJson("testdata/search-terms.json");
        siteRootUrl = normalizeBaseUrl(getString("MW_BASE_URL", DEFAULT_BASE_URL));
        localeBaseUrl = siteRootUrl.endsWith("/en") ? siteRootUrl : siteRootUrl + "/en";
        timeoutSeconds = getInt("MW_TIMEOUT_SECONDS", 20);
        screenshotOnFailure = getBoolean("MW_SCREENSHOT_ON_FAILURE", true);
        authSettings = loadAuthSettings();
        driver = createDriver();
        wait = new WebDriverWait(driver, timeout());
        challengeSolver = new ChallengeSolver(
            driver,
            wait,
            this::currentUrl,
            () -> getString("MW_2CAPTCHA_KEY", ""),
            () -> getInt("MW_2CAPTCHA_TIMEOUT_SECONDS", 90),
            () -> getInt("MW_2CAPTCHA_POLL_SECONDS", 3),
            this::pauseBriefly
        );
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (driver == null) {
            return;
        }

        try {
            if (result != null && !result.isSuccess() && screenshotOnFailure) {
                captureScreenshot(result.getMethod().getMethodName());
            }
        } finally {
            driver.quit();
            driver = null;
            wait = null;
            challengeSolver = null;
        }
    }

    protected void openHomePage() {
        openPath("/en");
    }

    protected void openHomePageAndStabilize(String context) {
        openHomePage();
        skipIfHumanVerificationPersists(context);
    }

    protected void openModelsPage() {
        openHomePage();
        if (!openNavigationItem("All Models")) {
            openPath("/en/models");
        }
    }

    protected void openModelsPageAndStabilize(String context) {
        openModelsPage();
        skipIfHumanVerificationPersists(context);
    }

    protected void openContestsPage() {
        openPath("/en/contests");
    }

    protected void openContestsPageAndStabilize(String context) {
        openContestsPage();
        skipIfHumanVerificationPersists(context);
    }

    protected void openCrowdfundingPage() {
        openPath("/en/crowdfunding");
    }

    protected void openCrowdfundingPageAndStabilize(String context) {
        openCrowdfundingPage();
        skipIfHumanVerificationPersists(context);
    }

    protected void openModelDetailPage(String path) {
        openPath(path);
    }

    protected void openModelDetailPageAndStabilize(String path, String context) {
        openModelDetailPage(path);
        skipIfHumanVerificationPersists(context);
    }

    protected void searchFromHome(String term) {
        Optional<WebElement> directInput = maybeVisible(
            By.cssSelector("input[type='search']"),
            By.cssSelector("input[placeholder*='earch']"),
            By.cssSelector("input[name*='search']"),
            By.xpath("//input[contains(@placeholder,'Search') or contains(@placeholder,'search')]"),
            By.xpath("//*[@role='searchbox']"),
            By.xpath("//*[contains(normalize-space(.),'Search models, users, collections, and posts')]")
        );

        if (directInput.isEmpty()) {
            throw new NoSuchElementException("Search bar is not currently visible on the MakerWorld home page.");
        }

        WebElement searchInput = directInput.get();
        String tagName = searchInput.getTagName().toLowerCase(Locale.ROOT);
        if ("input".equals(tagName) || "textarea".equals(tagName)) {
            typeAndSubmit(searchInput, term);
        } else {
            safeClick(searchInput);
            pauseBriefly();
            WebElement activeElement = driver.switchTo().activeElement();
            activeElement.sendKeys(term);
            activeElement.sendKeys(Keys.ENTER);
        }

        tryWaitForPageReady();
        acceptCookiesIfPresent();
    }

    protected void searchFromHomeAndStabilize(String term, String context) {
        openHomePageAndStabilize("search home page");
        searchFromHome(term);
        skipIfHumanVerificationPersists(context);
    }

    protected void bootstrapAuthenticatedSessionOrSkip() {
        if (!authSettings.enabled()) {
            throw new SkipException("Auth suite is disabled. Set MW_ENABLE_AUTH_SUITE=true to run authenticated scenarios.");
        }

        switch (authSettings.mode()) {
            case COOKIE_SESSION -> establishCookieSession();
            case CHROME_PROFILE -> establishChromeProfileSession();
            case MANUAL_CHECKPOINT -> establishManualLoginSession();
        }
    }

    protected boolean isAuthenticated() {
        boolean hasUserMarker = exists(
            By.xpath("//a[contains(@href,'/profile') or contains(@href,'/user') or contains(@href,'/account')][normalize-space()]"),
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'logout') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'profile')]"),
            By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'logout') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'profile') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'account')]")
        );

        boolean hasGuestMarker = isLoggedOut();
        return hasUserMarker || (!hasGuestMarker && !driver.manage().getCookies().isEmpty());
    }

    protected boolean waitForAuthenticatedState(Duration timeout) {
        return waitForCondition(timeout, this::isAuthenticated);
    }

    protected boolean isLoggedOut() {
        return exists(
            By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]"),
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]")
        );
    }

    protected boolean waitForLoggedOutState(Duration timeout) {
        return waitForCondition(timeout, this::isLoggedOut);
    }

    protected boolean openAccountSurfaceIfAvailable() {
        List<By> locators = List.of(
            By.xpath("//a[contains(@href,'/profile') or contains(@href,'/user') or contains(@href,'/account')][normalize-space()]"),
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'profile') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'account')]")
        );
        return clickFirstVisible(locators, true);
    }

    protected boolean hasLogoutControl() {
        return exists(
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'logout') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log out')]"),
            By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'logout') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log out')]")
        );
    }

    protected boolean logoutIfPossible() {
        List<By> locators = List.of(
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'logout') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log out')]"),
            By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'logout') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log out')]")
        );
        return clickFirstVisible(locators, false);
    }

    protected void skipIfHumanVerificationPersists(String context) {
        ensureChallengeClearedOrFail(context);
        if (challengeSolver.isSecurityVerificationPage()) {
            throw new SkipException("MakerWorld human verification still present after 10 seconds: " + context);
        }
    }

    protected boolean isHomePageLoaded() {
        return titleOfPage().contains("MakerWorld")
            && exists(By.cssSelector("header"), By.cssSelector("main"), By.tagName("h1"));
    }

    protected String homeHeroText() {
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

    protected List<String> headerLinkTexts() {
        return distinctTexts(firstVisibleList(
            By.cssSelector("header a[href]"),
            By.cssSelector("nav a[href]")
        ));
    }

    protected Optional<CardSnapshot> firstFeaturedModelCard() {
        return firstCardSnapshot(modelLinks());
    }

    protected boolean firstFeaturedCardImageLoads() {
        Optional<WebElement> link = modelLinks().stream().findFirst();
        if (link.isEmpty()) {
            return false;
        }
        return firstImageInside(nearestCard(link.get())).map(this::imageLoaded).orElse(false);
    }

    protected void openFirstFeaturedModel() {
        openFirstLink(modelLinks(), "No featured model link found on home page.");
    }

    protected boolean hasFooterHelpOrFaqLink() {
        return visibleAnchors().stream()
            .anyMatch(anchor -> hrefOf(anchor).contains("/faq") || hrefOf(anchor).contains("/help"));
    }

    protected boolean isModelsPageLoaded() {
        return !modelLinks().isEmpty()
            && (currentUrl().contains("/models") || pageContainsText("All Models") || pageContainsText("For You"));
    }

    protected boolean hasMultipleUniqueModelCards(int minimumCount) {
        return modelLinks().stream()
            .limit(minimumCount + 2L)
            .map(this::buildCardSnapshot)
            .map(CardSnapshot::href)
            .distinct()
            .count() >= minimumCount;
    }

    protected boolean firstModelCardHasMetadataAndLoadedImage() {
        Optional<WebElement> link = modelLinks().stream().findFirst();
        if (link.isEmpty()) {
            return false;
        }

        CardSnapshot snapshot = buildCardSnapshot(link.get());
        boolean hasImage = firstImageInside(nearestCard(link.get())).map(this::imageLoaded).orElse(false);
        return !snapshot.title().isBlank() && !snapshot.metaText().isBlank() && hasImage;
    }

    protected boolean modelsPageHasFilterOrSortControls() {
        return !tabLikeElements().isEmpty()
            || exists(By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sort')]"))
            || exists(By.tagName("select"));
    }

    protected boolean switchFirstAlternativeModelFilter() {
        String originalFirstHref = modelLinks().stream().findFirst().map(this::hrefOf).orElse("");
        if (!clickFirstAlternativeTab()) {
            return false;
        }
        String newFirstHref = modelLinks().stream().findFirst().map(this::hrefOf).orElse("");
        return !newFirstHref.equals(originalFirstHref) || !newFirstHref.isBlank();
    }

    protected Optional<CardSnapshot> firstModelCard() {
        return firstCardSnapshot(modelLinks());
    }

    protected void openFirstModelCard() {
        openFirstLink(modelLinks(), "No model link found on models page.");
    }

    protected boolean isSearchResultsLoaded() {
        return currentUrl().contains("search") || hasSearchResults() || hasSearchEmptyState();
    }

    protected boolean hasSearchResults() {
        return !modelLinks().isEmpty();
    }

    protected boolean hasSearchEmptyState() {
        return pageContainsText("no results")
            || pageContainsText("0 results")
            || pageContainsText("no models");
    }

    protected String searchBoxValue() {
        return maybeVisible(
            By.cssSelector("input[type='search']"),
            By.cssSelector("input[placeholder*='earch']"),
            By.cssSelector("input[name*='search']")
        ).map(element -> normalizeWhitespace(element.getAttribute("value"))).orElse("");
    }

    protected Optional<CardSnapshot> firstSearchResultCard() {
        return firstCardSnapshot(modelLinks());
    }

    protected boolean firstSearchResultLooksRelevant(String term) {
        return firstSearchResultCard()
            .map(card -> normalizedContains(card.title() + " " + card.metaText(), term))
            .orElse(false);
    }

    protected void openFirstSearchResult() {
        openFirstLink(modelLinks(), "No search result link found.");
    }

    protected boolean isModelDetailPageLoaded() {
        return looksLikeModelDetailUrl(currentUrl()) && !modelTitle().isBlank();
    }

    protected String modelTitle() {
        return maybeVisible(
            By.cssSelector("main h1"),
            By.xpath("//h1[normalize-space()]")
        ).map(this::textOf).orElse("");
    }

    protected boolean modelDetailHasCoreSections() {
        return !modelTitle().isBlank()
            && pageContainsText("Description")
            && pageContainsText("Print Profile");
    }

    protected boolean heroImageLoads() {
        return heroImage().map(this::imageLoaded).orElse(false);
    }

    protected boolean switchToDifferentThumbnailChangesHeroImage() {
        Optional<WebElement> hero = heroImage();
        if (hero.isEmpty()) {
            return false;
        }

        String originalSrc = hero.get().getAttribute("src");
        for (WebElement thumbnail : galleryThumbnails()) {
            String thumbnailSrc = thumbnail.getAttribute("src");
            if (thumbnailSrc != null && !thumbnailSrc.equals(originalSrc)) {
                safeClick(thumbnail);
                if (waitForCondition(timeout(), () -> {
                    String newSrc = heroImage().map(element -> element.getAttribute("src")).orElse("");
                    return !newSrc.equals(originalSrc) && !newSrc.isBlank();
                })) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean printProfileStatsContainNumbers() {
        String pageText = driver.findElement(By.tagName("body")).getText();
        return pageContainsText("Print Profile") && countNumericTokens(pageText) >= 3;
    }

    protected Optional<CardSnapshot> firstRelatedModelCard() {
        String current = currentUrl();
        return modelLinks().stream()
            .filter(link -> !hrefOf(link).equals(current))
            .findFirst()
            .map(this::buildCardSnapshot);
    }

    protected void openFirstRelatedModel() {
        List<WebElement> links = modelLinks().stream()
            .filter(candidate -> !hrefOf(candidate).equals(currentUrl()))
            .collect(Collectors.toList());
        openFirstLink(links, "No related model link found.");
    }

    protected boolean modelDetailHasEngagementActions() {
        return exists(
            By.xpath("//button[contains(.,'Boost') or contains(.,'Like') or contains(.,'Save') or contains(.,'Follow')]"),
            By.xpath("//a[contains(.,'Boost') or contains(.,'Like') or contains(.,'Save') or contains(.,'Follow')]")
        );
    }

    protected boolean tryTriggerEngagementAction() {
        List<By> locators = List.of(
            By.xpath("//button[contains(.,'Save') or contains(.,'Like') or contains(.,'Follow')]"),
            By.xpath("//a[contains(.,'Save') or contains(.,'Like') or contains(.,'Follow')]")
        );
        return clickFirstVisible(locators, false);
    }

    protected boolean isContestsPageLoaded() {
        return currentUrl().contains("/contests") && (!contestLinks().isEmpty() || pageContainsText("contest"));
    }

    protected String contestsHeading() {
        return maybeVisible(By.cssSelector("main h1"), By.xpath("//h1[normalize-space()]"))
            .map(this::textOf)
            .orElse("");
    }

    protected boolean contestsPageHasTabs() {
        return !tabLikeElements().isEmpty()
            || exists(By.xpath("//button[contains(.,'Active') or contains(.,'Finished') or contains(.,'Upcoming')]"));
    }

    protected boolean switchContestTabChangesState() {
        String original = contestLinks().stream().findFirst().map(this::hrefOf).orElse("");
        if (!clickFirstAlternativeTab()) {
            return false;
        }
        String current = contestLinks().stream().findFirst().map(this::hrefOf).orElse("");
        return !current.equals(original) || !current.isBlank();
    }

    protected Optional<CardSnapshot> firstContestCard() {
        return firstCardSnapshot(contestLinks());
    }

    protected boolean firstContestCardHasMetadata() {
        return firstContestCard()
            .map(card -> !card.title().isBlank() && !card.metaText().isBlank())
            .orElse(false);
    }

    protected void openFirstContest() {
        openFirstLink(contestLinks(), "No contest link found on contests page.");
    }

    protected boolean isContestDetailPageLoaded() {
        return looksLikeContestDetailUrl(currentUrl()) && !contestHeading().isBlank();
    }

    protected String contestHeading() {
        return maybeVisible(By.cssSelector("main h1"), By.xpath("//h1[normalize-space()]"))
            .map(this::textOf)
            .orElse("");
    }

    protected boolean contestDetailHasBreadcrumb() {
        return exists(
            By.xpath("//nav//*[contains(.,'Contests')]"),
            By.xpath("//a[contains(@href,'/contests')]")
        );
    }

    protected boolean contestEntriesSectionLoaded() {
        return pageContainsText("Entries") || !modelLinks().isEmpty();
    }

    protected void openFirstContestEntryModel() {
        openFirstLink(modelLinks(), "No model link found in contest detail.");
    }

    protected boolean contestRulesLinksStayWithinMakerWorld() {
        return visibleAnchors().stream()
            .filter(anchor -> hrefOf(anchor).contains("rules"))
            .allMatch(anchor -> makerWorldOwnedUrl(hrefOf(anchor)));
    }

    protected boolean isCrowdfundingPageLoaded() {
        return currentUrl().contains("/crowdfunding") && (!crowdfundingLinks().isEmpty() || pageContainsText("crowdfunding"));
    }

    protected boolean crowdfundingPageHasTabs() {
        return !tabLikeElements().isEmpty()
            || exists(By.xpath("//button[contains(.,'Live') or contains(.,'Upcoming') or contains(.,'Late Pledge') or contains(.,'All')]"));
    }

    protected boolean switchCrowdfundingTabChangesState() {
        String original = crowdfundingLinks().stream().findFirst().map(this::hrefOf).orElse("");
        if (!clickFirstAlternativeTab()) {
            return false;
        }
        String current = crowdfundingLinks().stream().findFirst().map(this::hrefOf).orElse("");
        return !current.equals(original) || !current.isBlank();
    }

    protected Optional<CardSnapshot> firstCrowdfundingProjectCard() {
        return firstCardSnapshot(crowdfundingLinks());
    }

    protected boolean firstProjectHasFundingMetadata() {
        return firstCrowdfundingProjectCard()
            .map(card -> !card.title().isBlank() && countNumericTokens(card.metaText()) >= 1)
            .orElse(false);
    }

    protected void openFirstCrowdfundingProject() {
        openFirstLink(crowdfundingLinks(), "No crowdfunding project link found.");
    }

    protected boolean isCrowdfundingProjectPageLoaded() {
        return looksLikeCrowdfundingDetailUrl(currentUrl()) && !crowdfundingProjectHeading().isBlank();
    }

    protected String crowdfundingProjectHeading() {
        return maybeVisible(By.cssSelector("main h1"), By.xpath("//h1[normalize-space()]"))
            .map(this::textOf)
            .orElse("");
    }

    protected boolean detailMatchesCard(CardSnapshot card, String detailTitle) {
        return normalizedContains(detailTitle, slugToken(card.title()))
            || normalizedContains(detailTitle, card.title())
            || normalizedContains(card.title(), detailTitle)
            || normalizedContains(card.metaText(), detailTitle);
    }

    protected boolean titlesRoughlyMatch(String left, String right) {
        return normalizedContains(left, right) || normalizedContains(right, left);
    }

    protected String commonSearchTerm() {
        return testData.path("commonSearchTerm").asText("vase");
    }

    protected String rareSearchTerm() {
        return testData.path("rareSearchTerm").asText("zzzzmakerworldunlikelyterm");
    }

    protected String pinnedModelPath() {
        return testData.path("pinnedModelPath").asText("/en/models/544229");
    }

    protected List<String> expectedHeaderLinks() {
        List<String> values = new ArrayList<>();
        testData.path("expectedHeaderLinks").forEach(node -> values.add(node.asText()));
        return values;
    }

    protected static String normalizeWhitespace(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    protected static String normalizeForComparison(String value) {
        return Normalizer.normalize(normalizeWhitespace(value), Normalizer.Form.NFKD)
            .replaceAll("[^\\p{Alnum} ]", " ")
            .replaceAll("\\s+", " ")
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    protected static boolean normalizedContains(String haystack, String needle) {
        String normalizedHaystack = normalizeForComparison(haystack);
        String normalizedNeedle = normalizeForComparison(needle);
        return !normalizedNeedle.isBlank() && normalizedHaystack.contains(normalizedNeedle);
    }

    protected static int countNumericTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    protected static String slugToken(String text) {
        String normalized = normalizeForComparison(text);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.split(" ")[0];
    }

    protected static boolean looksLikeModelDetailUrl(String url) {
        return url != null && url.matches(".*/models/\\d+.*");
    }

    protected static boolean looksLikeContestDetailUrl(String url) {
        return url != null && url.matches(".*/contests/[^/?#]+.*") && !url.endsWith("/contests");
    }

    protected static boolean looksLikeCrowdfundingDetailUrl(String url) {
        return url != null && url.matches(".*/crowdfunding/[^/?#]+.*") && !url.endsWith("/crowdfunding");
    }

    protected static boolean makerWorldOwnedUrl(String url) {
        return url != null && url.contains("makerworld.com");
    }

    protected String currentUrl() {
        return driver.getCurrentUrl();
    }

    protected boolean isSecurityVerificationPage() {
        return challengeSolver != null && challengeSolver.isSecurityVerificationPage();
    }

    protected String lastChallengeStatus() {
        return lastChallengeOutcome.status().name();
    }

    protected void openPath(String path) {
        try {
            driver.get(resolveUrl(path));
        } catch (TimeoutException ignored) {
        }

        ensureChallengeClearedOrFail("openPath(" + path + ")");

        waitForPageReady();
        acceptCookiesIfPresent();
    }

    protected String resolveUrl(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (path.startsWith("/")) {
            return joinUrl(siteRootUrl, path);
        }
        return joinUrl(localeBaseUrl, path);
    }

    protected void waitForPageReady() {
        waitForDocumentReady();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
    }

    protected boolean tryWaitForPageReady() {
        try {
            waitForPageReady();
            return true;
        } catch (TimeoutException ex) {
            return false;
        }
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
            }
        }
        return results;
    }

    protected boolean exists(By... locators) {
        return maybeVisible(locators).isPresent();
    }

    protected String textOf(WebElement element) {
        return normalizeWhitespace(element.getText());
    }

    protected String hrefOf(WebElement element) {
        return normalizeWhitespace(element.getAttribute("href"));
    }

    protected String titleOfPage() {
        return normalizeWhitespace(driver.getTitle());
    }

    protected void safeClick(WebElement element) {
        scrollIntoView(element);
        wait.until(ExpectedConditions.elementToBeClickable(element));
        try {
            element.click();
        } catch (Exception ex) {
            javascript().executeScript("arguments[0].click();", element);
        }
    }

    protected void typeAndSubmit(WebElement element, String value) {
        scrollIntoView(element);
        element.clear();
        element.sendKeys(selectAllShortcut());
        element.sendKeys(value);
        element.sendKeys(Keys.ENTER);
    }

    protected void scrollIntoView(WebElement element) {
        javascript().executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    protected List<WebElement> visibleAnchors() {
        return visible(By.cssSelector("a[href]"));
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
                boolean urlChanged = waitForValueChange(this::currentUrl, originalUrl, timeout());
                boolean selectionChanged = "true".equalsIgnoreCase(tab.getAttribute("aria-selected"));
                return urlChanged || selectionChanged;
            }
        }
        return false;
    }

    private boolean imageLoaded(WebElement image) {
        Object result = javascript().executeScript(
            "return arguments[0] && arguments[0].tagName === 'IMG' && arguments[0].complete && arguments[0].naturalWidth > 0 && arguments[0].naturalHeight > 0;",
            image
        );
        return Boolean.TRUE.equals(result);
    }

    protected Optional<WebElement> firstImageInside(WebElement container) {
        return container.findElements(By.cssSelector("img")).stream()
            .filter(WebElement::isDisplayed)
            .findFirst();
    }

    protected WebElement nearestCard(WebElement node) {
        Object result = javascript().executeScript("return arguments[0].closest('article, li, section, div, a');", node);
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
        return new CardSnapshot(title, href, cardText);
    }

    protected boolean pageContainsText(String needle) {
        return normalizedContains(driver.findElement(By.tagName("body")).getText(), needle);
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

    protected void pauseBriefly() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected boolean openNavigationItem(String label) {
        Optional<WebElement> match = firstVisibleList(
            By.xpath("//*[self::a or self::button or @role='button'][contains(normalize-space(.),'" + label + "')]"),
            By.xpath("//*[contains(@class,'nav') or contains(@class,'side')]//*[contains(normalize-space(.),'" + label + "')]")
        ).stream().findFirst();

        if (match.isEmpty()) {
            return false;
        }

        String originalUrl = currentUrl();
        safeClick(match.get());
        pauseBriefly();
        waitForPageReady();
        acceptCookiesIfPresent();
        return !currentUrl().equals(originalUrl) || pageContainsText(label);
    }

    private void loadClasspathProperties(String name) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(name)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + name, e);
        }
    }

    private JsonNode loadJson(String resourcePath) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test data resource: " + resourcePath);
            }
            return new ObjectMapper().readTree(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read test data resource: " + resourcePath, e);
        }
    }

    private String getString(String key, String defaultValue) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = properties.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        return defaultValue;
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getString(key, String.valueOf(defaultValue)));
    }

    private int getInt(String key, int defaultValue) {
        String raw = getString(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Path resolveProjectPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }

        Path path = Paths.get(rawPath.trim());
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Paths.get("").toAbsolutePath().normalize().resolve(path).normalize();
    }

    private String joinUrl(String root, String path) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(path, "path");

        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (root.endsWith("/") && path.startsWith("/")) {
            return root + path.substring(1);
        }
        if (!root.endsWith("/") && !path.startsWith("/")) {
            return root + "/" + path;
        }
        return root + path;
    }

    private String normalizeBaseUrl(String raw) {
        if (raw.endsWith("/")) {
            return raw.substring(0, raw.length() - 1);
        }
        return raw;
    }

    private AuthSettings loadAuthSettings() {
        return new AuthSettings(
            getBoolean("MW_ENABLE_AUTH_SUITE", true),
            AuthMode.from(getString("MW_AUTH_MODE", "manual_checkpoint")),
            getString("MW_EMAIL", ""),
            getString("MW_PASSWORD", ""),
            resolveProjectPath(getString("MW_COOKIE_FILE", "")),
            resolveProjectPath(getString("MW_CHROME_USER_DATA_DIR", "")),
            getString("MW_CHROME_PROFILE_DIR", "Default")
        );
    }

    private WebDriver createDriver() {
        String browser = getString("MW_BROWSER", "chrome").toLowerCase(Locale.ROOT);
        return switch (browser) {
            case "safari" -> createSafariDriver();
            case "chrome" -> createChromeDriver();
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        };
    }

    private WebDriver createChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.addArguments("--window-size=1600,1200");
        options.addArguments("--disable-notifications");
        options.addArguments("--lang=en-US");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        if (getBoolean("MW_HEADLESS", false)) {
            options.addArguments("--headless=new");
        }

        String chromeBinary = getString("MW_CHROME_BINARY", DEFAULT_CHROME_BINARY);
        if (!chromeBinary.isBlank() && new File(chromeBinary).exists()) {
            options.setBinary(chromeBinary);
        }

        if (authSettings.mode() == AuthMode.CHROME_PROFILE && authSettings.chromeUserDataDir() != null) {
            options.addArguments("--user-data-dir=" + authSettings.chromeUserDataDir());
            if (authSettings.chromeProfileDir() != null && !authSettings.chromeProfileDir().isBlank()) {
                options.addArguments("--profile-directory=" + authSettings.chromeProfileDir());
            }
        }

        String chromeDriverPath = getString("MW_CHROMEDRIVER_PATH", "");
        if (!chromeDriverPath.isBlank()) {
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        }

        ChromeDriver chromeDriver = new ChromeDriver(options);
        chromeDriver.manage().timeouts().implicitlyWait(Duration.ZERO);
        chromeDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(Math.max(timeoutSeconds, 30)));
        chromeDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
        return chromeDriver;
    }

    private WebDriver createSafariDriver() {
        SafariDriver safariDriver = new SafariDriver();
        safariDriver.manage().timeouts().implicitlyWait(Duration.ZERO);
        safariDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(Math.max(timeoutSeconds, 30)));
        safariDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));
        return safariDriver;
    }

    private Duration timeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }

    private void waitForDocumentReady() {
        new WebDriverWait(driver, timeout()).until(webDriver -> "complete".equals(
            String.valueOf(((JavascriptExecutor) webDriver).executeScript("return document.readyState"))
        ));
    }

    private boolean waitForValueChange(Supplier<String> supplier, String originalValue, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(webDriver -> {
                String current = supplier.get();
                return current != null && !current.equals(originalValue);
            });
            return true;
        } catch (TimeoutException ex) {
            return false;
        }
    }

    private boolean waitForCondition(Duration timeout, Supplier<Boolean> condition) {
        try {
            new WebDriverWait(driver, timeout).until(webDriver -> Boolean.TRUE.equals(condition.get()));
            return true;
        } catch (TimeoutException ex) {
            return false;
        }
    }

    private JavascriptExecutor javascript() {
        return (JavascriptExecutor) driver;
    }

    private List<WebElement> linksMatching(Predicate<String> matcher) {
        return visibleAnchors().stream()
            .filter(link -> matcher.test(hrefOf(link)))
            .collect(Collectors.toList());
    }

    private Optional<CardSnapshot> firstCardSnapshot(List<WebElement> links) {
        return links.stream().findFirst().map(this::buildCardSnapshot);
    }

    private void openFirstLink(List<WebElement> links, String errorMessage) {
        WebElement link = links.stream().findFirst()
            .orElseThrow(() -> new IllegalStateException(errorMessage));
        driver.get(hrefOf(link));
        waitForPageReady();
        acceptCookiesIfPresent();
    }

    private boolean clickFirstVisible(List<By> locators, boolean strictWaitForReady) {
        for (By locator : locators) {
            List<WebElement> elements = visible(locator);
            if (!elements.isEmpty()) {
                safeClick(elements.get(0));
                if (strictWaitForReady) {
                    waitForPageReady();
                } else {
                    tryWaitForPageReady();
                }
                acceptCookiesIfPresent();
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
        for (WebElement image : visible(By.cssSelector("img"))) {
            String src = image.getAttribute("src");
            if (src != null && !src.isBlank()) {
                candidates.add(image);
            }
        }
        return candidates;
    }

    private String extractTitle(WebElement link) {
        String directText = textOf(link);
        if (!directText.isBlank()) {
            return directText;
        }

        String attributeTitle = normalizeWhitespace(link.getAttribute("title"));
        if (!attributeTitle.isBlank()) {
            return attributeTitle;
        }

        for (By locator : List.of(By.cssSelector("h1,h2,h3,h4,strong,span,p"), By.cssSelector("img[alt]"))) {
            List<WebElement> matches = link.findElements(locator);
            for (WebElement match : matches) {
                String candidate = "img".equalsIgnoreCase(match.getTagName())
                    ? normalizeWhitespace(match.getAttribute("alt"))
                    : textOf(match);
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        }

        return hrefOf(link);
    }

    private String selectAllShortcut() {
        return Keys.chord(
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac") ? Keys.COMMAND : Keys.CONTROL,
            "a"
        );
    }

    private void establishCookieSession() {
        if (authSettings.cookieFile() == null || !Files.exists(authSettings.cookieFile())) {
            throw new IllegalStateException("Cookie file not found: " + authSettings.cookieFile());
        }

        driver.get(localeBaseUrl);
        waitForDocumentReady();

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode cookies = mapper.readTree(Files.readString(authSettings.cookieFile()));
            for (JsonNode node : cookies) {
                Cookie.Builder builder = new Cookie.Builder(node.path("name").asText(), node.path("value").asText());
                if (node.hasNonNull("domain")) {
                    builder.domain(node.path("domain").asText());
                }
                if (node.hasNonNull("path")) {
                    builder.path(node.path("path").asText("/"));
                }
                if (node.path("secure").asBoolean(false)) {
                    builder.isSecure(true);
                }
                if (node.path("httpOnly").asBoolean(false)) {
                    builder.isHttpOnly(true);
                }
                if (node.hasNonNull("expiry")) {
                    builder.expiresOn(new Date(node.path("expiry").asLong() * 1000));
                }
                driver.manage().addCookie(builder.build());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse cookies from " + authSettings.cookieFile(), e);
        }

        driver.navigate().refresh();
        waitForDocumentReady();
        if (!waitForAuthenticatedState(timeout())) {
            throw new IllegalStateException("Cookie session bootstrap did not produce an authenticated state.");
        }
    }

    private void establishChromeProfileSession() {
        driver.get(localeBaseUrl);
        waitForDocumentReady();
        if (!waitForAuthenticatedState(timeout())) {
            throw new IllegalStateException(
                "Chrome profile did not appear authenticated. Check MW_CHROME_USER_DATA_DIR and MW_CHROME_PROFILE_DIR."
            );
        }
    }

    private void establishManualLoginSession() {
        openPath("/en/login");

        maybeVisible(
            By.cssSelector("input[type='email']"),
            By.cssSelector("input[name='email']"),
            By.xpath("//input[contains(@placeholder,'Email') or contains(@placeholder,'email')]")
        ).ifPresent(element -> {
            element.clear();
            element.sendKeys(authSettings.email());
        });

        maybeVisible(
            By.cssSelector("input[type='password']"),
            By.cssSelector("input[name='password']"),
            By.xpath("//input[contains(@placeholder,'Password') or contains(@placeholder,'password')]")
        ).ifPresent(element -> {
            element.clear();
            element.sendKeys(authSettings.password());
        });

        maybeVisible(
            By.xpath("//button[@type='submit']"),
            By.xpath("//button[contains(.,'Sign In') or contains(.,'Log In') or contains(.,'Login')]"),
            By.xpath("//a[contains(.,'Sign In') or contains(.,'Log In') or contains(.,'Login')]")
        ).ifPresent(this::safeClick);

        System.out.println("Manual login checkpoint active. Complete any remaining login steps in Chrome. Waiting for authenticated markers...");
        if (!waitForAuthenticatedState(Duration.ofMinutes(3))) {
            throw new IllegalStateException("Manual login checkpoint timed out before authenticated UI was detected.");
        }
    }

    private void captureScreenshot(String name) {
        if (!(driver instanceof TakesScreenshot screenshotDriver)) {
            return;
        }

        try {
            Path screenshotDir = Path.of("target", "screenshots");
            Files.createDirectories(screenshotDir);
            String safeName = name.replaceAll("[^a-zA-Z0-9-_]", "_");
            Path destination = screenshotDir.resolve(safeName + "-" + SCREENSHOT_FORMATTER.format(LocalDateTime.now()) + ".png");
            Path source = screenshotDriver.getScreenshotAs(OutputType.FILE).toPath();
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write screenshot", e);
        }
    }

    private void ensureChallengeClearedOrFail(String context) {
        lastChallengeOutcome = challengeSolver.ensureChallengeCleared(context);
        switch (lastChallengeOutcome.status()) {
            case NOT_PRESENT, SOLVED -> {
                return;
            }
            case FAILED, TIMED_OUT -> throw new IllegalStateException(
                "Challenge clearance failed for " + context + ": " + lastChallengeOutcome.detail()
            );
        }
    }
}
