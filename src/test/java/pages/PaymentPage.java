package pages;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.nio.file.Path;

public class PaymentPage {
    private final Page page;

    // Card details form
    private final Locator nameOnCardInput;
    private final Locator cardNumberInput;
    private final Locator cvcInput;
    private final Locator expiryMonthInput;
    private final Locator expiryYearInput;
    private final Locator payAndConfirmButton;

    // Payment heading
    private final Locator paymentHeading;

    // Order Confirmation
    private final Locator orderConfirmedMessage;
    private final Locator downloadInvoiceButton;
    private final Locator continueButton;

    public PaymentPage(Page page) {
        this.page = page;

        // Payment form
        nameOnCardInput = page.locator("[data-qa='name-on-card']");
        cardNumberInput = page.locator("[data-qa='card-number']");
        cvcInput = page.locator("[data-qa='cvc']");
        expiryMonthInput = page.locator("[data-qa='expiry-month']");
        expiryYearInput = page.locator("[data-qa='expiry-year']");
        payAndConfirmButton = page.locator("[data-qa='pay-button']");

        // Heading
        paymentHeading = page.locator(".heading2 b:has-text('Payment')");

        // Confirmation page (after successful payment)
        orderConfirmedMessage = page.locator("[data-qa='order-placed'],.order-placed");
        downloadInvoiceButton = page.locator(".btn.check_out, a.btn.btn-default.check_out");
        continueButton = page.locator("[data-qa='continue-button']");
    }

    // ========== Page State ==========

    public boolean isPaymentFormVisible() {
        return nameOnCardInput.isVisible();
    }

    public boolean isPaymentHeadingVisible() {
        return paymentHeading.isVisible();
    }

    // ========== Fill Card Details ==========

    public void fillCardDetails(String nameOnCard, String cardNumber, String cvc,
                                String expiryMonth, String expiryYear) {
        nameOnCardInput.fill(nameOnCard);
        cardNumberInput.fill(cardNumber);
        cvcInput.fill(cvc);
        expiryMonthInput.fill(expiryMonth);
        expiryYearInput.fill(expiryYear);
    }

    public void clickPayAndConfirm() {
        payAndConfirmButton.click();
    }

    public void payWithCard(String nameOnCard, String cardNumber, String cvc,
                            String expiryMonth, String expiryYear) {
        fillCardDetails(nameOnCard, cardNumber, cvc, expiryMonth, expiryYear);
        clickPayAndConfirm();
    }

    // ========== Individual Field Access (for edge case tests) ==========

    public void fillNameOnCard(String name) {
        nameOnCardInput.fill(name);
    }

    public void fillCardNumber(String number) {
        cardNumberInput.fill(number);
    }

    public void fillCvc(String cvc) {
        cvcInput.fill(cvc);
    }

    public void fillExpiryMonth(String month) {
        expiryMonthInput.fill(month);
    }

    public void fillExpiryYear(String year) {
        expiryYearInput.fill(year);
    }

    public String getNameOnCardValue() {
        return nameOnCardInput.inputValue();
    }

    public String getCardNumberValue() {
        return cardNumberInput.inputValue();
    }

    public String getCvcValue() {
        return cvcInput.inputValue();
    }

    public String getExpiryMonthValue() {
        return expiryMonthInput.inputValue();
    }

    public String getExpiryYearValue() {
        return expiryYearInput.inputValue();
    }

    public boolean isPayAndConfirmVisible() {
        return payAndConfirmButton.isVisible();
    }

    // ========== Order Confirmation ==========

    public boolean isOrderConfirmedVisible() {
        return orderConfirmedMessage.isVisible();
    }

    public String getOrderConfirmedText() {
        return page.locator("p:has-text('Congratulations')").innerText();
    }

    public boolean isDownloadInvoiceVisible() {
        return downloadInvoiceButton.isVisible();
    }

    /**
     * Downloads invoice using listener-before-action pattern.
     * Registers download listener FIRST, then clicks the button.
     */
    public Download downloadInvoice() {
        return page.waitForDownload(() -> {
            downloadInvoiceButton.click();
        });
    }

    /**
     * Gets the file path of a downloaded invoice
     */
    public Path getDownloadedFilePath(Download download) {
        return download.path();
    }

    /**
     * Gets the suggested filename of the download
     */
    public String getDownloadedFileName(Download download) {
        return download.suggestedFilename();
    }

    public void clickContinue() {
        continueButton.click();
    }

    public boolean isContinueButtonVisible() {
        return continueButton.isVisible();
    }
}
