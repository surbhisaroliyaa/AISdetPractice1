package tests.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.FormData;
import config.ConfigReader;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ProductsApiTest {

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

    // ─── GET /api/productsList ───────────────────────────────────────────

    @Test(description = "GET productsList returns 200 with list of products")
    public void testGetAllProducts() {
        APIResponse response = api.get("/api/productsList");

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200);

        JsonArray products = body.getAsJsonArray("products");
        assertTrue(products.size() > 0, "Products list should not be empty");

        // Verify each product has required fields (contract testing)
        JsonObject firstProduct = products.get(0).getAsJsonObject();
        assertTrue(firstProduct.has("id"), "Product must have 'id'");
        assertTrue(firstProduct.has("name"), "Product must have 'name'");
        assertTrue(firstProduct.has("price"), "Product must have 'price'");
        assertTrue(firstProduct.has("brand"), "Product must have 'brand'");
        assertTrue(firstProduct.has("category"), "Product must have 'category'");
    }

    @Test(description = "POST to productsList returns 405 — wrong method")
    public void testPostToProductsList_WrongMethod() {
        APIResponse response = api.post("/api/productsList");

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 405);
        assertEquals(body.get("message").getAsString(),
                "This request method is not supported.");
    }

    // ─── POST /api/searchProduct ─────────────────────────────────────────

    @Test(description = "POST searchProduct with valid term returns matching products")
    public void testSearchProductValid() {
        APIResponse response = api.post("/api/searchProduct",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setForm(FormData.create()
                                .set("search_product", "top"))
        );

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200);

        JsonArray products = body.getAsJsonArray("products");
        assertTrue(products.size() > 0, "Search for 'top' should return results");
    }

    @Test(description = "POST searchProduct without search_product param returns 400")
    public void testSearchProductWithoutParam() {
        APIResponse response = api.post("/api/searchProduct");

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 400);
        assertEquals(body.get("message").getAsString(),
                "Bad request, search_product parameter is missing in POST request.");
    }

    // ─── GET /api/brandsList ─────────────────────────────────────────────

    @Test(description = "GET brandsList returns 200 with list of brands")
    public void testGetAllBrands() {
        APIResponse response = api.get("/api/brandsList");

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200);

        JsonArray brands = body.getAsJsonArray("brands");
        assertTrue(brands.size() > 0, "Brands list should not be empty");

        // Contract: each brand has id and brand name
        JsonObject firstBrand = brands.get(0).getAsJsonObject();
        assertTrue(firstBrand.has("id"), "Brand must have 'id'");
        assertTrue(firstBrand.has("brand"), "Brand must have 'brand'");
    }

    @Test(description = "PUT to brandsList returns 405 — wrong method")
    public void testPutToBrandsList_WrongMethod() {
        APIResponse response = api.put("/api/brandsList");

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 405);
        assertEquals(body.get("message").getAsString(),
                "This request method is not supported.");
    }
}
