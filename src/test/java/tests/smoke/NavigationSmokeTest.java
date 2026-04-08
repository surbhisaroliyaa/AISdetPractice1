package tests.smoke;

import base.BaseTest;
import config.ConfigReader;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.HomePage;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.testng.Assert.assertTrue;

public class NavigationSmokeTest extends BaseTest {

    HomePage homePage;

    @BeforeMethod
    public void initPage() {
        homePage = new HomePage(page);
        homePage.navigateToHome();
    }

    @Test
    public void verifyHomePageLoads() {
        assertThat(page).hasTitle("Automation Exercise");
    }

    @Test
    public void verifySliderIsVisible() {
        assertTrue(homePage.isSliderVisible(), "Slider carousel should be visible on home page");
    }

    @Test
    public void verifyFeaturedItemsVisible() {
        assertTrue(homePage.isFeaturedItemsSectionVisible(), "Featured items section should be visible");
    }

    @Test
    public void verifyCategorySidebarVisible() {
        assertTrue(homePage.isCategoryWidgetVisible(), "Category sidebar should be visible");
    }

    @Test
    public void verifyFooterSubscriptionVisible() {
        assertTrue(homePage.isFooterSubscriptionVisible(), "Footer subscription section should be visible");
    }

    @Test
    public void verifyProductsNavigation() {
        homePage.clickProducts();
        assertThat(page).hasURL(Pattern.compile(".*/products"));
    }

    @Test
    public void verifyCartNavigation() {
        homePage.clickCart();
        assertThat(page).hasURL(Pattern.compile(".*/view_cart"));
    }

    @Test
    public void verifySignupLoginNavigation() {
        homePage.clickSignupLogin();
        assertThat(page).hasURL(Pattern.compile(".*/login"));
    }

    @Test
    public void verifyContactUsNavigation() {
        homePage.clickContactUs();
        assertThat(page).hasURL(Pattern.compile(".*/contact_us"));
    }

    @Test
    public void verifyTestCasesNavigation() {
        homePage.clickTestCases();
        assertThat(page).hasURL(Pattern.compile(".*/test_cases"));
    }

    @Test
    public void verifyApiTestingNavigation() {
        homePage.clickApiTesting();
        assertThat(page).hasURL(Pattern.compile(".*/api_list"));
    }
}
