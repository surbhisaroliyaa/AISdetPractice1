package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import config.ConfigReader;

public class SignupLoginPage {
    private final Page page;

    // Login form locators
    private final Locator loginEmailInput;
    private final Locator loginPasswordInput;
    private final Locator loginButton;
    private final Locator loginErrorMessage;

    // Signup form locators
    private final Locator signupNameInput;
    private final Locator signupEmailInput;
    private final Locator signupButton;
    private final Locator signupErrorMessage;

    // Headings
    private final Locator loginHeading;
    private final Locator signupHeading;

    public SignupLoginPage(Page page) {
        this.page = page;

        // Login form
        loginEmailInput = page.locator("[data-qa='login-email']");
        loginPasswordInput = page.locator("[data-qa='login-password']");
        loginButton = page.locator("[data-qa='login-button']");
        loginErrorMessage = page.locator(".login-form p[style='color: red;']");

        // Signup form
        signupNameInput = page.locator("[data-qa='signup-name']");
        signupEmailInput = page.locator("[data-qa='signup-email']");
        signupButton = page.locator("[data-qa='signup-button']");
        signupErrorMessage = page.locator(".signup-form p[style='color: red;']");

        // Headings
        loginHeading = page.locator("h2:has-text('Login to your account')");
        signupHeading = page.locator("h2:has-text('New User Signup!')");
    }

    public void navigateToLoginPage() {
        page.navigate(ConfigReader.getBaseUrl() + "/login");
    }

    // Login actions
    public void login(String email, String password) {
        loginEmailInput.fill(email);
        loginPasswordInput.fill(password);
        loginButton.click();
    }

    // Signup actions (just name + email — takes you to full registration form)
    public void enterSignupDetails(String name, String email) {
        signupNameInput.fill(name);
        signupEmailInput.fill(email);
        signupButton.click();
    }

    // Verifications
    public boolean isLoginFormVisible() {
        return loginHeading.isVisible();
    }

    public boolean isSignupFormVisible() {
        return signupHeading.isVisible();
    }

    public String getLoginErrorMessage() {
        return loginErrorMessage.innerText();
    }

    public String getSignupErrorMessage() {
        return signupErrorMessage.innerText();
    }

    public boolean isLoginErrorVisible() {
        return loginErrorMessage.isVisible();
    }

    public boolean isSignupErrorVisible() {
        return signupErrorMessage.isVisible();
    }
}
