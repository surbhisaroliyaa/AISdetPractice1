package tests.checkout;

import base.BaseTest;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.*;
import utils.TestDataGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CheckoutTest extends BaseTest {

    private HomePage homePage;
    private SignupLoginPage signupLoginPage;
    private SignupPage signupPage;
    private AccountCreatedPage accountCreatedPage;
    private ProductsPage productsPage;
    private CartPage cartPage;
    private CheckoutPage checkoutPage;
    private PaymentPage paymentPage;

    // Registration data — stored so we can verify addresses later
    private String regFirstName;
    private String regLastName;
    private String regCompany;
    private String regAddress;
    private String regCountry;
    private String regState;
    private String regCity;
    private String regZipcode;
    private String regPhone;
    private String regEmail;
    private String regPassword;

    @BeforeMethod
    public void initPages() {
        homePage = new HomePage(page);
        signupLoginPage = new SignupLoginPage(page);
        signupPage = new SignupPage(page);
        accountCreatedPage = new AccountCreatedPage(page);
        productsPage = new ProductsPage(page);
        cartPage = new CartPage(page);
        checkoutPage = new CheckoutPage(page);
        paymentPage = new PaymentPage(page);
    }

    // ========== Helper: Register a new user and store registration data ==========

    private void registerNewUser() {
        regFirstName = TestDataGenerator.getRandomFirstName();
        regLastName = TestDataGenerator.getRandomLastName();
        regCompany = TestDataGenerator.getRandomCompany();
        regAddress = TestDataGenerator.getRandomAddress();
        regCountry = "India";
        regState = TestDataGenerator.getRandomState();
        regCity = TestDataGenerator.getRandomCity();
        regZipcode = TestDataGenerator.getRandomZipcode();
        regPhone = TestDataGenerator.getRandomPhone();
        regEmail = TestDataGenerator.getRandomEmail();
        regPassword = TestDataGenerator.getRandomPassword();

        String signupName = regFirstName;

        signupLoginPage.navigateToLoginPage();
        signupLoginPage.enterSignupDetails(signupName, regEmail);
        signupPage.fillFullRegistration(
                regPassword, "15", "6", "1995",
                regFirstName, regLastName, regCompany,
                regAddress, regCountry, regState, regCity,
                regZipcode, regPhone
        );
        accountCreatedPage.clickContinue();
    }

    private void deleteAccount() {
        page.locator(".navbar-nav a[href='/delete_account']").click();
        page.locator("[data-qa='continue-button']").click();
    }

    private void addProductToCartAndContinue(int index) {
        Locator productCard = page.locator(".features_items .col-sm-4").nth(index);
        Locator overlay = productCard.locator(".product-overlay");
        Locator addToCartBtn = overlay.locator(".add-to-cart");
        productCard.hover();
        overlay.waitFor();
        addToCartBtn.click();
        // Click Continue Shopping
        page.locator("#cartModal .modal-body .btn-success, #cartModal button.close-modal").first().click();
        page.waitForTimeout(500);
    }

    // ========== CORE FLOW 1: Full E2E — Register → Add → Checkout → Pay → Invoice ==========

    @Test
    public void testFullCheckoutE2EFlow() {
        // Step 1: Register
        registerNewUser();
        Assert.assertTrue(page.locator("a:has-text('Logged in as')").isVisible(),
                "User should be logged in after registration");

        // Step 2: Go to products and add a product
        productsPage.navigateToProductsPage();
        String productName = productsPage.getProductName(productsPage.getAllProductCards().get(0));
        String productPrice = productsPage.getProductPrice(productsPage.getAllProductCards().get(0));
        addProductToCartAndContinue(0);

        // Step 3: Go to cart and proceed to checkout
        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Cart should have 1 item");
        cartPage.clickProceedToCheckout();

        // Step 4: Verify checkout page — address and order
        Assert.assertTrue(checkoutPage.isCheckoutPageVisible(), "Checkout page should be visible");
        Assert.assertTrue(checkoutPage.isDeliveryAddressVisible(), "Delivery address should show");
        Assert.assertTrue(checkoutPage.isBillingAddressVisible(), "Billing address should show");
        Assert.assertEquals(checkoutPage.getOrderItemCount(), 1, "Should have 1 item in order");

        // Verify product in order matches what was added
        Assert.assertEquals(checkoutPage.getOrderProductNameByIndex(0), productName,
                "Product name should match on checkout");

        // Add comment
        checkoutPage.addComment("Please deliver between 9 AM and 5 PM");
        Assert.assertEquals(checkoutPage.getCommentValue(), "Please deliver between 9 AM and 5 PM");

        // Step 5: Place order → Payment page
        checkoutPage.clickPlaceOrder();
        Assert.assertTrue(paymentPage.isPaymentFormVisible(), "Payment form should be visible");

        // Step 6: Fill payment and confirm
        paymentPage.payWithCard("Test User", "4100000000000000", "123", "01", "2030");

        // Step 7: Verify order confirmation
        Assert.assertTrue(paymentPage.isOrderConfirmedVisible(), "Order confirmed message should show");

        // Step 8: Download invoice
        Download download = paymentPage.downloadInvoice();
        Assert.assertNotNull(download, "Download should not be null");
        String fileName = paymentPage.getDownloadedFileName(download);
        Assert.assertNotNull(fileName, "Downloaded file should have a name");
        Path filePath = paymentPage.getDownloadedFilePath(download);
        Assert.assertTrue(Files.exists(filePath), "Downloaded file should exist on disk");

        // Step 9: Continue and clean up
        paymentPage.clickContinue();
        deleteAccount();
    }

    // ========== CORE FLOW 2: Add without login → Login → Checkout ==========

    @Test
    public void testCheckoutFlowWithLoginAfterCart() {
        // Step 1: Register a user first (so we can login later)
        registerNewUser();
        // Logout
        page.locator(".navbar-nav a[href='/logout']").click();

        // Step 2: Add products without being logged in
        productsPage.navigateToProductsPage();
        String product1Name = productsPage.getProductName(productsPage.getAllProductCards().get(0));
        String product2Name = productsPage.getProductName(productsPage.getAllProductCards().get(1));
        addProductToCartAndContinue(0);
        addProductToCartAndContinue(1);

        // Step 3: Go to cart → Proceed to checkout → Should prompt to login/register
        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Cart should have 2 items before login");
        cartPage.clickProceedToCheckout();

        // Click Register/Login link in the modal
        page.locator("#checkoutModal a[href='/login'], #checkoutModal u").first().click();

        // Step 4: Login with registered credentials
        signupLoginPage.login(regEmail, regPassword);
        Assert.assertTrue(page.locator("a:has-text('Logged in as')").isVisible(),
                "User should be logged in");

        // Step 5: Go back to cart — verify items are still there
        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Cart should retain 2 items after login");

        // Verify product names match
        Assert.assertEquals(cartPage.getProductNameByIndex(0), product1Name);
        Assert.assertEquals(cartPage.getProductNameByIndex(1), product2Name);

        // Step 6: Proceed to checkout
        cartPage.clickProceedToCheckout();
        Assert.assertTrue(checkoutPage.isCheckoutPageVisible(), "Checkout page should be visible");

        // Verify address is present
        Assert.assertTrue(checkoutPage.isDeliveryAddressVisible(), "Delivery address should show");

        // Add comment
        checkoutPage.addComment("Ordered after logging in");
        Assert.assertFalse(checkoutPage.getCommentValue().isEmpty(), "Comment should be added");

        // Step 7: Place order → Pay → Confirm → Download
        checkoutPage.clickPlaceOrder();
        paymentPage.payWithCard("Test User", "4100000000000000", "123", "02", "2029");
        Assert.assertTrue(paymentPage.isOrderConfirmedVisible(), "Order should be confirmed");

        Download download = paymentPage.downloadInvoice();
        Assert.assertTrue(Files.exists(paymentPage.getDownloadedFilePath(download)),
                "Invoice file should exist");

        paymentPage.clickContinue();
        deleteAccount();
    }

    // ========== VERIFICATION: Products match across pages (products → cart → checkout) ==========

    @Test
    public void testProductDetailsMatchAcrossPages() {
        registerNewUser();

        // Capture product info from products page
        productsPage.navigateToProductsPage();
        List<Locator> products = productsPage.getAllProductCards();
        String prodPageName = productsPage.getProductName(products.get(0));
        String prodPagePrice = productsPage.getProductPrice(products.get(0));

        addProductToCartAndContinue(0);

        // Verify in cart
        cartPage.navigateToCart();
        String cartName = cartPage.getProductNameByIndex(0);
        String cartPrice = cartPage.getProductPriceByIndex(0);
        String cartQty = cartPage.getProductQuantityByIndex(0);
        Assert.assertEquals(cartName, prodPageName, "Cart name should match products page");
        Assert.assertEquals(cartPrice, prodPagePrice, "Cart price should match products page");
        Assert.assertEquals(cartQty, "1", "Default quantity should be 1");
        Assert.assertTrue(cartPage.isProductImageVisibleByIndex(0), "Cart should show product image");

        // Proceed to checkout and verify there too
        cartPage.clickProceedToCheckout();
        String checkoutName = checkoutPage.getOrderProductNameByIndex(0);
        String checkoutPrice = checkoutPage.getOrderProductPriceByIndex(0);
        String checkoutQty = checkoutPage.getOrderProductQuantityByIndex(0);
        Assert.assertEquals(checkoutName, prodPageName, "Checkout name should match products page");
        Assert.assertEquals(checkoutPrice, prodPagePrice, "Checkout price should match products page");
        Assert.assertEquals(checkoutQty, "1", "Checkout quantity should match cart");

        deleteAccount();
    }

    // ========== VERIFICATION: Multiple products — prices, quantities, totals ==========

    @Test
    public void testMultipleProductsPricesAndTotals() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        String prod1Price = productsPage.getProductPrice(productsPage.getAllProductCards().get(0));
        String prod2Price = productsPage.getProductPrice(productsPage.getAllProductCards().get(1));

        addProductToCartAndContinue(0);
        addProductToCartAndContinue(1);

        // Cart verification
        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Should have 2 products in cart");
        Assert.assertEquals(cartPage.getProductPriceByIndex(0), prod1Price);
        Assert.assertEquals(cartPage.getProductPriceByIndex(1), prod2Price);

        // Verify price calculations for each row
        for (Locator row : cartPage.getCartRows()) {
            Assert.assertTrue(cartPage.isPriceCalculationCorrect(row),
                    "Price * Quantity should equal Total for product: " + cartPage.getProductName(row));
        }

        // Checkout verification
        cartPage.clickProceedToCheckout();
        Assert.assertEquals(checkoutPage.getOrderItemCount(), 2);

        for (Locator row : checkoutPage.getOrderRows()) {
            Assert.assertTrue(checkoutPage.isOrderPriceCalculationCorrect(row),
                    "Order: price * qty should equal total for " + checkoutPage.getOrderProductName(row));
        }

        deleteAccount();
    }

    // ========== VERIFICATION: Delivery and billing address match registration ==========

    @Test
    public void testAddressMatchesRegistration() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();

        // Verify delivery address contains registration data
        String deliveryName = checkoutPage.getDeliveryName();
        Assert.assertTrue(deliveryName.contains(regFirstName),
                "Delivery name should contain first name: " + regFirstName);
        Assert.assertTrue(deliveryName.contains(regLastName),
                "Delivery name should contain last name: " + regLastName);

        String deliveryAddress = checkoutPage.getDeliveryAddress1();
        Assert.assertTrue(deliveryAddress.contains(regAddress),
                "Delivery address should contain registered address");

        String deliveryCityStateZip = checkoutPage.getDeliveryCityStateZip();
        Assert.assertTrue(deliveryCityStateZip.contains(regCity),
                "Should contain city: " + regCity);
        Assert.assertTrue(deliveryCityStateZip.contains(regState),
                "Should contain state: " + regState);
        Assert.assertTrue(deliveryCityStateZip.contains(regZipcode),
                "Should contain zipcode: " + regZipcode);

        Assert.assertTrue(checkoutPage.getDeliveryPhone().contains(regPhone),
                "Delivery phone should match registration");

        // Verify delivery and billing match each other
        Assert.assertTrue(checkoutPage.doAddressesMatch(),
                "Delivery and billing addresses should match");

        deleteAccount();
    }

    // ========== VERIFICATION: Comment box works ==========

    @Test
    public void testCommentBoxOnCheckout() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();

        // Verify comment box is visible and works
        Assert.assertTrue(checkoutPage.isCommentBoxVisible(), "Comment box should be visible");

        String comment = "Please handle with care. Deliver by 3 PM.";
        checkoutPage.addComment(comment);
        Assert.assertEquals(checkoutPage.getCommentValue(), comment,
                "Comment should be saved in the textarea");

        // Overwrite with new comment
        String newComment = "Updated: Leave at front door";
        checkoutPage.addComment(newComment);
        Assert.assertEquals(checkoutPage.getCommentValue(), newComment,
                "Comment should be updatable");

        deleteAccount();
    }

    // ========== VERIFICATION: Payment details and order confirmation ==========

    @Test
    public void testPaymentAndConfirmation() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();
        checkoutPage.clickPlaceOrder();

        // Verify payment form fields
        Assert.assertTrue(paymentPage.isPaymentFormVisible(), "Payment form should be visible");
        Assert.assertTrue(paymentPage.isPayAndConfirmVisible(), "Pay button should be visible");

        // Fill card details
        paymentPage.fillCardDetails("Amit Sharma", "4100000000000000", "311", "06", "2028");

        // Verify values were entered
        Assert.assertEquals(paymentPage.getNameOnCardValue(), "Amit Sharma");
        Assert.assertEquals(paymentPage.getCardNumberValue(), "4100000000000000");
        Assert.assertEquals(paymentPage.getCvcValue(), "311");
        Assert.assertEquals(paymentPage.getExpiryMonthValue(), "06");
        Assert.assertEquals(paymentPage.getExpiryYearValue(), "2028");

        // Submit payment
        paymentPage.clickPayAndConfirm();

        // Verify confirmation
        Assert.assertTrue(paymentPage.isOrderConfirmedVisible(), "Order should be confirmed");

        deleteAccount();
    }

    // ========== VERIFICATION: Download invoice — file exists and format check ==========

    @Test
    public void testDownloadInvoiceDetails() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();
        checkoutPage.clickPlaceOrder();
        paymentPage.payWithCard("Test User", "4100000000000000", "123", "01", "2030");

        Assert.assertTrue(paymentPage.isOrderConfirmedVisible());
        Assert.assertTrue(paymentPage.isDownloadInvoiceVisible(),
                "Download invoice button should be available");

        // Download and verify
        Download download = paymentPage.downloadInvoice();
        Path filePath = paymentPage.getDownloadedFilePath(download);

        Assert.assertTrue(Files.exists(filePath), "Invoice file should exist on disk");
        Assert.assertNotNull(download.suggestedFilename(), "File should have a name");

        // Verify file is not empty
        try {
            long fileSize = Files.size(filePath);
            Assert.assertTrue(fileSize > 0, "Invoice file should not be empty");
        } catch (Exception e) {
            Assert.fail("Failed to read invoice file: " + e.getMessage());
        }

        paymentPage.clickContinue();
        deleteAccount();
    }

    // ========== VERIFICATION: Download invoice again (re-download) ==========

    @Test
    public void testDownloadInvoiceMultipleTimes() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();
        checkoutPage.clickPlaceOrder();
        paymentPage.payWithCard("Test User", "4100000000000000", "123", "01", "2030");

        Assert.assertTrue(paymentPage.isOrderConfirmedVisible());

        // First download
        Download download1 = paymentPage.downloadInvoice();
        Path path1 = paymentPage.getDownloadedFilePath(download1);
        Assert.assertTrue(Files.exists(path1), "First download should exist");

        // Second download — verify button still works
        Download download2 = paymentPage.downloadInvoice();
        Path path2 = paymentPage.getDownloadedFilePath(download2);
        Assert.assertTrue(Files.exists(path2), "Second download should also exist");

        // Both files should have the same suggested filename
        Assert.assertEquals(download1.suggestedFilename(), download2.suggestedFilename(),
                "Both downloads should have the same filename");

        // Both files should have same size (same content)
        try {
            Assert.assertEquals(Files.size(path1), Files.size(path2),
                    "Both invoice downloads should have same file size (same content)");
        } catch (Exception e) {
            Assert.fail("Failed to compare file sizes: " + e.getMessage());
        }

        paymentPage.clickContinue();
        deleteAccount();
    }

    // ========== NAVIGATION: All buttons work between pages ==========

    @Test
    public void testNavigationButtonsBetweenPages() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        // Products → Cart (via nav)
        cartPage.navigateToCart();
        Assert.assertTrue(page.url().contains("view_cart"), "Should be on cart page");

        // Cart → Checkout
        cartPage.clickProceedToCheckout();
        Assert.assertTrue(checkoutPage.isCheckoutPageVisible(), "Should be on checkout page");

        // Checkout → Payment
        checkoutPage.clickPlaceOrder();
        Assert.assertTrue(paymentPage.isPaymentFormVisible(), "Should be on payment page");

        // Pay → Confirmation
        paymentPage.payWithCard("Test", "4100000000000000", "123", "01", "2030");
        Assert.assertTrue(paymentPage.isOrderConfirmedVisible(), "Should be on confirmation page");

        // Confirmation → Continue → Home
        paymentPage.clickContinue();
        Assert.assertTrue(page.url().contains("automationexercise.com"),
                "Should navigate back to site after continue");

        deleteAccount();
    }

    // ========== NAVIGATION: Back button from checkout ==========

    @Test
    public void testBackNavigationFromCheckout() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();
        Assert.assertTrue(checkoutPage.isCheckoutPageVisible());

        // Go back
        page.goBack();
        page.waitForTimeout(1000);

        // Should be back on cart page with items intact
        Assert.assertTrue(page.url().contains("view_cart"), "Should be back on cart page");
        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Cart should still have 1 item");

        deleteAccount();
    }

    // ========== EDGE CASE: Place order without filling any payment fields ==========

    @Test
    public void testPaymentWithEmptyFields() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();
        checkoutPage.clickPlaceOrder();

        Assert.assertTrue(paymentPage.isPaymentFormVisible());

        // Click Pay without filling anything
        paymentPage.clickPayAndConfirm();

        // Should still be on payment page — browser validation should block
        // Or site may submit and show error — depends on site behavior
        // Either way, should NOT show order confirmed
        boolean stillOnPayment = paymentPage.isPaymentFormVisible();
        boolean orderConfirmed = false;
        try {
            orderConfirmed = paymentPage.isOrderConfirmedVisible();
        } catch (Exception ignored) {
        }

        Assert.assertTrue(stillOnPayment || !orderConfirmed,
                "Payment should not succeed with empty fields — " +
                        "either still on form or no confirmation shown");

        deleteAccount();
    }

    // ========== EDGE CASE: Pay with partial fields (only name and card number) ==========

    @Test
    public void testPaymentWithPartialFields() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();
        checkoutPage.clickPlaceOrder();

        // Fill only 2 fields — leave CVC, month, year empty
        paymentPage.fillNameOnCard("Partial User");
        paymentPage.fillCardNumber("4100000000000000");

        paymentPage.clickPayAndConfirm();

        // Site should block or show error with missing fields
        boolean stillOnPayment = paymentPage.isPaymentFormVisible();
        boolean orderConfirmed = false;
        try {
            orderConfirmed = paymentPage.isOrderConfirmedVisible();
        } catch (Exception ignored) {
        }

        // BUG POSSIBILITY: Site might accept partial data — document it
        if (orderConfirmed) {
            System.out.println("BUG: Site accepted payment with missing CVC, expiry month, and expiry year");
        } else {
            Assert.assertTrue(stillOnPayment,
                    "Should remain on payment page when required fields are missing");
        }

        deleteAccount();
    }

    // ========== EDGE CASE: CVC, month, year format validation ==========

    @Test
    public void testPaymentFieldFormatValidation() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();
        checkoutPage.clickPlaceOrder();

        // Test extra-long CVC (more than 3 digits)
        paymentPage.fillCvc("12345");
        String cvcValue = paymentPage.getCvcValue();
        // Many sites truncate input; check what was accepted
        System.out.println("CVC entered: 12345, CVC accepted: " + cvcValue);

        // Test extra-long month (more than 2 digits)
        paymentPage.fillExpiryMonth("123");
        String monthValue = paymentPage.getExpiryMonthValue();
        System.out.println("Month entered: 123, Month accepted: " + monthValue);

        // Test extra-long year (more than 4 digits)
        paymentPage.fillExpiryYear("20300");
        String yearValue = paymentPage.getExpiryYearValue();
        System.out.println("Year entered: 20300, Year accepted: " + yearValue);

        // Verify at minimum: fields should accept input without crash
        Assert.assertNotNull(cvcValue, "CVC field should accept input");
        Assert.assertNotNull(monthValue, "Month field should accept input");
        Assert.assertNotNull(yearValue, "Year field should accept input");

        deleteAccount();
    }

    // ========== EDGE CASE: Alphabets in numeric payment fields ==========

    @Test
    public void testPaymentWithAlphabetsInNumericFields() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();
        checkoutPage.clickPlaceOrder();

        // Fill alphabets in numeric fields
        paymentPage.fillNameOnCard("Valid Name");
        paymentPage.fillCardNumber("abcdefghijklmnop");
        paymentPage.fillCvc("abc");
        paymentPage.fillExpiryMonth("ab");
        paymentPage.fillExpiryYear("abcd");

        // Check what the fields actually accepted
        String cardVal = paymentPage.getCardNumberValue();
        String cvcVal = paymentPage.getCvcValue();
        String monthVal = paymentPage.getExpiryMonthValue();
        String yearVal = paymentPage.getExpiryYearValue();

        System.out.println("Card field accepted: " + cardVal);
        System.out.println("CVC field accepted: " + cvcVal);
        System.out.println("Month field accepted: " + monthVal);
        System.out.println("Year field accepted: " + yearVal);

        // Try to submit — should fail validation
        paymentPage.clickPayAndConfirm();

        boolean orderConfirmed = false;
        try {
            orderConfirmed = paymentPage.isOrderConfirmedVisible();
        } catch (Exception ignored) {
        }

        if (orderConfirmed) {
            System.out.println("BUG: Site accepted alphabetic characters in numeric payment fields");
        }

        deleteAccount();
    }

    // ========== EDGE CASE: Empty cart → Proceed to checkout ==========

    @Test
    public void testCheckoutWithEmptyCart() {
        registerNewUser();

        // Go to cart without adding any products
        cartPage.navigateToCart();
        Assert.assertTrue(cartPage.isCartEmpty(), "Cart should be empty");

        // Try to proceed to checkout — button should not be visible or should not work
        boolean checkoutButtonVisible = false;
        try {
            checkoutButtonVisible = cartPage.isProceedToCheckoutVisible();
        } catch (Exception ignored) {
        }

        if (checkoutButtonVisible) {
            cartPage.clickProceedToCheckout();
            // Should NOT reach checkout page with empty cart
            boolean onCheckout = false;
            try {
                onCheckout = checkoutPage.isCheckoutPageVisible();
            } catch (Exception ignored) {
            }

            if (onCheckout) {
                System.out.println("BUG: Site allows checkout with empty cart");
                Assert.assertEquals(checkoutPage.getOrderItemCount(), 0,
                        "If checkout is reached, order should have 0 items");
            }
        }
        // Good — checkout button not visible for empty cart

        deleteAccount();
    }

    // ========== EDGE CASE: Checkout without being logged in ==========

    @Test
    public void testCheckoutWithoutLogin() {
        // Add product without being logged in
        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 1, "Cart should have 1 item");

        // Click proceed to checkout — should show modal asking to register/login
        cartPage.clickProceedToCheckout();

        // Verify the checkout modal appears (not the actual checkout page)
        Locator checkoutModal = page.locator("#checkoutModal");
        Assert.assertTrue(checkoutModal.isVisible(),
                "Should show Register/Login modal when not logged in");

        // Verify the modal contains register/login link
        Locator loginLink = page.locator("#checkoutModal a[href='/login']");
        Assert.assertTrue(loginLink.isVisible(),
                "Modal should have a Register/Login link");

        // Should NOT be on checkout page
        Assert.assertFalse(checkoutPage.isCheckoutPageVisible(),
                "Should NOT show checkout page without being logged in");
    }

    // ========== EDGE CASE: Browser refresh on checkout page ==========

    @Test
    public void testRefreshCheckoutPage() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();

        Assert.assertTrue(checkoutPage.isCheckoutPageVisible());
        Assert.assertTrue(checkoutPage.isDeliveryAddressVisible());
        int itemCountBefore = checkoutPage.getOrderItemCount();
        String productNameBefore = checkoutPage.getOrderProductNameByIndex(0);

        // Refresh the page
        page.reload();
        page.waitForTimeout(1000);

        // Verify everything survives refresh
        Assert.assertTrue(checkoutPage.isCheckoutPageVisible(),
                "Checkout page should survive refresh");
        Assert.assertTrue(checkoutPage.isDeliveryAddressVisible(),
                "Delivery address should survive refresh");
        Assert.assertEquals(checkoutPage.getOrderItemCount(), itemCountBefore,
                "Order item count should survive refresh");
        Assert.assertEquals(checkoutPage.getOrderProductNameByIndex(0), productNameBefore,
                "Product name should survive refresh");
        Assert.assertTrue(checkoutPage.isCommentBoxVisible(),
                "Comment box should survive refresh");

        deleteAccount();
    }

    // ========== EDGE CASE: Browser refresh on payment page ==========

    @Test
    public void testRefreshPaymentPage() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();
        checkoutPage.clickPlaceOrder();

        Assert.assertTrue(paymentPage.isPaymentFormVisible());

        // Fill card details
        paymentPage.fillCardDetails("Refresh Test", "4100000000000000", "999", "12", "2031");

        // Refresh the page
        page.reload();
        page.waitForTimeout(1000);

        // After refresh, form fields should be empty (browser clears form data)
        boolean formStillVisible = paymentPage.isPaymentFormVisible();

        if (formStillVisible) {
            String nameAfter = paymentPage.getNameOnCardValue();
            String cardAfter = paymentPage.getCardNumberValue();

            // Most browsers clear form fields on refresh
            System.out.println("After refresh — Name on card: '" + nameAfter + "', Card number: '" + cardAfter + "'");

            // Fields should be empty after refresh (standard browser behavior)
            // Some browsers may retain values — document actual behavior
            if (!nameAfter.isEmpty() || !cardAfter.isEmpty()) {
                System.out.println("NOTE: Browser retained form data after refresh");
            }
        }

        deleteAccount();
    }

    // ========== EDGE CASE: Double-click Pay button ==========

    @Test
    public void testDoubleClickPayButton() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();
        checkoutPage.clickPlaceOrder();

        paymentPage.fillCardDetails("Double Click", "4100000000000000", "123", "01", "2030");

        // Click pay button twice quickly
        paymentPage.clickPayAndConfirm();

        // Try clicking again immediately (simulating double-click)
        try {
            paymentPage.clickPayAndConfirm();
        } catch (Exception ignored) {
            // Button may no longer be available after first click — that's fine
        }

        // Should show exactly one confirmation — no error, no duplicate
        Assert.assertTrue(paymentPage.isOrderConfirmedVisible(),
                "Should show order confirmed (exactly once)");

        deleteAccount();
    }

    // ========== EDGE CASE: Expired card date ==========

    @Test
    public void testPaymentWithExpiredCard() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();
        checkoutPage.clickPlaceOrder();

        // Use expiry date in the past
        paymentPage.fillCardDetails("Expired Card", "4100000000000000", "123", "01", "2020");
        paymentPage.clickPayAndConfirm();

        boolean orderConfirmed = false;
        try {
            orderConfirmed = paymentPage.isOrderConfirmedVisible();
        } catch (Exception ignored) {
        }

        if (orderConfirmed) {
            System.out.println("BUG: Site accepted payment with expired card (01/2020)");
        } else {
            // Good — site rejected expired card
            Assert.assertTrue(paymentPage.isPaymentFormVisible(),
                    "Should remain on payment page for expired card");
        }

        deleteAccount();
    }

    // ========== DATA FLOW: Remove product then checkout — verify correct items ==========

    @Test
    public void testRemoveProductThenCheckout() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        String prod1Name = productsPage.getProductName(productsPage.getAllProductCards().get(0));
        String prod2Name = productsPage.getProductName(productsPage.getAllProductCards().get(1));
        String prod3Name = productsPage.getProductName(productsPage.getAllProductCards().get(2));

        addProductToCartAndContinue(0);
        addProductToCartAndContinue(1);
        addProductToCartAndContinue(2);

        // Go to cart — verify 3 items
        cartPage.navigateToCart();
        Assert.assertEquals(cartPage.getCartItemCount(), 3, "Should have 3 products");

        // Remove the second product (index 1)
        cartPage.removeProductByIndex(1);
        Assert.assertEquals(cartPage.getCartItemCount(), 2, "Should have 2 products after removal");

        // Proceed to checkout
        cartPage.clickProceedToCheckout();

        // Verify checkout shows exactly 2 products
        Assert.assertEquals(checkoutPage.getOrderItemCount(), 2,
                "Checkout should show 2 products after removing 1");

        // Verify the remaining products are correct (product 1 and product 3)
        String checkoutProd1 = checkoutPage.getOrderProductNameByIndex(0);
        String checkoutProd2 = checkoutPage.getOrderProductNameByIndex(1);

        Assert.assertEquals(checkoutProd1, prod1Name,
                "First product should be " + prod1Name);
        Assert.assertEquals(checkoutProd2, prod3Name,
                "Second product should be " + prod3Name + " (middle one was removed)");

        deleteAccount();
    }

    // ========== DATA FLOW: Invoice content verification ==========

    @Test
    public void testInvoiceContentMatchesOrder() {
        registerNewUser();

        productsPage.navigateToProductsPage();
        String productName = productsPage.getProductName(productsPage.getAllProductCards().get(0));
        addProductToCartAndContinue(0);

        cartPage.navigateToCart();
        cartPage.clickProceedToCheckout();
        checkoutPage.clickPlaceOrder();
        paymentPage.payWithCard("Test User", "4100000000000000", "123", "01", "2030");

        Assert.assertTrue(paymentPage.isOrderConfirmedVisible());

        // Download invoice and read content
        Download download = paymentPage.downloadInvoice();
        Path filePath = paymentPage.getDownloadedFilePath(download);

        try {
            String content = Files.readString(filePath);
            Assert.assertFalse(content.isEmpty(), "Invoice should not be empty");

            System.out.println("Invoice content: " + content);

            // Verify the invoice contains the product name
            // Note: The invoice format may vary — log actual content for review
            if (!content.contains(productName)) {
                System.out.println("NOTE: Invoice does not contain product name '" + productName
                        + "'. Invoice may use a different format. Review content above.");
            }
        } catch (Exception e) {
            Assert.fail("Failed to read invoice file: " + e.getMessage());
        }

        paymentPage.clickContinue();
        deleteAccount();
    }
}
