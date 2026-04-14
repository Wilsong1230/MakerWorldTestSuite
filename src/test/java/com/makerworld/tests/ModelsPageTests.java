package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import com.makerworld.pages.ModelDetailPage;
import com.makerworld.pages.ModelsPage;
import com.makerworld.utils.AssertionUtils;
import com.makerworld.utils.CardSnapshot;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

public class ModelsPageTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void modelsListingLoadsMultipleUniqueCards() {
        ModelsPage modelsPage = new ModelsPage(driver, config).open();

        if (modelsPage.isSecurityVerificationPage()) {
            throw new SkipException("MakerWorld security verification blocked the All Models surface.");
        }

        Assert.assertTrue(modelsPage.isLoaded(), "Expected the models page to load.");
        Assert.assertTrue(modelsPage.hasMultipleUniqueCards(3), "Expected multiple unique model cards.");
    }

    @Test(groups = {"regression", "content", "media"})
    public void firstModelCardHasMetadataAndImage() {
        ModelsPage modelsPage = new ModelsPage(driver, config).open();

        if (modelsPage.isSecurityVerificationPage()) {
            throw new SkipException("MakerWorld security verification blocked the All Models surface.");
        }

        Assert.assertTrue(modelsPage.firstCardHasMetadataAndLoadedImage(), "Expected the first model card to expose metadata and a loaded image.");
    }

    @Test(groups = {"regression", "content"})
    public void modelsPageExposesFilterOrSortControls() {
        ModelsPage modelsPage = new ModelsPage(driver, config).open();

        if (modelsPage.isSecurityVerificationPage()) {
            throw new SkipException("MakerWorld security verification blocked the All Models surface.");
        }

        Assert.assertTrue(modelsPage.hasFilterOrSortControls(), "Expected filter or sort controls on the models page.");
    }

    @Test(groups = {"regression", "content"})
    public void changingFilterChangesVisibleState() {
        ModelsPage modelsPage = new ModelsPage(driver, config).open();

        if (modelsPage.isSecurityVerificationPage()) {
            throw new SkipException("MakerWorld security verification blocked the All Models surface.");
        }

        Assert.assertTrue(modelsPage.switchFirstAlternativeFilter(), "Expected selecting another filter or tab to change the visible state.");
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void firstModelCardMatchesOpenedDetailPage() {
        ModelsPage modelsPage = new ModelsPage(driver, config).open();

        if (modelsPage.isSecurityVerificationPage()) {
            throw new SkipException("MakerWorld security verification blocked the All Models surface.");
        }

        CardSnapshot card = modelsPage.firstModelCard()
            .orElseThrow(() -> new AssertionError("Expected at least one model card."));

        ModelDetailPage detailPage = modelsPage.openFirstModel();

        if (detailPage.isSecurityVerificationPage()) {
            throw new SkipException("MakerWorld security verification blocked models-page detail navigation.");
        }

        Assert.assertTrue(detailPage.isLoaded(), "Expected to land on a model detail page.");
        Assert.assertTrue(
            AssertionUtils.normalizedContains(detailPage.modelTitle(), AssertionUtils.slugToken(card.title()))
                || AssertionUtils.normalizedContains(card.metaText(), detailPage.modelTitle()),
            "Expected opened detail page to match the model card title."
        );
    }
}
