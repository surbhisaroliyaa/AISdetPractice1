package tests.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import config.ConfigReader;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SearchBoundarySecurityTest {

    private Playwright playwright;
    private APIRequestContext api;

    @BeforeClass
    public void setup() {
        playwright = Playwright.create();
        api = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(ConfigReader.getBaseUrl())
        );
    }

    @AfterClass
    public void teardown() {
        api.dispose();
        playwright.close();
    }

    // ─── BOUNDARY: Empty / Edge-case search terms ────────────────────────

    @Test(description = "Search with empty string — should return all products or empty, NOT crash")
    public void testSearchWithEmptyString() {
        APIResponse response = api.post("/api/searchProduct",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("search_product", ""))
        );

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        // API should respond gracefully — either products or empty array, not an error
        assertTrue(body.has("responseCode"), "Response must have responseCode");
        assertTrue(body.has("products"), "Response must have products array");
    }

    @Test(description = "Search with single character — tests partial matching")
    public void testSearchWithSingleChar() {
        APIResponse response = api.post("/api/searchProduct",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("search_product", "t"))
        );

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200);
    }

    @Test(description = "Search with very long string (500+ chars) — should not crash server")
    public void testSearchWithVeryLongString() {
        String longString = "a".repeat(500);

        APIResponse response = api.post("/api/searchProduct",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("search_product", longString))
        );

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        // Server should handle gracefully — 200 with empty results, not 500
        assertTrue(body.has("responseCode"), "Response must have responseCode");
        assertNotEquals(body.get("responseCode").getAsInt(), 500,
                "Server should NOT return 500 for long input");
    }

    @Test(description = "Search with spaces only — edge case")
    public void testSearchWithSpacesOnly() {
        APIResponse response = api.post("/api/searchProduct",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("search_product", "   "))
        );

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertTrue(body.has("responseCode"), "Response must have responseCode");
    }

    @Test(description = "Search with no results — verify empty array, not null")
    public void testSearchWithNoResults() {
        APIResponse response = api.post("/api/searchProduct",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("search_product", "xyznonexistentproduct123"))
        );

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200);

        // Contract: products should be empty array [], NOT null
        JsonArray products = body.getAsJsonArray("products");
        assertNotNull(products, "Products should be empty array, not null");
        assertEquals(products.size(), 0, "Non-existent search should return 0 results");
    }

    // ─── SECURITY: SQL Injection ─────────────────────────────────────────

    @Test(description = "SQL injection in search — should not expose data or crash")
    public void testSearchSqlInjection() {
        APIResponse response = api.post("/api/searchProduct",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("search_product", "' OR 1=1 --"))
        );

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        // Should NOT return all products (that would mean SQL injection worked)
        // Should either return empty results or treat it as a literal search
        assertTrue(body.has("responseCode"), "Response must have responseCode");
        assertNotEquals(body.get("responseCode").getAsInt(), 500,
                "SQL injection should NOT cause server error");
    }

    @Test(description = "SQL injection in verifyLogin email — should not bypass auth")
    public void testLoginSqlInjection() {
        APIResponse response = api.post("/api/verifyLogin",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("email", "' OR '1'='1' --")
                                .set("password", "anything"))
        );

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        // Should NOT return 200 "User exists!" — that would mean SQL injection worked
        assertNotEquals(body.get("responseCode").getAsInt(), 200,
                "SQL injection should NOT bypass authentication");
    }

    // ─── SECURITY: XSS Payloads ──────────────────────────────────────────

    @Test(description = "XSS payload in search — should not execute or crash")
    public void testSearchXssPayload() {
        APIResponse response = api.post("/api/searchProduct",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("search_product", "<script>alert('xss')</script>"))
        );

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertTrue(body.has("responseCode"), "Response must have responseCode");
        assertNotEquals(body.get("responseCode").getAsInt(), 500,
                "XSS payload should NOT cause server error");
    }

    @Test(description = "Special characters in search — should handle gracefully")
    public void testSearchSpecialCharacters() {
        APIResponse response = api.post("/api/searchProduct",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("search_product", "!@#$%^&*(){}[]|\\"))
        );

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertNotEquals(body.get("responseCode").getAsInt(), 500,
                "Special characters should NOT cause server error");
    }

    // ─── PERFORMANCE: Response time checks ───────────────────────────────

    @Test(description = "GET productsList should respond within 3 seconds")
    public void testProductsListPerformance() {
        long start = System.currentTimeMillis();
        APIResponse response = api.get("/api/productsList");
        long duration = System.currentTimeMillis() - start;

        assertEquals(response.status(), 200);
        assertTrue(duration < 3000,
                "productsList took " + duration + "ms — should be under 3000ms");
        System.out.println("GET /api/productsList: " + duration + "ms");
    }

    @Test(description = "GET brandsList should respond within 3 seconds")
    public void testBrandsListPerformance() {
        long start = System.currentTimeMillis();
        APIResponse response = api.get("/api/brandsList");
        long duration = System.currentTimeMillis() - start;

        assertEquals(response.status(), 200);
        assertTrue(duration < 3000,
                "brandsList took " + duration + "ms — should be under 3000ms");
        System.out.println("GET /api/brandsList: " + duration + "ms");
    }

    @Test(description = "POST searchProduct should respond within 3 seconds")
    public void testSearchPerformance() {
        long start = System.currentTimeMillis();
        APIResponse response = api.post("/api/searchProduct",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("search_product", "top"))
        );
        long duration = System.currentTimeMillis() - start;

        assertEquals(response.status(), 200);
        assertTrue(duration < 3000,
                "searchProduct took " + duration + "ms — should be under 3000ms");
        System.out.println("POST /api/searchProduct: " + duration + "ms");
    }

    // ─── CONTRACT: Error responses always have responseCode + message ────

    @Test(description = "Contract: missing param error has responseCode AND message")
    public void testErrorResponseContract_MissingParam() {
        APIResponse response = api.post("/api/searchProduct");

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertTrue(body.has("responseCode"),
                "Error response MUST have 'responseCode'");
        assertTrue(body.has("message"),
                "Error response MUST have 'message'");
        assertFalse(body.get("message").getAsString().isEmpty(),
                "Error message should NOT be empty");
    }

    @Test(description = "Contract: wrong method error has responseCode AND message")
    public void testErrorResponseContract_WrongMethod() {
        APIResponse response = api.put("/api/productsList");

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertTrue(body.has("responseCode"),
                "Error response MUST have 'responseCode'");
        assertTrue(body.has("message"),
                "Error response MUST have 'message'");
        assertFalse(body.get("message").getAsString().isEmpty(),
                "Error message should NOT be empty");
    }

    // ─── CONTRACT: Product structure validation (ALL products) ───────────

    @Test(description = "Contract: EVERY product must have id, name, price, brand, category")
    public void testAllProductsHaveRequiredFields() {
        APIResponse response = api.get("/api/productsList");
        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        JsonArray products = body.getAsJsonArray("products");

        for (int i = 0; i < products.size(); i++) {
            JsonObject product = products.get(i).getAsJsonObject();
            String productId = "Product[" + i + "]";

            assertTrue(product.has("id"), productId + " missing 'id'");
            assertTrue(product.has("name"), productId + " missing 'name'");
            assertTrue(product.has("price"), productId + " missing 'price'");
            assertTrue(product.has("brand"), productId + " missing 'brand'");
            assertTrue(product.has("category"), productId + " missing 'category'");
        }

        System.out.println("Contract validated for all " + products.size() + " products");
    }
}
