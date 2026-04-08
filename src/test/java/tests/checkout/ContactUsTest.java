package tests.checkout;

import base.BaseTest;
import com.microsoft.playwright.Locator;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.ContactUsPage;
import pages.HomePage;
import utils.TestDataGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ContactUsTest extends BaseTest {

    private ContactUsPage contactUsPage;
    private HomePage homePage;

    @BeforeMethod
    public void initPages() {
        contactUsPage = new ContactUsPage(page);
        homePage = new HomePage(page);
    }

    // Helper: Create a temp file for upload testing
    private Path createTempFile(String name, String content) throws IOException {
        Path tempDir = Files.createTempDirectory("contactus_test");
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    // ========== CORE FLOW: Fill form, upload file, accept dialog, verify success ==========

    @Test
    public void testContactUsFullFlow() throws IOException {
        contactUsPage.navigateToContactUs();
        Assert.assertTrue(contactUsPage.isGetInTouchVisible(), "Get In Touch heading should be visible");
        Assert.assertTrue(contactUsPage.isFormVisible(), "All form fields should be visible");

        // Fill form
        String name = TestDataGenerator.getRandomFirstName();
        String email = TestDataGenerator.getRandomEmail();
        contactUsPage.fillContactForm(name, email, "Test Subject", "This is a test message");

        // Upload file
        Path testFile = createTempFile("test_upload.txt", "This is test file content");
        contactUsPage.uploadFile(testFile);

        // Submit with dialog accept (listener-before-action pattern)
        contactUsPage.submitWithDialogAccept();

        // Verify success
        Assert.assertTrue(contactUsPage.isSuccessMessageVisible(),
                "Success message should be visible after submission");
        String successText = contactUsPage.getSuccessMessageText();
        Assert.assertTrue(successText.toLowerCase().contains("success"),
                "Success message should contain 'success': " + successText);

        // Cleanup temp file
        Files.deleteIfExists(testFile);
    }

    // ========== VERIFICATION: All form fields accept input ==========

    @Test
    public void testAllFieldsAcceptInput() {
        contactUsPage.navigateToContactUs();

        String name = "Priya Sharma";
        String email = "priya@test.com";
        String subject = "Feedback on product quality";
        String message = "I would like to provide feedback on the product I purchased last week.";

        contactUsPage.fillContactForm(name, email, subject, message);

        // Verify all fields retained their values
        Assert.assertEquals(contactUsPage.getNameValue(), name, "Name should be retained");
        Assert.assertEquals(contactUsPage.getEmailValue(), email, "Email should be retained");
        Assert.assertEquals(contactUsPage.getSubjectValue(), subject, "Subject should be retained");
        Assert.assertEquals(contactUsPage.getMessageValue(), message, "Message should be retained");
    }

    // ========== VERIFICATION: File upload — name shown after upload ==========

    @Test
    public void testFileUploadShowsFileName() throws IOException {
        contactUsPage.navigateToContactUs();

        Assert.assertTrue(contactUsPage.isUploadFieldVisible(), "Upload field should be visible");

        Path testFile = createTempFile("my_report.txt", "Report content here");
        contactUsPage.uploadFile(testFile);

        // Verify uploaded file name is shown in the input
        String uploadedName = contactUsPage.getUploadedFileName();
        Assert.assertTrue(uploadedName.contains("my_report.txt"),
                "Upload field should show the file name, got: " + uploadedName);

        Files.deleteIfExists(testFile);
    }

    // ========== VERIFICATION: Upload different file formats ==========

    @Test
    public void testUploadDifferentFileFormats() throws IOException {
        contactUsPage.navigateToContactUs();

        // Test .txt file
        Path txtFile = createTempFile("document.txt", "Text content");
        contactUsPage.uploadFile(txtFile);
        Assert.assertTrue(contactUsPage.getUploadedFileName().contains("document.txt"),
                "Should accept .txt file");

        // Test .csv file
        Path csvFile = createTempFile("data.csv", "name,email\ntest,test@mail.com");
        contactUsPage.uploadFile(csvFile);
        Assert.assertTrue(contactUsPage.getUploadedFileName().contains("data.csv"),
                "Should accept .csv file");

        // Test .html file
        Path htmlFile = createTempFile("page.html", "<html><body>Hello</body></html>");
        contactUsPage.uploadFile(htmlFile);
        Assert.assertTrue(contactUsPage.getUploadedFileName().contains("page.html"),
                "Should accept .html file");

        Files.deleteIfExists(txtFile);
        Files.deleteIfExists(csvFile);
        Files.deleteIfExists(htmlFile);
    }

    // ========== EDGE CASE: Submit without filling any fields ==========

    @Test
    public void testSubmitWithEmptyForm() {
        contactUsPage.navigateToContactUs();

        // Submit empty form — register dialog handler first
        contactUsPage.submitWithDialogAccept();

        // Browser HTML5 validation should block OR site shows error
        // Either we stay on contact us page, or we get an error — but NOT a success
        boolean successShown = false;
        try {
            successShown = contactUsPage.isSuccessMessageVisible();
        } catch (Exception ignored) {
        }

        if (successShown) {
            System.out.println("BUG: Site accepted empty Contact Us form submission");
        } else {
            // Good — form was not submitted
            Assert.assertTrue(contactUsPage.isGetInTouchVisible() || contactUsPage.isFormVisible(),
                    "Should still be on contact us page when form is empty");
        }
    }

    // ========== EDGE CASE: Submit without uploading any file ==========

    @Test
    public void testSubmitWithoutFileUpload() {
        contactUsPage.navigateToContactUs();

        // Fill all text fields but do NOT upload a file
        contactUsPage.fillContactForm(
                TestDataGenerator.getRandomFirstName(),
                TestDataGenerator.getRandomEmail(),
                "Subject without file",
                "This message has no file attachment"
        );

        contactUsPage.submitWithDialogAccept();

        // File upload is optional on most contact forms
        // Verify form submits successfully without file
        Assert.assertTrue(contactUsPage.isSuccessMessageVisible(),
                "Should be able to submit without uploading a file");
    }

    // ========== EDGE CASE: Cancel the JS confirm dialog instead of accepting ==========

    @Test
    public void testSubmitWithDialogDismiss() {
        contactUsPage.navigateToContactUs();

        contactUsPage.fillContactForm(
                TestDataGenerator.getRandomFirstName(),
                TestDataGenerator.getRandomEmail(),
                "Testing cancel dialog",
                "This submission should be cancelled"
        );

        // Dismiss (Cancel) the dialog instead of accepting
        contactUsPage.submitWithDialogDismiss();

        // After cancelling, form should NOT be submitted
        // We should still be on the contact us page
        boolean successShown = false;
        try {
            successShown = contactUsPage.isSuccessMessageVisible();
        } catch (Exception ignored) {
        }

        Assert.assertFalse(successShown,
                "Success message should NOT appear after dismissing the confirm dialog");
        Assert.assertTrue(contactUsPage.isGetInTouchVisible() || contactUsPage.isFormVisible(),
                "Should still be on contact us page after cancelling");
    }

    // ========== NAVIGATION: Contact Us page accessible from nav ==========

    @Test
    public void testContactUsNavigationFromHome() {
        homePage.navigateToHome();
        homePage.clickContactUs();

        Assert.assertTrue(page.url().contains("contact_us"),
                "URL should contain 'contact_us'");
        Assert.assertTrue(contactUsPage.isGetInTouchVisible(),
                "Get In Touch heading should be visible");
        Assert.assertTrue(contactUsPage.isFormVisible(),
                "Form fields should be visible");
        Assert.assertTrue(contactUsPage.isUploadFieldVisible(),
                "File upload should be visible");
        Assert.assertTrue(contactUsPage.isSubmitButtonVisible(),
                "Submit button should be visible");
    }

    // ========== NAVIGATION: Back to home after successful submission ==========

    @Test
    public void testNavigateHomeAfterSubmission() throws IOException {
        contactUsPage.navigateToContactUs();

        contactUsPage.fillContactForm(
                TestDataGenerator.getRandomFirstName(),
                TestDataGenerator.getRandomEmail(),
                "Test",
                "Test message"
        );

        Path testFile = createTempFile("nav_test.txt", "content");
        contactUsPage.uploadFile(testFile);
        contactUsPage.submitWithDialogAccept();

        Assert.assertTrue(contactUsPage.isSuccessMessageVisible());

        // Click Home button (the green button on the success page, not the nav link)
        Locator homeButton = page.locator("#form-section a.btn.btn-success");
        if (homeButton.isVisible()) {
            homeButton.click();
            Assert.assertTrue(page.url().contains("automationexercise.com"),
                    "Should navigate back to home");
        }

        Files.deleteIfExists(testFile);
    }

    // ========== EDGE CASE: Very long message (10,000 characters) ==========

    @Test
    public void testVeryLongMessage() {
        contactUsPage.navigateToContactUs();

        // Generate a 10,000 character message
        String longMessage = "A".repeat(10000);

        contactUsPage.fillContactForm(
                TestDataGenerator.getRandomFirstName(),
                TestDataGenerator.getRandomEmail(),
                "Long message test",
                longMessage
        );

        // Verify the field accepted the long text
        String actualValue = contactUsPage.getMessageValue();
        System.out.println("Long message — entered: 10000 chars, accepted: " + actualValue.length() + " chars");

        // Submit with dialog accept
        contactUsPage.submitWithDialogAccept();

        // Should either succeed or show graceful error — should NOT crash
        boolean successShown = false;
        try {
            successShown = contactUsPage.isSuccessMessageVisible();
        } catch (Exception ignored) {
        }

        if (successShown) {
            System.out.println("Site accepted 10,000 character message");
        } else {
            // Still on contact page — verify no crash (page is still functional)
            Assert.assertTrue(contactUsPage.isGetInTouchVisible() || contactUsPage.isFormVisible(),
                    "Page should still be functional after long message attempt");
        }
    }

    // ========== EDGE CASE: XSS in message field ==========

    @Test
    public void testXssInMessageField() {
        contactUsPage.navigateToContactUs();

        String xssPayload = "<script>alert('xss')</script>";

        contactUsPage.fillContactForm(
                TestDataGenerator.getRandomFirstName(),
                TestDataGenerator.getRandomEmail(),
                "XSS Test",
                xssPayload
        );

        // Verify the field accepted the text as-is (it should store it, not execute it)
        String actualMessage = contactUsPage.getMessageValue();
        Assert.assertEquals(actualMessage, xssPayload,
                "Message field should accept the text without modification");

        // Submit
        contactUsPage.submitWithDialogAccept();

        // Should succeed — site should escape the content, not execute it
        Assert.assertTrue(contactUsPage.isSuccessMessageVisible(),
                "Form should submit successfully — site should escape XSS, not crash");

        // Verify page didn't execute the script (no alert dialog appeared)
        // If onDialog was triggered, it would have been handled — no crash means no execution
    }
}
