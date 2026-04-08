package tests.cart;

import base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import config.ConfigReader;
import pages.CartPage;
import pages.ProductDetailPage;
import pages.ProductsPage;
import pages.SignupLoginPage;
import utils.TestDataGenerator;
import org.testng.Assert;
import org.testng.annotations.*;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class CartTest extends BaseTest {

    private CartPage cartPage;
    private ProductsPage productsPage;

    @BeforeMethod
    public void initPages() {
        cartPage = new CartPage(page);
        productsPage = new ProductsPage(page);
    }

    // ========== Helper Methods ==========

    private void goToProducts() {
        productsPage.navigateToProductsPage();
    }

    private void addProductAndContinue(int index) {
        cartPage.hoverAndAddToCart(index);
        cartPage.clickContinueShopping();
    }

    // ========== Test 1: State-by-State Cart Verification ==========

    @Test(priority = 1)
    public void testCartStateByState_AddAndRemoveProducts() {
        goToProducts();

        // State 1: Add first product, verify cart has 1 item
        addProductAndContinue(0);
        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Cart should have 1 item after adding first product");
        String firstProductName = cartPage.getProductNameByIndex(0);
        Assert.assertFalse(firstProductName.isEmpty(), "Product name should not be empty");

        // State 2: Go back to products, add second product, verify cart has 2 items
        goToProducts();
        addProductAndContinue(1);
        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Cart should have 2 items after adding second product");

        // State 3: Remove first product, verify cart has 1 item
        cartPage.removeProductByIndex(0);
        page.waitForTimeout(1000);
        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Cart should have 1 item after removing one");

        // State 4: Remove remaining product, verify cart is empty
        cartPage.removeProductByIndex(0);
        page.waitForTimeout(1000);
        Assert.assertTrue(cartPage.isCartEmpty(), "Cart should be empty after removing all products");
    }

    // ========== Test 2: Hover and Add to Cart ==========

    @Test(priority = 2)
    public void testHoverAndAddToCart() {
        goToProducts();

        // Hover over first product — overlay should appear, click add to cart
        Locator firstProduct = page.locator(".features_items .col-sm-4").nth(0);
        Locator overlay = firstProduct.locator(".product-overlay");
        Locator addToCartBtn = overlay.locator(".add-to-cart");

        firstProduct.hover();
        overlay.waitFor();
        Assert.assertTrue(overlay.isVisible(), "Product overlay should appear on hover");

        addToCartBtn.click();
        Assert.assertTrue(cartPage.isCartModalVisible(), "Cart confirmation modal should appear");

        cartPage.clickContinueShopping();

        // Verify product was added
        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Cart should have the hovered product");
    }

    // ========== Test 3: Modal — Continue Shopping vs View Cart ==========

    @Test(priority = 3)
    public void testModalContinueShoppingAndViewCart() {
        goToProducts();

        // Product 1: Click "Continue Shopping" — should stay on products page
        cartPage.hoverAndAddToCart(0);
        Assert.assertTrue(cartPage.isCartModalVisible(), "Modal should appear after adding product");
        cartPage.clickContinueShopping();
        assertThat(page).hasURL(java.util.regex.Pattern.compile(".*/products"));

        // Product 2: Click "View Cart" — should navigate to cart page
        cartPage.hoverAndAddToCart(1);
        Assert.assertTrue(cartPage.isCartModalVisible(), "Modal should appear after adding second product");
        cartPage.clickViewCartInModal();
        assertThat(page).hasURL(java.util.regex.Pattern.compile(".*/view_cart"));

        // Both products should be in the cart
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Cart should have both products");
    }

    // ========== Test 4: Price * Quantity = Total Verification ==========

    @Test(priority = 4)
    public void testPriceTimesQuantityEqualsTotal() {
        goToProducts();
        addProductAndContinue(0);
        addProductAndContinue(1);

        cartPage.navigateToCart();

        List<Locator> rows = cartPage.getCartRows();
        Assert.assertTrue(rows.size() >= 2, "Should have at least 2 products in cart");

        for (int i = 0; i < rows.size(); i++) {
            Locator row = rows.get(i);
            Assert.assertTrue(cartPage.isPriceCalculationCorrect(row),
                    "Price * Quantity should equal Total for product at index " + i
                            + " | Price: " + cartPage.getProductPrice(row)
                            + " | Qty: " + cartPage.getProductQuantity(row)
                            + " | Total: " + cartPage.getProductTotal(row));
        }
    }

    // ========== Test 5: Set Quantity on Detail Page, Verify in Cart ==========

    @Test(priority = 5)
    public void testSetQuantityOnDetailPageAndVerifyCart() {
        goToProducts();
        productsPage.clickViewProduct(0);

        ProductDetailPage detailPage = new ProductDetailPage(page);
        detailPage.setQuantity("4");
        detailPage.clickAddToCart();

        Assert.assertTrue(detailPage.isCartConfirmationVisible(), "Confirmation modal should appear");
        detailPage.clickContinueShopping();

        cartPage.navigateToCart();

        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Cart should have 1 product");
        Assert.assertEquals(cartPage.getProductQuantityByIndex(0).trim(), "4",
                "Cart quantity should match what was set on detail page");

        // Verify total = price * 4
        Assert.assertTrue(cartPage.isPriceCalculationCorrect(cartPage.getCartRows().get(0)),
                "Total should be price * 4");
    }

    // ========== Test 6: Cart Retained After Login ==========

    @Test(priority = 6)
    public void testCartRetainedAfterLogin() {
        // First register a new account
        String email = TestDataGenerator.getRandomEmail();
        String password = TestDataGenerator.getRandomPassword();
        String firstName = TestDataGenerator.getRandomFirstName();

        // Register
        page.navigate(ConfigReader.getBaseUrl() + "/login");
        SignupLoginPage loginPage = new SignupLoginPage(page);
        loginPage.enterSignupDetails(firstName, email);

        // Fill minimal registration
        page.locator("#id_gender1").click();
        page.locator("[data-qa='password']").fill(password);
        page.locator("[data-qa='first_name']").fill(firstName);
        page.locator("[data-qa='last_name']").fill(TestDataGenerator.getRandomLastName());
        page.locator("[data-qa='address']").fill(TestDataGenerator.getRandomAddress());
        page.locator("[data-qa='country']").selectOption("India");
        page.locator("[data-qa='state']").fill(TestDataGenerator.getRandomState());
        page.locator("[data-qa='city']").fill(TestDataGenerator.getRandomCity());
        page.locator("[data-qa='zipcode']").fill(TestDataGenerator.getRandomZipcode());
        page.locator("[data-qa='mobile_number']").fill(TestDataGenerator.getRandomPhone());
        page.locator("[data-qa='create-account']").click();
        page.locator("[data-qa='continue-button']").click();

        // Now logout
        page.locator(".navbar-nav a[href='/logout']").click();

        // Add products to cart while logged out
        goToProducts();
        addProductAndContinue(0);
        addProductAndContinue(1);

        // Note the products added
        cartPage.navigateToCart();
        int itemsBefore = cartPage.getCartItemCount();
        Assert.assertEquals(itemsBefore, 2, "Should have 2 items before login");

        // Now login
        page.navigate(ConfigReader.getBaseUrl() + "/login");
        loginPage = new SignupLoginPage(page);
        loginPage.login(email, password);

        // Go to cart — items should still be there
        cartPage.navigateToCart();
        int itemsAfter = cartPage.getCartItemCount();
        Assert.assertEquals(itemsAfter, 2, "Cart should retain items after login");

        // Cleanup: delete account
        page.navigate(ConfigReader.getBaseUrl() + "/delete_account");
    }

    // ========== Test 7: Add Products from Two Different Pages ==========

    @Test(priority = 7)
    public void testAddProductsFromDifferentPages() {
        // Add product from Products page
        goToProducts();
        String productFromList = productsPage.getFirstProductNameFromList();
        addProductAndContinue(0);

        // Add product from Product Detail page
        productsPage.clickViewProduct(1);
        ProductDetailPage detailPage = new ProductDetailPage(page);
        String productFromDetail = detailPage.getProductName();
        detailPage.clickAddToCart();
        detailPage.clickContinueShopping();

        // Navigate to cart — both should be present
        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Cart should have products from both pages");

        // Verify both product names are in the cart
        String cartProduct1 = cartPage.getProductNameByIndex(0);
        String cartProduct2 = cartPage.getProductNameByIndex(1);
        Assert.assertTrue(
                (cartProduct1.equals(productFromList) || cartProduct2.equals(productFromList)),
                "Product added from list page should be in cart");
        Assert.assertTrue(
                (cartProduct1.equals(productFromDetail) || cartProduct2.equals(productFromDetail)),
                "Product added from detail page should be in cart");
    }

    // ========== Test 8: Add Same Product Twice ==========

    @Test(priority = 8)
    public void testAddSameProductTwice() {
        goToProducts();

        // Add same product (index 0) twice
        addProductAndContinue(0);
        addProductAndContinue(0);

        cartPage.navigateToCart();

        // The same product added twice should show quantity 2 (or 2 rows depending on site behavior)
        // automationexercise.com shows quantity 2 for the same product
        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Same product added twice should be 1 row");
        Assert.assertEquals(cartPage.getProductQuantityByIndex(0).trim(), "2",
                "Quantity should be 2 when same product added twice");
    }

    // ========== Test 9: Quantity Change Verification ==========

    @Test(priority = 9)
    public void testQuantityChangeOnDetailPageAndVerifyTotal() {
        // Add product from detail page with quantity 3
        goToProducts();
        productsPage.clickViewProduct(0);

        ProductDetailPage detailPage = new ProductDetailPage(page);
        detailPage.setQuantity("3");
        detailPage.clickAddToCart();
        detailPage.clickContinueShopping();

        // Navigate to cart and verify
        cartPage.navigateToCart();

        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Cart should have 1 product");
        Assert.assertEquals(cartPage.getProductQuantityByIndex(0).trim(), "3",
                "Quantity should be 3");

        // Verify total price = unit price * 3
        Locator row = cartPage.getCartRows().get(0);
        int unitPrice = cartPage.extractPrice(cartPage.getProductPrice(row));
        int totalPrice = cartPage.extractPrice(cartPage.getProductTotal(row));
        Assert.assertEquals(totalPrice, unitPrice * 3,
                "Total should be unit price * 3. Unit: " + unitPrice + ", Total: " + totalPrice);
    }

    // ========== Test 10: Empty Cart State ==========

    @Test(priority = 10)
    public void testEmptyCartState() {
        cartPage.navigateToCart();

        Assert.assertTrue(cartPage.isCartEmpty(), "Cart should be empty when no products added");
        String emptyText = cartPage.getEmptyCartText();
        Assert.assertTrue(emptyText.toLowerCase().contains("empty") || emptyText.toLowerCase().contains("here"),
                "Empty cart message should indicate the cart is empty. Got: " + emptyText);
    }

    // ========== Test 11: Cart Item Count Verification ==========

    @Test(priority = 11)
    public void testCartItemCountAfterAdding() {
        goToProducts();

        // Add 3 products
        addProductAndContinue(0);
        addProductAndContinue(1);
        addProductAndContinue(2);

        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 3, "Cart should show 3 items");
    }

    // ========== Test 12: Remove Product and Verify Count ==========

    @Test(priority = 12)
    public void testRemoveProductAndVerifyCount() {
        goToProducts();

        // Add 2 products
        addProductAndContinue(0);
        addProductAndContinue(1);

        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Cart should have 2 items");

        // Remove one
        cartPage.removeProductByIndex(0);
        page.waitForTimeout(1000);
        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Cart should have 1 item after removal");
    }

    // ========== Test 13: Multiple Products Add and Empty Cart ==========

    @Test(priority = 13)
    public void testMultipleProductsAndEmptyCartAfterRemovingAll() {
        goToProducts();

        // Add 3 products
        addProductAndContinue(0);
        addProductAndContinue(1);
        addProductAndContinue(2);

        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 3, "Cart should have 3 products");

        // Verify all have names and prices
        for (int i = 0; i < 3; i++) {
            Assert.assertFalse(cartPage.getProductNameByIndex(i).isEmpty(),
                    "Product " + i + " should have a name");
            Assert.assertTrue(cartPage.getProductPriceByIndex(i).contains("Rs."),
                    "Product " + i + " should have a price with Rs.");
        }

        // Remove all products one by one — always remove index 0 as rows shift up
        cartPage.removeProductByIndex(0);
        page.waitForTimeout(1000);
        cartPage.removeProductByIndex(0);
        page.waitForTimeout(1000);
        cartPage.removeProductByIndex(0);
        page.waitForTimeout(1000);

        Assert.assertTrue(cartPage.isCartEmpty(), "Cart should be empty after removing all products");
    }

    // ========== Test 14: Proceed to Checkout Button ==========

    @Test(priority = 14)
    public void testProceedToCheckoutButton() {
        goToProducts();
        addProductAndContinue(0);

        cartPage.navigateToCart();
        Assert.assertTrue(cartPage.isProceedToCheckoutVisible(),
                "Proceed to Checkout button should be visible");

        cartPage.clickProceedToCheckout();

        // The site shows a modal asking to login/register OR goes to checkout
        // For a non-logged-in user, it shows a modal with "Register / Login" link
        Locator checkoutModal = page.locator("#checkoutModal");
        Locator checkoutPage = page.locator(".step-one");

        // Either a modal appears (not logged in) or we navigate to checkout
        boolean modalAppeared = checkoutModal.isVisible();
        boolean onCheckout = page.url().contains("checkout");

        Assert.assertTrue(modalAppeared || onCheckout,
                "Clicking Proceed to Checkout should show login modal or go to checkout page");
    }

    // ========== Test 15: Back Navigation ==========

    @Test(priority = 15)
    public void testBackNavigationFromCart() {
        goToProducts();
        addProductAndContinue(0);

        cartPage.navigateToCart();
        Assert.assertTrue(cartPage.getCartItemCount() >= 1, "Cart should have items");

        // Go back to products page
        page.goBack();
        page.waitForLoadState();

        assertThat(page).hasURL(java.util.regex.Pattern.compile(".*/products"));
        Assert.assertTrue(productsPage.isFeaturedItemsVisible(), "Products page should load after back navigation");
    }

    // ========== Test 16: Browser Refresh Retains Cart ==========

    @Test(priority = 16)
    public void testBrowserRefreshRetainsCart() {
        goToProducts();
        addProductAndContinue(0);
        addProductAndContinue(1);

        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Cart should have 2 items before refresh");
        String productNameBefore = cartPage.getProductNameByIndex(0);

        // Refresh the page
        page.reload();
        page.waitForLoadState();

        // Cart should still have items
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Cart should retain 2 items after browser refresh");
        String productNameAfter = cartPage.getProductNameByIndex(0);
        Assert.assertEquals(productNameAfter, productNameBefore,
                "Product name should be same after refresh");
    }

    // ========== Test 17: Click Product Name in Cart → Detail Page ==========

    @Test(priority = 17)
    public void testClickProductNameInCartNavigatesToDetail() {
        goToProducts();
        addProductAndContinue(0);

        cartPage.navigateToCart();
        String cartProductName = cartPage.getProductNameByIndex(0);

        // Click product name link
        cartPage.clickProductNameLinkByIndex(0);
        page.waitForLoadState();

        // Should navigate to product detail page
        assertThat(page).hasURL(java.util.regex.Pattern.compile(".*/product_details/\\d+"));

        // Product name on detail page should match cart
        ProductDetailPage detailPage = new ProductDetailPage(page);
        Assert.assertEquals(detailPage.getProductName(), cartProductName,
                "Product detail name should match what was in cart");
    }

    // ========== Test 18: Product Image Visible in Cart Rows ==========

    @Test(priority = 18)
    public void testProductImageVisibleInCartRows() {
        goToProducts();
        addProductAndContinue(0);
        addProductAndContinue(1);

        cartPage.navigateToCart();

        List<Locator> rows = cartPage.getCartRows();
        for (int i = 0; i < rows.size(); i++) {
            Locator row = rows.get(i);
            Assert.assertTrue(cartPage.isProductImageVisible(row),
                    "Product image should be visible for cart row " + i);
            String imgSrc = cartPage.getProductImageSrc(row);
            Assert.assertNotNull(imgSrc, "Product image src should not be null for row " + i);
            Assert.assertFalse(imgSrc.isEmpty(), "Product image src should not be empty for row " + i);
        }
    }

    // ========== Test 19: Cart Row Displays All Details ==========

    @Test(priority = 19)
    public void testCartRowDisplaysAllDetails() {
        goToProducts();
        addProductAndContinue(0);

        cartPage.navigateToCart();

        Locator row = cartPage.getCartRows().get(0);

        // Every cart row should show: image, name, price, quantity, total
        Assert.assertTrue(cartPage.isProductImageVisible(row), "Image should be visible");
        Assert.assertFalse(cartPage.getProductName(row).isEmpty(), "Name should not be empty");
        Assert.assertTrue(cartPage.getProductPrice(row).contains("Rs."), "Price should contain Rs.");
        Assert.assertFalse(cartPage.getProductQuantity(row).trim().isEmpty(), "Quantity should not be empty");
        Assert.assertTrue(cartPage.getProductTotal(row).contains("Rs."), "Total should contain Rs.");

        // Category should also be displayed
        String category = cartPage.getProductCategory(row);
        Assert.assertFalse(category.isEmpty(), "Category should be displayed below product name");
    }

    // ========== Test 20: Zero Quantity Edge Case ==========

    @Test(priority = 20)
    public void testZeroQuantityOnDetailPage() {
        goToProducts();
        productsPage.clickViewProduct(0);

        ProductDetailPage detailPage = new ProductDetailPage(page);
        detailPage.setQuantity("0");
        detailPage.clickAddToCart();

        // Modal may or may not appear — check cart behavior
        page.waitForTimeout(1000);
        cartPage.navigateToCart();

        // With quantity 0, cart should either be empty or site should have rejected it
        int itemCount = cartPage.getCartItemCount();
        if (itemCount > 0) {
            // If site allowed it, quantity should be 0 or 1 (site may default to 1)
            String qty = cartPage.getProductQuantityByIndex(0).trim();
            Assert.assertTrue(qty.equals("0") || qty.equals("1"),
                    "Zero quantity should result in 0 or 1 in cart. Got: " + qty);
        }
        // If cart is empty, that's also acceptable behavior — site rejected qty 0
    }

    // ========== Test 21: Negative Quantity Edge Case ==========

    @Test(priority = 21)
    public void testNegativeQuantityOnDetailPage() {
        goToProducts();
        productsPage.clickViewProduct(0);

        ProductDetailPage detailPage = new ProductDetailPage(page);
        detailPage.setQuantity("-1");
        detailPage.clickAddToCart();

        page.waitForTimeout(1000);
        cartPage.navigateToCart();

        // Negative quantity should not break the site
        // KNOWN BUG: automationexercise.com accepts -1 as quantity and shows it in cart
        // A real site should reject negative quantities or default to 1
        int itemCount = cartPage.getCartItemCount();
        if (itemCount > 0) {
            String qty = cartPage.getProductQuantityByIndex(0).trim();
            int qtyNum = Integer.parseInt(qty);
            // Document the bug: site allows negative quantity
            // In a real project, this would be filed as a bug report
            if (qtyNum < 0) {
                System.out.println("[BUG FOUND] Site accepts negative quantity: " + qty
                        + ". This should be rejected by input validation.");
            }
        }
        // Page should not be broken regardless
        Assert.assertTrue(page.url().contains("view_cart"), "Should still be on cart page");
    }

    // ========== Test 22: Very Large Quantity Edge Case ==========

    @Test(priority = 22)
    public void testVeryLargeQuantity() {
        goToProducts();
        productsPage.clickViewProduct(0);

        ProductDetailPage detailPage = new ProductDetailPage(page);
        detailPage.setQuantity("99999");
        detailPage.clickAddToCart();

        page.waitForTimeout(1000);
        cartPage.navigateToCart();

        // Site should handle gracefully — either accept it or cap it
        if (cartPage.getCartItemCount() > 0) {
            String qty = cartPage.getProductQuantityByIndex(0).trim();
            int qtyNum = Integer.parseInt(qty);
            Assert.assertTrue(qtyNum > 0, "Quantity should be a positive number. Got: " + qty);

            // Verify price calculation still works (no overflow/NaN)
            Locator row = cartPage.getCartRows().get(0);
            String totalText = cartPage.getProductTotal(row);
            Assert.assertTrue(totalText.contains("Rs."),
                    "Total should still show Rs. format even with large quantity");
        }
    }

    // ========== Test 23: Decimal Quantity Edge Case ==========

    @Test(priority = 23)
    public void testDecimalQuantityOnDetailPage() {
        goToProducts();
        productsPage.clickViewProduct(0);

        ProductDetailPage detailPage = new ProductDetailPage(page);
        detailPage.setQuantity("2.5");
        detailPage.clickAddToCart();

        page.waitForTimeout(1000);
        cartPage.navigateToCart();

        // HTML number input may round or reject decimals
        // Site should not crash
        if (cartPage.getCartItemCount() > 0) {
            String qty = cartPage.getProductQuantityByIndex(0).trim();
            // Quantity should be a whole number (browser rounds or site rejects decimal)
            Assert.assertFalse(qty.contains("."),
                    "Cart quantity should not be a decimal. Got: " + qty);
        }
        // Page should still be functional
        assertThat(page).hasURL(java.util.regex.Pattern.compile(".*/view_cart"));
    }
}
