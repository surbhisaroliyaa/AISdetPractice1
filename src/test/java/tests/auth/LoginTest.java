package tests.auth;

import base.BaseTest;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AccountCreatedPage;
import pages.SignupLoginPage;
import pages.SignupPage;
import utils.TestDataGenerator;

import java.nio.file.Paths;

public class LoginTest extends BaseTest {

    private SignupLoginPage signupLoginPage;

    // Shared credentials — created once in @BeforeClass, used by all login tests
    private static String testEmail;
    private static String testPassword;
    private static String testName;

    @BeforeClass
    public void createTestAccount() {
        // Create a fresh account that all login tests will use
        testName = TestDataGenerator.getRandomFirstName();
        testEmail = TestDataGenerator.getRandomEmail();
        testPassword = TestDataGenerator.getRandomPassword();

        // Use a temporary context to register the account
        BrowserContext setupContext = getBrowser().newContext();
        setupContext.setDefaultTimeout(30000);
        com.microsoft.playwright.Page setupPage = setupContext.newPage();

        // Block ads in setup context too
        setupPage.route("**/*", route -> {
            String url = route.request().url();
            if (url.contains("google") || url.contains("doubleclick")
                    || url.contains("adservice") || url.contains("googlesyndication")
                    || url.contains("analytics")) {
                route.abort();
            } else {
                route.resume();
            }
        });

        setupPage.navigate(config.ConfigReader.getBaseUrl() + "/login");
        setupPage.locator("[data-qa='signup-name']").fill(testName);
        setupPage.locator("[data-qa='signup-email']").fill(testEmail);
        setupPage.locator("[data-qa='signup-button']").click();

        // Fill registration form
        setupPage.locator("#id_gender1").check();
        setupPage.locator("[data-qa='password']").fill(testPassword);
        setupPage.locator("[data-qa='days']").selectOption("15");
        setupPage.locator("[data-qa='months']").selectOption("6");
        setupPage.locator("[data-qa='years']").selectOption("1995");
        setupPage.locator("[data-qa='first_name']").fill(testName);
        setupPage.locator("[data-qa='last_name']").fill(TestDataGenerator.getRandomLastName());
        setupPage.locator("[data-qa='company']").fill(TestDataGenerator.getRandomCompany());
        setupPage.locator("[data-qa='address']").fill(TestDataGenerator.getRandomAddress());
        setupPage.locator("[data-qa='country']").selectOption("India");
        setupPage.locator("[data-qa='state']").fill(TestDataGenerator.getRandomState());
        setupPage.locator("[data-qa='city']").fill(TestDataGenerator.getRandomCity());
        setupPage.locator("[data-qa='zipcode']").fill(TestDataGenerator.getRandomZipcode());
        setupPage.locator("[data-qa='mobile_number']").fill(TestDataGenerator.getRandomPhone());
        setupPage.locator("[data-qa='create-account']").click();

        setupPage.locator("[data-qa='continue-button']").click();

        // Save storageState — this is the logged-in session
        setupContext.storageState(
                new BrowserContext.StorageStateOptions()
                        .setPath(Paths.get("auth-state.json"))
        );

        setupContext.close();
    }

    @BeforeMethod
    public void initPages() {
        signupLoginPage = new SignupLoginPage(page);
    }

    @Test
    public void testValidLogin() {
        signupLoginPage.navigateToLoginPage();
        Assert.assertTrue(signupLoginPage.isLoginFormVisible(), "Login form should be visible");

        signupLoginPage.login(testEmail, testPassword);

        // Verify: user is logged in and name is shown in navbar
        Assert.assertTrue(page.locator("a:has-text('Logged in as')").isVisible(),
                "User should be logged in after valid credentials");
    }

    @Test
    public void testInvalidPassword() {
        signupLoginPage.navigateToLoginPage();
        signupLoginPage.login(testEmail, "WrongPassword123!");

        Assert.assertTrue(signupLoginPage.isLoginErrorVisible(),
                "Error message should be visible for wrong password");
        Assert.assertEquals(signupLoginPage.getLoginErrorMessage(),
                "Your email or password is incorrect!",
                "Error should indicate invalid credentials");
    }

    @Test
    public void testInvalidEmail() {
        signupLoginPage.navigateToLoginPage();
        signupLoginPage.login("nonexistent_" + System.currentTimeMillis() + "@test.com",
                "AnyPassword123");

        Assert.assertTrue(signupLoginPage.isLoginErrorVisible(),
                "Error message should be visible for non-existent email");
        Assert.assertEquals(signupLoginPage.getLoginErrorMessage(),
                "Your email or password is incorrect!",
                "Error should indicate invalid credentials");
    }

    @Test
    public void testLogout() {
        // First login
        signupLoginPage.navigateToLoginPage();
        signupLoginPage.login(testEmail, testPassword);
        Assert.assertTrue(page.locator("a:has-text('Logged in as')").isVisible(),
                "User should be logged in");

        // Then logout
        page.locator(".navbar-nav a[href='/logout']").click();

        // Verify: back on login page, login form is visible
        Assert.assertTrue(signupLoginPage.isLoginFormVisible(),
                "Login form should be visible after logout");
        Assert.assertFalse(page.locator("a:has-text('Logged in as')").isVisible(),
                "Logged in indicator should NOT be visible after logout");
    }

    @Test
    public void testLoginWithEmptyCredentials() {
        signupLoginPage.navigateToLoginPage();

        // Click login without entering anything
        signupLoginPage.login("", "");

        // Browser's HTML5 validation should block submission — still on login page
        Assert.assertTrue(signupLoginPage.isLoginFormVisible(),
                "Should still be on login page — empty credentials should be blocked");
        Assert.assertFalse(page.locator("a:has-text('Logged in as')").isVisible(),
                "Should NOT be logged in with empty credentials");
    }

    @Test
    public void testLoginWithEmptyPassword() {
        signupLoginPage.navigateToLoginPage();

        // Valid email but empty password
        signupLoginPage.login(testEmail, "");

        // Browser validation should block — still on login page
        Assert.assertTrue(signupLoginPage.isLoginFormVisible(),
                "Should still be on login page — empty password should be blocked");
    }

    @Test
    public void testLoginWithEmptyEmail() {
        signupLoginPage.navigateToLoginPage();

        // Empty email but valid password
        signupLoginPage.login("", testPassword);

        // Browser validation should block — still on login page
        Assert.assertTrue(signupLoginPage.isLoginFormVisible(),
                "Should still be on login page — empty email should be blocked");
    }

    @Test
    public void testStorageStateLogin() {
        // Create a NEW context loaded with saved auth state — no login needed
        BrowserContext authContext = getBrowser().newContext(
                new Browser.NewContextOptions()
                        .setStorageStatePath(Paths.get("auth-state.json"))
        );
        com.microsoft.playwright.Page authPage = authContext.newPage();

        // Block ads
        authPage.route("**/*", route -> {
            String url = route.request().url();
            if (url.contains("google") || url.contains("doubleclick")
                    || url.contains("adservice") || url.contains("googlesyndication")
                    || url.contains("analytics")) {
                route.abort();
            } else {
                route.resume();
            }
        });

        // Navigate directly — should be logged in already
        authPage.navigate(config.ConfigReader.getBaseUrl());
        Assert.assertTrue(authPage.locator("a:has-text('Logged in as')").isVisible(),
                "User should be automatically logged in via storageState");

        authContext.close();
    }
}
