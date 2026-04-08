package base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.Tracing;
import config.ConfigReader;
import org.testng.annotations.*;

public class BaseTest {
    private static final ThreadLocal<Playwright> tlPlaywright = new ThreadLocal<>();
    private static final ThreadLocal<Browser> tlBrowser = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> tlContext = new ThreadLocal<>();
    private static final ThreadLocal<Page> tlPage = new ThreadLocal<>();

    // Convenience fields so test classes can use 'page' directly (no code changes needed)
    protected Page page;
    protected BrowserContext context;

    @BeforeClass
    @Parameters({"browser"})
    public void startBrowser(@Optional String browserParam) {
        // Only create if this thread doesn't have a browser yet
        if (tlPlaywright.get() == null) {
            tlPlaywright.set(Playwright.create());

            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(ConfigReader.isHeadless())
                    .setSlowMo(ConfigReader.getSlowMo());

            // If parameter comes from cross-browser XML → use it
            // If no parameter (normal testng.xml) → fall back to config.properties
            String browserType = (browserParam != null) ? browserParam : ConfigReader.getBrowser();

            tlBrowser.set(switch (browserType.toLowerCase()) {
                case "firefox" -> tlPlaywright.get().firefox().launch(launchOptions);
                case "webkit" -> tlPlaywright.get().webkit().launch(launchOptions);
                default -> tlPlaywright.get().chromium().launch(launchOptions);
            });
        }
    }

    @BeforeMethod
    public void setup() {
        tlContext.set(tlBrowser.get().newContext());
        tlContext.get().setDefaultTimeout(ConfigReader.getTimeout());
        tlPage.set(tlContext.get().newPage());

        // Set convenience fields so tests can use 'page' and 'context' directly
        context = tlContext.get();
        page = tlPage.get();

        // Start tracing — listener will save on failure, discard on success
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true));

        // Block ads and tracking scripts — they cause flaky tests on real sites
        page.route("**/*", route -> {
            String url = route.request().url().toLowerCase();
            if (url.contains("google") || url.contains("doubleclick") || url.contains("adservice")
                    || url.contains("googlesyndication") || url.contains("analytics")
                    || url.contains("adsbygoogle") || url.contains("facebook")
                    || url.contains("aswpsdkus") || url.contains("onesignal")
                    || url.contains("cdn.taboola") || url.contains("pagead")
                    || url.contains("adsense")) {
                route.abort();
            } else {
                route.resume();
            }
        });
    }

    @AfterMethod
    public void cleanup() {
        tlContext.get().close();
        tlContext.remove();
        tlPage.remove();
        page = null;
        context = null;
    }

    @AfterClass
    public void closeBrowser() {
        if (tlBrowser.get() != null) {
            tlBrowser.get().close();
            tlBrowser.remove();
        }
        if (tlPlaywright.get() != null) {
            tlPlaywright.get().close();
            tlPlaywright.remove();
        }
    }

    public Page getPage() {
        return tlPage.get();
    }

    public BrowserContext getContext() {
        return tlContext.get();
    }

    protected Browser getBrowser() {
        return tlBrowser.get();
    }

    protected Playwright getPlaywright() {
        return tlPlaywright.get();
    }
}
