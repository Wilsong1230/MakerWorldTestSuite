package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ModelsPageTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void modelsListingLoadsMultipleUniqueCards() {
        openModelsPageAndStabilize("All Models surface");

        Assert.assertTrue(isModelsPageLoaded(), "Expected the models page to load.");
        Assert.assertTrue(hasMultipleUniqueModelCards(3), "Expected multiple unique model cards.");
    }

    @Test(groups = {"regression", "content", "media"})
    public void firstModelCardHasMetadataAndImage() {
        openModelsPageAndStabilize("All Models card metadata");

        Assert.assertTrue(firstModelCardHasMetadataAndLoadedImage(), "Expected the first model card to expose metadata and a loaded image.");
    }

    @Test(groups = {"regression", "content"})
    public void modelsPageExposesFilterOrSortControls() {
        openModelsPageAndStabilize("All Models filters and sort");

        Assert.assertTrue(modelsPageHasFilterOrSortControls(), "Expected filter or sort controls on the models page.");
    }

    @Test(groups = {"regression", "content"})
    public void changingFilterChangesVisibleState() {
        openModelsPageAndStabilize("All Models filter switching");

        Assert.assertTrue(switchFirstAlternativeModelFilter(), "Expected selecting another filter or tab to change the visible state.");
    }

    @Test(groups = {"smoke", "regression", "content"})
    public void firstModelCardMatchesOpenedDetailPage() {
        openModelsPageAndStabilize("All Models card-to-detail navigation");
        CardSnapshot card = firstModelCard()
            .orElseThrow(() -> new AssertionError("Expected at least one model card."));

        openFirstModelCard();

        Assert.assertTrue(isModelDetailPageLoaded(), "Expected to land on a model detail page.");
        Assert.assertTrue(detailMatchesCard(card, modelTitle()), "Expected opened detail page to match the model card title.");
    }
}
