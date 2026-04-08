package tests.auth;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AccountCreatedPage;
import pages.SignupLoginPage;
import pages.SignupPage;
import utils.TestDataGenerator;

public class RegisterTest extends BaseTest {

    private SignupLoginPage signupLoginPage;
    private SignupPage signupPage;
    private AccountCreatedPage accountCreatedPage;

    @BeforeMethod
    public void initPages() {
        signupLoginPage = new SignupLoginPage(page);
        signupPage = new SignupPage(page);
        accountCreatedPage = new AccountCreatedPage(page);
    }

    @Test
    public void testValidRegistration() {
        String name = TestDataGenerator.getRandomFirstName();
        String email = TestDataGenerator.getRandomEmail();
        String password = TestDataGenerator.getRandomPassword();

        // Step 1: Go to login page and enter signup details
        signupLoginPage.navigateToLoginPage();
        Assert.assertTrue(signupLoginPage.isSignupFormVisible(), "Signup form should be visible");
        signupLoginPage.enterSignupDetails(name, email);

        // Step 2: Fill the full registration form
        Assert.assertTrue(signupPage.isAccountInfoVisible(), "Account info form should be visible");
        signupPage.fillFullRegistration(
                password, "15", "6", "1995",
                TestDataGenerator.getRandomFirstName(),
                TestDataGenerator.getRandomLastName(),
                TestDataGenerator.getRandomCompany(),
                TestDataGenerator.getRandomAddress(),
                "India",
                TestDataGenerator.getRandomState(),
                TestDataGenerator.getRandomCity(),
                TestDataGenerator.getRandomZipcode(),
                TestDataGenerator.getRandomPhone()
        );

        // Step 3: Verify account created
        Assert.assertTrue(accountCreatedPage.isAccountCreatedVisible(),
                "Account Created message should be visible");
        accountCreatedPage.clickContinue();

        // Step 4: Verify logged in on home page
        Assert.assertTrue(page.locator("a:has-text('Logged in as')").isVisible(),
                "User should be logged in after registration");

        // Step 5: Clean up — delete the account to avoid data pile-up
        page.locator(".navbar-nav a[href='/delete_account']").click();
        Assert.assertTrue(page.locator("[data-qa='account-deleted']").isVisible(),
                "Account should be deleted");
        page.locator("[data-qa='continue-button']").click();
    }

    @Test
    public void testDuplicateEmailRegistration() {
        String name = TestDataGenerator.getRandomFirstName();
        String email = TestDataGenerator.getRandomEmail();
        String password = TestDataGenerator.getRandomPassword();

        // First registration — create the account
        signupLoginPage.navigateToLoginPage();
        signupLoginPage.enterSignupDetails(name, email);
        signupPage.fillFullRegistration(
                password, "10", "3", "1990",
                TestDataGenerator.getRandomFirstName(),
                TestDataGenerator.getRandomLastName(),
                TestDataGenerator.getRandomCompany(),
                TestDataGenerator.getRandomAddress(),
                "India",
                TestDataGenerator.getRandomState(),
                TestDataGenerator.getRandomCity(),
                TestDataGenerator.getRandomZipcode(),
                TestDataGenerator.getRandomPhone()
        );
        accountCreatedPage.clickContinue();

        // Logout so we can try registering again with same email
        page.locator(".navbar-nav a[href='/logout']").click();

        // Second registration — same email should fail
        signupLoginPage.enterSignupDetails(name, email);

        Assert.assertTrue(signupLoginPage.isSignupErrorVisible(),
                "Error message should be visible for duplicate email");
        Assert.assertEquals(signupLoginPage.getSignupErrorMessage(),
                "Email Address already exist!",
                "Error should indicate email already exists");

        // Clean up — login and delete the account
        signupLoginPage.login(email, password);
        page.locator(".navbar-nav a[href='/delete_account']").click();
        page.locator("[data-qa='continue-button']").click();
    }

    @Test
    public void testRequiredFieldsValidation() {
        String name = TestDataGenerator.getRandomFirstName();
        String email = TestDataGenerator.getRandomEmail();

        // Go to signup page with valid name + email (required to reach registration form)
        signupLoginPage.navigateToLoginPage();
        signupLoginPage.enterSignupDetails(name, email);
        Assert.assertTrue(signupPage.isAccountInfoVisible(), "Registration form should be visible");

        // Leave all fields empty and click Create Account
        // Don't fill password, address, city, state, zipcode, phone — all required
        signupPage.clickCreateAccount();

        // Browser's built-in HTML5 validation should prevent submission
        // We verify we're still on the signup page (form was NOT submitted)
        Assert.assertTrue(signupPage.isAccountInfoVisible(),
                "Should still be on registration form — browser validation should block submission");

        // Verify the page did NOT navigate to account_created
        Assert.assertFalse(page.url().contains("account_created"),
                "Should NOT reach account created page with empty required fields");
    }

    @Test
    public void testSignupWithEmptyName() {
        signupLoginPage.navigateToLoginPage();

        // Try signup with empty name but valid email
        signupLoginPage.enterSignupDetails("", TestDataGenerator.getRandomEmail());

        // Browser validation should block — still on login page
        Assert.assertTrue(signupLoginPage.isSignupFormVisible(),
                "Should still be on login page — empty name should be blocked");
    }

    @Test
    public void testSignupWithEmptyEmail() {
        signupLoginPage.navigateToLoginPage();

        // Try signup with valid name but empty email
        signupLoginPage.enterSignupDetails(TestDataGenerator.getRandomFirstName(), "");

        // Browser validation should block — still on login page
        Assert.assertTrue(signupLoginPage.isSignupFormVisible(),
                "Should still be on login page — empty email should be blocked");
    }

    @Test
    public void testRegistrationWithEvilData() {
        // Test with XSS payload in name field — site should not execute script
        String evilName = "<script>alert('xss')</script>";
        String email = TestDataGenerator.getRandomEmail();
        String password = TestDataGenerator.getRandomPassword();

        signupLoginPage.navigateToLoginPage();
        signupLoginPage.enterSignupDetails(evilName, email);

        // If we reach the signup form, the site didn't crash — that's good
        // Fill rest of form with normal data
        signupPage.fillFullRegistration(
                password, "1", "1", "2000",
                "<img src=x onerror=alert('xss')>",  // XSS in first name
                "O'Brien",                              // SQL-like special char in last name
                "Company & Co. \"Ltd\"",                // Special chars in company
                "123 Test St; DROP TABLE users;--",     // SQL injection in address
                "India",
                TestDataGenerator.getRandomState(),
                TestDataGenerator.getRandomCity(),
                TestDataGenerator.getRandomZipcode(),
                TestDataGenerator.getRandomPhone()
        );

        // Verify: site should either create the account (escaping the data)
        // or show a validation error — but NOT crash or execute scripts
        boolean accountCreated = accountCreatedPage.isAccountCreatedVisible();
        Assert.assertTrue(accountCreated,
                "Account should be created even with special characters (site should escape them)");

        // Clean up
        accountCreatedPage.clickContinue();
        page.locator(".navbar-nav a[href='/delete_account']").click();
        page.locator("[data-qa='continue-button']").click();
    }
}
