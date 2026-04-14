package com.makerworld.auth;

import java.nio.file.Path;

import com.makerworld.core.ConfigManager;

public record AuthConfig(
    boolean authSuiteEnabled,
    AuthMode mode,
    String email,
    String password,
    Path cookieFile,
    Path chromeUserDataDir,
    String chromeProfileDir
) {
    public enum AuthMode {
        MANUAL_CHECKPOINT,
        COOKIE_SESSION,
        CHROME_PROFILE;

        public static AuthMode from(String raw) {
            if (raw == null || raw.isBlank()) {
                return MANUAL_CHECKPOINT;
            }

            return switch (raw.trim().toLowerCase()) {
                case "cookie_session" -> COOKIE_SESSION;
                case "chrome_profile" -> CHROME_PROFILE;
                default -> MANUAL_CHECKPOINT;
            };
        }
    }

    public static AuthConfig from(ConfigManager config) {
        return new AuthConfig(
            config.getBoolean("MW_ENABLE_AUTH_SUITE", true),
            AuthMode.from(config.getString("MW_AUTH_MODE", "manual_checkpoint")),
            config.getEmail(),
            config.getPassword(),
            config.resolveProjectPath(config.getString("MW_COOKIE_FILE", "")),
            config.resolveProjectPath(config.getString("MW_CHROME_USER_DATA_DIR", "")),
            config.getString("MW_CHROME_PROFILE_DIR", "Default")
        );
    }
}
