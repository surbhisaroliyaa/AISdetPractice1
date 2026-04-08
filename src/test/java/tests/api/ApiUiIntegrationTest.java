package tests.api;

import base.BaseTest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import config.ConfigReader;
import org.testng.annotations.*;

import pages.SignupLoginPage;
import utils.TestDataGenerator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.*;

/**
 * Integration test: API setup → UI test → API cleanup
 *
 * This is the most impressive pattern for interviews:
 * - Step 1: Create user via API (fast — no browser, <1 second)
 * - Step 2: Login via UI (realistic — tests the actual login flow)
 * - Step 3: Verify logged-in state via UI
 * - Step 4: Delete user via API (reliable cleanup — no UI flakiness)
 */
public class ApiUiIntegrationTest extends BaseTest {

    private APIRequestContext api;

    private final String testEmail = TestDataGenerator.getRandomEmail();
    private final String testPassword = TestDataGenerator.getRandomPassword();
    private final String testName = TestDataGenerator.getRandomFirstName();
    private final String testLastName = TestDataGenerator.getRandomLastName();

    @BeforeClass
    @Parameters({"browser"})
    @Override
    public void startBrowser(@Optional String browserParam) {
        super.startBrowser(browserParam);

        // Create a SEPARATE API context — not tied to the browser
        api = getPlaywright().request().newContext(
                new APIRequest.NewContextOptions()
                        .setBaseURL(ConfigReader.getBaseUrl())
        );
    }

    @AfterClass
    @Override
    public void closeBrowser() {
        api.dispose();
        super.closeBrowser();
    }

    @Test(description = "Step 1: Create user via API (fast setup)", priority = 1)
    public void step1_CreateUserViaApi() {
        long startTime = System.currentTimeMillis();

        APIResponse response = api.post("/api/createAccount",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("name", testName)
                                .set("email", testEmail)
                                .set("password", testPassword)
                                .set("title", "Mr")
                                .set("birth_date", "10")
                                .set("birth_month", "3")
                                .set("birth_year", "1995")
                                .set("firstname", testName)
                                .set("lastname", testLastName)
                                .set("company", TestDataGenerator.getRandomCompany())
                                .set("address1", TestDataGenerator.getRandomAddress())
                                .set("address2", "")
                                .set("country", "India")
                                .set("zipcode", TestDataGenerator.getRandomZipcode())
                                .set("state", TestDataGenerator.getRandomState())
                                .set("city", TestDataGenerator.getRandomCity())
                                .set("mobile_number", TestDataGenerator.getRandomPhone()))
        );

        long duration = System.currentTimeMillis() - startTime;

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 201, "User creation failed");
        System.out.println("API user creation took: " + duration + "ms");
    }

    @Test(description = "Step 2: Login via UI and verify logged-in state",
            priority = 2, dependsOnMethods = "step1_CreateUserViaApi")
    public void step2_LoginViaUiAndVerify() {
        // Login via UI — this is what we're actually testing (the real user flow)
        SignupLoginPage loginPage = new SignupLoginPage(page);
        loginPage.navigateToLoginPage();
        loginPage.login(testEmail, testPassword);

        // Verify: logged in — username visible in header
        assertThat(page.locator("a:has-text('Logged in as')")).isVisible();
        assertTrue(
                page.locator("a:has-text('Logged in as')").innerText().contains(testName),
                "Logged in username should contain: " + testName
        );

        // Verify: logged in user sees Logout, NOT Signup/Login
        assertThat(page.locator(".navbar-nav a[href='/logout']")).isVisible();
        assertThat(page.locator("a:has-text('Signup / Login')")).not().isVisible();
    }

    @Test(description = "Step 4: Delete user via API (reliable cleanup)",
            priority = 3, alwaysRun = true)
    public void step3_DeleteUserViaApi() {
        long startTime = System.currentTimeMillis();

        APIResponse response = api.delete("/api/deleteAccount",
                RequestOptions.create()
                        .setForm(FormData.create()
                                .set("email", testEmail)
                                .set("password", testPassword))
        );

        long duration = System.currentTimeMillis() - startTime;

        JsonObject body = JsonParser.parseString(response.text()).getAsJsonObject();
        assertEquals(body.get("responseCode").getAsInt(), 200, "User deletion failed");
        System.out.println("API user deletion took: " + duration + "ms");
    }

    @Test(description = "Step 4: Verify deletion — user cannot login",
            priority = 4, dependsOnMethods = "step3_DeleteUserViaApi")
    public void step4_VerifyDeletion() {
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
