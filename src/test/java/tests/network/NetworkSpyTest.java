package tests.network;

import base.BaseTest;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import config.ConfigReader;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.testng.Assert.*;

/**
 * NETWORK SPY TESTS — Observe and verify network traffic without modifying it.
 *
 * Two spying approaches:
 *   1. page.onRequest / page.onResponse / page.onRequestFailed — LISTENERS (passive, observe-only)
 *   2. page.route() + route.fallback() — INTERCEPTORS used as spies (capture data, then pass request along)
 *
 * Key difference:
 *   - Listeners (onRequest/onResponse) = you OBSERVE. Cannot change anything.
 *   - Routes (page.route) = you CONTROL. Can block/mock/modify. But can also just spy and fallback.
 *
 * In real jobs, spying is used to:
 *   - Debug slow pages (find which request takes longest)
 *   - Verify the frontend sends correct data to APIs
 *   - Detect unexpected failed requests
 *   - Count how many API calls a page makes (performance budgeting)
 */
public class NetworkSpyTest extends BaseTest {

    // ═══════════════════════════════════════════════════════════════════
    // SPY WITH LISTENERS — page.onRequest / page.onResponse
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Spy on ALL requests during home page load using passive listeners.
     * Verifies no page resources return HTTP errors (4xx/5xx).
     *
     * page.onResponse(callback) — fires for EVERY response the browser receives.
     * You cannot modify anything — only observe.
     */
    @Test(description = "Spy on home page load - verify no HTTP errors in page resources")
    public void spyOnHomePageRequests_verifyNoErrors() {
        // CopyOnWriteArrayList = thread-safe list, because listeners fire on Playwright's internal thread
        List<String> errorResponses = new CopyOnWriteArrayList<>();

        // onResponse = PASSIVE LISTENER
        // Fires for every response the browser receives (HTML, CSS, JS, images, APIs)
        // Cannot modify the response — only read status, url, headers, body
        page.onResponse(response -> {
            if (response.status() >= 400) {
                errorResponses.add(response.status() + " → " + response.url());
            }
        });

        page.navigate(ConfigReader.getBaseUrl());

        // NETWORKIDLE = wait until no network requests for 500ms
        // This ensures ALL resources have finished loading before we assert
        // Critical for SPAs where data loads AFTER initial HTML render
        // For server-rendered sites like automationexercise.com, LOAD state is usually enough
        page.waitForLoadState(LoadState.NETWORKIDLE);

        assertTrue(errorResponses.isEmpty(),
                "Page resources should not return HTTP errors. Found:\n"
                        + String.join("\n", errorResponses));
    }

    /**
     * Count and categorize ALL requests the products page makes.
     * Useful for performance budgeting — "this page should not make more than X requests."
     *
     * page.onRequest(callback) — fires for EVERY outgoing request.
     * Includes requests that later get blocked by route handlers (ads).
     */
    @Test(description = "Count and categorize all requests on the products page")
    public void countTotalRequestsOnProductsPage() {
        List<String> allRequests = new CopyOnWriteArrayList<>();

        // onRequest = fires for EVERY request the browser makes
        // This fires BEFORE the route handler decides to block/resume/fulfill
        // So you'll see ad requests here even though BaseTest blocks them
        page.onRequest(request -> {
            allRequests.add(request.method() + " " + request.url());
        });

        page.navigate(ConfigReader.getBaseUrl() + "/products");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Categorize requests by resource type using URL patterns
        long cssCount = allRequests.stream().filter(u -> u.contains(".css")).count();
        long jsCount = allRequests.stream().filter(u -> u.contains(".js")).count();
        long imageCount = allRequests.stream()
                .filter(u -> u.contains(".png") || u.contains(".jpg")
                        || u.contains(".gif") || u.contains(".webp") || u.contains(".jpeg"))
                .count();

        System.out.println("═══ Request Breakdown for Products Page ═══");
        System.out.println("Total requests:  " + allRequests.size());
        System.out.println("CSS files:       " + cssCount);
        System.out.println("JS files:        " + jsCount);
        System.out.println("Images:          " + imageCount);
        System.out.println("═══════════════════════════════════════════");

        // Basic sanity assertions
        assertTrue(allRequests.size() > 0, "Page should make at least some requests");
        assertTrue(imageCount > 0, "Products page should request product thumbnail images");
    }

    /**
     * Log ALL network traffic (requests, responses, failures) for the home page.
     * Verifies no non-ad requests fail.
     *
     * Uses all three listeners together:
     *   - page.onRequest — every outgoing request
     *   - page.onResponse — every received response
     *   - page.onRequestFailed — network failures and blocked requests
     *
     * In real jobs, this pattern is used to:
     *   - Debug flaky tests ("which request is failing?")
     *   - Create network activity reports
     *   - Detect when third-party scripts break your page
     */
    @Test(description = "Log all network traffic and verify no non-ad requests fail")
    public void logAllTraffic_verifyNoNonAdFailures() {
        List<String> requests = new CopyOnWriteArrayList<>();
        List<String> responses = new CopyOnWriteArrayList<>();
        List<String> realFailures = new CopyOnWriteArrayList<>();

        // LISTENER 1: Log every outgoing request
        page.onRequest(request -> {
            requests.add("→ " + request.method() + " " + request.url());
        });

        // LISTENER 2: Log every incoming response
        page.onResponse(response -> {
            responses.add("← " + response.status() + " " + response.url());
        });

        // LISTENER 3: Log failed requests
        // Requests blocked by route.abort() show up here as failures
        // We exclude known ad domains since BaseTest intentionally blocks them
        page.onRequestFailed(request -> {
            String url = request.url().toLowerCase();
            boolean isIntentionallyBlocked = url.contains("google") || url.contains("doubleclick")
                    || url.contains("analytics") || url.contains("onesignal")
                    || url.contains("facebook") || url.contains("adsbygoogle")
                    || url.contains("googlesyndication") || url.contains("adservice")
                    || url.contains("aswpsdkus") || url.contains("cdn.taboola")
                    || url.contains("pagead") || url.contains("adsense");
            if (!isIntentionallyBlocked) {
                realFailures.add("✗ FAILED: " + request.url() + " | " + request.failure());
            }
        });

        page.navigate(ConfigReader.getBaseUrl());
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Print full traffic log for debugging
        System.out.println("═══ Network Traffic Log ═══");
        requests.forEach(System.out::println);
        System.out.println("--- Responses ---");
        responses.forEach(System.out::println);
        System.out.println("═══════════════════════════");
        System.out.println("Requests sent:    " + requests.size());
        System.out.println("Responses received: " + responses.size());
        System.out.println("Non-ad failures:  " + realFailures.size());

        // Real page resources should not fail — only blocked ads should fail
        assertTrue(realFailures.isEmpty(),
                "Found non-ad request failures:\n" + String.join("\n", realFailures));
    }

    // ═══════════════════════════════════════════════════════════════════
    // SPY WITH ROUTE INTERCEPTION — page.route() + route.fallback()
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Spy on the Products API by intercepting the request, recording details,
     * then forwarding to the real server.
     *
     * route.fallback() = "I'm done observing, let the next handler deal with it."
     * Unlike route.resume() which sends directly to server, fallback() lets
     * other registered routes (like BaseTest's ad blocker) still process the request.
     *
     * In a SPA, the page would call this API automatically on load.
     * On automationexercise.com (server-rendered), we trigger it via fetch().
     */
    @Test(description = "Spy on Products API - capture request details and verify response")
    public void spyOnProductsApi_verifyResponseStructure() {
        page.navigate(ConfigReader.getBaseUrl());

        // Spy variables — capture request details inside the route handler
        List<String> spiedMethods = new CopyOnWriteArrayList<>();
        List<Map<String, String>> spiedHeaders = new CopyOnWriteArrayList<>();

        // Register a SPY route for the products API
        // Pattern: **/api/productsList matches any URL ending with /api/productsList
        page.route("**/api/productsList", route -> {
            // CAPTURE — record what we want to verify later
            spiedMethods.add(route.request().method());
            spiedHeaders.add(new HashMap<>(route.request().headers()));

            // FALLBACK — pass request to next handler (BaseTest's route)
            // The request eventually reaches the real server unchanged
            // We're just a "spy camera" — observe and let through
            route.fallback();
        });

        // Trigger the API call via browser's fetch()
        // waitForResponse() sets up a listener FIRST, then runs the callback
        // This ensures we don't miss the response due to timing
        Response apiResponse = page.waitForResponse(
                resp -> resp.url().contains("/api/productsList"),
                () -> page.evaluate("fetch('/api/productsList')")
        );

        // Verify spy captured the request correctly
        assertEquals(spiedMethods.size(), 1, "Spy should capture exactly one request");
        assertEquals(spiedMethods.get(0), "GET", "Products API uses GET method");

        // Verify the real server response
        assertEquals(apiResponse.status(), 200, "Products API should return 200");
        String body = apiResponse.text();
        assertTrue(body.contains("\"products\""), "Response should contain 'products' key");
        assertTrue(body.contains("\"id\""), "Products should have 'id' field");
        assertTrue(body.contains("\"name\""), "Products should have 'name' field");
        assertTrue(body.contains("\"price\""), "Products should have 'price' field");
        assertTrue(body.contains("\"brand\""), "Products should have 'brand' field");
    }

    /**
     * Spy on the Brands API — same pattern as products, different endpoint.
     * Verifies the brands API returns expected data structure.
     */
    @Test(description = "Spy on Brands API - verify response contains brand data")
    public void spyOnBrandsApi_verifyResponseStructure() {
        page.navigate(ConfigReader.getBaseUrl());

        List<String> spiedUrls = new CopyOnWriteArrayList<>();

        // Spy route for brands API
        page.route("**/api/brandsList", route -> {
            spiedUrls.add(route.request().url());
            route.fallback(); // Observe and pass through
        });

        Response apiResponse = page.waitForResponse(
                resp -> resp.url().contains("/api/brandsList"),
                () -> page.evaluate("fetch('/api/brandsList')")
        );

        // Verify spy captured it
        assertEquals(spiedUrls.size(), 1, "Should spy exactly one brands request");

        // Verify response structure
        assertEquals(apiResponse.status(), 200);
        String body = apiResponse.text();
        assertTrue(body.contains("\"brands\""), "Response should contain 'brands' key");
        assertTrue(body.contains("\"brand\""), "Each brand should have 'brand' field");
    }

    /**
     * Spy on the Search API — uses POST method with form data.
     * Captures the request body (search term) and verifies the response.
     *
     * This demonstrates spying on POST requests — useful for verifying
     * the frontend sends correct form data to the backend.
     */
    @Test(description = "Spy on Search API - verify POST request sends correct data")
    public void spyOnSearchApi_verifySearchResults() {
        page.navigate(ConfigReader.getBaseUrl());

        List<String> spiedMethods = new CopyOnWriteArrayList<>();
        List<String> spiedPostData = new CopyOnWriteArrayList<>();

        // Spy route for search API — captures method and POST body
        page.route("**/api/searchProduct", route -> {
            spiedMethods.add(route.request().method());
            String postData = route.request().postData();
            if (postData != null) {
                spiedPostData.add(postData);
            }
            route.fallback();
        });

        // Trigger search API via POST with form data
        Response apiResponse = page.waitForResponse(
                resp -> resp.url().contains("/api/searchProduct"),
                () -> page.evaluate("""
                        () => {
                            const formData = new FormData();
                            formData.append('search_product', 'top');
                            return fetch('/api/searchProduct', { method: 'POST', body: formData });
                        }
                        """)
        );

        // Verify spy captured POST request
        assertEquals(spiedMethods.size(), 1);
        assertEquals(spiedMethods.get(0), "POST", "Search API uses POST method");

        // Verify response has search results
        assertEquals(apiResponse.status(), 200);
        String body = apiResponse.text();
        assertTrue(body.contains("\"products\""), "Search response should contain products");
    }

    /**
     * Spy on category navigation — when user clicks a category link,
     * verify the browser navigates to the correct URL.
     *
     * Uses request.isNavigationRequest() to filter only page navigations
     * from all the other requests (CSS, JS, images).
     *
     * In real jobs, this verifies routing/deep-linking works correctly.
     */
    @Test(description = "Spy on category navigation - verify correct URL is loaded")
    public void spyOnCategoryNavigation_verifyCorrectUrl() {
        page.navigate(ConfigReader.getBaseUrl() + "/products");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Spy on navigation requests only (not CSS/JS/images)
        List<String> navigationUrls = new CopyOnWriteArrayList<>();
        page.onRequest(request -> {
            // isNavigationRequest() = true only for the main page URL change
            // Filters out all sub-resource requests (images, scripts, stylesheets)
            if (request.isNavigationRequest()) {
                navigationUrls.add(request.url());
            }
        });

        // Click Women category to expand it
        page.locator(".category-products .panel-default a[href='#Women']").click();
        page.locator("#Women").waitFor();

        // Click Dress subcategory — triggers page navigation
        page.locator("#Women a:has-text('Dress')").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Verify navigation went to the correct category URL
        assertTrue(navigationUrls.stream().anyMatch(url -> url.contains("category_products")),
                "Should navigate to category URL. Captured URLs: " + navigationUrls);
    }
}
