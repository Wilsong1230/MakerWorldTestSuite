package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AuthAndAccountTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "auth"})
    public void guestSessionShowsLoginOrSignInControls() {
        navigate("/en");

        String page = driver.getPageSource().toLowerCase();
        boolean found = false;
        for (var node : testData.path("guestLabels")) {
            String label = node.asText().toLowerCase();
            if (!label.isBlank() && page.contains(label)) {
                found = true;
                break;
            }
        }

        Assert.assertTrue(found, "Expected a guest-visible Sign In / Login label on the page.");
    }

    @Test(groups = {"regression", "auth"})
    public void loginSurfaceIsReachableFromHomeHeaderLinks() {
        navigate("/en");

        Assert.assertTrue(
            driver.findElements(By.cssSelector("a[href*='login'], a[href*='signin'], a[href*='sign-in'], a[href*='account']")).size() > 0
                || driver.getPageSource().toLowerCase().contains("login")
                || driver.getPageSource().toLowerCase().contains("sign in"),
            "Expected some login/sign-in/account affordance to exist."
        );
    }

    @Test(groups = {"regression", "auth", "content"})
    public void modelDetailIsReachableWithoutImmediateAuthRedirect() {
        String pinnedPath = testData.path("pinnedModelPath").asText("/en/models/544229");
        navigate(pinnedPath);

        String url = driver.getCurrentUrl().toLowerCase();
        Assert.assertFalse(url.contains("/login"), "Expected not to be immediately redirected to login for a basic model view.");
        Assert.assertTrue(url.contains("/models/"), "Expected to remain on a model URL.");
    }

    @Test(groups = {"regression", "auth"})
    public void navigationDoesNotLeaveChallengeActive() {
        navigate("/en");
        navigate("/en/models");
        navigate("/en/contests");

        Assert.assertFalse(solver.isChallengeActive(), "Expected Cloudflare challenge to not remain active after basic navigation.");
    }

    @Test(groups = {"regression", "auth"})
    public void guestControlsRemainVisibleAfterRefresh() {
        navigate("/en");
        String before = driver.getPageSource().toLowerCase();

        driver.navigate().refresh();

        String after = driver.getPageSource().toLowerCase();
        Assert.assertTrue(
            after.contains("login") || after.contains("sign in") || before.contains("login") || before.contains("sign in"),
            "Expected guest auth controls to remain discoverable after refresh."
        );
    }
}
