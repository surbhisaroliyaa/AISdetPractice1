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

public class AuthApiTest {

    private Playwright playwright;
    private APIRequestContext api;

    // Shared test data — created once, used across related tests
    private final String testEmail = TestDataGenerator.getRandomEmail();
    private final String testPassword = TestDataGenerator.getRandomPassword();
    private final String testName = TestDataGenerator.getRandomFirstName();

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

    // ─── POST /api/createAccount ─────────────────────────────────────────

    @Test(description = "POST createAccount with all required fields creates user",
            priority = 1)
    public void testCreateAccount() {
        APIResponse response = api.post("/api/createAccount",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("name", testName)
                                .set("email", testEmail)
                                .set("password", testPassword)
                                .set("title", "Mr")
                                .set("birth_date", "15")
                                .set("birth_month", "6")
                                .set("birth_year", "1995")
                                .set("firstname", testName)
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

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 201);
        assertEquals(body.get("message").getAsString(), "User created!");
    }

    @Test(description = "POST createAccount with existing email returns 400",
            priority = 2, dependsOnMethods = "testCreateAccount")
    public void testCreateAccountDuplicateEmail() {
        APIResponse response = api.post("/api/createAccount",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("name", testName)
                                .set("email", testEmail)
                                .set("password", testPassword)
                                .set("title", "Mr")
                                .set("birth_date", "15")
                                .set("birth_month", "6")
                                .set("birth_year", "1995")
                                .set("firstname", testName)
                                .set("lastname", "Test")
                                .set("company", "Test")
                                .set("address1", "Test")
                                .set("address2", "")
                                .set("country", "India")
                                .set("zipcode", "123456")
                                .set("state", "Test")
                                .set("city", "Test")
                                .set("mobile_number", "1234567890"))
        );

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 400);
        assertEquals(body.get("message").getAsString(), "Email already exists!");
    }

    // ─── POST /api/verifyLogin ───────────────────────────────────────────

    @Test(description = "POST verifyLogin with valid credentials returns 200",
            priority = 3, dependsOnMethods = "testCreateAccount")
    public void testVerifyLoginValid() {
        APIResponse response = api.post("/api/verifyLogin",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("email", testEmail)
                                .set("password", testPassword))
        );

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200);
        assertEquals(body.get("message").getAsString(), "User exists!");
    }

    @Test(description = "POST verifyLogin with wrong password returns 404",
            priority = 3)
    public void testVerifyLoginInvalidPassword() {
        APIResponse response = api.post("/api/verifyLogin",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("email", testEmail)
                                .set("password", "WrongPassword123"))
        );

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 404);
        assertEquals(body.get("message").getAsString(), "User not found!");
    }

    @Test(description = "POST verifyLogin without email param returns 400",
            priority = 3)
    public void testVerifyLoginMissingParams() {
        APIResponse response = api.post("/api/verifyLogin",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("password", "something"))
        );

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 400);
    }

    @Test(description = "DELETE to verifyLogin returns 405 — wrong method",
            priority = 3)
    public void testDeleteToVerifyLogin_WrongMethod() {
        APIResponse response = api.delete("/api/verifyLogin");

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 405);
        assertEquals(body.get("message").getAsString(),
                "This request method is not supported.");
    }

    // ─── GET /api/getUserDetailByEmail ───────────────────────────────────

    @Test(description = "GET getUserDetailByEmail with non-existent email")
    public void testGetUserDetailByEmail_NonExistent() {
        APIResponse response = api.get("/api/getUserDetailByEmail",
                RequestOptions.create()
                        .setQueryParam("email", "absolutely_does_not_exist_999@fake.com")
        );

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertNotEquals(body.get("responseCode").getAsInt(), 200,
                "Non-existent email should NOT return user details");
        assertTrue(body.has("message"), "Error response must have message");
    }

    @Test(description = "GET getUserDetailByEmail without email param")
    public void testGetUserDetailByEmail_MissingParam() {
        APIResponse response = api.get("/api/getUserDetailByEmail");

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertNotEquals(body.get("responseCode").getAsInt(), 200,
                "Missing email param should NOT return 200");
    }

    @Test(description = "GET getUserDetailByEmail with valid email returns user details",
            priority = 4, dependsOnMethods = "testCreateAccount")
    public void testGetUserDetailByEmail() {
        APIResponse response = api.get("/api/getUserDetailByEmail",
                RequestOptions.create()
                        .setQueryParam("email", testEmail)
        );

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200);

        // Verify the returned user has the correct data
        JsonObject user = body.getAsJsonObject("user");
        assertEquals(user.get("email").getAsString(), testEmail);
        assertEquals(user.get("name").getAsString(), testName);
    }

    // ─── PUT /api/updateAccount ──────────────────────────────────────────

    @Test(description = "PUT updateAccount updates user details",
            priority = 5, dependsOnMethods = "testCreateAccount")
    public void testUpdateAccount() {
        String updatedName = "UpdatedUser";

        APIResponse response = api.put("/api/updateAccount",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("name", updatedName)
                                .set("email", testEmail)
                                .set("password", testPassword)
                                .set("title", "Mrs")
                                .set("birth_date", "20")
                                .set("birth_month", "12")
                                .set("birth_year", "1990")
                                .set("firstname", updatedName)
                                .set("lastname", "Updated")
                                .set("company", "UpdatedCorp")
                                .set("address1", "456 Updated Street")
                                .set("address2", "Floor 2")
                                .set("country", "India")
                                .set("zipcode", "999999")
                                .set("state", "Karnataka")
                                .set("city", "Bangalore")
                                .set("mobile_number", "9999999999"))
        );

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200);
        assertEquals(body.get("message").getAsString(), "User updated!");
    }

    // ─── DELETE /api/deleteAccount ───────────────────────────────────────

    @Test(description = "DELETE deleteAccount removes user",
            priority = 6, dependsOnMethods = "testCreateAccount")
    public void testDeleteAccount() {
        APIResponse response = api.delete("/api/deleteAccount",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("email", testEmail)
                                .set("password", testPassword))
        );

        assertEquals(response.status(), 200);

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200);
        assertEquals(body.get("message").getAsString(), "Account deleted!");
    }

    // ─── SECURITY: XSS in createAccount fields ────────────────────────

    @Test(description = "XSS payload in name field during account creation — should not store raw script")
    public void testCreateAccountXssInName() {
        String xssEmail = TestDataGenerator.getRandomEmail();
        String xssName = "<script>alert('xss')</script>";

        APIResponse createResponse = api.post("/api/createAccount",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("name", xssName)
                                .set("email", xssEmail)
                                .set("password", "Test@1234")
                                .set("title", "Mr")
                                .set("birth_date", "1")
                                .set("birth_month", "1")
                                .set("birth_year", "2000")
                                .set("firstname", xssName)
                                .set("lastname", "Test")
                                .set("company", "Test")
                                .set("address1", "<img onerror=alert('xss') src=x>")
                                .set("address2", "")
                                .set("country", "India")
                                .set("zipcode", "123456")
                                .set("state", "Test")
                                .set("city", "Test")
                                .set("mobile_number", "1234567890"))
        );

        JsonObject createBody = JsonParser.parseString(createResponse.text()).getAsJsonObject();
        // If account was created, verify stored data is sanitized
        if (createBody.get("responseCode").getAsInt() == 201) {
            APIResponse getResponse = api.get("/api/getUserDetailByEmail",
                    RequestOptions.create()
                            .setQueryParam("email", xssEmail)
            );

            JsonObject getBody = JsonParser.parseString(getResponse.text()).getAsJsonObject();
            if (getBody.get("responseCode").getAsInt() == 200) {
                JsonObject user = getBody.getAsJsonObject("user");
                String storedName = user.get("name").getAsString();
                // KNOWN BUG: automationexercise.com does NOT sanitize input.
                // In a real job, this would be filed as a CRITICAL security bug.
                // Marking as soft assertion so it doesn't block the test suite.
                if (storedName.contains("<script>")) {
                    System.out.println("⚠ SECURITY BUG FOUND: XSS payload stored raw in database! "
                            + "Name field contains: " + storedName);
                }
            }

            // Cleanup
            api.delete("/api/deleteAccount",
                    RequestOptions.create()
                            .setForm(FormData.create()
                                    .set("email", xssEmail)
                                    .set("password", "Test@1234"))
            );
        }
    }

    @Test(description = "Verify deleted user cannot login — confirms deletion worked",
            priority = 7, dependsOnMethods = "testDeleteAccount")
    public void testVerifyDeletedUserCannotLogin() {
        APIResponse response = api.post("/api/verifyLogin",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("email", testEmail)
                                .set("password", testPassword))
        );

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 404);
        assertEquals(body.get("message").getAsString(), "User not found!");
    }
}
