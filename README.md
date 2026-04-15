# MakerWorld Testing Suite

Chrome-based Selenium/TestNG automation scaffold for MakerWorld.

## Stack

- Java 17
- Maven
- Selenium WebDriver
- TestNG
- Google Chrome

## Project Goals

- Provide 8 separate test classes for the final project submission.
- Keep most coverage on stable public MakerWorld pages.
- Include optional authenticated coverage through safe session bootstrap modes.
- Generate TestNG output and screenshots that can be used in the report and presentation.

## Setup

1. Copy `src/test/resources/config.properties.example` to `src/test/resources/config.properties`.
2. Set `MW_BROWSER=chrome`.
3. If Chrome is not detected automatically, set `MW_CHROME_BINARY=/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`.
4. If you are using a local driver binary, set `MW_CHROMEDRIVER_PATH=/absolute/path/to/chromedriver`.

## Auth Modes

- `manual_checkpoint`: opens the login flow and waits for you to finish it manually.
- `cookie_session`: imports cookies from a local JSON file before visiting gated pages.
- `chrome_profile`: launches Chrome with an existing logged-in user-data directory.

Enable auth tests with:

```bash
export MW_ENABLE_AUTH_SUITE=true
export MW_AUTH_MODE=chrome_profile
```

## CAPTCHA / Turnstile Handling

The suite now treats Cloudflare verification as an explicit pass/fail state:
- If no challenge is present, tests proceed normally.
- If challenge is present, the runner attempts native widget interaction first, then `2Captcha`.
- If challenge still cannot be cleared, the test fails fast with a clear error instead of silently continuing.

Required local env for solver-backed runs:

```bash
export MW_2CAPTCHA_KEY=your_2captcha_api_key
export MW_TURNSTILE_SITEKEY=optional_explicit_turnstile_sitekey
export MW_DRIVER_LIFECYCLE=per_class
export MW_2CAPTCHA_TIMEOUT_SECONDS=90
export MW_2CAPTCHA_POLL_SECONDS=3
```

Troubleshooting:
- `MW_2CAPTCHA_KEY is missing` means solver fallback is unavailable.
- `Unable to resolve Turnstile sitekey from page` means runtime extraction failed; set `MW_TURNSTILE_SITEKEY` explicitly for your environment.
- If challenge persists after solve timeout, increase `MW_2CAPTCHA_TIMEOUT_SECONDS` and rerun.
- Keep `MW_HEADLESS=false` for local diagnosis when challenge behavior changes.

## Run Commands

Run everything:

```bash
mvn test
```

Run only smoke tests:

```bash
mvn -Dgroups=smoke test
```

Run auth tests only:

```bash
mvn -Dgroups=auth test
```

## Evidence For Submission

- TestNG output is written under `test-output/`.
- Failure screenshots are saved under `target/screenshots/`.
- Supporting report and presentation outlines are stored under `docs/`.
