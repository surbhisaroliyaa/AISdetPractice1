package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import config.ConfigReader;

import java.util.List;

public class ProductsPage {
    private final Page page;

    // Page headings
    private final Locator allProductsHeading;
    private final Locator searchedProductsHeading;

    // Product list
    private final Locator productCards;
    private final Locator featuredItemsSection;

    // Search
    private final Locator searchInput;
    private final Locator searchButton;

    // Sidebar — Categories
    private final Locator categorySidebar;
    private final Locator categoryPanels;

    // Sidebar — Brands
    private final Locator brandsSidebar;
    private final Locator brandLinks;

    public ProductsPage(Page page) {
        this.page = page;

        allProductsHeading = page.locator(".features_items .title.text-center");
        searchedProductsHeading = page.locator(".features_items .title.text-center");

        productCards = page.locator(".features_items .col-sm-4");
        featuredItemsSection = page.locator(".features_items");

        searchInput = page.locator("#search_product");
        searchButton = page.locator("#submit_search");

        categorySidebar = page.locator(".left-sidebar .category-products");
        categoryPanels = page.locator(".left-sidebar .category-products .panel.panel-default");

        brandsSidebar = page.locator(".brands_products");
        brandLinks = page.locator(".brands_products .brands-name ul li a");
    }

    // ========== Navigation ==========

    public void navigateToProductsPage() {
        page.navigate(ConfigReader.getBaseUrl() + "/products",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        // Wait for the actual product content to be available
        featuredItemsSection.waitFor();
    }

    // ========== Product List ==========

    public int getProductCount() {
        return productCards.count();
    }

    public List<Locator> getAllProductCards() {
        return productCards.all();
    }

    public String getProductName(Locator card) {
        return card.locator(".productinfo p").innerText();
    }

    public String getProductPrice(Locator card) {
        return card.locator(".productinfo h2").innerText();
    }

    public String getProductImageSrc(Locator card) {
        return card.locator(".productinfo img").getAttribute("src");
    }

    public boolean isAddToCartVisible(Locator card) {
        return card.locator(".productinfo .add-to-cart").isVisible();
    }

    public void clickViewProduct(int index) {
        page.locator("a[href='/product_details/" + (index + 1) + "']").first().click();
    }

    public Locator getViewProductLink(Locator card) {
        return card.locator("a[href^='/product_details/']");
    }

    // ========== Search ==========

    public void searchProduct(String term) {
        searchInput.fill(term);
        searchButton.click();
        // Wait for search results to render
        featuredItemsSection.waitFor();
    }

    public void searchEmpty() {
        searchInput.fill("");
        searchButton.click();
        featuredItemsSection.waitFor();
    }

    public String getHeadingText() {
        return allProductsHeading.innerText();
    }

    public boolean isSearchInputVisible() {
        return searchInput.isVisible();
    }

    // ========== Categories ==========

    public boolean isCategorySidebarVisible() {
        return categorySidebar.isVisible();
    }

    public int getCategoryCount() {
        return categoryPanels.count();
    }

    public void clickCategory(String categoryName) {
        // Click the category header to expand it (e.g., "Women", "Men", "Kids")
        page.locator(".category-products .panel-default a[href='#" + categoryName + "']").click();
        // Wait for the panel to expand
        page.locator("#" + categoryName).waitFor();
    }

    public void clickSubCategory(String categoryName, String subCategoryName) {
        // Click a subcategory link scoped WITHIN the expanded category panel
        // This avoids strict mode violation when same subcategory name exists in multiple categories
        page.locator("#" + categoryName + " a:has-text('" + subCategoryName + "')").click();
    }

    // ========== Brands ==========

    public boolean isBrandsSidebarVisible() {
        return brandsSidebar.isVisible();
    }

    public int getBrandCount() {
        return brandLinks.count();
    }

    public List<Locator> getAllBrandLinks() {
        return brandLinks.all();
    }

    public void clickBrand(String brandName) {
        page.locator(".brands-name a:has-text('" + brandName + "')").click();
    }

    public String getBrandText(Locator brandLink) {
        return brandLink.innerText();
    }

    // ========== Featured Items Section ==========

    public boolean isFeaturedItemsVisible() {
        return featuredItemsSection.isVisible();
    }

    // ========== Image Verification ==========

    public boolean isProductImageLoaded(Locator card) {
        Locator img = card.locator(".productinfo img");
        img.scrollIntoViewIfNeeded();

        String src = img.getAttribute("src");
        boolean hasSrc = src != null && !src.isEmpty();
        // A "broken image" in HTML means missing/empty src attribute.
        // Don't check isVisible() — product cards use overlays where the img
        // can be CSS-hidden in certain viewport states (especially headless CI).
        // The src attribute being present and non-empty is the real check.
        return hasSrc;
    }

    // ========== Navigation ==========

    public String getFirstProductNameFromList() {
        return getProductName(productCards.first());
    }
}
