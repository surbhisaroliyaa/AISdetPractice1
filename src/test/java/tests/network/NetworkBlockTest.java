package tests.network;

import base.BaseTest;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import config.ConfigReader;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.testng.Assert.*;

/**
 * NETWORK BLOCK TESTS — Abort unwanted requests to improve test speed and reliability.
 *
 * route.abort() = kill the request. The server never sees it. The browser gets a network error.
 *
 * Blocking is used in real jobs to:
 *   - Prevent flaky tests caused by third-party ads/analytics scripts
 *   - Speed up tests by not loading images, fonts, or heavy scripts
 *   - Simulate network failures for specific resources
 *
 * BaseTest already blocks known ad/tracking domains. These tests verify that blocking
 * works correctly AND demonstrate additional blocking patterns.
 *
 * IMPORTANT — route registration order matters:
 *   Routes registered LATER have HIGHER priority (checked first).
 *   Use route.fallback() to pass to the next handler in the chain.
 *   Use route.abort() / route.resume() to terminate the chain.
 */
public class NetworkBlockTest extends BaseTest {

    /**
     * Verify that BaseTest's ad-blocking routes are working.
     * Uses page.onRequestFailed() to confirm ad URLs are being aborted.
     *
     * page.onRequestFailed(callback) — fires when a request fails due to:
     *   - route.abort() (intentional blocking)
     *   - Network errors (DNS failure, timeout, connection refused)
     *   - CORS violations
     *
     * request.failure() returns the error reason (e.g., "net::ERR_FAILED").
     */
    @Test(description = "Verify ad-blocking routes prevent tracking requests from loading")
    public void verifyAdBlockingPreventsTrackingRequests() {
        // Collect all failed (blocked) request URLs
        List<String> blockedUrls = new CopyOnWriteArrayList<>();

        // onRequestFailed fires for every request that doesn't get a response
        // Requests blocked by route.abort() appear here
        page.onRequestFailed(request -> {
            blockedUrls.add(request.url().toLowerCase());
        });

        // Navigate — BaseTest's route handler will abort ad/tracking requests
        page.navigate(ConfigReader.getBaseUrl());
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Print what was blocked for visibility
        System.out.println("═══ Blocked Requests ═══");
        blockedUrls.forEach(url -> System.out.println("✗ " + url));
        System.out.println("Total blocked: " + blockedUrls.size());
        System.out.println("════════════════════════");

        // Verify at least some ad/tracking requests were blocked
        // automationexercise.com loads Google ads and analytics
        assertTrue(blockedUrls.size() > 0,
                "BaseTest should block at least some ad/tracking requests");

        // Verify known ad domains appear in blocked list
        boolean hasGoogleBlocked = blockedUrls.stream()
                .anyMatch(url -> url.contains("google") || url.contains("doubleclick")
                        || url.contains("googlesyndication"));
        assertTrue(hasGoogleBlocked,
                "Google ad/analytics domains should be blocked. Blocked: " + blockedUrls);
    }

    /**
     * Block ALL image requests and verify the page still loads and is functional.
     * Demonstrates: using route.abort() on resource types + route.fallback() for chain.
     *
     * route.fallback() vs route.resume():
     *   - fallback() = "I don't want to handle this, pass to next route handler"
     *     → BaseTest's ad blocker still runs for non-image requests
     *   - resume() = "Send this directly to the server"
     *     → Bypasses ALL other handlers including BaseTest's ad blocker
     *
     * Rule: Use fallback() when other handlers in the chain should still run.
     *       Use resume() when you want to skip all other handlers.
     */
    @Test(description = "Block all images and verify page still loads and is functional")
    public void blockAllImages_verifyPageStillFunctional() {
        int[] blockedImageCount = {0};

        // Register image-blocking route AFTER BaseTest's route (higher priority)
        // This route catches image requests; everything else falls through to BaseTest
        page.route("**/*", route -> {
            String url = route.request().url().toLowerCase();
            if (url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png")
                    || url.endsWith(".gif") || url.endsWith(".webp") || url.endsWith(".svg")) {
                blockedImageCount[0]++;
                // abort() = kill the request, browser gets net::ERR_FAILED
                // The server never sees this request
                route.abort();
            } else {
                // fallback() = pass to next handler (BaseTest's ad blocker)
                // This is CRUCIAL — if we used resume() here, ads would load
                // because we'd bypass BaseTest's blocking entirely
                route.fallback();
            }
        });

        page.navigate(ConfigReader.getBaseUrl() + "/products");

        // NETWORKIDLE = no network requests for 500ms
        // With images blocked, this should be reached faster
        page.waitForLoadState(LoadState.NETWORKIDLE);

        System.out.println("Blocked " + blockedImageCount[0] + " image requests");

        // Page should still be functional without images
        // Headings, text, navigation, and interactive elements should work
        assertTrue(page.locator(".features_items").isVisible(),
                "Products section should be visible even without images");
        assertTrue(page.locator(".features_items .title").isVisible(),
                "Page heading should be visible");

        // Navigation should still work
        assertTrue(page.locator(".navbar-nav").isVisible(),
                "Navigation bar should be visible");

        // Verify we actually blocked some images
        assertTrue(blockedImageCount[0] > 0,
                "Should have blocked at least some image requests on products page");
    }

    /**
     * Block specific third-party domains that commonly cause test flakiness.
     * Tests blocking of: OneSignal (push notifications), chat widgets,
     * cookie consent banners, and social media trackers.
     *
     * This extends BaseTest's blocking with additional domains.
     * In real jobs, you'd maintain a blocklist specific to your application.
     */
    @Test(description = "Block specific third-party scripts - OneSignal, chat widgets, cookie banners")
    public void blockSpecificThirdPartyDomains() {
        List<String> blockedDomains = new CopyOnWriteArrayList<>();

        // Extended blocklist — covers common third-party services
        // that cause test flakiness in real applications
        page.route("**/*", route -> {
            String url = route.request().url().toLowerCase();

            // OneSignal — push notification service
            // Chat widgets — Intercom, Drift, Zendesk, LiveChat, Tawk.to
            // Cookie consent — CookieBot, OneTrust, TrustArc
            // Social — Facebook pixel, Twitter pixel
            boolean shouldBlock = url.contains("onesignal")
                    || url.contains("intercom") || url.contains("drift.com")
                    || url.contains("zendesk") || url.contains("livechat")
                    || url.contains("tawk.to")
                    || url.contains("cookiebot") || url.contains("onetrust")
                    || url.contains("trustarc") || url.contains("cookielaw")
                    || url.contains("fbevents") || url.contains("connect.facebook")
                    || url.contains("platform.twitter");

            if (shouldBlock) {
                blockedDomains.add(url);
                route.abort();
            } else {
                // fallback() preserves the handler chain
                // BaseTest's ad blocker (registered earlier) will still process this request
                route.fallback();
            }
        });

        page.navigate(ConfigReader.getBaseUrl());
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Print blocked domains for visibility
        if (!blockedDomains.isEmpty()) {
            System.out.println("═══ Blocked Third-Party Domains ═══");
            blockedDomains.forEach(url -> System.out.println("✗ " + url));
            System.out.println("═══════════════════════════════════");
        } else {
            System.out.println("No additional third-party domains found to block "
                    + "(BaseTest may have already caught them)");
        }

        // Page should load correctly regardless of what was blocked
        assertTrue(page.title().contains("Automation"),
                "Page should load correctly with third-party scripts blocked");
    }

    /**
     * Verify that selective blocking does NOT break the site's own JavaScript.
     * Blocks only third-party scripts while allowing first-party scripts to run.
     *
     * This is the anti-pattern test for Pitfall 5 from Pillar 1:
     * "Don't block ALL scripts — only block SPECIFIC third-party scripts."
     *
     * We verify site functionality by checking that:
     *   - Dynamic elements render (requires JS)
     *   - Click handlers work (requires JS)
     *   - Category accordions expand (requires JS)
     */
    @Test(description = "Verify ad-blocking does not break site's own JavaScript functionality")
    public void verifySelectiveBlocking_siteJavaScriptStillWorks() {
        page.navigate(ConfigReader.getBaseUrl() + "/products");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Test 1: Category accordion requires JavaScript to expand/collapse
        page.locator(".category-products .panel-default a[href='#Women']").click();
        // If JS is broken, this waitFor would time out
        page.locator("#Women").waitFor();
        assertTrue(page.locator("#Women").isVisible(),
                "Category accordion should expand — requires working JavaScript");

        // Test 2: Search form submission requires JavaScript
        page.locator("#search_product").fill("blue");
        page.locator("#submit_search").click();
        // If JS is broken, search wouldn't execute
        page.locator(".features_items").waitFor();
        assertTrue(page.locator(".features_items").isVisible(),
                "Search should work — requires JavaScript for form handling");

        // Test 3: Slider/carousel requires JavaScript (if present on this page)
        // This just verifies the page didn't crash due to missing scripts
        assertFalse(page.locator(".features_items .col-sm-4").all().isEmpty(),
                "Product cards should be rendered — page JS is working");
    }
}
