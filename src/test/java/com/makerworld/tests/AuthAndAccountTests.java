package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import java.time.Duration;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AuthAndAccountTests extends BaseTest {
    @BeforeClass(alwaysRun = true)
    public void bootstrapAuthOncePerClass() {
        bootstrapAuthenticatedSessionOrSkip();
    }

    private void assertChallengeHealthy(String context) {
        String status = lastChallengeStatus();
        Assert.assertFalse(
            "FAILED".equals(status) || "TIMED_OUT".equals(status),
            "Expected challenge state to be healthy for " + context + ", but was " + status
        );
    }

    @Test(groups = {"smoke", "regression", "auth"})
    public void authBootstrapProducesAuthenticatedMarkers() {
        openHomePageAndStabilize("authenticated home page");

        assertChallengeHealthy("auth bootstrap");
        Assert.assertTrue(isAuthenticated(), "Expected authenticated markers after auth bootstrap.");
    }

    @Test(groups = {"regression", "auth"})
    public void authenticatedUserCanReachAccountSurface() {
        openHomePageAndStabilize("authenticated account surface");

        assertChallengeHealthy("account surface");
        Assert.assertTrue(isAuthenticated(), "Expected authenticated markers before opening account surface.");
        Assert.assertTrue(openAccountSurfaceIfAvailable(), "Expected an account or profile surface to be available.");
    }

    @Test(groups = {"regression", "auth", "content"})
    public void authenticatedModelPageExposesGatedActionSurface() {
        openModelDetailPageAndStabilize(pinnedModelPath(), "authenticated model detail");

        assertChallengeHealthy("authenticated model detail");
        Assert.assertTrue(modelDetailHasEngagementActions(), "Expected gated engagement actions to be visible for authenticated users.");
        Assert.assertTrue(tryTriggerEngagementAction(), "Expected to trigger at least one engagement action.");
        Assert.assertFalse(currentUrl().contains("/login"), "Expected gated action to stay in authenticated flow.");
    }

    @Test(groups = {"regression", "auth"})
    public void authenticatedSessionSurvivesRefresh() {
        openHomePageAndStabilize("authenticated refresh baseline");

        assertChallengeHealthy("refresh baseline");
        Assert.assertTrue(isAuthenticated(), "Expected authenticated state before refresh.");

        driver.navigate().refresh();

        Assert.assertTrue(waitForAuthenticatedState(Duration.ofSeconds(timeoutSeconds)), "Expected authenticated state to survive refresh.");
    }

    @Test(groups = {"regression", "auth"})
    public void logoutReturnsGuestVisibleControls() {
        openHomePageAndStabilize("authenticated logout flow");

        assertChallengeHealthy("logout flow");
        Assert.assertTrue(hasLogoutControl(), "Expected a logout control for authenticated users.");
        Assert.assertTrue(logoutIfPossible(), "Expected logout action to be available.");
        Assert.assertTrue(waitForLoggedOutState(Duration.ofSeconds(timeoutSeconds)), "Expected guest-visible controls after logout.");
    }

    @Test(groups = {"regression", "auth"})
    public void challengeStateIsResolvedAfterAuthenticatedNavigation() {
        openModelDetailPageAndStabilize(pinnedModelPath(), "challenge-state validation");

        String status = lastChallengeStatus();
        Assert.assertTrue(
            "NOT_PRESENT".equals(status) || "SOLVED".equals(status),
            "Expected challenge status to be NOT_PRESENT or SOLVED, but was " + status
        );
    }
}
