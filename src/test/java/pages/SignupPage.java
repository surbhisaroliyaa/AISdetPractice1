package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class SignupPage {
    private final Page page;

    // Account Information
    private final Locator titleMr;
    private final Locator titleMrs;
    private final Locator nameInput;
    private final Locator passwordInput;
    private final Locator daysDropdown;
    private final Locator monthsDropdown;
    private final Locator yearsDropdown;
    private final Locator newsletterCheckbox;
    private final Locator specialOffersCheckbox;

    // Address Information
    private final Locator firstNameInput;
    private final Locator lastNameInput;
    private final Locator companyInput;
    private final Locator addressInput;
    private final Locator address2Input;
    private final Locator countryDropdown;
    private final Locator stateInput;
    private final Locator cityInput;
    private final Locator zipcodeInput;
    private final Locator mobileNumberInput;

    // Submit
    private final Locator createAccountButton;

    // Headings
    private final Locator accountInfoHeading;
    private final Locator addressInfoHeading;

    public SignupPage(Page page) {
        this.page = page;

        // Account Information
        titleMr = page.locator("#id_gender1");
        titleMrs = page.locator("#id_gender2");
        nameInput = page.locator("[data-qa='name']");
        passwordInput = page.locator("[data-qa='password']");
        daysDropdown = page.locator("[data-qa='days']");
        monthsDropdown = page.locator("[data-qa='months']");
        yearsDropdown = page.locator("[data-qa='years']");
        newsletterCheckbox = page.locator("#newsletter");
        specialOffersCheckbox = page.locator("#optin");

        // Address Information
        firstNameInput = page.locator("[data-qa='first_name']");
        lastNameInput = page.locator("[data-qa='last_name']");
        companyInput = page.locator("[data-qa='company']");
        addressInput = page.locator("[data-qa='address']");
        address2Input = page.locator("[data-qa='address2']");
        countryDropdown = page.locator("[data-qa='country']");
        stateInput = page.locator("[data-qa='state']");
        cityInput = page.locator("[data-qa='city']");
        zipcodeInput = page.locator("[data-qa='zipcode']");
        mobileNumberInput = page.locator("[data-qa='mobile_number']");

        // Submit
        createAccountButton = page.locator("[data-qa='create-account']");

        // Headings
        accountInfoHeading = page.locator("h2 b:has-text('Enter Account Information')");
        addressInfoHeading = page.locator("h2 b:has-text('Address Information')");
    }

    public boolean isAccountInfoVisible() {
        return accountInfoHeading.isVisible();
    }

    public boolean isAddressInfoVisible() {
        return addressInfoHeading.isVisible();
    }

    public void selectTitleMr() {
        titleMr.check();
    }

    public void selectTitleMrs() {
        titleMrs.check();
    }

    public void fillPassword(String password) {
        passwordInput.fill(password);
    }

    public void selectDateOfBirth(String day, String month, String year) {
        daysDropdown.selectOption(day);
        monthsDropdown.selectOption(month);
        yearsDropdown.selectOption(year);
    }

    public void checkNewsletter() {
        newsletterCheckbox.check();
    }

    public void checkSpecialOffers() {
        specialOffersCheckbox.check();
    }

    public void fillAddressDetails(String firstName, String lastName, String company,
                                   String address, String address2, String country,
                                   String state, String city, String zipcode,
                                   String mobileNumber) {
        firstNameInput.fill(firstName);
        lastNameInput.fill(lastName);
        companyInput.fill(company);
        addressInput.fill(address);
        address2Input.fill(address2);
        countryDropdown.selectOption(country);
        stateInput.fill(state);
        cityInput.fill(city);
        zipcodeInput.fill(zipcode);
        mobileNumberInput.fill(mobileNumber);
    }

    public void clickCreateAccount() {
        createAccountButton.click();
    }

    public void fillFullRegistration(String password, String day, String month, String year,
                                     String firstName, String lastName, String company,
                                     String address, String country, String state,
                                     String city, String zipcode, String mobileNumber) {
        selectTitleMr();
        fillPassword(password);
        selectDateOfBirth(day, month, year);
        checkNewsletter();
        fillAddressDetails(firstName, lastName, company, address, "",
                country, state, city, zipcode, mobileNumber);
        clickCreateAccount();
    }
}
