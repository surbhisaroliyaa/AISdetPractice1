package listeners;

import base.BaseTest;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.microsoft.playwright.Page;
import config.ConfigReader;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestListener implements ITestListener {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> currentTest = new ThreadLocal<>();

    @Override
    public synchronized void onStart(ITestContext context) {
        // Guard: create reporter ONCE even though onStart fires per <test> tag in testng.xml
        if (extent != null) return;

        ExtentSparkReporter spark = new ExtentSparkReporter("reports/TestReport.html");
        spark.config().setDocumentTitle("AISdetPractice1 — Test Report");
        spark.config().setReportName("Automation Exercise Test Results");

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Browser", ConfigReader.getBrowser());
        extent.setSystemInfo("Headless", String.valueOf(ConfigReader.isHeadless()));
        extent.setSystemInfo("Base URL", ConfigReader.getBaseUrl());
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        extent.setSystemInfo("Java", System.getProperty("java.version"));
    }

    @Override
    public void onTestStart(ITestResult result) {
        // Create one ExtentTest per test method — stored in ThreadLocal for parallel safety
        ExtentTest test = extent.createTest(
                result.getMethod().getMethodName(),
                result.getMethod().getDescription()
        );
        currentTest.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        currentTest.get().pass("Test passed.");

        // Discard trace — no need to save for passing tests
        stopTracing(result, false);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = currentTest.get();
        test.fail("Test failed: " + result.getThrowable().getMessage());

        // Take screenshot — null check the page first
        Object instance = result.getInstance();
        if (instance instanceof BaseTest baseTest) {
            Page page = baseTest.getPage();
            if (page != null) {
                try {
                    // Unique filename: MethodName_timestamp.png
                    String screenshotName = result.getMethod().getMethodName()
                            + "_" + System.currentTimeMillis() + ".png";
                    Path screenshotDir = Paths.get("reports", "screenshots");
                    Files.createDirectories(screenshotDir);
                    Path screenshotPath = screenshotDir.resolve(screenshotName);

                    // Capture full page screenshot
                    page.screenshot(new Page.ScreenshotOptions()
                            .setPath(screenshotPath)
                            .setFullPage(true));

                    // Attach to report — use relative path so HTML can find the image
                    test.addScreenCaptureFromPath("screenshots/" + screenshotName);
                } catch (IOException e) {
                    test.warning("Could not save screenshot: " + e.getMessage());
                }
            } else {
                test.warning("Page was null — screenshot could not be captured.");
            }
        }

        // Save trace for failed tests
        stopTracing(result, true);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        currentTest.get().skip("Test skipped: " + result.getThrowable().getMessage());

        // Discard trace for skipped tests
        stopTracing(result, false);
    }

    @Override
    public void onFinish(ITestContext context) {
        // flush() writes the HTML file to disk — without this, report is never created
        extent.flush();
    }

    /**
     * Stops tracing and saves the trace file only if saveTrace is true (i.e., test failed).
     * If saveTrace is false, trace is discarded (no path = not saved).
     */
    private void stopTracing(ITestResult result, boolean saveTrace) {
        Object instance = result.getInstance();
        if (instance instanceof BaseTest baseTest) {
            var ctx = baseTest.getContext();
            if (ctx != null) {
                try {
                    if (saveTrace) {
                        String traceName = result.getMethod().getMethodName()
                                + "_" + System.currentTimeMillis() + ".zip";
                        Path traceDir = Paths.get("traces");
                        Files.createDirectories(traceDir);
                        ctx.tracing().stop(new com.microsoft.playwright.Tracing.StopOptions()
                                .setPath(traceDir.resolve(traceName)));
                    } else {
                        ctx.tracing().stop();
                    }
                } catch (Exception e) {
                    // Tracing may not have started (e.g., API tests with no context)
                }
            }
        }
    }

    /** Access current test from outside if needed (e.g., for mid-test logging). */
    public static ExtentTest getCurrentTest() {
        return currentTest.get();
    }
}
