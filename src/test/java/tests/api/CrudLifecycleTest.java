package tests.api;

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
import utils.TestDataGenerator;

import static org.testng.Assert.*;

/**
 * Full CRUD lifecycle in ONE class with strict ordering:
 * Create → Read → Update → Read (verify update) → Delete → Read (verify deletion)
 *
 * This is the most thorough way to test a resource lifecycle.
 * Each step depends on the previous — if Create fails, everything is skipped.
 */
public class CrudLifecycleTest {

    private Playwright playwright;
    private APIRequestContext api;

    private final String email = TestDataGenerator.getRandomEmail();
    private final String password = TestDataGenerator.getRandomPassword();
    private final String originalName = TestDataGenerator.getRandomFirstName();
    private final String updatedName = "CrudUpdated";

    @BeforeClass
    public void setup() {
        playwright = Playwright.create();
        api = playwright.request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(ConfigReader.getBaseUrl())
        );
    }

    @AfterClass(alwaysRun = true)
    public void teardown() {
        // Safety net — try to delete user even if tests failed midway
        api.delete("/api/deleteAccount",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("email", email)
                                .set("password", password))
        );
        api.dispose();
        playwright.close();
    }

    // ─── Step 1: CREATE ──────────────────────────────────────────────────

    @Test(description = "CRUD Step 1: Create account", priority = 1)
    public void step1_Create() {
        APIResponse response = api.post("/api/createAccount",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("name", originalName)
                                .set("email", email)
                                .set("password", password)
                                .set("title", "Mr")
                                .set("birth_date", "15")
                                .set("birth_month", "6")
                                .set("birth_year", "1995")
                                .set("firstname", originalName)
                                .set("lastname", TestDataGenerator.getRandomLastName())
                                .set("company", TestDataGenerator.getRandomCompany())
                                .set("address1", TestDataGenerator.getRandomAddress())
                                .set("address2", "")
                                .set("country", "India")
                                .set("zipcode", TestDataGenerator.getRandomZipcode())
                                .set("state", TestDataGenerator.getRandomState())
                                .set("city", TestDataGenerator.getRandomCity())
                                .set("mobile_number", TestDataGenerator.getRandomPhone()))
        );

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 201);
        assertEquals(body.get("message").getAsString(), "User created!");
    }

    // ─── Step 2: READ — verify created data ──────────────────────────────

    @Test(description = "CRUD Step 2: Read — verify created user has correct data",
            priority = 2, dependsOnMethods = "step1_Create")
    public void step2_ReadAfterCreate() {
        APIResponse response = api.get("/api/getUserDetailByEmail",
                RequestOptions.create()
                        .setQueryParam("email", email)
        );

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200);

        JsonObject user = body.getAsJsonObject("user");
        assertEquals(user.get("name").getAsString(), originalName,
                "Name should match what was sent during creation");
        assertEquals(user.get("email").getAsString(), email,
                "Email should match what was sent during creation");
    }

    // ─── Step 3: UPDATE ──────────────────────────────────────────────────

    @Test(description = "CRUD Step 3: Update account with new name and city",
            priority = 3, dependsOnMethods = "step2_ReadAfterCreate")
    public void step3_Update() {
        APIResponse response = api.put("/api/updateAccount",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("name", updatedName)
                                .set("email", email)
                                .set("password", password)
                                .set("title", "Mrs")
                                .set("birth_date", "25")
                                .set("birth_month", "12")
                                .set("birth_year", "1990")
                                .set("firstname", updatedName)
                                .set("lastname", "UpdatedLast")
                                .set("company", "UpdatedCorp")
                                .set("address1", "999 Updated Road")
                                .set("address2", "Block Z")
                                .set("country", "India")
                                .set("zipcode", "999999")
                                .set("state", "Tamil Nadu")
                                .set("city", "Chennai")
                                .set("mobile_number", "8888888888"))
        );

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200);
        assertEquals(body.get("message").getAsString(), "User updated!");
    }

    // ─── Step 4: READ — verify update was actually applied ───────────────

    @Test(description = "CRUD Step 4: Read — verify update was saved (catches 'UI says updated but backend didn't save' bugs)",
            priority = 4, dependsOnMethods = "step3_Update")
    public void step4_ReadAfterUpdate() {
        APIResponse response = api.get("/api/getUserDetailByEmail",
                RequestOptions.create()
                        .setQueryParam("email", email)
        );

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200);

        JsonObject user = body.getAsJsonObject("user");
        assertEquals(user.get("name").getAsString(), updatedName,
                "Name should be UPDATED value, not original");
    }

    // ─── Step 5: DELETE ──────────────────────────────────────────────────

    @Test(description = "CRUD Step 5: Delete the account",
            priority = 5, dependsOnMethods = "step4_ReadAfterUpdate")
    public void step5_Delete() {
        APIResponse response = api.delete("/api/deleteAccount",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("email", email)
                                .set("password", password))
        );

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200);
        assertEquals(body.get("message").getAsString(), "Account deleted!");
    }

    // ─── Step 6: READ — verify deletion (MOST IMPORTANT STEP) ───────────

    @Test(description = "CRUD Step 6: Verify deletion — getUserDetail should fail (catches 'soft delete didn't work' bugs)",
            priority = 6, dependsOnMethods = "step5_Delete")
    public void step6_ReadAfterDelete() {
        // Verify via getUserDetailByEmail — should NOT find the user
        APIResponse detailResponse = api.get("/api/getUserDetailByEmail",
                RequestOptions.create()
                        .setQueryParam("email", email)
        );

        JsonObject detailBody = JsonParser.parseString(detailResponse.text()).getAsJsonObject();
        assertNotEquals(detailBody.get("responseCode").getAsInt(), 200,
                "Deleted user should NOT be found via getUserDetailByEmail");

        // Verify via verifyLogin — should NOT authenticate
        APIResponse loginResponse = api.post("/api/verifyLogin",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("email", email)
                                .set("password", password))
        );

        JsonObject loginBody = JsonParser.parseString(loginResponse.text()).getAsJsonObject();
        assertEquals(loginBody.get("responseCode").getAsInt(), 404);
        assertEquals(loginBody.get("message").getAsString(), "User not found!");
    }

    // ─── Step 7: DELETE again — should handle gracefully ─────────────────

    @Test(description = "CRUD Step 7: Delete already-deleted account — should not crash",
            priority = 7, dependsOnMethods = "step5_Delete")
    public void step7_DeleteAlreadyDeleted() {
        APIResponse response = api.delete("/api/deleteAccount",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("email", email)
                                .set("password", password))
        );

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        // Should NOT be 500 — should handle gracefully (either 404 or "Account not found")
        assertNotEquals(body.get("responseCode").getAsInt(), 500,
                "Deleting already-deleted account should NOT cause server error");
    }
}
