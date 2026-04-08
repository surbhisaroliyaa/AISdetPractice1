# AI SDET Master Plan — From Manual Tester to Job-Ready AI SDET

> **Who is this for:** A manual tester with Playwright basics done, who wants to become an AI SDET — not by memorizing syntax, but by deeply understanding concepts, using AI as a multiplier, and being interview-ready.

> **Philosophy:** You don't write code from scratch. AI writes code. YOU are the brain — you decide what to test, direct the AI, review the output, debug failures, and explain everything confidently.

---

## How This Plan Works — The Three Pillars

Every topic follows this structure:

### Pillar 1: Deep Understanding (Concept Session)
- I explain the concept — what, why, how, real-world scenarios
- You read, ask questions, discuss until you truly GET it
- We do "Explain it back" — I ask you interview-style questions, you answer

### Pillar 2: Hands-On with AI (AI SDET Workflow)
- You describe what you want in plain English
- AI generates the code
- You REVIEW every line — spot issues, understand the logic
- You RUN it — debug when it fails (this is where real learning happens)
- You learn to PROMPT effectively

### Pillar 3: Job Readiness (Interview + Portfolio)
- Interview questions after every topic
- Framework design discussions
- Build a portfolio project you can showcase
- Learn to TALK about AI-assisted testing

---

## Your Current Progress

**Already Completed (PlaywrightDemo project):**
- Browser launch, navigation, locators (CSS, role-based, advanced)
- Actions (click, fill, keyboard), assertions, validations
- Waits & synchronization, frames, Shadow DOM
- File upload/download/dialogs, window handling
- Page Object Model, TestNG (DataProvider, BaseTest)

**Already Built (AISdetPractice1 project):**
- ConfigReader, BaseTest with ad-blocking, HomePage POM
- Navigation smoke tests (9 tests passing)
- testng.xml with test suite structure
- TestDataGenerator utility

---

## PHASE 1: PLAYWRIGHT DEEP DIVE (Days 1-10)
> Goal: Understand every Playwright concept so deeply you can explain it to anyone

---

### Day 1 — Playwright Architecture: How It Actually Works Under the Hood

**Pillar 1: Concept Session**

Most people use Playwright without understanding HOW it works. This is what separates a strong SDET from a script-kiddie.

**The Architecture:**
```
Your Java Test Code
       ↓ (sends commands via WebSocket)
Playwright Server (Node.js process)
       ↓ (uses CDP/DevTools Protocol for Chromium, or native protocol for FF/WebKit)
Browser Process (Chromium / Firefox / WebKit)
       ↓
Web Page (DOM, JavaScript, Network)
```

**Key concepts to understand:**

1. **Playwright ≠ Browser.** Playwright is a CONTROLLER. It sends commands to the browser via protocols.

2. **Why Playwright is faster than Selenium:**
   - Selenium uses HTTP requests (slow round-trips) → WebDriver → Browser
   - Playwright uses persistent WebSocket connection (fast, bidirectional) → Browser
   - This is why Playwright can listen for events (network, console, dialogs) in real-time

3. **The Object Hierarchy:**
   ```
   Playwright (the library)
     └── Browser (one browser instance — Chromium/Firefox/WebKit)
           └── BrowserContext (like an incognito window — isolated cookies, storage)
                 └── Page (one tab)
                       └── Frame (iframes within the page)
                             └── Locator (points to element(s) on the page)
   ```

4. **Why BrowserContext matters:**
   - Each context is ISOLATED — separate cookies, localStorage, session
   - Two contexts = two completely independent users
   - This is how parallel tests work — each test gets its own context
   - `storageState` saves/loads a context's auth state

5. **Auto-Wait — Playwright's killer feature:**
   - When you say `page.click("#button")`, Playwright doesn't just click immediately
   - It waits for: element to be visible, enabled, stable (not animating), receiving events
   - This eliminates 90% of flaky tests that plague Selenium frameworks
   - You almost NEVER need explicit waits in Playwright

6. **Locator vs ElementHandle:**
   - `Locator` = a RECIPE for finding an element. It re-evaluates every time you use it.
   - `ElementHandle` = a direct reference to a DOM element. If the DOM changes, it's stale.
   - Always use Locator. Never use ElementHandle (it's like Selenium's stale element problem).

**Pillar 2: Hands-On**

Open your AISdetPractice1 project. Look at `BaseTest.java` and trace the hierarchy:
- Where is Playwright created? Where is Browser? Context? Page?
- What's `static` vs instance? Why?
- What happens if two tests run in parallel with the current setup?

Ask me: "Explain the BaseTest lifecycle step by step" — then follow along with the code.

**Pillar 3: Interview Questions**

- Q: "Explain Playwright's architecture."
  - A: Playwright communicates with browsers via WebSocket using the DevTools Protocol (for Chromium) or native protocols (for Firefox/WebKit). This gives it a persistent bidirectional connection, which is why it's faster than Selenium's HTTP-based WebDriver protocol. The hierarchy is Playwright → Browser → BrowserContext → Page → Locator.

- Q: "What is BrowserContext and why is it important?"
  - A: BrowserContext is like an incognito session — isolated cookies, localStorage, and state. It enables test isolation (each test gets clean state) and parallel execution (each thread gets its own context). You can also save/load auth state via `storageState()`.

- Q: "How does Playwright's auto-wait work?"
  - A: Every action (click, fill, etc.) automatically waits for the element to be visible, enabled, stable, and ready to receive events. This is built into the framework — no need for explicit waits like Selenium's WebDriverWait. It's the main reason Playwright tests are less flaky.

- Q: "Locator vs ElementHandle?"
  - A: Locator is lazy — it finds the element fresh each time you interact with it. ElementHandle is eager — it's a direct reference that goes stale if the DOM changes. Always use Locator. ElementHandle is Playwright's equivalent of Selenium's stale element reference problem.

**Checkpoint:** Can you draw the Playwright hierarchy on paper and explain each layer? If yes, move to Day 2.

---

### Day 2 — Registration & Login: Forms, Auth State, storageState

**Pillar 1: Concept Session**

**Authentication in test frameworks — the #1 interview topic:**

Every test automation project needs to handle login. The naive approach (login before every test) wastes time. The smart approach:

```
Setup Phase (runs ONCE):
  → Login via UI or API
  → Save session to auth-state.json using storageState()

Every Test:
  → Create new BrowserContext WITH storageState
  → Test starts already logged in
  → No login step needed
  → 2-3 seconds saved per test × 100 tests = 5 minutes saved
```

**How storageState works:**
- `context.storageState(new BrowserContext.StorageStateOptions().setPath(path))` saves:
  - All cookies (session ID, auth tokens)
  - localStorage (user preferences, cached data)
  - This is everything the browser needs to "remember" you're logged in

**Form automation concepts:**
| Element | Playwright Method | Why |
|---------|------------------|-----|
| Text input | `fill("value")` | Clears existing text first, then types |
| Dropdown (select) | `selectOption("value")` | Works with `<select>` elements |
| Radio button | `check()` | Clicks the specific radio |
| Checkbox | `check()` / `uncheck()` | `check()` only checks if unchecked |
| Date picker | `fill("2024-01-15")` or click through UI | Depends on implementation |
| File upload | `setInputFiles(path)` | Sets file on `<input type="file">` |

**Pillar 2: Hands-On**

Prompt to give me:
> "Create SignupPage and LoginPage for automationexercise.com. Create RegisterTest (valid registration with random data, duplicate email error) and LoginTest (valid login, invalid credentials, logout). Implement storageState saving so other tests can reuse the logged-in session."

After I generate the code:
1. Read the SignupPage — how many form fields does it handle?
2. Find where `storageState` is saved — what file? When is it called?
3. Run the register test — watch the browser fill the form
4. Run the login test — does it use storageState or login fresh?
5. Break it: change the password in LoginTest to "wrong" — what error do you see?

**Pillar 3: Interview Questions**

- Q: "How do you handle authentication in your framework?"
  - A: I login once (either via UI or API), save the session using `context.storageState()` to a JSON file. All subsequent tests create a new BrowserContext loading that state file — they start already authenticated. This saves 2-3 seconds per test and eliminates login-related flakiness.

- Q: "What's the difference between fill() and type()?"
  - A: `fill()` clears the field first then sets the value instantly. `type()` simulates typing character by character (useful for typeahead/autocomplete testing). In most cases, `fill()` is preferred because it's faster.

- Q: "How do you handle dynamic registration data?"
  - A: TestDataGenerator creates random emails (UUID-based), names, and addresses for every test run. This ensures test isolation — tests don't fail on re-run due to "email already exists" errors.

---

### Day 3 — Products & Search: Lists, Dynamic Content, Data Extraction

**Pillar 1: Concept Session**

**Working with lists of elements:**

E-commerce products, search results, table rows — all require working with MULTIPLE elements.

```java
// Get ALL product cards on the page
List<Locator> products = page.locator(".productinfo").all();

// Loop through each
for (Locator product : products) {
    String name = product.locator("p").innerText();
    String price = product.locator("h2").innerText();
}

// Count
int count = page.locator(".productinfo").count();
```

**Key concept: Locator.all() vs Locator.count()**
- `all()` returns a Java List<Locator> — you can iterate
- `count()` returns just the number — faster when you only need the count
- Both re-evaluate every time — if the page changes, the results change

**Search testing strategy:**
| Scenario | What to verify | Why |
|----------|---------------|-----|
| Valid search term | Results contain the term | Basic functionality |
| No results search | Empty state message | Error handling |
| Empty search | All products or error | Edge case |
| Special characters | No crash, proper handling | Security/robustness |
| Partial match | "To" finds "Top" | Search algorithm |

**Pillar 2: Hands-On**

Prompt to give me:
> "Create ProductsPage and ProductDetailPage for automationexercise.com. Create tests for: product list loads correctly, search with valid term shows relevant results, search with invalid term shows appropriate message, clicking a product shows full details (name, price, category, availability, brand)."

After I generate:
1. How does it get ALL products? Look for `.all()` or `.count()`
2. How does search verification work? Does it check EACH result contains the search term?
3. Run the search test — try different search terms manually too
4. Break it: search for `<script>alert('xss')</script>` — what happens?

**Pillar 3: Interview Questions**

- Q: "How do you test search functionality?"
  - A: I cover: valid search (results match), empty search (edge case), no-results search (negative), special characters (security), and partial match. I verify not just that results appear but that each result actually contains the search term — this catches bugs where search returns irrelevant results.

- Q: "How do you handle dynamic content?"
  - A: Playwright Locators are lazy — they re-find elements each time. For lists, I use `locator.all()` to get current elements. For content that loads asynchronously, Playwright's auto-wait handles it. For truly dynamic content (changes on every load), I verify structure and data types rather than exact values.

---

### Day 4 — Cart: State Management, Hover Actions, Table Extraction

**Pillar 1: Concept Session**

**Cart is the #1 bug-prone area in e-commerce.** Why?
- Multiple pages write to the same state (product page → cart → checkout)
- Quantity calculations can have rounding errors
- Session vs cookie storage — cart can be lost on login
- Concurrent users (two tabs adding to same cart)

**State testing concept:**
```
Empty Cart → Add item → Cart with 1 item → Add another → Cart with 2 items
                                                 ↓
                                          Remove one → Cart with 1 item
                                                 ↓
                                          Remove all → Empty Cart
```
Test EVERY transition. Verify the state is correct AFTER each action.

**Hover actions:**
```java
// Hover reveals a hidden overlay
page.locator(".product-image-wrapper").first().hover();
// Now the overlay is visible — click "Add to cart"
page.locator(".overlay .add-to-cart").first().click();
```
In Playwright, `hover()` moves the virtual mouse over the element, triggering CSS `:hover` states and JavaScript mouseover events.

**Pillar 2: Hands-On**

Prompt to give me:
> "Create CartPage for automationexercise.com. Create tests: add single product verify cart, add multiple products verify all present, remove a product verify updated cart, set quantity on detail page verify in cart, add products without login then login verify cart retained."

After I generate:
1. How is cart data extracted? (table rows → cell data)
2. How does hover-to-add-to-cart work?
3. How does it handle the "Continue Shopping" modal?
4. Run the cart persistence test — does it really survive login?

**Pillar 3: Interview Questions**

- Q: "How do you test a shopping cart?"
  - A: I test the full state lifecycle: empty → add single → add multiple → update quantity → remove single → remove all → empty. I also test cross-session persistence (cart survives login) and data accuracy (price × quantity = total). Cart bugs are common because multiple pages interact with the same state.

- Q: "What is state testing?"
  - A: Testing that the application correctly transitions between states and that data is preserved correctly at each state. For a cart: empty state, items-added state, items-removed state. Each transition (add, remove, update quantity) is a test. This catches bugs where state gets corrupted during transitions.

---

### Day 5 — Checkout E2E: Multi-Page Flows, File Download, Dialog Handling

**Pillar 1: Concept Session**

**E2E testing — the most valuable and most fragile test type:**
```
Register → Login → Browse → Add to Cart → Checkout → Payment → Confirmation → Download Invoice
```

Why it's valuable: Tests the COMPLETE user journey. If this works, customers can buy things.
Why it's fragile: 8 steps = 8 potential failure points. If step 3 fails, steps 4-8 all fail.

**The testing balance:**
```
Have 2-3 E2E tests for critical journeys (checkout, registration)
Have 20-30 focused tests for individual features (add to cart, search, login)
Have 15-20 API tests for backend validation
```

**File download in Playwright:**
```java
Download download = page.waitForDownload(() -> {
    page.click("#download-invoice");
});
String path = download.path().toString(); // Where the file was saved
```
`waitForDownload()` registers a listener BEFORE the action, then the lambda triggers the action.

**Dialog handling:**
```java
page.onDialog(dialog -> {
    // dialog.type() — "alert", "confirm", "prompt"
    // dialog.message() — the text in the dialog
    dialog.accept(); // or dialog.dismiss()
});
// THEN trigger the action that causes the dialog
page.click("#submit");
```
CRITICAL: Register the handler BEFORE the action. If you click first, the dialog appears and Playwright misses it.

**Pillar 2: Hands-On**

Prompt to give me:
> "Create CheckoutPage and ContactUsPage. Create CheckoutFlowTest: register → add product → checkout → verify address → pay → confirm. Create InvoiceDownloadTest: complete purchase then download invoice. Create ContactUsTest: fill form, upload file, handle JS confirm dialog, verify success."

After I generate:
1. How many steps is the checkout test? Count them.
2. Where is `waitForDownload()` used? Is the listener registered before or after the click?
3. Where is the dialog handler registered? Before or after the trigger?
4. Run the E2E test — how long does it take? (This is why storageState matters)

**Pillar 3: Interview Questions**

- Q: "How do you handle file downloads in Playwright?"
  - A: Use `page.waitForDownload()` which takes a lambda. The lambda performs the click that triggers download. Playwright captures the download event, and I can verify the file path, name, and even read its contents. The key is that the listener must be registered BEFORE the action.

- Q: "How do you handle JavaScript dialogs?"
  - A: Register `page.onDialog()` handler BEFORE the action that triggers the dialog. In the handler, I can read the message and either `accept()` or `dismiss()`. The common mistake is registering after the trigger — the dialog gets auto-dismissed and the handler misses it.

- Q: "What's your approach to E2E testing?"
  - A: I keep E2E tests minimal — only critical business flows (checkout, registration). Each E2E covers the happy path end-to-end. Individual features get their own focused tests. This way, if the cart test fails, I know it's a cart issue, not a side-effect of a long E2E chain.

---

### Day 6 — API Testing: Playwright's APIRequestContext

**Pillar 1: Concept Session**

**Why API testing matters (the Test Pyramid):**
```
         /  UI Tests  \        ← Slow, expensive, flaky (5-10 per feature)
        / (Playwright)  \
       /─────────────────\
      /    API Tests      \    ← Fast, reliable, thorough (20-30 per feature)
     / (Playwright API)    \
    /───────────────────────\
   /      Unit Tests         \  ← Fastest, cheapest (developer writes these)
  /───────────────────────────\
```

**Playwright for API testing — why it's brilliant:**
Most SDETs use RestAssured for API testing and Selenium/Playwright for UI. But Playwright has `APIRequestContext` built in:
- Same framework for UI AND API
- Share cookies/auth between API and UI tests
- No extra dependency needed

**HTTP Methods — you MUST know these:**
| Method | Purpose | Example | Success Code |
|--------|---------|---------|-------------|
| GET | Read data | Get all products | 200 |
| POST | Create data | Create new user | 201 |
| PUT | Update data | Update user profile | 200 |
| DELETE | Remove data | Delete account | 200 |
| PATCH | Partial update | Update only email | 200 |

**Status codes — memorize these for interviews:**
| Code | Meaning | When |
|------|---------|------|
| 200 | OK | Request succeeded |
| 201 | Created | New resource created |
| 400 | Bad Request | Invalid input (missing params, wrong format) |
| 401 | Unauthorized | Not logged in / bad credentials |
| 403 | Forbidden | Logged in but no permission |
| 404 | Not Found | Resource doesn't exist |
| 405 | Method Not Allowed | Used POST on a GET-only endpoint |
| 500 | Internal Server Error | Bug in the server |

**API + UI Integration test — the most impressive thing in interviews:**
```
1. POST /api/createAccount → create user via API (fast, no browser needed)
2. UI: Login with that user → verify logged in (tests the real UI)
3. DELETE /api/deleteAccount → cleanup via API (fast, reliable cleanup)
```

**Pillar 2: Hands-On**

Prompt to give me:
> "Create API tests using Playwright's APIRequestContext for automationexercise.com. Cover all endpoints: GET /api/productsList, POST /api/searchProduct, GET /api/brandsList, POST /api/verifyLogin, POST /api/createAccount, DELETE /api/deleteAccount, PUT /api/updateAccount, GET /api/getUserDetailByEmail. Also test wrong HTTP methods (POST on GET endpoints). Create one integration test: create user via API → login via UI → verify → delete via API."

After I generate:
1. How is `APIRequestContext` created? Different from Browser context?
2. How does it send form data? (look for `setForm()` or `setData()`)
3. How does the integration test switch between API and UI?
4. Run the API tests — notice how much faster they are than UI tests

**Pillar 3: Interview Questions**

- Q: "Can Playwright do API testing?"
  - A: Yes, `APIRequestContext` is built in. I use it for backend validation, test data setup (create via API before UI test), and cleanup (delete via API after test). Advantage over RestAssured: same framework, shared auth state, no extra dependency.

- Q: "What is the Test Pyramid and how do you follow it?"
  - A: More unit tests at the bottom (developer's job), API tests in the middle (my primary focus — fast, reliable), UI tests at the top (only for critical user journeys). I aim for 60% API, 30% UI, 10% manual/exploratory.

- Q: "How do you create test data?"
  - A: Via API in `@BeforeMethod` — much faster than UI. Create the data, run the test, delete via API in `@AfterMethod`. This keeps tests independent and fast. Only use UI for data creation when testing the creation flow itself.

---

### Day 7 — Network Interception: Mocking, Blocking, Spying

**Pillar 1: Concept Session**

**`page.route()` — one of Playwright's most powerful features:**

```java
page.route("**/api/products**", route -> {
    // Option 1: Let it through (spy)
    route.resume();

    // Option 2: Block it
    route.abort();

    // Option 3: Fake the response (mock)
    route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setBody("{\"products\": []}"));
});
```

**Three modes:**
| Mode | Method | Use Case |
|------|--------|----------|
| **Spy** | `route.resume()` | Monitor request/response without changing anything |
| **Mock** | `route.fulfill()` | Return fake response — test without backend |
| **Block** | `route.abort()` | Block ads, tracking, slow resources |

**Real-world use cases:**

1. **Test error handling:** Mock API to return 500 → verify UI shows error message gracefully
2. **Test loading states:** Mock API with delayed response → verify spinner shows
3. **Test empty states:** Mock API to return empty array → verify "No products found" message
4. **Speed up tests:** Block analytics/ads (already done in your BaseTest!)
5. **Independence:** Frontend team tests without backend being ready

**Your BaseTest already does this!** Look at the `route.abort()` calls — it blocks Google Analytics, DoubleClick ads, etc. This is a real-world pattern.

**Pillar 2: Hands-On**

Prompt to give me:
> "Create NetworkInterceptionTest: 1) Spy on product API call and verify response data, 2) Mock the product API to return custom JSON and verify UI renders it, 3) Mock API to return 500 error and verify how the page handles it, 4) Block all image requests and verify page still loads (faster). Add detailed comments explaining each route option."

After I generate:
1. Find the three different route handlers — spy, mock, block
2. How does the mock provide fake JSON? Look for `route.fulfill()`
3. What happens when you mock an error (500)?
4. Run the block-images test — is the page noticeably faster?

**Pillar 3: Interview Questions**

- Q: "How do you test error scenarios in the UI?"
  - A: I use `page.route()` to mock API responses. I can return 500 (server error), 403 (forbidden), empty data, or slow responses. This lets me test how the UI handles every error condition without needing to break the actual backend.

- Q: "How do you make tests faster?"
  - A: Block unnecessary resources — ads, analytics, tracking scripts, and non-essential images — using `route.abort()`. In my framework, BaseTest already blocks Google Analytics and ad networks. This reduces page load time by 30-50%.

- Q: "What's the difference between mocking and stubbing?"
  - A: Mock replaces the entire response (`route.fulfill()` with custom body). Stub intercepts and modifies parts of the real response (`route.resume()` with overrides). Playwright supports both.

---

### Day 8 — ExtentReports: Reporting, Screenshots on Failure, Listeners

**Pillar 1: Concept Session**

**Why reporting matters in real projects:**
- You don't show stakeholders a terminal with green text
- QA Lead wants: "How many tests passed? What failed? Show me the screenshot."
- Developer wants: "Which page broke? What did it look like when it failed?"

**TestNG Listener — the hook into test lifecycle:**
```java
public class TestListener implements ITestListener {
    @Override
    public void onTestStart(ITestResult result) {
        // Test is about to run — create report entry
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        // Test passed — log pass status
    }

    @Override
    public void onTestFailure(ITestResult result) {
        // Test FAILED — take screenshot, attach to report
        BaseTest test = (BaseTest) result.getInstance();
        byte[] screenshot = test.getPage().screenshot();
        // Attach screenshot to ExtentReports
    }

    @Override
    public void onFinish(ITestContext context) {
        // All tests done — flush report to HTML file
    }
}
```

**Real project workflow when a test fails:**
```
Test fails in CI → Pipeline goes red → Slack notification
  → QA opens ExtentReport HTML → Sees screenshot of failure
  → Opens Playwright Trace → Replays test step by step
  → Is it a real bug? → File Jira with screenshot + trace
  → Is it a test issue? → Fix test, commit, re-run
```

**Pillar 2: Hands-On**

Prompt to give me:
> "Create TestListener implementing ITestListener with ExtentReports integration. On test failure: capture screenshot via Playwright page.screenshot(), save to reports/screenshots/, attach to ExtentReport. Enable tracing in BaseTest — save traces only on failure. Register listener in testng.xml."

After I generate:
1. How does the listener get the Page object from the test instance?
2. Where are screenshots saved? Where is the HTML report?
3. Make a test fail on purpose — run it — open the report in browser
4. Check the trace file — open it with Trace Viewer

**Pillar 3: Interview Questions**

- Q: "What reporting do you use in your framework?"
  - A: ExtentReports with a TestNG Listener. On test failure, I automatically capture a Playwright screenshot and attach it to the report. I also save Playwright traces for failed tests — traces include DOM snapshots, network logs, and step-by-step screenshots. Reports are published as CI/CD artifacts.

- Q: "How do you debug a test that only fails in CI?"
  - A: I have Playwright tracing enabled for all tests in CI. When a test fails, I download the trace from CI artifacts, open it in Trace Viewer, and replay every step — I can see the exact state of the page, DOM, and network at each action. This is far more effective than reading logs.

---

### Day 9 — Parallel Execution, Cross-Browser, ThreadLocal

**Pillar 1: Concept Session**

**Why parallel execution matters:**
```
Sequential: 50 tests × 3 sec each = 150 seconds (2.5 min)
Parallel (5 threads): 50 tests / 5 threads × 3 sec = 30 seconds
```
In real projects with 500+ tests, this is the difference between 25 minutes and 5 minutes.

**ThreadLocal — the CRITICAL concept:**

Without ThreadLocal:
```
Thread 1 → uses shared Page → clicks "Login"
Thread 2 → uses SAME shared Page → clicks "Products" ← COLLISION!
Result: Test crashes, random failures, chaos
```

With ThreadLocal:
```
Thread 1 → has its OWN Page → clicks "Login" (isolated)
Thread 2 → has its OWN Page → clicks "Products" (isolated)
Result: Both pass independently
```

```java
private static ThreadLocal<Page> threadPage = new ThreadLocal<>();

@BeforeMethod
public void setup() {
    Page page = context.newPage();
    threadPage.set(page); // Each thread stores its OWN page
}

public Page getPage() {
    return threadPage.get(); // Each thread gets ITS OWN page
}
```

**Cross-browser testing:**
```xml
<!-- testng-crossbrowser.xml -->
<test name="Chrome Tests">
    <parameter name="browser" value="chromium"/>
    <classes><class name="tests.smoke.NavigationSmokeTest"/></classes>
</test>
<test name="Firefox Tests">
    <parameter name="browser" value="firefox"/>
    <classes><class name="tests.smoke.NavigationSmokeTest"/></classes>
</test>
<test name="Safari Tests">
    <parameter name="browser" value="webkit"/>
    <classes><class name="tests.smoke.NavigationSmokeTest"/></classes>
</test>
```

**Pillar 2: Hands-On**

Prompt to give me:
> "Make BaseTest thread-safe using ThreadLocal for Playwright, Browser, Context, and Page. Update testng.xml for parallel classes with thread-count=3. Create testng-crossbrowser.xml that runs smoke tests on all 3 browsers. Verify it works."

After I generate:
1. Find every `ThreadLocal` — what is stored in each?
2. How does `@BeforeMethod` and `@AfterMethod` handle cleanup per thread?
3. Run tests in parallel — do they all pass? Any timing issues?
4. Run cross-browser — does Firefox/WebKit behave differently?

**Pillar 3: Interview Questions**

- Q: "How do you run tests in parallel?"
  - A: TestNG's `parallel="classes"` in testng.xml, with `thread-count` set based on CI resources. The critical part is thread safety — I use `ThreadLocal` for Playwright, Browser, BrowserContext, and Page so each thread has its own isolated browser instance.

- Q: "What is ThreadLocal?"
  - A: A Java construct where each thread has its own copy of a variable. In test automation, it means Thread 1 has its own Page, Thread 2 has its own Page — they never interfere. Without it, parallel tests would crash because two threads would control the same browser page.

- Q: "How does cross-browser testing work in Playwright?"
  - A: Playwright bundles Chromium, Firefox, and WebKit — no driver management. I configure the browser in testng.xml parameters or config.properties. Same test code runs on all three. Playwright's API is identical across browsers — `page.click()` works the same everywhere.

---

### Day 10 — CI/CD: GitHub Actions Pipeline

**Pillar 1: Concept Session**

**CI/CD is non-negotiable. Every company asks about it.**

```
Developer pushes code to GitHub
  → GitHub Actions detects the push
  → Starts a Linux/Windows VM in the cloud
  → Installs Java 21 + Maven + Playwright browsers
  → Runs: mvn test (headless mode — no screen)
  → Tests pass? ✅ Pipeline green → merge allowed
  → Tests fail? ❌ Pipeline red → merge blocked
  → Uploads reports/traces as downloadable artifacts
```

**Key points:**
- `headless=true` in CI — no screen available
- Playwright browsers must be installed: `mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps"`
- Environment variable `CI=true` is set automatically by GitHub
- Test reports are uploaded as "artifacts" — downloadable from the GitHub Actions page

**Your framework already supports this!** `ConfigReader` checks for `CI` env var and loads `config-ci.properties` when set.

**Pillar 2: Hands-On**

Prompt to give me:
> "Create .github/workflows/tests.yml: trigger on push, setup Java 21, install Playwright browsers, run mvn test, upload reports/ and traces/ as artifacts. Create config-ci.properties with headless=true. Push to GitHub and verify pipeline runs."

After I generate:
1. Read the workflow YAML — understand each step
2. What triggers the pipeline? What Java version?
3. Where are artifacts uploaded? How to download them?
4. Push to GitHub — watch the pipeline in the Actions tab

**Pillar 3: Interview Questions**

- Q: "How do you run tests in CI/CD?"
  - A: GitHub Actions with a workflow that installs Java, Maven, and Playwright browsers. Tests run in headless mode. Reports and traces are uploaded as artifacts. Pipeline blocks merges if tests fail.

- Q: "How do you debug CI failures?"
  - A: Download the ExtentReport and Playwright traces from CI artifacts. Open the trace in Trace Viewer — it shows every action, network call, and page state. This is equivalent to watching the test run live.

---

## PHASE 2: AI SDET SKILLS (Days 11-15)
> Goal: Learn to work like an AI SDET — using AI tools as your primary weapon

---

### Day 11 — Claude Code Mastery: Effective Prompting for Test Generation

**Pillar 1: Concept Session**

**An AI SDET's most important skill is PROMPTING.**

Not coding. Not framework design. PROMPTING.

**Bad prompts vs Good prompts:**

| Bad Prompt | Good Prompt |
|------------|-------------|
| "Write a test" | "Write a TestNG test for the login page at automationexercise.com. Test valid login with random data from TestDataGenerator, verify 'Logged in as' text appears. Test invalid login, verify error message. Follow the BaseTest + POM pattern in this project." |
| "Test the cart" | "Create AddToCartTest extending BaseTest. Hover on the first product to reveal the overlay, click Add to Cart, handle the Continue Shopping modal, then navigate to cart and verify the product name, price, and quantity match." |
| "Fix this error" | "This test fails with TimeoutError on `page.locator('.product-overlay')`. The overlay should appear on hover but isn't appearing. The product card locator is `.productinfo`. Debug: is the hover working? Is the overlay class correct? Check the actual page HTML." |

**Prompting framework for test generation:**
1. **Context:** What page/feature? What URL?
2. **Pattern:** Follow existing BaseTest, POM pattern
3. **Scenarios:** Positive, negative, edge cases — be specific
4. **Assertions:** What exactly to verify
5. **Data:** Where does test data come from?

**Pillar 2: Hands-On**

Practice prompting exercises:
1. Describe the subscription feature in plain English → generate tests
2. Describe the scroll-up/down feature → generate tests
3. Describe the recommended items section → generate tests
4. For each: review the code, run it, fix issues

The goal is NOT the code — it's getting good at describing what you want.

**Pillar 3: Interview Questions**

- Q: "How do you use AI tools in your testing work?"
  - A: I use Claude Code for test generation — I describe scenarios in detail (what page, what actions, what to verify, what pattern to follow), and the AI generates Playwright tests matching my framework. I review every line, run the tests, and debug issues. I also use AI for root cause analysis — I paste error logs and page HTML, and it helps identify the issue.

- Q: "Doesn't using AI mean you're not really an SDET?"
  - A: AI handles the CODE, not the TESTING. I decide what to test (test strategy, risk analysis, edge cases), how to design the framework (POM, config, reporting), and I validate the output. An AI SDET who generates wrong tests that pass for wrong reasons is worse than useless. My value is in the testing knowledge and the ability to direct and validate AI output.

---

### Day 12 — Claude API Integration: Building AI-Powered Test Tools

**Pillar 1: Concept Session**

**Three ways to use AI in your test framework:**

1. **Test Scenario Generation**
   - Send a Jira ticket / requirement → get test scenarios back
   - Send page HTML → get suggested test cases

2. **Self-Healing Locators**
   - Test fails due to changed locator → capture page HTML → ask AI for new locator → retry
   - Logs the fix for human review

3. **Test Result Analysis**
   - Send failure logs + screenshot → get root cause analysis
   - "This test failed because the login button class changed from 'btn-primary' to 'btn-login'"

**Anthropic API basics:**
```java
// 1. Create client
AnthropicClient client = AnthropicClient.builder()
    .apiKey(System.getenv("ANTHROPIC_API_KEY"))
    .build();

// 2. Send message
Message response = client.messages().create(MessageCreateParams.builder()
    .model("claude-haiku-4-5-20251001")  // Fast, cheap model
    .maxTokens(1024)
    .addUserMessage("Suggest Playwright locators for: " + pageHtml)
    .build());

// 3. Read response
String suggestion = response.content().get(0).text();
```

**Pillar 2: Hands-On**

Prompt to give me:
> "Add Anthropic Java SDK to pom.xml. Create AiTestHelper with methods: suggestTestScenarios(pageHtml) and suggestLocator(pageHtml, elementDescription). Create a SelfHealingLocator wrapper that retries with AI-suggested locator on failure. Use claude-haiku-4-5-20251001 for cost efficiency."

After I generate:
1. Where is the API key read from? (Environment variable — never hardcoded)
2. How does the self-healing flow work? (try → catch → get HTML → ask AI → retry)
3. What model is used and why? (Haiku — cheapest, fastest, good enough for locator suggestions)

**Pillar 3: Interview Questions**

- Q: "How do you use AI APIs in testing?"
  - A: I integrate Claude API for three use cases: generating test scenarios from page HTML, self-healing locators (when a test fails due to a locator change, the framework asks Claude to suggest a new locator from the current page HTML), and failure analysis (sending error context to get root cause suggestions). All suggestions are logged for human review.

- Q: "What is a self-healing test?"
  - A: When a locator fails, instead of immediately failing the test, the framework captures the current page HTML, sends it to an AI API with a description of what element we're looking for, gets a suggested new locator, and retries. If the retry succeeds, it logs the fix for human review. This reduces maintenance when developers make minor UI changes.

---

### Day 13 — Visual Regression + Advanced Playwright Features

**Pillar 1: Concept Session**

**Visual regression — catching bugs that functional tests miss:**
- Developer changes CSS → all functional tests pass → but the button is now invisible (white on white)
- Screenshot comparison detects pixel-level changes

**How it works:**
1. First run: takes screenshot → saves as "baseline"
2. Subsequent runs: takes screenshot → compares pixel-by-pixel with baseline
3. Difference found → test fails → shows the diff image

**Advanced Playwright features to know:**

| Feature | Use Case | Code |
|---------|----------|------|
| Emulation | Test mobile viewport | `browser.newContext(new Browser.NewContextOptions().setViewportSize(375, 812))` |
| Geolocation | Test location-based features | `.setGeolocation(lat, long)` |
| Permissions | Test notification prompts | `.setPermissions(Arrays.asList("geolocation"))` |
| Network throttling | Test on slow connections | Via Chrome DevTools Protocol |
| Console logging | Capture JS errors | `page.onConsoleMessage(msg -> ...)` |

**Pillar 2: Hands-On**

Prompt to give me:
> "Create VisualRegressionTest: take screenshots of home page, login page, products page — compare against baselines. Create a test that runs on mobile viewport (iPhone 12). Create a test that captures all console errors on a page."

**Pillar 3: Interview Questions**

- Q: "How do you do visual testing?"
  - A: Playwright's `assertThat(page).toHaveScreenshot()` handles baselines automatically. First run creates the baseline, subsequent runs compare. I configure `maxDiffPixelRatio` for tolerance. For dynamic content, I mask those areas. I run visual tests on one browser only (Chrome) to avoid false positives from font rendering differences.

---

### Day 14 — Data-Driven Testing + TestNG DataProvider

**Pillar 1: Concept Session**

**Data-driven testing = same test, multiple data sets:**
```java
@DataProvider(name = "loginData")
public Object[][] loginData() {
    return new Object[][] {
        {"valid@email.com", "Pass123!", true, "Logged in as"},
        {"wrong@email.com", "Pass123!", false, "incorrect"},
        {"", "", false, "incorrect"},
        {"valid@email.com", "", false, "incorrect"},
    };
}

@Test(dataProvider = "loginData")
public void testLogin(String email, String password, boolean shouldPass, String expectedText) {
    // ONE test method, FOUR executions with different data
}
```

**Why this matters:**
- Without DataProvider: 4 separate test methods doing the same thing with different data
- With DataProvider: 1 method, 4 data rows. Adding a new scenario = adding one row.
- Real projects have Excel/CSV data sources, not hardcoded arrays

**Pillar 2: Hands-On**

Prompt to give me:
> "Create a DataDrivenLoginTest using TestNG @DataProvider. Test login with: valid credentials, wrong password, wrong email, empty fields, SQL injection attempt. Single test method, multiple data rows. Each row has email, password, expected result, and expected message."

**Pillar 3: Interview Questions**

- Q: "What is data-driven testing?"
  - A: Same test logic executed with multiple data sets using TestNG's `@DataProvider`. I define data rows (valid, invalid, edge case inputs) and one test method iterates through them. This reduces code duplication and makes adding new test scenarios as simple as adding a data row.

---

### Day 15 — Complete Framework Review + Full Suite Run

**Pillar 1: Concept Session**

**Framework design — the #1 interview discussion:**

Your framework should follow this architecture (you've built all of this!):
```
testng.xml (entry point — controls what runs)
    → TestListener (reporting + screenshots)
        → BaseTest (browser lifecycle, config, tracing)
            → Page Objects (element interactions)
                → Tests (assertions, validations)
                    → Utils (data generation, AI helpers)
```

**What makes a GOOD framework:**
| Aspect | Your Framework | Why It Matters |
|--------|---------------|----------------|
| Config-driven | ConfigReader + properties | Switch environments without code changes |
| Page Object Model | Separate pages/ package | UI changes only need one file update |
| Test isolation | New BrowserContext per test | Tests don't affect each other |
| Thread-safe | ThreadLocal | Parallel execution works |
| Reporting | ExtentReports + screenshots | Stakeholders can read results |
| Debugging | Tracing + screenshots on failure | CI failures are diagnosable |
| CI/CD ready | GitHub Actions + headless | Tests run automatically |
| Data-driven | DataProvider + TestDataGenerator | Flexible test data |
| AI-powered | Claude API for self-healing | Modern, forward-looking |

**Pillar 2: Hands-On**

Run the complete test suite. Fix any failures. Ensure:
1. All tests pass locally
2. CI pipeline is green
3. ExtentReport generates correctly
4. Traces are saved for failures

**Pillar 3: Interview Questions**

- Q: "Walk me through your test automation framework."
  - A: [Practice this out loud. Explain each layer, why you chose Playwright over Selenium, how CI/CD works, how reporting works, what AI integration you have. This is THE interview question.]

---

## PHASE 3: JOB READINESS (Days 16-20)
> Goal: Prepare for interviews, build portfolio, practice explaining everything

---

### Day 16 — Test Strategy & Test Planning

**Concept:**
When you join a company, you don't start writing code. You start with a TEST STRATEGY.

**Test strategy document structure:**
1. **Scope** — what are we testing? (features, pages, APIs)
2. **Approach** — test pyramid split (60% API, 30% UI, 10% manual)
3. **Tools** — Playwright Java + TestNG + ExtentReports + GitHub Actions
4. **Environments** — dev, staging, production
5. **Data strategy** — how test data is created and cleaned up
6. **Risk areas** — what's most likely to break? (checkout, auth, cart)
7. **Timeline** — when are tests ready?

**Practice:** Write a test strategy for automationexercise.com as if it were a real project.

---

### Day 17 — Interview Preparation: Technical Questions

**Practice these out loud (not just reading):**

| Category | Questions to Prepare |
|----------|---------------------|
| Framework Design | "Describe your automation framework", "Why Playwright?", "How do you handle test data?" |
| Playwright Specifics | "How does auto-wait work?", "Locator vs ElementHandle?", "How does storageState work?" |
| Testing Concepts | "Test Pyramid?", "When to automate vs manual?", "How do you handle flaky tests?" |
| CI/CD | "How do tests run in your pipeline?", "How do you debug CI failures?" |
| API Testing | "How does Playwright handle APIs?", "What status codes do you know?" |
| AI in Testing | "How do you use AI?", "What are AI's limitations in testing?" |

**Method:** Read the question. Close your eyes. Answer out loud for 60 seconds. Check against the answers in this document.

---

### Day 18 — Interview Preparation: Scenario Questions

**These are the HARD interview questions:**

**Scenario 1: "You've joined a new project. The application is an e-commerce site. There's no automation. What do you do?"**
> First week: Understand the application, identify critical flows, set up the framework (Maven + Playwright + TestNG + POM). Second week: Automate smoke tests (home page, login). Third week: Core features (products, cart, checkout). Fourth week: API tests, reporting, CI/CD. Ongoing: Expand coverage, add visual tests, maintain framework.

**Scenario 2: "100 tests were passing yesterday. Today 30 are failing. What do you do?"**
> Check if it's a deployment issue (new code broke something) or environment issue (server down). Look at WHICH tests failed — if they're all in one area (e.g., all cart tests), likely a code change in that area. Check the CI pipeline — is the environment healthy? Open ExtentReport for screenshots. If it's a real bug → file Jira. If it's a test issue → fix and commit.

**Scenario 3: "Your tests take 45 minutes. The team wants them under 10 minutes. How?"**
> 1) Run in parallel — 5 threads cuts time by 5x. 2) Split into smoke (5 min, runs on every push) and regression (full suite, runs nightly). 3) Replace slow UI tests with faster API tests. 4) Block unnecessary resources (ads, analytics). 5) Use storageState to skip login. 6) Profile slowest tests and optimize.

---

### Day 19 — Portfolio & Resume Building

**Your GitHub project should have:**
1. Clean README with: what it does, how to run, framework architecture diagram
2. Well-organized code (packages, naming, comments where needed)
3. Green CI pipeline badge
4. ExtentReport sample screenshot in README
5. 50+ tests covering multiple areas

**Resume bullet points (for AI SDET role):**
- Built a test automation framework using Playwright Java + TestNG with 65+ tests covering UI, API, and visual regression
- Implemented AI-powered test generation and self-healing locators using Claude API
- Set up CI/CD pipeline on GitHub Actions with automated reporting (ExtentReports) and Playwright tracing for failure debugging
- Achieved 70% faster test execution through parallel execution and network optimization
- Integrated API testing using Playwright's built-in APIRequestContext, following the Test Pyramid approach

**LinkedIn summary:**
> AI SDET specializing in Playwright Java automation. I combine deep testing expertise with AI tools (Claude Code, Claude API) to accelerate test creation and maintenance. My approach: design test strategy → generate tests with AI → validate output → integrate into CI/CD. Experience with API testing, visual regression, cross-browser testing, and self-healing test frameworks.

---

### Day 20 — Mock Interview + Final Review

**Do a mock interview:**

Have a friend (or use Claude) ask you these questions in order:
1. Tell me about yourself and your testing experience.
2. Walk me through your automation framework.
3. Why did you choose Playwright over Selenium?
4. How do you handle authentication in your framework?
5. Explain the Test Pyramid and how you implement it.
6. How do you use AI in your testing?
7. How do you handle flaky tests?
8. A critical production bug is found. Walk me through your process.
9. Your test suite takes 30 minutes. How do you optimize?
10. What's the most challenging test you've automated?

**After the mock interview:** Identify weak areas. Revisit those concept sessions.

---

## PROGRESS TRACKER

| Day | Phase | Topic | Status |
|-----|-------|-------|--------|
| 1 | Playwright Deep Dive | Architecture & How It Works | Not Started |
| 2 | Playwright Deep Dive | Registration, Login, Auth State | Not Started |
| 3 | Playwright Deep Dive | Products, Search, Lists | Not Started |
| 4 | Playwright Deep Dive | Cart, State Management, Hover | Not Started |
| 5 | Playwright Deep Dive | Checkout E2E, Download, Dialogs | Not Started |
| 6 | Playwright Deep Dive | API Testing (APIRequestContext) | Not Started |
| 7 | Playwright Deep Dive | Network Interception & Mocking | Not Started |
| 8 | Playwright Deep Dive | Reporting, Screenshots, Tracing | Not Started |
| 9 | Playwright Deep Dive | Parallel, Cross-Browser, ThreadLocal | Not Started |
| 10 | Playwright Deep Dive | CI/CD with GitHub Actions | Not Started |
| 11 | AI SDET Skills | Effective Prompting for Test Gen | Not Started |
| 12 | AI SDET Skills | Claude API + Self-Healing | Not Started |
| 13 | AI SDET Skills | Visual Regression + Advanced Features | Not Started |
| 14 | AI SDET Skills | Data-Driven Testing (DataProvider) | Not Started |
| 15 | AI SDET Skills | Full Framework Review + Run | Not Started |
| 16 | Job Readiness | Test Strategy & Planning | Not Started |
| 17 | Job Readiness | Interview Prep: Technical | Not Started |
| 18 | Job Readiness | Interview Prep: Scenarios | Not Started |
| 19 | Job Readiness | Portfolio & Resume | Not Started |
| 20 | Job Readiness | Mock Interview + Final Review | Not Started |

---

## KEY DIFFERENCE FROM PREVIOUS PLANS

| Before (Old Approach) | Now (AI SDET Approach) |
|----------------------|----------------------|
| Copy prompt → paste → run → next | Understand concept → THEN build → THEN explain back |
| Focus on making code work | Focus on understanding WHY it works |
| Just build features | Build + learn to explain + interview prep |
| AI writes, you watch | AI writes, you review + debug + improve |
| No interview prep | Interview questions built into every day |
| No portfolio thinking | Build showcase project from day 1 |
