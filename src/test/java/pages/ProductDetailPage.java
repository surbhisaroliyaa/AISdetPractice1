package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class ProductDetailPage {
    private final Page page;

    // Product information section
    private final Locator productInformation;
    private final Locator productName;
    private final Locator productPrice;
    private final Locator productCategory;
    private final Locator productAvailability;
    private final Locator productCondition;
    private final Locator productBrand;
    private final Locator productImage;
    private final Locator quantityInput;
    private final Locator addToCartButton;

    public ProductDetailPage(Page page) {
        this.page = page;

        productInformation = page.locator(".product-information");
        productName = page.locator(".product-information h2");
        productPrice = page.locator(".product-information span span");
        productCategory = page.locator(".product-information p:has-text('Category')");
        productAvailability = page.locator(".product-information p:has-text('Availability')");
        productCondition = page.locator(".product-information p:has-text('Condition')");
        productBrand = page.locator(".product-information p:has-text('Brand')");
        productImage = page.locator(".product-information img, .view-product img");
        quantityInput = page.locator("#quantity");
        addToCartButton = page.locator("button.cart");
    }

    // ========== Product Details ==========

    public boolean isProductInformationVisible() {
        return productInformation.isVisible();
    }

    public String getProductName() {
        return productName.innerText();
    }

    public String getProductPrice() {
        return productPrice.innerText();
    }

    public String getProductCategory() {
        return productCategory.innerText();
    }

    public String getProductAvailability() {
        return productAvailability.innerText();
    }

    public String getProductCondition() {
        return productCondition.innerText();
    }

    public String getProductBrand() {
        return productBrand.innerText();
    }

    public boolean isProductImageVisible() {
        return productImage.first().isVisible();
    }

    public String getProductImageSrc() {
        return productImage.first().getAttribute("src");
    }

    // ========== Quantity & Cart ==========

    public String getQuantityValue() {
        return quantityInput.inputValue();
    }

    public void setQuantity(String quantity) {
        quantityInput.fill(quantity);
    }

    public boolean isAddToCartVisible() {
        return addToCartButton.isVisible();
    }

    public void clickAddToCart() {
        addToCartButton.click();
    }

    // ========== Add to Cart Confirmation ==========

    public boolean isCartConfirmationVisible() {
        // Wait for the modal to appear after clicking add to cart
        Locator modal = page.locator(".modal-dialog .modal-content");
        modal.waitFor();
        return modal.isVisible();
    }

    public String getCartConfirmationText() {
        return page.locator(".modal-body p").first().innerText();
    }

    public void clickContinueShopping() {
        page.locator("button.close-modal, .modal-footer button").first().click();
    }
}
