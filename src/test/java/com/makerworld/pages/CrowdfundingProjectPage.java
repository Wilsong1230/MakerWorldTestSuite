package com.makerworld.pages;

import com.makerworld.core.ConfigManager;
import com.makerworld.utils.AssertionUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CrowdfundingProjectPage extends BasePage {
    public CrowdfundingProjectPage(WebDriver driver, ConfigManager config) {
        super(driver, config);
    }

    public boolean isLoaded() {
        return AssertionUtils.looksLikeCrowdfundingDetailUrl(currentUrl()) && !heading().isBlank();
    }

    public String heading() {
        return maybeVisible(By.cssSelector("main h1"), By.xpath("//h1[normalize-space()]"))
            .map(this::textOf)
            .orElse("");
    }
}
