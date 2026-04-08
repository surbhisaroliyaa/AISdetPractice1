package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;

public class CheckoutPage {
    private final Page page;

    // Delivery Address
    private final Locator deliveryAddressSection;
    private final Locator deliveryFirstLastName;
    private final Locator deliveryCompany;
    private final Locator deliveryAddress1;
    private final Locator deliveryAddress2;
    private final Locator deliveryCityStateZip;
    private final Locator deliveryCountry;
    private final Locator deliveryPhone;

    // Billing Address
    private final Locator billingAddressSection;
    private final Locator billingFirstLastName;
    private final Locator billingCompany;
    private final Locator billingAddress1;
    private final Locator billingAddress2;
    private final Locator billingCityStateZip;
    private final Locator billingCountry;
    private final Locator billingPhone;

    // Order Details (product table on checkout page)
    private final Locator orderTable;
    private final Locator orderRows;
    private final Locator orderTotal;

    // Comment box
    private final Locator commentTextarea;

    // Actions
    private final Locator placeOrderButton;

    public CheckoutPage(Page page) {
        this.page = page;

        // Delivery Address — #address_delivery
        deliveryAddressSection = page.locator("#address_delivery");
        deliveryFirstLastName = page.locator("#address_delivery .address_firstname.address_lastname");
        deliveryCompany = page.locator("#address_delivery .address_address1.address_address2").first();
        deliveryAddress1 = page.locator("#address_delivery .address_address1.address_address2").nth(1);
        deliveryAddress2 = page.locator("#address_delivery .address_address1.address_address2").nth(2);
        deliveryCityStateZip = page.locator("#address_delivery .address_city.address_state_name.address_postcode");
        deliveryCountry = page.locator("#address_delivery .address_country_name");
        deliveryPhone = page.locator("#address_delivery .address_phone");

        // Billing Address — #address_invoice
        billingAddressSection = page.locator("#address_invoice");
        billingFirstLastName = page.locator("#address_invoice .address_firstname.address_lastname");
        billingCompany = page.locator("#address_invoice .address_address1.address_address2").first();
        billingAddress1 = page.locator("#address_invoice .address_address1.address_address2").nth(1);
        billingAddress2 = page.locator("#address_invoice .address_address1.address_address2").nth(2);
        billingCityStateZip = page.locator("#address_invoice .address_city.address_state_name.address_postcode");
        billingCountry = page.locator("#address_invoice .address_country_name");
        billingPhone = page.locator("#address_invoice .address_phone");

        // Order details table — filter to product rows only (have id like "product-1")
        orderTable = page.locator("#cart_info");
        orderRows = page.locator("#cart_info tbody tr[id^='product-']");
        orderTotal = page.locator("#cart_info .cart_total_price");

        // Comment
        commentTextarea = page.locator("textarea.form-control");

        // Place Order
        placeOrderButton = page.locator("a.check_out");
    }

    // ========== Page State ==========

    public boolean isCheckoutPageVisible() {
        return deliveryAddressSection.isVisible();
    }

    public boolean isOrderTableVisible() {
        return orderTable.isVisible();
    }

    // ========== Delivery Address ==========

    public String getDeliveryName() {
        return deliveryFirstLastName.innerText();
    }

    public String getDeliveryCompany() {
        return deliveryCompany.innerText();
    }

    public String getDeliveryAddress1() {
        return deliveryAddress1.innerText();
    }

    public String getDeliveryAddress2() {
        return deliveryAddress2.innerText();
    }

    public String getDeliveryCityStateZip() {
        return deliveryCityStateZip.innerText();
    }

    public String getDeliveryCountry() {
        return deliveryCountry.innerText();
    }

    public String getDeliveryPhone() {
        return deliveryPhone.innerText();
    }

    public boolean isDeliveryAddressVisible() {
        return deliveryAddressSection.isVisible();
    }

    // ========== Billing Address ==========

    public String getBillingName() {
        return billingFirstLastName.innerText();
    }

    public String getBillingCompany() {
        return billingCompany.innerText();
    }

    public String getBillingAddress1() {
        return billingAddress1.innerText();
    }

    public String getBillingAddress2() {
        return billingAddress2.innerText();
    }

    public String getBillingCityStateZip() {
        return billingCityStateZip.innerText();
    }

    public String getBillingCountry() {
        return billingCountry.innerText();
    }

    public String getBillingPhone() {
        return billingPhone.innerText();
    }

    public boolean isBillingAddressVisible() {
        return billingAddressSection.isVisible();
    }

    // ========== Address Comparison ==========

    public boolean doAddressesMatch() {
        return getDeliveryName().equals(getBillingName())
                && getDeliveryCompany().equals(getBillingCompany())
                && getDeliveryAddress1().equals(getBillingAddress1())
                && getDeliveryCityStateZip().equals(getBillingCityStateZip())
                && getDeliveryCountry().equals(getBillingCountry())
                && getDeliveryPhone().equals(getBillingPhone());
    }

    // ========== Order Details ==========

    public int getOrderItemCount() {
        return orderRows.count();
    }

    public List<Locator> getOrderRows() {
        return orderRows.all();
    }

    public String getOrderProductName(Locator row) {
        return row.locator("td.cart_description h4 a").innerText();
    }

    public String getOrderProductPrice(Locator row) {
        return row.locator("td.cart_price p").innerText();
    }

    public String getOrderProductQuantity(Locator row) {
        return row.locator("td.cart_quantity button").innerText();
    }

    public String getOrderProductTotal(Locator row) {
        return row.locator("td.cart_total p.cart_total_price").innerText();
    }

    public boolean isOrderProductImageVisible(Locator row) {
        return row.locator("td.cart_product img").isVisible();
    }

    public String getOrderProductNameByIndex(int index) {
        return getOrderProductName(orderRows.nth(index));
    }

    public String getOrderProductPriceByIndex(int index) {
        return getOrderProductPrice(orderRows.nth(index));
    }

    public String getOrderProductQuantityByIndex(int index) {
        return getOrderProductQuantity(orderRows.nth(index));
    }

    public String getOrderProductTotalByIndex(int index) {
        return getOrderProductTotal(orderRows.nth(index));
    }

    public int extractPrice(String priceText) {
        return Integer.parseInt(priceText.replaceAll("[^0-9]", ""));
    }

    public boolean isOrderPriceCalculationCorrect(Locator row) {
        int price = extractPrice(getOrderProductPrice(row));
        int quantity = Integer.parseInt(getOrderProductQuantity(row).trim());
        int total = extractPrice(getOrderProductTotal(row));
        return price * quantity == total;
    }

    // ========== Comment Box ==========

    public void addComment(String comment) {
        commentTextarea.fill(comment);
    }

    public String getCommentValue() {
        return commentTextarea.inputValue();
    }

    public boolean isCommentBoxVisible() {
        return commentTextarea.isVisible();
    }

    // ========== Actions ==========

    public void clickPlaceOrder() {
        placeOrderButton.click();
    }

    public boolean isPlaceOrderVisible() {
        return placeOrderButton.isVisible();
    }
}
