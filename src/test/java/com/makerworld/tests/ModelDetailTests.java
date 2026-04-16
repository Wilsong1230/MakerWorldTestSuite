package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ModelDetailTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void pinnedModelShowsCoreSections() {
        openModelDetailPageAndStabilize(pinnedModelPath(), "pinned model core sections");

        Assert.assertTrue(isModelDetailPageLoaded(), "Expected pinned model detail page to load.");
        Assert.assertTrue(modelDetailHasCoreSections(), "Expected core sections such as Description and Print Profile.");
    }

    @Test(groups = {"regression", "content", "media"})
    public void galleryThumbnailSwitchChangesHeroImage() {
        openModelDetailPageAndStabilize(pinnedModelPath(), "pinned model gallery interaction");

        Assert.assertTrue(switchToDifferentThumbnailChangesHeroImage(), "Expected a gallery thumbnail click to change the hero image.");
    }

    @Test(groups = {"smoke", "regression", "media"})
    public void heroImageLoadsSuccessfully() {
        openModelDetailPageAndStabilize(pinnedModelPath(), "pinned model hero image");

        Assert.assertTrue(heroImageLoads(), "Expected the main model image to load correctly.");
    }

    @Test(groups = {"regression", "content"})
    public void printProfileStatsContainParseableValues() {
        openModelDetailPageAndStabilize(pinnedModelPath(), "pinned model print-profile stats");

        Assert.assertTrue(printProfileStatsContainNumbers(), "Expected visible print-profile content with numeric values.");
    }

    @Test(groups = {"regression", "content"})
    public void relatedModelOpensAnotherModelDetailPage() {
        openModelDetailPageAndStabilize(pinnedModelPath(), "pinned model related content");
        CardSnapshot relatedCard = firstRelatedModelCard()
            .orElseThrow(() -> new AssertionError("Expected at least one related model link."));

        openFirstRelatedModel();

        Assert.assertTrue(isModelDetailPageLoaded(), "Expected related model to open another detail page.");
        Assert.assertTrue(titlesRoughlyMatch(modelTitle(), relatedCard.title()), "Expected related detail title to match the related card title.");
    }
}
