package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import config.ConfigReader;

public class HomePage {
    private final Page page;

    // Subscription locators
    private final Locator subscribeEmailInput;
    private final Locator subscribeButton;
    private final Locator subscribeSuccessMessage;

    public HomePage(Page page) {
        this.page = page;
        subscribeEmailInput = page.locator("#susbscribe_email");
        subscribeButton = page.locator("#subscribe");
        subscribeSuccessMessage = page.locator("#success-subscribe .alert-success");
    }

    // Navigation
    public void navigateToHome() {
        page.navigate(ConfigReader.getBaseUrl());
    }

    // All nav clicks scoped to header navbar to avoid duplicate links in footer
    private Locator navLink(String href) {
        return page.locator(".navbar-nav a[href='" + href + "']");
    }

    public void clickProducts() {
        navLink("/products").click();
    }

    public void clickCart() {
        navLink("/view_cart").click();
    }

    public void clickSignupLogin() {
        navLink("/login").click();
    }

    public void clickTestCases() {
        navLink("/test_cases").click();
    }

    public void clickApiTesting() {
        navLink("/api_list").click();
    }

    public void clickContactUs() {
        navLink("/contact_us").click();
    }

    public void enterSubscriptionEmail(String email) {
        subscribeEmailInput.scrollIntoViewIfNeeded();
        subscribeEmailInput.fill(email);
    }

    public void clickSubscribe() {
        subscribeButton.click();
    }

    public void subscribe(String email) {
        enterSubscriptionEmail(email);
        clickSubscribe();
    }

    public String getSubscribeSuccessMessage() {
        return subscribeSuccessMessage.innerText();
    }

    public boolean isSubscribeSuccessVisible() {
        return subscribeSuccessMessage.isVisible();
    }

    // Scroll
    public Locator getScrollUpArrow() {
        return page.locator("#scrollUp");
    }

    public boolean isLogoVisible() {
        return page.locator(".logo img").isVisible();
    }

    // Verifications
    public boolean isSliderVisible() {
        return page.locator("#slider-carousel").isVisible();
    }

    public boolean isFooterSubscriptionVisible() {
        return page.locator(".footer-widget .single-widget").first().isVisible();
    }

    public boolean isCategoryWidgetVisible() {
        return page.locator(".left-sidebar .category-products").isVisible();
    }

    public boolean isFeaturedItemsSectionVisible() {
        return page.locator(".features_items").isVisible();
    }

    public Locator getNavLinks() {
        return page.locator(".navbar-nav li a");
    }

    public String getPageTitle() {
        return page.title();
    }
}
