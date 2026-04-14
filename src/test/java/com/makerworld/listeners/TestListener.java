package com.makerworld.listeners;

import com.makerworld.base.BaseTest;
import com.makerworld.utils.ScreenshotUtil;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {
    @Override
    public void onTestFailure(ITestResult result) {
        WebDriver driver = BaseTest.getThreadDriver();
        if (driver != null && BaseTest.getThreadConfig() != null && BaseTest.getThreadConfig().isScreenshotOnFailure()) {
            ScreenshotUtil.capture(driver, result.getMethod().getMethodName());
        }
    }

    @Override
    public void onStart(ITestContext context) {
        // No-op.
    }

    @Override
    public void onFinish(ITestContext context) {
        // No-op.
    }
}
