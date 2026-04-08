package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import config.ConfigReader;

import java.nio.file.Path;

public class ContactUsPage {
    private final Page page;

    // Form fields
    private final Locator nameInput;
    private final Locator emailInput;
    private final Locator subjectInput;
    private final Locator messageInput;
    private final Locator uploadFileInput;
    private final Locator submitButton;

    // Page elements
    private final Locator getInTouchHeading;
    private final Locator successMessage;

    public ContactUsPage(Page page) {
        this.page = page;

        nameInput = page.locator("[data-qa='name']");
        emailInput = page.locator("[data-qa='email']");
        subjectInput = page.locator("[data-qa='subject']");
        messageInput = page.locator("[data-qa='message']");
        uploadFileInput = page.locator("input[name='upload_file']");
        submitButton = page.locator("[data-qa='submit-button']");

        getInTouchHeading = page.locator("h2:has-text('Get In Touch')");
        successMessage = page.locator("#contact-page .status.alert-success");
    }

    // ========== Navigation ==========

    public void navigateToContactUs() {
        page.navigate(ConfigReader.getBaseUrl() + "/contact_us");
    }

    // ========== Page State ==========

    public boolean isGetInTouchVisible() {
        return getInTouchHeading.isVisible();
    }

    public boolean isFormVisible() {
        return nameInput.isVisible() && emailInput.isVisible()
                && subjectInput.isVisible() && messageInput.isVisible();
    }

    // ========== Fill Form ==========

    public void fillName(String name) {
        nameInput.fill(name);
    }

    public void fillEmail(String email) {
        emailInput.fill(email);
    }

    public void fillSubject(String subject) {
        subjectInput.fill(subject);
    }

    public void fillMessage(String message) {
        messageInput.fill(message);
    }

    public void fillContactForm(String name, String email, String subject, String message) {
        fillName(name);
        fillEmail(email);
        fillSubject(subject);
        fillMessage(message);
    }

    // ========== File Upload ==========

    public void uploadFile(Path filePath) {
        uploadFileInput.setInputFiles(filePath);
    }

    public String getUploadedFileName() {
        return uploadFileInput.inputValue();
    }

    public boolean isUploadFieldVisible() {
        return uploadFileInput.isVisible();
    }

    // ========== Submit ==========

    /**
     * Clicks submit. IMPORTANT: Register page.onDialog() BEFORE calling this
     * if you need to handle the confirm dialog.
     */
    public void clickSubmit() {
        submitButton.click();
    }

    /**
     * Submits form with dialog handler that accepts the JS confirm.
     * Sets up listener BEFORE the click (listener-before-action pattern).
     */
    public void submitWithDialogAccept() {
        page.onDialog(dialog -> dialog.accept());
        submitButton.click();
    }

    /**
     * Submits form with dialog handler that dismisses (cancels) the JS confirm.
     */
    public void submitWithDialogDismiss() {
        page.onDialog(dialog -> dialog.dismiss());
        submitButton.click();
    }

    public boolean isSubmitButtonVisible() {
        return submitButton.isVisible();
    }

    // ========== Success Verification ==========

    public boolean isSuccessMessageVisible() {
        return successMessage.isVisible();
    }

    public String getSuccessMessageText() {
        return successMessage.innerText();
    }

    // ========== Field Values (for verification) ==========

    public String getNameValue() {
        return nameInput.inputValue();
    }

    public String getEmailValue() {
        return emailInput.inputValue();
    }

    public String getSubjectValue() {
        return subjectInput.inputValue();
    }

    public String getMessageValue() {
        return messageInput.inputValue();
    }
}
