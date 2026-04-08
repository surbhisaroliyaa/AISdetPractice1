# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Java 21 test automation framework using **Microsoft Playwright** (1.56.0) and **TestNG** (7.9.0) for UI testing against https://automationexercise.com. Built with Maven.

## Build & Test Commands

```bash
# Run all tests (uses testng.xml suite)
mvn clean test

# Run a specific test class
mvn clean test -Dtest=NavigationSmokeTest

# Run with CI configuration (uses config-ci.properties)
CI=true mvn clean test
```

## Architecture

**Page Object Model** pattern with these layers:

- **`base/BaseTest.java`** — Playwright lifecycle management. Static `browser` shared across tests; per-test `context` and `page` for isolation. Blocks ad/tracking scripts via route interception to prevent flaky tests. All test classes extend this.
- **`config/ConfigReader.java`** — Loads `src/test/resources/config.properties` (or `config-ci.properties` when `CI` env var is set). Provides typed getters for base URL, browser type, headless mode, timeout, and slowMo.
- **`pages/`** — Page Objects encapsulating locators and interactions (currently: `HomePage`).
- **`tests/`** — Test classes organized by category under subpackages (`smoke/`, `auth/`, `products/`, `cart/`, `checkout/`, `misc/`, `api/`). Most are stubs defined in testng.xml but not yet implemented.
- **`utils/TestDataGenerator.java`** — Random test data (names, emails, addresses, passwords) with India-specific city/state presets.

## Test Suite Configuration

`src/test/resources/testng.xml` defines 7 test groups mapping to packages under `tests.*`. Maven Surefire plugin is configured to use this file.

## Key Conventions

- Browser type is configurable via `config.properties` (`chromium`, `firefox`, `webkit`).
- Tests use Playwright's `assertThat(page)` for URL/title assertions and TestNG assertions for element visibility.
- Navigation tests use regex pattern matching for URL verification.
- ExtentReports generates HTML reports in `reports/`. Playwright traces go to `traces/`.
