package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import config.ConfigReader;

import java.util.List;

public class CartPage {
    private final Page page;

    // Cart table
    private final Locator cartTable;
    private final Locator cartRows;
    private final Locator emptyCartMessage;

    // Cart actions
    private final Locator proceedToCheckoutButton;

    // Navigation
    private final Locator cartNavLink;

    public CartPage(Page page) {
        this.page = page;

        cartTable = page.locator("#cart_info_table");
        cartRows = page.locator("#cart_info_table tbody tr");
        emptyCartMessage = page.locator("#empty_cart");

        proceedToCheckoutButton = page.locator(".check_out");
        cartNavLink = page.locator(".navbar-nav a[href='/view_cart']");
    }

    // ========== Navigation ==========

    public void navigateToCart() {
        page.navigate(ConfigReader.getBaseUrl() + "/view_cart",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    }

    public void clickCartNav() {
        cartNavLink.click();
    }

    // ========== Cart State ==========

    public boolean isCartTableVisible() {
        return cartTable.isVisible();
    }

    public boolean isCartEmpty() {
        return emptyCartMessage.isVisible();
    }

    public String getEmptyCartText() {
        return emptyCartMessage.innerText();
    }

    public int getCartItemCount() {
        if (isCartEmpty()) return 0;
        return cartRows.count();
    }

    // ========== Cart Row Data ==========

    public List<Locator> getCartRows() {
        return cartRows.all();
    }

    public String getProductName(Locator row) {
        return row.locator("td.cart_description h4 a").innerText();
    }

    public String getProductPrice(Locator row) {
        return row.locator("td.cart_price p").innerText();
    }

    public String getProductQuantity(Locator row) {
        return row.locator("td.cart_quantity button").innerText();
    }

    public String getProductTotal(Locator row) {
        return row.locator("td.cart_total p.cart_total_price").innerText();
    }

    // Get data by row index (0-based)
    public String getProductNameByIndex(int index) {
        return getProductName(cartRows.nth(index));
    }

    public String getProductPriceByIndex(int index) {
        return getProductPrice(cartRows.nth(index));
    }

    public String getProductQuantityByIndex(int index) {
        return getProductQuantity(cartRows.nth(index));
    }

    public String getProductTotalByIndex(int index) {
        return getProductTotal(cartRows.nth(index));
    }

    // ========== Cart Actions ==========

    public void removeProduct(Locator row) {
        row.locator("a.cart_quantity_delete").click();
    }

    public void removeProductByIndex(int index) {
        cartRows.nth(index).locator("a.cart_quantity_delete").click();
        // Wait for row to be removed
        page.waitForTimeout(1000);
    }

    public void clickProceedToCheckout() {
        proceedToCheckoutButton.click();
    }

    public boolean isProceedToCheckoutVisible() {
        return proceedToCheckoutButton.isVisible();
    }

    // ========== Price Helpers ==========

    /**
     * Extracts numeric price from text like "Rs. 500"
     */
    public int extractPrice(String priceText) {
        return Integer.parseInt(priceText.replaceAll("[^0-9]", ""));
    }

    /**
     * Verifies price * quantity = total for a given row
     */
    public boolean isPriceCalculationCorrect(Locator row) {
        int price = extractPrice(getProductPrice(row));
        int quantity = Integer.parseInt(getProductQuantity(row).trim());
        int total = extractPrice(getProductTotal(row));
        return price * quantity == total;
    }

    // ========== Add to Cart from Products Page ==========

    /**
     * Hovers over a product card and clicks Add to Cart overlay
     */
    public void hoverAndAddToCart(int productIndex) {
        Locator productCard = page.locator(".features_items .col-sm-4").nth(productIndex);
        Locator overlay = productCard.locator(".product-overlay");
        Locator addToCartBtn = overlay.locator(".add-to-cart");

        productCard.hover();
        overlay.waitFor();
        addToCartBtn.click();
    }

    /**
     * Clicks "Continue Shopping" on the modal after adding to cart
     */
    public void clickContinueShopping() {
        Locator modal = page.locator("#cartModal");
        modal.waitFor();
        page.locator("#cartModal .modal-body .btn-success, #cartModal button.close-modal").first().click();
        // Wait for modal to close
        page.waitForTimeout(500);
    }

    /**
     * Clicks "View Cart" link in the modal after adding to cart
     */
    public void clickViewCartInModal() {
        Locator modal = page.locator("#cartModal");
        modal.waitFor();
        page.locator("#cartModal a[href='/view_cart'], #cartModal u").first().click();
    }

    /**
     * Checks if the add-to-cart confirmation modal is visible
     */
    public boolean isCartModalVisible() {
        Locator modal = page.locator("#cartModal .modal-content");
        modal.waitFor();
        return modal.isVisible();
    }

    public String getCartModalText() {
        return page.locator("#cartModal .modal-body p").first().innerText();
    }

    // ========== Cart Row Display ==========

    /**
     * Checks if product image is visible in a cart row
     */
    public boolean isProductImageVisible(Locator row) {
        Locator img = row.locator("td.cart_product img");
        return img.isVisible();
    }

    public String getProductImageSrc(Locator row) {
        return row.locator("td.cart_product img").getAttribute("src");
    }

    public boolean isProductImageVisibleByIndex(int index) {
        return isProductImageVisible(cartRows.nth(index));
    }

    /**
     * Clicks the product name link in cart to navigate to product detail
     */
    public void clickProductNameLink(Locator row) {
        row.locator("td.cart_description h4 a").click();
    }

    public void clickProductNameLinkByIndex(int index) {
        cartRows.nth(index).locator("td.cart_description h4 a").click();
    }

    /**
     * Gets the product category text shown below the name in cart
     */
    public String getProductCategory(Locator row) {
        return row.locator("td.cart_description p").innerText();
    }
}
