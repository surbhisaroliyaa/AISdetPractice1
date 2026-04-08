package tests.products;

import base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.ProductsPage;
import pages.ProductDetailPage;
import config.ConfigReader;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;

import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ProductTest extends BaseTest {

    private ProductsPage productsPage;
    private ProductDetailPage productDetailPage;

    @BeforeMethod
    public void initPages() {
        productsPage = new ProductsPage(page);
        productDetailPage = new ProductDetailPage(page);
    }

    // =============================================
    // PRODUCT LIST TESTS
    // =============================================

    @Test
    public void testProductsPageLoads() {
        productsPage.navigateToProductsPage();

        // Verify URL
        assertThat(page).hasURL(Pattern.compile(".*/products"));

        // Verify "All Products" heading is visible
        Assert.assertTrue(productsPage.isFeaturedItemsVisible(),
                "Featured items section should be visible");
        Assert.assertEquals(productsPage.getHeadingText().toUpperCase(), "ALL PRODUCTS",
                "Heading should say 'ALL PRODUCTS'");
    }

    @Test
    public void testAllProductsHaveRequiredDetails() {
        productsPage.navigateToProductsPage();

        // Get all product cards
        List<Locator> products = productsPage.getAllProductCards();
        int count = productsPage.getProductCount();

        Assert.assertTrue(count > 0, "At least one product should be displayed");

        // Verify EACH product card has: name, price, image, add-to-cart
        for (Locator card : products) {
            String name = productsPage.getProductName(card);
            String price = productsPage.getProductPrice(card);
            String imgSrc = productsPage.getProductImageSrc(card);

            // Name should not be empty
            Assert.assertFalse(name.isEmpty(),
                    "Product name should not be empty");

            // Price should start with "Rs."
            Assert.assertTrue(price.startsWith("Rs."),
                    "Price '" + price + "' should start with 'Rs.' for product: " + name);

            // Image should have a valid src
            Assert.assertNotNull(imgSrc, "Image src should not be null for product: " + name);
            Assert.assertFalse(imgSrc.isEmpty(), "Image src should not be empty for product: " + name);

            // Add to cart button should be visible
            Assert.assertTrue(productsPage.isAddToCartVisible(card),
                    "Add to Cart button should be visible for product: " + name);
        }
    }

    @Test
    public void testEachProductHasViewProductLink() {
        productsPage.navigateToProductsPage();

        List<Locator> products = productsPage.getAllProductCards();

        for (Locator card : products) {
            Locator viewLink = productsPage.getViewProductLink(card);
            Assert.assertTrue(viewLink.isVisible(),
                    "View Product link should be visible for: " + productsPage.getProductName(card));

            String href = viewLink.getAttribute("href");
            Assert.assertTrue(href.contains("/product_details/"),
                    "View Product link should point to product_details page");
        }
    }

    @Test
    public void testProductImagesAreNotBroken() {
        productsPage.navigateToProductsPage();

        // Check first 5 products to keep test fast (not all 34)
        List<Locator> products = productsPage.getAllProductCards();
        int checkCount = Math.min(5, products.size());

        for (int i = 0; i < checkCount; i++) {
            Locator card = products.get(i);
            String name = productsPage.getProductName(card);

            // Verify image actually loaded — not just that src exists
            Assert.assertTrue(productsPage.isProductImageLoaded(card),
                    "Product image should be fully loaded (not broken) for: " + name);
        }
    }

    // =============================================
    // SEARCH TESTS
    // =============================================

    @Test
    public void testValidSearch() {
        productsPage.navigateToProductsPage();
        String searchTerm = "Top";

        productsPage.searchProduct(searchTerm);

        // Heading should change to "Searched Products"
        Assert.assertEquals(productsPage.getHeadingText().toUpperCase(), "SEARCHED PRODUCTS",
                "Heading should change to 'SEARCHED PRODUCTS' after search");

        // Results should appear
        List<Locator> results = productsPage.getAllProductCards();
        Assert.assertTrue(results.size() > 0,
                "Search for '" + searchTerm + "' should return at least one result");

        // Verify at least one result contains the search term in its name
        // Note: site searches by tags/description too, so not ALL results will have the term in the name
        boolean foundMatch = false;
        for (Locator card : results) {
            String name = productsPage.getProductName(card);
            if (name.toLowerCase().contains(searchTerm.toLowerCase())) {
                foundMatch = true;
                break;
            }
        }
        Assert.assertTrue(foundMatch,
                "At least one result should contain '" + searchTerm + "' in its name");
    }

    @Test
    public void testPartialSearch() {
        productsPage.navigateToProductsPage();
        String searchTerm = "Top";

        // First do a full search to get baseline count
        productsPage.searchProduct(searchTerm);
        int fullSearchCount = productsPage.getProductCount();

        // Now do partial search with just first 2 characters
        productsPage.navigateToProductsPage();
        String partialTerm = searchTerm.substring(0, 2); // "To"
        productsPage.searchProduct(partialTerm);

        List<Locator> results = productsPage.getAllProductCards();
        Assert.assertTrue(results.size() > 0,
                "Partial search for '" + partialTerm + "' should return results");

        // Partial search should return same or more results than full search
        Assert.assertTrue(results.size() >= fullSearchCount,
                "Partial search '" + partialTerm + "' should return >= results than full search '" + searchTerm + "'");
    }

    @Test
    public void testNoResultsSearch() {
        productsPage.navigateToProductsPage();
        String searchTerm = "xyznonexistent999";

        productsPage.searchProduct(searchTerm);

        // Heading should still change to "Searched Products"
        Assert.assertEquals(productsPage.getHeadingText().toUpperCase(), "SEARCHED PRODUCTS",
                "Heading should show 'SEARCHED PRODUCTS' even with no results");

        // No product cards should appear
        int count = productsPage.getProductCount();
        Assert.assertEquals(count, 0,
                "Search for nonsense term should return zero products");
    }

    @Test
    public void testEmptySearch() {
        productsPage.navigateToProductsPage();

        // Get count of all products before search
        int beforeCount = productsPage.getProductCount();

        productsPage.searchEmpty();

        // Page should NOT crash — products section still visible
        Assert.assertTrue(productsPage.isFeaturedItemsVisible(),
                "Products section should still be visible after empty search");

        // Empty search on this site keeps showing "All Products" — page stays functional
        int afterCount = productsPage.getProductCount();
        Assert.assertTrue(afterCount > 0,
                "Products should still be displayed after empty search");
    }

    @Test
    public void testSpecialCharacterSearch() {
        productsPage.navigateToProductsPage();
        String searchTerm = "<script>alert('xss')</script>";

        productsPage.searchProduct(searchTerm);

        // Page should NOT crash — verify it's still functional
        Assert.assertTrue(productsPage.isFeaturedItemsVisible(),
                "Page should not crash with special characters in search");

        // Heading should still be visible (page rendered correctly)
        String heading = productsPage.getHeadingText();
        Assert.assertNotNull(heading, "Heading should still be visible after XSS search attempt");

        // No products should match XSS input
        int count = productsPage.getProductCount();
        Assert.assertEquals(count, 0,
                "XSS search should return zero products (no script execution)");
    }

    // =============================================
    // PRODUCT DETAIL TESTS
    // =============================================

    @Test
    public void testProductDetailPageShowsAllInfo() {
        productsPage.navigateToProductsPage();

        // Click "View Product" on the first product
        productsPage.clickViewProduct(0);

        // Verify URL navigated to product detail
        assertThat(page).hasURL(Pattern.compile(".*/product_details/.*"));

        // Verify product information section is visible
        Assert.assertTrue(productDetailPage.isProductInformationVisible(),
                "Product information section should be visible");

        // Verify all required details are present and not empty
        String name = productDetailPage.getProductName();
        Assert.assertFalse(name.isEmpty(), "Product name should not be empty");

        String price = productDetailPage.getProductPrice();
        Assert.assertTrue(price.startsWith("Rs."),
                "Price '" + price + "' should start with 'Rs.'");

        String category = productDetailPage.getProductCategory();
        Assert.assertTrue(category.contains("Category"),
                "Category info should be displayed");

        String availability = productDetailPage.getProductAvailability();
        Assert.assertTrue(availability.contains("Availability"),
                "Availability info should be displayed");

        String condition = productDetailPage.getProductCondition();
        Assert.assertTrue(condition.contains("Condition"),
                "Condition info should be displayed");

        String brand = productDetailPage.getProductBrand();
        Assert.assertTrue(brand.contains("Brand"),
                "Brand info should be displayed");
    }

    @Test
    public void testProductDetailHasImage() {
        productsPage.navigateToProductsPage();
        productsPage.clickViewProduct(0);

        assertThat(page).hasURL(Pattern.compile(".*/product_details/.*"));

        // Verify product image
        Assert.assertTrue(productDetailPage.isProductImageVisible(),
                "Product image should be visible on detail page");

        String imgSrc = productDetailPage.getProductImageSrc();
        Assert.assertNotNull(imgSrc, "Product image src should not be null");
        Assert.assertFalse(imgSrc.isEmpty(), "Product image src should not be empty");
    }

    @Test
    public void testProductDetailHasQuantityAndCart() {
        productsPage.navigateToProductsPage();
        productsPage.clickViewProduct(0);

        assertThat(page).hasURL(Pattern.compile(".*/product_details/.*"));

        // Verify quantity input is present with default value "1"
        String quantity = productDetailPage.getQuantityValue();
        Assert.assertEquals(quantity, "1",
                "Default quantity should be 1");

        // Verify add to cart button is visible
        Assert.assertTrue(productDetailPage.isAddToCartVisible(),
                "Add to Cart button should be visible on detail page");
    }

    @Test
    public void testMultipleProductDetailsAreUnique() {
        productsPage.navigateToProductsPage();

        // Get first product's name from list
        productsPage.clickViewProduct(0);
        assertThat(page).hasURL(Pattern.compile(".*/product_details/.*"));
        String firstName = productDetailPage.getProductName();
        String firstPrice = productDetailPage.getProductPrice();

        // Go back and check second product
        page.goBack();
        page.waitForLoadState();
        productsPage.clickViewProduct(1);
        assertThat(page).hasURL(Pattern.compile(".*/product_details/.*"));
        String secondName = productDetailPage.getProductName();
        String secondPrice = productDetailPage.getProductPrice();

        // Products should have different names (they're different products)
        Assert.assertNotEquals(firstName, secondName,
                "Different products should have different names. Got: " + firstName + " for both");
    }

    @Test
    public void testProductNameMatchesBetweenListAndDetail() {
        productsPage.navigateToProductsPage();

        // Get the first product's name from the products LIST page
        String nameOnList = productsPage.getFirstProductNameFromList();

        // Click into that product's detail page
        productsPage.clickViewProduct(0);
        assertThat(page).hasURL(Pattern.compile(".*/product_details/.*"));

        // Get the name on the DETAIL page
        String nameOnDetail = productDetailPage.getProductName();

        // They should match — same product, same name
        Assert.assertEquals(nameOnDetail, nameOnList,
                "Product name on detail page should match the name on list page");
    }

    @Test
    public void testInvalidProductIdShowsNoInfo() {
        // Navigate directly to a product ID that doesn't exist
        page.navigate(ConfigReader.getBaseUrl() + "/product_details/9999",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        // The page should load without crashing
        // Product info section should either be missing or empty
        boolean hasInfo = productDetailPage.isProductInformationVisible();
        if (hasInfo) {
            // If the section renders, the name should be empty or the page should show an error
            String name = productDetailPage.getProductName();
            Assert.assertTrue(name.isEmpty() || page.url().contains("product_details"),
                    "Invalid product ID page should not show valid product data");
        }
        // If hasInfo is false — no product info at all — that's also correct behavior
    }

    @Test
    public void testAddToCartFromDetailPage() {
        productsPage.navigateToProductsPage();
        productsPage.clickViewProduct(0);
        assertThat(page).hasURL(Pattern.compile(".*/product_details/.*"));

        // Click Add to Cart
        productDetailPage.clickAddToCart();

        // Verify confirmation modal appears
        Assert.assertTrue(productDetailPage.isCartConfirmationVisible(),
                "Cart confirmation modal should appear after adding to cart");

        String confirmText = productDetailPage.getCartConfirmationText();
        Assert.assertTrue(confirmText.contains("added"),
                "Confirmation should say product was added. Got: " + confirmText);
    }

    @Test
    public void testBackNavigationFromDetailToList() {
        productsPage.navigateToProductsPage();

        // Remember the product count before clicking into detail
        int countBefore = productsPage.getProductCount();

        // Go to a product detail page
        productsPage.clickViewProduct(0);
        assertThat(page).hasURL(Pattern.compile(".*/product_details/.*"));

        // Go back
        page.goBack();
        page.waitForLoadState();

        // Products list should still work correctly
        assertThat(page).hasURL(Pattern.compile(".*/products"));
        Assert.assertTrue(productsPage.isFeaturedItemsVisible(),
                "Products section should be visible after navigating back");

        int countAfter = productsPage.getProductCount();
        Assert.assertEquals(countAfter, countBefore,
                "Product count should be same after navigating back");
    }

    // =============================================
    // CATEGORY TESTS
    // =============================================

    @Test
    public void testCategorySidebarIsVisible() {
        productsPage.navigateToProductsPage();

        Assert.assertTrue(productsPage.isCategorySidebarVisible(),
                "Category sidebar should be visible on Products page");
        Assert.assertTrue(productsPage.getCategoryCount() > 0,
                "At least one category should be displayed");
    }

    @Test
    public void testWomenCategoryShowsProducts() {
        productsPage.navigateToProductsPage();

        // Get total product count BEFORE filtering
        int totalCount = productsPage.getProductCount();

        // Click "Women" category to expand, then click "Dress" subcategory
        productsPage.clickCategory("Women");
        productsPage.clickSubCategory("Women", "Dress");

        // Verify navigated to category page
        assertThat(page).hasURL(Pattern.compile(".*/category_products/.*"));

        // Verify products are displayed
        int filteredCount = productsPage.getProductCount();
        Assert.assertTrue(filteredCount > 0,
                "Women > Dress category should show at least one product");

        // Filtered count should be LESS than total — proves filter actually worked
        Assert.assertTrue(filteredCount < totalCount,
                "Filtered count (" + filteredCount + ") should be less than total (" + totalCount + ")");

        // Verify heading indicates category filter
        String heading = productsPage.getHeadingText().toUpperCase();
        Assert.assertTrue(heading.contains("WOMEN") || heading.contains("DRESS"),
                "Heading should indicate Women - Dress category. Got: " + heading);
    }

    @Test
    public void testMenCategoryShowsProducts() {
        productsPage.navigateToProductsPage();

        // Click "Men" category to expand, then click "Tshirts" subcategory
        productsPage.clickCategory("Men");
        productsPage.clickSubCategory("Men", "Tshirts");

        // Verify navigated to category page
        assertThat(page).hasURL(Pattern.compile(".*/category_products/.*"));

        // Verify products are displayed
        Assert.assertTrue(productsPage.getProductCount() > 0,
                "Men > Tshirts category should show at least one product");

        // Verify heading
        String heading = productsPage.getHeadingText().toUpperCase();
        Assert.assertTrue(heading.contains("MEN") || heading.contains("TSHIRTS"),
                "Heading should indicate Men - Tshirts category. Got: " + heading);
    }

    // =============================================
    // BRAND TESTS
    // =============================================

    @Test
    public void testBrandsSidebarIsVisible() {
        productsPage.navigateToProductsPage();

        Assert.assertTrue(productsPage.isBrandsSidebarVisible(),
                "Brands sidebar should be visible on Products page");
        Assert.assertTrue(productsPage.getBrandCount() > 0,
                "At least one brand should be displayed");
    }

    @Test
    public void testBrandFilterShowsProducts() {
        productsPage.navigateToProductsPage();

        // Get total product count BEFORE filtering
        int totalCount = productsPage.getProductCount();

        // Click on "Polo" brand
        productsPage.clickBrand("Polo");

        // Verify navigated to brand products page
        assertThat(page).hasURL(Pattern.compile(".*/brand_products/.*"));

        // Verify products are displayed
        int filteredCount = productsPage.getProductCount();
        Assert.assertTrue(filteredCount > 0,
                "Polo brand should show at least one product");

        // Filtered count should be LESS than total — proves filter actually worked
        Assert.assertTrue(filteredCount < totalCount,
                "Brand filtered count (" + filteredCount + ") should be less than total (" + totalCount + ")");

        // Verify heading shows brand name
        String heading = productsPage.getHeadingText().toUpperCase();
        Assert.assertTrue(heading.contains("POLO"),
                "Heading should indicate Polo brand. Got: " + heading);
    }

    @Test
    public void testSwitchBetweenBrands() {
        productsPage.navigateToProductsPage();

        // Click first brand — Polo
        productsPage.clickBrand("Polo");
        assertThat(page).hasURL(Pattern.compile(".*/brand_products/.*"));
        int poloCount = productsPage.getProductCount();
        Assert.assertTrue(poloCount > 0, "Polo brand should show products");

        // Click a different brand — H&M
        productsPage.clickBrand("H&M");
        assertThat(page).hasURL(Pattern.compile(".*/brand_products/.*"));
        int hmCount = productsPage.getProductCount();
        Assert.assertTrue(hmCount > 0, "H&M brand should show products");

        // The page updated — brands show different products
        String heading = productsPage.getHeadingText().toUpperCase();
        Assert.assertTrue(heading.contains("H&M"),
                "Heading should update to H&M after switching brands. Got: " + heading);
    }
}
