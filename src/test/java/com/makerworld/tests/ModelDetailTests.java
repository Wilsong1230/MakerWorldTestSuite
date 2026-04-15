package com.makerworld.tests;

import com.makerworld.base.BaseTest;
import com.makerworld.pages.ModelDetailPage;
import com.makerworld.utils.AssertionUtils;
import com.makerworld.utils.CardSnapshot;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ModelDetailTests extends BaseTest {
    @Test(groups = {"smoke", "regression", "content"})
    public void pinnedModelShowsCoreSections() {
        ModelDetailPage detailPage = new ModelDetailPage(driver, config).open(testData.pinnedModelPath());
        skipIfHumanVerificationPersists(detailPage, "pinned model core sections");

        Assert.assertTrue(detailPage.isLoaded(), "Expected pinned model detail page to load.");
        Assert.assertTrue(detailPage.hasCoreSections(), "Expected core sections such as Description and Print Profile.");
    }

    @Test(groups = {"regression", "content", "media"})
    public void galleryThumbnailSwitchChangesHeroImage() {
        ModelDetailPage detailPage = new ModelDetailPage(driver, config).open(testData.pinnedModelPath());
        skipIfHumanVerificationPersists(detailPage, "pinned model gallery interaction");

        Assert.assertTrue(detailPage.switchToDifferentThumbnailChangesHeroImage(), "Expected a gallery thumbnail click to change the hero image.");
    }

    @Test(groups = {"smoke", "regression", "media"})
    public void heroImageLoadsSuccessfully() {
        ModelDetailPage detailPage = new ModelDetailPage(driver, config).open(testData.pinnedModelPath());
        skipIfHumanVerificationPersists(detailPage, "pinned model hero image");

        Assert.assertTrue(detailPage.heroImageLoads(), "Expected the main model image to load correctly.");
    }

    @Test(groups = {"regression", "content"})
    public void printProfileStatsContainParseableValues() {
        ModelDetailPage detailPage = new ModelDetailPage(driver, config).open(testData.pinnedModelPath());
        skipIfHumanVerificationPersists(detailPage, "pinned model print-profile stats");

        Assert.assertTrue(detailPage.printProfileStatsContainNumbers(), "Expected visible print-profile content with numeric values.");
    }

    @Test(groups = {"regression", "content"})
    public void relatedModelOpensAnotherModelDetailPage() {
        ModelDetailPage detailPage = new ModelDetailPage(driver, config).open(testData.pinnedModelPath());
        skipIfHumanVerificationPersists(detailPage, "pinned model related content");
        CardSnapshot relatedCard = detailPage.firstRelatedModelCard()
            .orElseThrow(() -> new AssertionError("Expected at least one related model link."));

        ModelDetailPage relatedDetail = detailPage.openFirstRelatedModel();
        skipIfHumanVerificationPersists(relatedDetail, "related model detail navigation");

        Assert.assertTrue(relatedDetail.isLoaded(), "Expected related model to open another detail page.");
        Assert.assertTrue(
            AssertionUtils.normalizedContains(relatedDetail.modelTitle(), relatedCard.title())
                || AssertionUtils.normalizedContains(relatedCard.title(), relatedDetail.modelTitle()),
            "Expected related detail title to match the related card title."
        );
    }
}
