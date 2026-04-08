package tests.misc;

import base.BaseTest;
import config.ConfigReader;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.HomePage;
import utils.TestDataGenerator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class SubscriptionTest extends BaseTest {

    HomePage homePage;

    @BeforeMethod
    public void initPage() {
        homePage = new HomePage(page);
    }

    // =============================================
    // VALID SUBSCRIPTION
    // =============================================

    @Test
    public void testValidEmailSubscription() {
        homePage.navigateToHome();
        homePage.subscribe(TestDataGenerator.getRandomEmail());

        Assert.assertTrue(homePage.isSubscribeSuccessVisible(),
                "Success message should appear after valid subscription");
        Assert.assertTrue(homePage.getSubscribeSuccessMessage().contains("You have been successfully subscribed!"),
                "Success message text should confirm subscription");
    }

    // =============================================
    // NEGATIVE / EDGE CASES
    // =============================================

    @Test
    public void testEmptyEmailSubscription() {
        homePage.navigateToHome();

        // HTML5 validation should prevent submission with empty email
        homePage.enterSubscriptionEmail("");
        homePage.clickSubscribe();

        // The input has type="email" — browser blocks submission and shows validation popup
        // Verify we're still on the same page and no success message appeared
        String validationMessage = (String) page.locator("#susbscribe_email")
                .evaluate("el => el.validationMessage");
        Assert.assertFalse(validationMessage.isEmpty(),
                "Browser should show validation error for empty email");
        Assert.assertFalse(homePage.isSubscribeSuccessVisible(),
                "Success message should NOT appear for empty email");
    }

    @Test
    public void testInvalidEmailFormatSubscription() {
        homePage.navigateToHome();
        homePage.enterSubscriptionEmail("not-an-email");
        homePage.clickSubscribe();

        // HTML5 type="email" validation blocks invalid formats
        String validationMessage = (String) page.locator("#susbscribe_email")
                .evaluate("el => el.validationMessage");
        Assert.assertFalse(validationMessage.isEmpty(),
                "Browser should show validation error for invalid email format");
        Assert.assertFalse(homePage.isSubscribeSuccessVisible(),
                "Success message should NOT appear for invalid email");
    }

    // =============================================
    // CROSS-PAGE SUBSCRIPTION
    // =============================================

    @Test
    public void testSubscriptionFromProductsPage() {
        page.navigate(ConfigReader.getBaseUrl() + "/products");
        homePage.subscribe(TestDataGenerator.getRandomEmail());

        Assert.assertTrue(homePage.isSubscribeSuccessVisible(),
                "Subscription should work from Products page");
        Assert.assertTrue(homePage.getSubscribeSuccessMessage().contains("You have been successfully subscribed!"),
                "Success message should appear on Products page");
    }

    @Test
    public void testSubscriptionFromCartPage() {
        page.navigate(ConfigReader.getBaseUrl() + "/view_cart");
        homePage.subscribe(TestDataGenerator.getRandomEmail());

        Assert.assertTrue(homePage.isSubscribeSuccessVisible(),
                "Subscription should work from Cart page");
        Assert.assertTrue(homePage.getSubscribeSuccessMessage().contains("You have been successfully subscribed!"),
                "Success message should appear on Cart page");
    }

    // =============================================
    // LOGGED-IN STATE
    // =============================================

    @Test
    public void testSubscriptionWhileLoggedIn() {
        // Create account and login using a temporary flow
        String email = TestDataGenerator.getRandomEmail();
        String password = TestDataGenerator.getRandomPassword();
        String name = TestDataGenerator.getRandomFirstName();

        // Register via signup
        page.navigate(ConfigReader.getBaseUrl() + "/login");
        page.locator("[data-qa='signup-name']").fill(name);
        page.locator("[data-qa='signup-email']").fill(email);
        page.locator("[data-qa='signup-button']").click();

        page.locator("#id_gender1").check();
        page.locator("[data-qa='password']").fill(password);
        page.locator("[data-qa='days']").selectOption("10");
        page.locator("[data-qa='months']").selectOption("5");
        page.locator("[data-qa='years']").selectOption("1995");
        page.locator("[data-qa='first_name']").fill(name);
        page.locator("[data-qa='last_name']").fill(TestDataGenerator.getRandomLastName());
        page.locator("[data-qa='company']").fill(TestDataGenerator.getRandomCompany());
        page.locator("[data-qa='address']").fill(TestDataGenerator.getRandomAddress());
        page.locator("[data-qa='country']").selectOption("India");
        page.locator("[data-qa='state']").fill(TestDataGenerator.getRandomState());
        page.locator("[data-qa='city']").fill(TestDataGenerator.getRandomCity());
        page.locator("[data-qa='zipcode']").fill(TestDataGenerator.getRandomZipcode());
        page.locator("[data-qa='mobile_number']").fill(TestDataGenerator.getRandomPhone());
        page.locator("[data-qa='create-account']").click();
        page.locator("[data-qa='continue-button']").click();

        // Now logged in — verify subscription still works
        assertThat(page.locator("a:has-text('Logged in as')")).isVisible();
        homePage.subscribe(TestDataGenerator.getRandomEmail());

        Assert.assertTrue(homePage.isSubscribeSuccessVisible(),
                "Subscription should work while logged in");
        Assert.assertTrue(homePage.getSubscribeSuccessMessage().contains("You have been successfully subscribed!"),
                "Success message should appear while logged in");
    }
}
