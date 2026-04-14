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
