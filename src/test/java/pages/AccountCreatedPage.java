package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class AccountCreatedPage {
    private final Page page;

    private final Locator accountCreatedHeading;
    private final Locator continueButton;

    public AccountCreatedPage(Page page) {
        this.page = page;
        accountCreatedHeading = page.locator("[data-qa='account-created']");
        continueButton = page.locator("[data-qa='continue-button']");
    }

    public boolean isAccountCreatedVisible() {
        return accountCreatedHeading.isVisible();
    }

    public String getHeadingText() {
        return accountCreatedHeading.innerText();
    }

    public void clickContinue() {
        continueButton.click();
    }
}
