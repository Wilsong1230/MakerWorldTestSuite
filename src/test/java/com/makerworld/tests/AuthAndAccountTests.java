package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import com.makerworld.pages.AccountPage;
import com.makerworld.pages.HomePage;
import com.makerworld.pages.ModelDetailPage;
import java.time.Duration;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AuthAndAccountTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "auth"})
    public void authBootstrapProducesAuthenticatedMarkers() {
        bootstrapAuthenticatedSessionOrSkip();
        HomePage homePage = new HomePage(driver, config).open();
        skipIfHumanVerificationPersists(homePage, "authenticated home page");

        Assert.assertTrue(authVerifier().isAuthenticated(), "Expected authenticated markers after auth bootstrap.");
    }

    @Test(groups = {"regression", "auth"})
    public void authenticatedUserCanReachAccountSurface() {
        bootstrapAuthenticatedSessionOrSkip();
        HomePage homePage = new HomePage(driver, config).open();
        skipIfHumanVerificationPersists(homePage, "authenticated account surface");
        AccountPage accountPage = new AccountPage(driver, config);

        Assert.assertTrue(accountPage.hasAuthenticatedMarkers(), "Expected authenticated markers before opening account surface.");
        Assert.assertTrue(accountPage.openAccountSurfaceIfAvailable(), "Expected an account or profile surface to be available.");
    }

    @Test(groups = {"regression", "auth", "content"})
    public void authenticatedModelPageExposesGatedActionSurface() {
        bootstrapAuthenticatedSessionOrSkip();
        ModelDetailPage detailPage = new ModelDetailPage(driver, config).open(testData.pinnedModelPath());
        skipIfHumanVerificationPersists(detailPage, "authenticated model detail");

        Assert.assertTrue(detailPage.hasEngagementActions(), "Expected gated engagement actions to be visible for authenticated users.");
        Assert.assertTrue(detailPage.tryTriggerEngagementAction(), "Expected to trigger at least one engagement action.");
        Assert.assertFalse(driver.getCurrentUrl().contains("/login"), "Expected gated action to stay in authenticated flow.");
    }

    @Test(groups = {"regression", "auth"})
    public void authenticatedSessionSurvivesRefresh() {
        bootstrapAuthenticatedSessionOrSkip();
        HomePage homePage = new HomePage(driver, config).open();
        skipIfHumanVerificationPersists(homePage, "authenticated refresh baseline");

        Assert.assertTrue(authVerifier().isAuthenticated(), "Expected authenticated state before refresh.");

        driver.navigate().refresh();

        Assert.assertTrue(
            authVerifier().waitForAuthenticatedState(Duration.ofSeconds(config.getTimeoutSeconds())),
            "Expected authenticated state to survive refresh."
        );
    }

    @Test(groups = {"regression", "auth"})
    public void logoutReturnsGuestVisibleControls() {
        bootstrapAuthenticatedSessionOrSkip();
        HomePage homePage = new HomePage(driver, config).open();
        skipIfHumanVerificationPersists(homePage, "authenticated logout flow");
        AccountPage accountPage = new AccountPage(driver, config);

        Assert.assertTrue(accountPage.hasLogoutControl(), "Expected a logout control for authenticated users.");
        Assert.assertTrue(accountPage.logoutIfPossible(), "Expected logout action to be available.");
        Assert.assertTrue(
            authVerifier().waitForLoggedOutState(Duration.ofSeconds(config.getTimeoutSeconds())),
            "Expected guest-visible controls after logout."
        );
    }
}
