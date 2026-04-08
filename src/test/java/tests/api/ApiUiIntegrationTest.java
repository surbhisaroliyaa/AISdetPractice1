package tests.api;

import base.BaseTest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import config.ConfigReader;
import org.testng.annotations.*;

import pages.SignupLoginPage;
import utils.TestDataGenerator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.*;

/**
 * Integration test: UI setup → UI test → API cleanup
 *
 * Pattern:
 * - Setup: Create user via UI signup in @BeforeClass (proven to work in CI)
 * - Step 1: Login via UI (realistic — tests the actual login flow)
 * - Step 2: Delete user via API (reliable cleanup — no UI flakiness)
 * - Step 3: Verify deletion via API
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

        // Create user via UI in a separate context (same pattern as LoginTest)
        // Must be in @BeforeClass — creating in @Test causes login failures in CI
        createTestUser();
    }

    private void createTestUser() {
        long startTime = System.currentTimeMillis();

        BrowserContext setupContext = getBrowser().newContext();
        setupContext.setDefaultTimeout(30000);
        com.microsoft.playwright.Page setupPage = setupContext.newPage();

        // Block ads in setup context
        setupPage.route("**/*", route -> {
            String url = route.request().url().toLowerCase();
            if (url.contains("google") || url.contains("doubleclick")
                    || url.contains("adservice") || url.contains("googlesyndication")
                    || url.contains("analytics") || url.contains("adsbygoogle")
                    || url.contains("facebook") || url.contains("aswpsdkus")
                    || url.contains("onesignal") || url.contains("cdn.taboola")
                    || url.contains("pagead") || url.contains("adsense")) {
                route.abort();
            } else {
                route.resume();
            }
        });

        setupPage.navigate(ConfigReader.getBaseUrl() + "/login");
        setupPage.locator("[data-qa='signup-name']").fill(testName);
        setupPage.locator("[data-qa='signup-email']").fill(testEmail);
        setupPage.locator("[data-qa='signup-button']").click();

        // Fill registration form
        setupPage.locator("#id_gender1").check();
        setupPage.locator("[data-qa='password']").fill(testPassword);
        setupPage.locator("[data-qa='days']").selectOption("10");
        setupPage.locator("[data-qa='months']").selectOption("3");
        setupPage.locator("[data-qa='years']").selectOption("1995");
        setupPage.locator("[data-qa='first_name']").fill(testName);
        setupPage.locator("[data-qa='last_name']").fill(testLastName);
        setupPage.locator("[data-qa='company']").fill(TestDataGenerator.getRandomCompany());
        setupPage.locator("[data-qa='address']").fill(TestDataGenerator.getRandomAddress());
        setupPage.locator("[data-qa='country']").selectOption("India");
        setupPage.locator("[data-qa='state']").fill(TestDataGenerator.getRandomState());
        setupPage.locator("[data-qa='city']").fill(TestDataGenerator.getRandomCity());
        setupPage.locator("[data-qa='zipcode']").fill(TestDataGenerator.getRandomZipcode());
        setupPage.locator("[data-qa='mobile_number']").fill(TestDataGenerator.getRandomPhone());
        setupPage.locator("[data-qa='create-account']").click();

        // Wait for confirmation
        setupPage.locator("h2:has-text('Account Created')").waitFor();
        setupPage.locator("[data-qa='continue-button']").click();

        try {
            setupContext.close();
        } catch (Exception e) {
            // Context may already be closed if browser was recycled in parallel execution
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("UI user creation took: " + duration + "ms");
    }

    @AfterClass
    @Override
    public void closeBrowser() {
        api.dispose();
        super.closeBrowser();
    }

    @Test(description = "Step 1: Login via UI and verify logged-in state", priority = 1)
    public void step1_LoginViaUiAndVerify() {
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

    @Test(description = "Step 2: Delete user via API (reliable cleanup)",
            priority = 2, alwaysRun = true)
    public void step2_DeleteUserViaApi() {
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

    @Test(description = "Step 3: Verify deletion — user cannot login",
            priority = 3, dependsOnMethods = "step2_DeleteUserViaApi")
    public void step3_VerifyDeletion() {
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
