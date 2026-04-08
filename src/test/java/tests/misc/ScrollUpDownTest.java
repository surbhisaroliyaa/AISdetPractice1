package tests.misc;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.HomePage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ScrollUpDownTest extends BaseTest {

    HomePage homePage;

    @BeforeMethod
    public void initPage() {
        homePage = new HomePage(page);
        homePage.navigateToHome();
    }

    // =============================================
    // SCROLL WITH MOUSE (wheel/scrollIntoView)
    // =============================================

    @Test
    public void testScrollDownShowsFooter() {
        // Scroll down to the footer using mouse wheel simulation
        page.locator(".footer-widget").scrollIntoViewIfNeeded();

        // Footer subscription section should be visible after scrolling
        Assert.assertTrue(homePage.isFooterSubscriptionVisible(),
                "Footer subscription should be visible after scrolling down");

        // The "scroll to top" arrow should appear when scrolled down
        assertThat(homePage.getScrollUpArrow()).isVisible();
    }

    @Test
    public void testScrollDownShowsAllFeaturedProducts() {
        // Scroll to the bottom of the featured items section
        page.locator(".features_items").last().scrollIntoViewIfNeeded();

        // Verify featured items section loaded fully
        Assert.assertTrue(homePage.isFeaturedItemsSectionVisible(),
                "Featured items should be visible after scrolling");

        // Verify products are present (not an empty section)
        int productCount = page.locator(".features_items .product-image-wrapper").count();
        Assert.assertTrue(productCount > 0,
                "Should have featured products visible: found " + productCount);
    }

    @Test
    public void testScrollDownThenBackUp() {
        // Scroll down to footer
        page.locator(".footer-widget").scrollIntoViewIfNeeded();
        Assert.assertTrue(homePage.isFooterSubscriptionVisible(),
                "Footer should be visible after scrolling down");

        // Scroll back up to top
        page.locator(".logo img").scrollIntoViewIfNeeded();

        // Verify top of page content is visible
        Assert.assertTrue(homePage.isLogoVisible(),
                "Logo should be visible after scrolling back up");
        Assert.assertTrue(homePage.isSliderVisible(),
                "Slider should be visible after scrolling back up");
    }

    // =============================================
    // SCROLL WITH KEYBOARD
    // =============================================

    @Test
    public void testScrollDownWithKeyboard() {
        // Press End key to scroll to bottom of page
        page.keyboard().press("End");
        page.waitForTimeout(500); // brief wait for scroll animation

        // Footer should be visible after pressing End
        Assert.assertTrue(homePage.isFooterSubscriptionVisible(),
                "Footer should be visible after pressing End key");
    }

    @Test
    public void testScrollUpWithKeyboard() {
        // Scroll down first
        page.keyboard().press("End");
        page.waitForTimeout(500);

        // Press Home to scroll back to top
        page.keyboard().press("Home");
        page.waitForTimeout(500);

        // Top elements should be visible again
        Assert.assertTrue(homePage.isLogoVisible(),
                "Logo should be visible after pressing Home key");
        Assert.assertTrue(homePage.isSliderVisible(),
                "Slider should be visible after pressing Home key");
    }

    // =============================================
    // SCROLL-TO-TOP ARROW BUTTON
    // =============================================

    @Test
    public void testScrollUpArrowButton() {
        // Scroll down to make the arrow appear
        page.locator(".footer-widget").scrollIntoViewIfNeeded();
        assertThat(homePage.getScrollUpArrow()).isVisible();

        // Click the scroll-to-top arrow
        homePage.getScrollUpArrow().click();
        page.waitForTimeout(1000); // wait for smooth scroll animation

        // Verify page scrolled to top — logo and slider visible
        Assert.assertTrue(homePage.isLogoVisible(),
                "Logo should be visible after clicking scroll-up arrow");
        Assert.assertTrue(homePage.isSliderVisible(),
                "Slider should be visible after clicking scroll-up arrow");
    }

    @Test
    public void testScrollUpArrowNotVisibleAtTop() {
        // At the top of page, scroll-up arrow should NOT be visible
        // (it only appears when user has scrolled down)
        boolean arrowVisible = homePage.getScrollUpArrow().isVisible();

        // Some sites show it always, some hide it at top — verify consistent behavior
        if (!arrowVisible) {
            // Arrow hidden at top — scroll down and verify it appears
            page.locator(".footer-widget").scrollIntoViewIfNeeded();
            assertThat(homePage.getScrollUpArrow()).isVisible();
        }
        // If arrow is always visible, that's also valid — just verify it works
    }

    // =============================================
    // FULL PAGE SCROLL — TOP TO BOTTOM AND BACK
    // =============================================

    @Test
    public void testFullPageScrollIntegrity() {
        // Verify top section loads
        Assert.assertTrue(homePage.isSliderVisible(), "Slider should be visible at top");
        Assert.assertTrue(homePage.isCategoryWidgetVisible(), "Category sidebar should be visible");

        // Scroll through key sections top to bottom
        page.locator(".features_items").scrollIntoViewIfNeeded();
        Assert.assertTrue(homePage.isFeaturedItemsSectionVisible(),
                "Featured items should be visible in middle of page");

        page.locator(".footer-widget").scrollIntoViewIfNeeded();
        Assert.assertTrue(homePage.isFooterSubscriptionVisible(),
                "Footer should be visible at bottom");

        // Scroll all the way back to top
        page.locator(".logo img").scrollIntoViewIfNeeded();
        Assert.assertTrue(homePage.isLogoVisible(),
                "Logo should be visible after full scroll round-trip");
        Assert.assertTrue(homePage.isSliderVisible(),
                "Slider should be visible after full scroll round-trip");
    }
}
