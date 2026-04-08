package tests.network;

import base.BaseTest;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import config.ConfigReader;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.testng.Assert.*;

/**
 * NETWORK MOCK TESTS — Fake API responses to test scenarios without touching the real server.
 *
 * route.fulfill() = return a fake response. The server is NEVER contacted.
 *   You control: status code, headers, body (JSON, HTML, anything).
 *
 * IMPORTANT — automationexercise.com is SERVER-RENDERED:
 *   The products page HTML is built on the server. The browser does NOT make separate
 *   API calls to /api/productsList to render products. So mocking the API won't change
 *   what the products PAGE shows.
 *
 *   These tests demonstrate the PATTERN by mocking the API and verifying the mock via fetch().
 *   In a SPA (React/Angular/Vue), the SAME code would affect the rendered UI because
 *   the browser fetches data via API calls after the page loads.
 *
 *   In your future job (likely a SPA), these patterns will directly control what the UI shows.
 *
 * Also covers:
 *   - route.resume(options) — modify request headers (language, auth) before forwarding
 *   - route.fallback(options) — modify AND preserve handler chain
 *   - HAR recording — record all network traffic for offline replay
 */
public class NetworkMockTest extends BaseTest {

    // ═══════════════════════════════════════════════════════════════════
    // MOCK — Empty state, error state, edge cases
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Mock the Products API to return an EMPTY product list.
     * Tests: What does the UI show when there are no products?
     *
     * route.fulfill(options) — return a completely fake response:
     *   .setStatus(200)        — HTTP status code
     *   .setContentType(...)   — MIME type (application/json, text/html, etc.)
     *   .setBody(...)          — response body (JSON string, HTML, raw text)
     *
     * SPA behavior: UI would show "No products found" or empty state component.
     * Server-rendered: No UI change (HTML already contains products from server).
     */
    @Test(description = "Mock empty product list - verify empty response returned (SPA pattern)")
    public void mockEmptyProductList_verifyEmptyResponse() {
        page.navigate(ConfigReader.getBaseUrl());

        // Mock: intercept API call and return empty products array
        page.route("**/api/productsList", route -> {
            // fulfill() = return fake response, server is NEVER contacted
            // In a SPA, this empty list would make the UI show "No products found"
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"responseCode\": 200, \"products\": []}")
            );
        });

        // Call the API — should get our mocked empty response, not real data
        Response response = page.waitForResponse(
                resp -> resp.url().contains("/api/productsList"),
                () -> page.evaluate("fetch('/api/productsList')")
        );

        // Verify the mock was applied — response should have empty products
        assertEquals(response.status(), 200, "Mock should return 200");
        String body = response.text();
        assertTrue(body.contains("\"products\": []") || body.contains("\"products\":[]"),
                "Mock should return empty products array. Got: " + body);
    }

    /**
     * Mock a 500 Internal Server Error response.
     * Tests: What does the UI show when the server is down?
     *
     * This is DIFFERENT from route.abort():
     *   - abort()  = network failure (server unreachable) → browser shows "ERR_FAILED"
     *   - fulfill(500) = server responded WITH an error → app shows "Server Error" message
     *
     * SPA behavior: App would catch the 500 and show an error message/retry button.
     */
    @Test(description = "Mock server error 500 - verify error response returned (SPA pattern)")
    public void mockServerError500_verifyErrorResponse() {
        page.navigate(ConfigReader.getBaseUrl());

        // Mock: return a 500 Internal Server Error
        page.route("**/api/productsList", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(500)
                    .setContentType("application/json")
                    // Real servers often return error details in JSON
                    .setBody("{\"error\": \"Internal Server Error\", \"message\": \"Database connection failed\"}")
            );
        });

        Response response = page.waitForResponse(
                resp -> resp.url().contains("/api/productsList"),
                () -> page.evaluate("fetch('/api/productsList')")
        );

        // Verify we got the mocked 500 error, not the real 200 response
        assertEquals(response.status(), 500, "Mock should return 500 error");
        String body = response.text();
        assertTrue(body.contains("Internal Server Error"),
                "Error response should contain error message. Got: " + body);
    }

    /**
     * Mock a SLOW response to test timeout handling.
     * Simulates a server that takes too long to respond.
     *
     * In real jobs, this tests:
     *   - Does the UI show a loading spinner?
     *   - Does the app timeout gracefully after X seconds?
     *   - Does the retry mechanism kick in?
     *
     * Note: Thread.sleep in route handler blocks Playwright's dispatcher thread.
     * For short delays (1-2s) this is fine for testing. For longer delays,
     * use external async mechanisms.
     */
    @Test(description = "Mock slow API response - verify response eventually returns")
    public void mockSlowResponse_verifyEventualReturn() {
        page.navigate(ConfigReader.getBaseUrl());

        long[] fulfillTime = {0};

        // Mock: respond after a 2-second delay
        page.route("**/api/productsList", route -> {
            try {
                // Simulate slow server — 2 second delay before responding
                // In a real SPA test, you'd assert the loading spinner appears during this delay
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            fulfillTime[0] = System.currentTimeMillis();
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"responseCode\": 200, \"products\": [{\"id\": 1, \"name\": \"Delayed Product\"}]}")
            );
        });

        long startTime = System.currentTimeMillis();

        Response response = page.waitForResponse(
                resp -> resp.url().contains("/api/productsList"),
                () -> page.evaluate("fetch('/api/productsList')")
        );

        long elapsed = System.currentTimeMillis() - startTime;

        // Verify the response came back (eventually) with our mocked data
        assertEquals(response.status(), 200);
        assertTrue(response.text().contains("Delayed Product"),
                "Should eventually receive the delayed mock response");

        // Verify the delay actually happened (at least 1.5s to account for timing)
        assertTrue(elapsed >= 1500,
                "Response should have been delayed. Elapsed: " + elapsed + "ms");
        System.out.println("Slow response took " + elapsed + "ms (mocked 2s delay)");
    }

    /**
     * Mock product data with extremely long name and price values.
     * Tests: Does the UI handle overflow? Do long values break the layout?
     *
     * SPA behavior: Product cards might overflow, text might clip, layout might break.
     * This test verifies the mock data is returned correctly — UI assertions would follow in a SPA.
     */
    @Test(description = "Mock long product name and price - verify overflow data returned")
    public void mockLongProductNameAndPrice_verifyData() {
        page.navigate(ConfigReader.getBaseUrl());

        String longName = "A".repeat(500); // 500-character product name
        String longPrice = "Rs. " + "9".repeat(50); // Absurdly long price

        // Mock: return product with extremely long values
        page.route("**/api/productsList", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"responseCode\": 200, \"products\": ["
                            + "{\"id\": 1, \"name\": \"" + longName + "\", "
                            + "\"price\": \"" + longPrice + "\", "
                            + "\"brand\": \"TestBrand\", "
                            + "\"category\": {\"category\": \"TestCategory\"}}"
                            + "]}")
            );
        });

        Response response = page.waitForResponse(
                resp -> resp.url().contains("/api/productsList"),
                () -> page.evaluate("fetch('/api/productsList')")
        );

        // Verify the long values are in the response
        String body = response.text();
        assertTrue(body.contains(longName),
                "Response should contain the 500-char product name");
        assertTrue(body.contains(longPrice),
                "Response should contain the long price");
        // In a SPA, you'd now check: page.locator(".product-name").textContent()
        // to see if the UI truncates, wraps, or overflows
    }

    /**
     * Mock product data with special characters (XSS payloads, unicode, emojis).
     * Tests: Does the UI render special characters safely? Is XSS prevented?
     *
     * SPA behavior: If the frontend doesn't sanitize, XSS payloads could execute.
     * This is a SECURITY test pattern — critical for any web application.
     */
    @Test(description = "Mock special characters in product data - verify safe handling")
    public void mockSpecialCharactersInProductData() {
        page.navigate(ConfigReader.getBaseUrl());

        // Test data with special characters, HTML, and XSS payloads
        // JSON-escaped to be valid JSON
        String specialName = "<script>alert('xss')</script> & \\\"quotes\\\" <b>bold</b>";
        String unicodeName = "Ünïcödë Prödüct 日本語 العربية";

        page.route("**/api/productsList", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody("{\"responseCode\": 200, \"products\": ["
                            + "{\"id\": 1, \"name\": \"" + specialName + "\", "
                            + "\"price\": \"Rs. 0\", \"brand\": \"Test<br>Brand\", "
                            + "\"category\": {\"category\": \"Test&Category\"}},"
                            + "{\"id\": 2, \"name\": \"" + unicodeName + "\", "
                            + "\"price\": \"Rs. 999\", \"brand\": \"UnicodeBrand\", "
                            + "\"category\": {\"category\": \"International\"}}"
                            + "]}")
            );
        });

        Response response = page.waitForResponse(
                resp -> resp.url().contains("/api/productsList"),
                () -> page.evaluate("fetch('/api/productsList')")
        );

        // Verify special characters are present in the response
        assertEquals(response.status(), 200);
        String body = response.text();
        assertTrue(body.contains("script"), "Response should contain script tag (to test XSS handling)");
        assertTrue(body.contains("International"), "Response should contain unicode product");
        // In a SPA, you'd assert:
        // - The <script> tag is rendered as TEXT, not executed as code
        // - Unicode characters display correctly
        // - HTML entities are escaped in the DOM
    }

    /**
     * Mock product data with a MISSING price field.
     * Tests: Does the UI handle missing data gracefully?
     *
     * SPA behavior: Product card might show "undefined", "$NaN", crash, or show a placeholder.
     * This catches frontend bugs where code assumes a field always exists.
     */
    @Test(description = "Mock product with missing price field - verify graceful handling")
    public void mockMissingPriceField_verifyGracefulHandling() {
        page.navigate(ConfigReader.getBaseUrl());

        // Mock: return product WITHOUT the price field
        page.route("**/api/productsList", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    // Notice: no "price" key in the product object
                    .setBody("{\"responseCode\": 200, \"products\": ["
                            + "{\"id\": 1, \"name\": \"Product Without Price\", "
                            + "\"brand\": \"TestBrand\", "
                            + "\"category\": {\"category\": \"TestCategory\"}}"
                            + "]}")
            );
        });

        Response response = page.waitForResponse(
                resp -> resp.url().contains("/api/productsList"),
                () -> page.evaluate("fetch('/api/productsList')")
        );

        // Verify the response does NOT contain a price field
        assertEquals(response.status(), 200);
        String body = response.text();
        assertTrue(body.contains("Product Without Price"), "Product name should be present");
        assertFalse(body.contains("\"price\""),
                "Response should NOT contain price field — testing missing field scenario");
        // In a SPA, you'd check: does the product card show "N/A", "$0", "undefined", or crash?
    }

    // ═══════════════════════════════════════════════════════════════════
    // MODIFY — Change request/response headers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Modify the Accept-Language header to French on the page navigation request.
     * Tests: Does the site serve content in a different language?
     *
     * route.fallback(options) — modify the request AND pass it to the next handler.
     *   .setHeaders(map) — replace all headers with the given map
     *
     * Why fallback() instead of resume():
     *   - fallback(options) modifies the request, then passes to next handler (BaseTest's ad blocker)
     *   - resume(options) modifies the request, then sends DIRECTLY to server (skips ad blocker)
     *   We want ad blocking to still work, so we use fallback().
     *
     * Note: automationexercise.com doesn't support French — this demonstrates the PATTERN.
     * Real multilingual sites (e.g., Wikipedia, MDN) would serve French content.
     */
    @Test(description = "Modify Accept-Language header to French - verify header is sent")
    public void modifyRequestHeader_addFrenchLanguage() {
        // Spy variable to verify the header was actually modified
        List<String> capturedLanguageHeaders = new CopyOnWriteArrayList<>();

        // Register header-modifying route BEFORE navigation
        // This route modifies the Accept-Language on EVERY request
        page.route("**/*", route -> {
            Map<String, String> headers = new HashMap<>(route.request().headers());
            // Set French as preferred language
            headers.put("accept-language", "fr-FR,fr;q=0.9,en;q=0.1");

            // Capture the language header for verification
            capturedLanguageHeaders.add(headers.get("accept-language"));

            // fallback(options) = modify request AND let next handler (BaseTest) process it
            // This preserves the ad-blocking chain while adding our language header
            route.fallback(new Route.FallbackOptions().setHeaders(headers));
        });

        page.navigate(ConfigReader.getBaseUrl());
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Verify the language header was applied to requests
        assertFalse(capturedLanguageHeaders.isEmpty(),
                "Should have modified language header on at least one request");
        assertTrue(capturedLanguageHeaders.get(0).contains("fr-FR"),
                "First request should have French Accept-Language header");

        // Page should still load correctly (site doesn't support French, but shouldn't break)
        assertTrue(page.title().contains("Automation"),
                "Page should load correctly with modified language header");

        System.out.println("Modified Accept-Language on " + capturedLanguageHeaders.size() + " requests");
    }

    /**
     * Inject a Bearer token auth header on all API requests.
     * Demonstrates: skipping the login UI by injecting auth tokens directly.
     *
     * This is a CRITICAL real-job pattern:
     *   - Login through UI: ~3-5 seconds per test (open page, fill form, click, wait)
     *   - Inject auth header: ~0 seconds (token is added to every request automatically)
     *
     * Two approaches for authenticated testing:
     *   1. storageState (Day 2) — for COOKIE-based auth (saves cookies/localStorage)
     *   2. Header injection (this test) — for TOKEN-based auth (JWT, Bearer)
     *   Most modern APIs use tokens → this pattern is more common.
     */
    @Test(description = "Inject Bearer auth token on API requests - verify token is sent")
    public void injectAuthHeader_bearerTokenPattern() {
        page.navigate(ConfigReader.getBaseUrl());

        // Spy: capture headers on API requests to verify token was injected
        List<String> capturedAuthHeaders = new CopyOnWriteArrayList<>();

        // Inject auth header ONLY on API requests (not page/CSS/image requests)
        // Pattern: **/api/** matches only API endpoints
        page.route("**/api/**", route -> {
            Map<String, String> headers = new HashMap<>(route.request().headers());
            // Inject Bearer token — in real jobs, this would be a valid JWT
            headers.put("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test-token");

            // Capture for verification
            String authHeader = headers.get("Authorization");
            capturedAuthHeaders.add(authHeader);

            // fallback() with modified headers — preserves handler chain
            route.fallback(new Route.FallbackOptions().setHeaders(headers));
        });

        // Make an API call — the auth header should be automatically injected
        Response response = page.waitForResponse(
                resp -> resp.url().contains("/api/productsList"),
                () -> page.evaluate("fetch('/api/productsList')")
        );

        // Verify auth header was injected
        assertFalse(capturedAuthHeaders.isEmpty(), "Auth header should be injected on API call");
        assertTrue(capturedAuthHeaders.get(0).startsWith("Bearer "),
                "Auth header should be a Bearer token");

        // API still responds (automationexercise.com ignores the auth header)
        // In a real app, this token would authenticate the request
        assertEquals(response.status(), 200, "API should respond (token ignored by this server)");
        System.out.println("Injected auth header on " + capturedAuthHeaders.size() + " API request(s)");
    }

    // ═══════════════════════════════════════════════════════════════════
    // MOCK — Full custom product JSON
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Mock the Products API with a complete custom JSON response.
     * Verifies the entire mock structure is returned correctly.
     *
     * In a SPA, this test would verify:
     *   - Custom product name renders in the product card
     *   - Custom price renders correctly
     *   - Custom brand appears in the brand filter
     *   - Custom category appears in the sidebar
     *
     * This is the most common mock pattern in real test automation.
     */
    @Test(description = "Mock full custom product JSON and verify complete response structure")
    public void mockFullCustomProductJson_verifyCompleteResponse() {
        page.navigate(ConfigReader.getBaseUrl());

        // Full mock response mimicking the real API structure
        String mockJson = """
                {
                    "responseCode": 200,
                    "products": [
                        {
                            "id": 999,
                            "name": "Mock Test Product",
                            "price": "Rs. 1234",
                            "brand": "MockBrand",
                            "category": {
                                "usertype": {"usertype": "Women"},
                                "category": "Tops"
                            }
                        },
                        {
                            "id": 1000,
                            "name": "Second Mock Product",
                            "price": "Rs. 5678",
                            "brand": "MockBrand2",
                            "category": {
                                "usertype": {"usertype": "Men"},
                                "category": "Jeans"
                            }
                        }
                    ]
                }
                """;

        // Mock the API to return our custom JSON
        page.route("**/api/productsList", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(200)
                    .setContentType("application/json")
                    .setBody(mockJson)
            );
        });

        // Fetch the API — should get our mock, not real data
        Response response = page.waitForResponse(
                resp -> resp.url().contains("/api/productsList"),
                () -> page.evaluate("fetch('/api/productsList')")
        );

        // Verify complete mock response structure
        assertEquals(response.status(), 200);
        String body = response.text();

        // Verify custom products are present
        assertTrue(body.contains("Mock Test Product"), "First mock product should be present");
        assertTrue(body.contains("Second Mock Product"), "Second mock product should be present");

        // Verify custom prices
        assertTrue(body.contains("Rs. 1234"), "Custom price should be present");
        assertTrue(body.contains("Rs. 5678"), "Second custom price should be present");

        // Verify custom brands
        assertTrue(body.contains("MockBrand"), "Custom brand should be present");

        // Verify real product data is NOT present — mock completely replaced the response
        assertFalse(body.contains("Blue Top"),
                "Real product 'Blue Top' should NOT be in mocked response");

        // In a SPA, you'd now verify:
        // assertThat(page.locator(".product-name").first()).hasText("Mock Test Product");
        // assertThat(page.locator(".product-price").first()).hasText("Rs. 1234");
    }

    // ═══════════════════════════════════════════════════════════════════
    // ABORT vs FULFILL comparison
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Demonstrate the difference between abort() and fulfill(500).
     * Both prevent the real server from being contacted, but the browser sees different things:
     *
     *   abort()      → Network failure. Browser gets ERR_FAILED. No HTTP status.
     *                   Like unplugging the ethernet cable for that one request.
     *                   SPA shows: "Network error" or "Could not connect"
     *
     *   fulfill(500) → Server responded with error. Browser gets HTTP 500.
     *                   Server was "reachable" but returned an error.
     *                   SPA shows: "Server error" or "Something went wrong"
     *
     * These test DIFFERENT error handling paths in the application.
     */
    @Test(description = "Compare abort() vs fulfill(500) - different error types")
    public void compareAbortVsFulfill500_differentErrorTypes() {
        page.navigate(ConfigReader.getBaseUrl());

        // TEST 1: abort() — simulates network failure
        page.route("**/api/productsList", route -> route.abort());

        // fetch() with abort() throws a TypeError (network error)
        Object abortResult = page.evaluate("""
                async () => {
                    try {
                        await fetch('/api/productsList');
                        return 'success';
                    } catch (e) {
                        return 'network_error: ' + e.message;
                    }
                }
                """);
        assertTrue(abortResult.toString().contains("network_error"),
                "abort() should cause a network error. Got: " + abortResult);

        // Remove the abort route before registering the fulfill route
        page.unroute("**/api/productsList");

        // TEST 2: fulfill(500) — simulates server error response
        page.route("**/api/productsList", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(500)
                    .setContentType("application/json")
                    .setBody("{\"error\": \"Server Error\"}")
            );
        });

        // fetch() with fulfill(500) succeeds (no network error) but returns 500 status
        Object fulfillResult = page.evaluate("""
                async () => {
                    try {
                        const resp = await fetch('/api/productsList');
                        return 'status: ' + resp.status;
                    } catch (e) {
                        return 'network_error: ' + e.message;
                    }
                }
                """);
        assertEquals(fulfillResult.toString(), "status: 500",
                "fulfill(500) should return HTTP 500, not a network error. Got: " + fulfillResult);

        System.out.println("abort() result:      " + abortResult);
        System.out.println("fulfill(500) result: " + fulfillResult);
    }

    // ═══════════════════════════════════════════════════════════════════
    // HAR RECORDING — Record and replay network traffic
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Record all network traffic to a HAR (HTTP Archive) file.
     *
     * HAR = a standard format that records every request and response.
     * Think of it as a "VCR recording" of all network traffic.
     *
     * HAR recording is set on the BrowserContext level (not page level):
     *   - browser.newContext(new NewContextOptions().setRecordHarPath(path))
     *   - HAR file is written when context.close() is called
     *
     * Use cases:
     *   - Replay network in CI (no internet needed)
     *   - Debug test failures (see exactly what happened)
     *   - Test against a data snapshot (data never changes)
     *   - Work offline when server is down
     *
     * To REPLAY a HAR file later:
     *   page.routeFromHAR(Paths.get("recording.har"),
     *       new Page.RouteFromHAROptions().setNotFound(HarNotFound.FALLBACK));
     */
    @Test(description = "Record network traffic to HAR file and verify file is created")
    public void harRecordProducts_verifyFileCreated() throws Exception {
        Path harDir = Paths.get("har");
        Path harFile = harDir.resolve("products-recording.har");

        // Create har/ directory if it doesn't exist
        Files.createDirectories(harDir);

        // Delete old recording if it exists (start fresh)
        Files.deleteIfExists(harFile);

        // Create a NEW context with HAR recording enabled
        // This is separate from BaseTest's context because HAR recording
        // must be configured at context creation time
        BrowserContext harContext = getBrowser().newContext(
                new Browser.NewContextOptions()
                        .setRecordHarPath(harFile)
                        // Optional: only record specific URL patterns
                        // .setRecordHarUrlFilter(Pattern.compile(".*automationexercise.*"))
        );

        Page harPage = harContext.newPage();

        // Navigate and interact — all network traffic is being recorded
        harPage.navigate(ConfigReader.getBaseUrl() + "/products");
        harPage.waitForLoadState(LoadState.NETWORKIDLE);

        // Close context — THIS is when the HAR file is actually written to disk
        harContext.close();

        // Verify HAR file was created and has content
        assertTrue(Files.exists(harFile), "HAR file should be created at: " + harFile);
        long fileSize = Files.size(harFile);
        assertTrue(fileSize > 0, "HAR file should not be empty. Size: " + fileSize + " bytes");

        System.out.println("HAR file created: " + harFile.toAbsolutePath());
        System.out.println("HAR file size: " + (fileSize / 1024) + " KB");

        // To REPLAY this recording in a future test:
        // page.routeFromHAR(harFile,
        //     new Page.RouteFromHAROptions().setNotFound(HarNotFound.FALLBACK));
        // page.navigate(ConfigReader.getBaseUrl() + "/products");
        // // All responses come from HAR file — no real network calls
    }
}
