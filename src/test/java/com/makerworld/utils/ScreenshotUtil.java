package com.makerworld.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public final class ScreenshotUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ScreenshotUtil() {
    }

    public static Path capture(WebDriver driver, String name) {
        if (!(driver instanceof TakesScreenshot screenshotDriver)) {
            return null;
        }

        try {
            Path screenshotDir = Path.of("target", "screenshots");
            Files.createDirectories(screenshotDir);

            String safeName = name.replaceAll("[^a-zA-Z0-9-_]", "_");
            Path destination = screenshotDir.resolve(safeName + "-" + FORMATTER.format(LocalDateTime.now()) + ".png");
            Path source = screenshotDriver.getScreenshotAs(OutputType.FILE).toPath();
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            return destination;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write screenshot", e);
        }
    }
}
