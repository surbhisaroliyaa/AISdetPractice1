# Day 0 — Playwright Java Brushup (Deep Revision)

> This is not a cheat sheet. This is a deep-understanding guide — written so you can confidently explain every concept in an interview, debug any issue, and make smart framework decisions.
>
> Every topic covers: WHY it exists, HOW it works under the hood, WHEN to use what, common mistakes, and interview answers written the way you'd actually speak to an interviewer.

---

## Topic 1: Browser Launch & Navigation

### The Problem This Solves

When you test a website manually, you open a browser, go to a URL, click around, and verify things. Automation needs to do the exact same thing — but programmatically. The very first challenge is: **how do you control a browser from Java code?**

Playwright solves this by acting as a **remote control**. Your Java code doesn't touch the browser directly — it sends commands through Playwright's protocol, and Playwright talks to the browser engine internally.

### The 4-Object Hierarchy — Understanding It Deeply

Everything in Playwright revolves around 4 objects that form a chain. If you understand this chain deeply, the entire framework makes sense.

#### Object 1: Playwright (The Factory)

```java
Playwright playwright = Playwright.create();
```

This is the **entry point** — the starting object that gives you access to everything else. Think of it as the **power source**. Without creating this first, nothing else works.

What actually happens when you call `Playwright.create()`? It starts a **Node.js server process** behind the scenes. Yes — even though you're writing Java, Playwright's core runs on Node.js. Your Java code communicates with this Node.js process. You don't need to know Node.js — but knowing this helps you understand why:
- You must `close()` it when done (to kill the process)
- It takes a moment to start up
- You only want to create it once

#### Object 2: Browser (The Expensive One)

```java
Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
```

This **launches an actual browser process**. When you call this, Playwright downloads and starts a real Chromium/Firefox/WebKit binary. This is why it's expensive — it's spawning a whole new process on your machine.

**Three browser engines available:**
- `playwright.chromium()` — Chromium (same engine as Chrome and Edge)
- `playwright.firefox()` — Firefox
- `playwright.webkit()` — WebKit (same engine as Safari)

**Why is this important?** Because you can test on all three engines from the same code. In Selenium, you'd need separate drivers (ChromeDriver, GeckoDriver, etc.) and they behave slightly differently. In Playwright, the API is identical — you just swap one word.

The `LaunchOptions` let you configure the browser:
- `setHeadless(true/false)` — headless means no visible window (faster, used in CI)
- `setSlowMo(500)` — adds a delay between every action (useful for debugging/demos)
- `setChannel("chrome")` — use installed Chrome instead of bundled Chromium

**Key architectural decision:** Browser should be created **once** and shared across all tests in a class. That's why it's `static` and initialized in `@BeforeClass`. Launching a browser takes 1-3 seconds — you don't want that before every single test.

#### Object 3: BrowserContext (The Isolation Layer)

```java
BrowserContext context = browser.newContext();
```

This is the **most important concept** to understand deeply, because it's what makes Playwright tests reliable.

A BrowserContext is like an **incognito window**. It has its own:
- Cookies (separate from other contexts)
- LocalStorage and SessionStorage (separate)
- Cache (separate)
- Authentication state (separate)

**Why does this matter for testing?** Imagine you have two tests:
- Test 1: Login as Admin, change settings
- Test 2: Login as regular User, verify settings

If they share cookies, Test 1's login might leak into Test 2. Test 2 might accidentally run as Admin. Your tests are now **coupled** — the result depends on which one runs first. This is a nightmare.

With separate contexts, each test starts with a **completely clean slate**. No cookies, no session, nothing. Test 1 cannot affect Test 2. This is called **test isolation** and it's the foundation of reliable automation.

**Context is cheap to create** — it's just an in-memory configuration, not a new process. Creating 100 contexts is fast. That's why we create a new one in `@BeforeMethod` (before every test) and close it in `@AfterMethod`.

You can also configure contexts with options:
```java
BrowserContext context = browser.newContext(new Browser.NewContextOptions()
    .setViewportSize(1920, 1080)         // Screen size
    .setLocale("en-US")                   // Language
    .setTimezoneId("America/New_York")    // Timezone
    .setGeolocation(40.7128, -74.0060)    // GPS coordinates
    .setPermissions(Arrays.asList("geolocation"))  // Browser permissions
);
```

This means you can test mobile viewports, different languages, different timezones — all without changing the browser.

#### Object 4: Page (The Tab)

```java
Page page = context.newPage();
```

A Page is a **single tab** in the browser. This is the object you'll interact with 90% of the time — navigating, finding elements, clicking, typing, asserting.

Each context can have multiple pages (tabs). Each page is independent within its context but shares the context's cookies/storage.

### The Complete Setup — Putting It Together

```java
// ONCE per test class — expensive setup
static Playwright playwright;
static Browser browser;

@BeforeClass
static void launchBrowser() {
    playwright = Playwright.create();
    browser = playwright.chromium().launch();
}

// EVERY test — cheap, fresh isolation
BrowserContext context;
Page page;

@BeforeMethod
void createContextAndPage() {
    context = browser.newContext();
    page = context.newPage();
}

@AfterMethod
void closeContext() {
    context.close();  // Closes all pages in this context too
}

@AfterClass
static void closeBrowser() {
    browser.close();
    playwright.close();  // Kills the Node.js process
}
```

### Navigation — More Than Just "Go To URL"

```java
page.navigate("https://automationexercise.com");
```

What actually happens when you call `navigate()`? It's NOT just "go to this URL." It's actually:
1. Start navigating to the URL
2. Wait for the server to respond
3. Wait for the page to reach a certain **load state**
4. Only THEN return control to your code

That "wait for load state" part is crucial. By default, it waits for the `load` event — which means all HTML, CSS, images, and scripts have finished loading.

But sometimes you want to customize this:

| WaitUntilState | What It Waits For | When To Use |
|---|---|---|
| `LOAD` (default) | Everything — HTML, CSS, images, scripts | Most websites — the safe default |
| `DOMCONTENTLOADED` | HTML parsed, but images/CSS may still be loading | When you need speed and don't care about images |
| `NETWORKIDLE` | No network requests for 500ms | SPAs (React, Angular) that fetch data after initial load |
| `COMMIT` | Server's first byte received | Almost never — too early, page isn't ready |

```java
page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
```

**Real-world scenario:** You're testing a React dashboard. `navigate()` with default `LOAD` returns, but the dashboard data hasn't loaded yet (it comes from API calls after the page loads). Switch to `NETWORKIDLE` — it waits until all those API calls finish.

### Other Navigation Methods

```java
page.goBack();       // Browser back button — auto-waits for load
page.goForward();    // Browser forward button — auto-waits for load
page.reload();       // Refresh the page — auto-waits for load
```

These seem simple, but notice: **every one of them auto-waits for the page to load**. In Selenium, you'd need to manually wait after navigation. In Playwright, it's built in.

### Interview Q&A

**Q: Walk me through how you set up a Playwright test.**

A: "Playwright has a 4-object hierarchy: Playwright, Browser, BrowserContext, and Page. First I create a Playwright instance — this is the entry point that starts the underlying Node.js process. Then I launch a Browser — this is expensive because it spawns an actual browser process, so I do it once in `@BeforeClass` and make it static so all tests in the class share it.

For each test, I create a fresh BrowserContext in `@BeforeMethod`. This is the key to test isolation — each context is like an incognito window with its own cookies, storage, and session. So even if Test 1 logs in as Admin, Test 2 starts with a completely clean state. Then I open a Page from that context, which is like a tab. After each test, I close the context in `@AfterMethod`, and after all tests, I close the browser and Playwright instance.

The reason for this separation is performance and isolation. Browser launch takes seconds, so we share it. Context creation is milliseconds, so we create fresh ones per test. This gives us both speed and reliability."

**Q: What does navigate() actually do? Is it just going to a URL?**

A: "No, it's more than that. When you call `page.navigate()`, it starts the navigation AND waits for the page to reach a load state before returning control to your code. By default it waits for the `load` event, meaning all resources like HTML, CSS, images, and scripts have loaded. You can customize this — for example, with SPAs like React apps, I'd use `NETWORKIDLE` which waits until there are no network requests for 500ms, because SPAs often fetch data after the initial page load. This built-in waiting is a big reason Playwright tests are less flaky than Selenium."

**Q: Why is Browser static but Context is not?**

A: "It comes down to cost and purpose. Browser creation is expensive — it launches an actual browser process, which takes 1-3 seconds. You don't want to do that before every test, so you create it once as a static field shared across the whole test class. BrowserContext, on the other hand, is cheap — it's an in-memory configuration, not a new process. But it serves a critical purpose: test isolation. Each context has separate cookies, storage, and session state, so tests can't interfere with each other. That's why we create a fresh context per test in `@BeforeMethod` and close it in `@AfterMethod`. It's the balance between performance and isolation."

**Q: What browsers does Playwright support? How do you switch between them?**

A: "Playwright supports three engines: Chromium, Firefox, and WebKit. The beautiful thing is the API is identical for all three — you just change `playwright.chromium()` to `playwright.firefox()` or `playwright.webkit()`. In our framework, the browser type comes from a config file, so we can switch browsers by changing one property without touching any test code. This is a big advantage over Selenium where different browser drivers sometimes have slightly different behaviors."

---

## Topic 2: Locators — Finding Elements

### The Problem This Solves

Before you can click a button, fill a field, or check if something is visible — you need to **find that element on the page**. A locator is how you tell Playwright "I'm talking about THIS specific element."

### The Fundamental Difference From Selenium

In Selenium, when you find an element, you get back **the actual element** — a WebElement object that points to a specific DOM node at that moment:

```java
// Selenium — finds element RIGHT NOW, stores reference
WebElement btn = driver.findElement(By.id("submit"));
// If the page re-renders, this reference is STALE
btn.click();  // StaleElementReferenceException!
```

In Playwright, a locator is **not the element itself — it's a recipe for finding the element**. It's a set of instructions. Every time you use it, it executes those instructions fresh:

```java
// Playwright — stores the INSTRUCTIONS, not the element
Locator btn = page.locator("#submit");  // Nothing happens here — just saves the recipe
btn.click();  // NOW it finds the element and clicks — fresh lookup
btn.click();  // Finds AGAIN from scratch, clicks — always fresh
```

**Why does this matter so much?** Modern web apps constantly re-render. React, Angular, Vue — they destroy and recreate DOM elements all the time. In Selenium, this causes the dreaded `StaleElementReferenceException`. In Playwright, it literally cannot happen because every interaction starts with a fresh lookup.

This is called **lazy evaluation** — the locator doesn't do anything until you actually use it. It's one of Playwright's biggest architectural advantages.

### Two Families of Locators

Playwright gives you two categories of locators, and understanding when to use which is critical.

#### Family 1: User-Facing Locators (The Preferred Way)

These find elements the way a **human** perceives the page — by visible text, labels, roles. They don't depend on CSS classes, IDs, or HTML structure.

**`getByRole()`** — The king of locators. Every HTML element has an implicit ARIA role:

```
<button>Login</button>         --> role: BUTTON, accessible name: "Login"
<a href="/home">Home</a>      --> role: LINK, accessible name: "Home"
<input type="text">           --> role: TEXTBOX
<input type="checkbox">       --> role: CHECKBOX
<h1>Welcome</h1>              --> role: HEADING (level 1)
<select>...</select>           --> role: COMBOBOX
<textarea>                    --> role: TEXTBOX
<img alt="Logo">              --> role: IMG, accessible name: "Logo"
```

```java
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login"));
```

Why is this the best? Because it matches how a real user interacts. A user doesn't think "I'll click the element with class `btn-primary`." They think "I'll click the Login button." `getByRole` mirrors that human thinking. Even if devs change the class from `btn-primary` to `btn-main`, the button still says "Login" and is still a button — the locator still works.

It also validates accessibility — if `getByRole` can find your element, it means screen readers can too. You're testing accessibility for free.

**`getByLabel()`** — For form inputs that have a `<label>`:

```html
<label for="email">Email Address</label>
<input id="email" type="text">
```

```java
page.getByLabel("Email Address");  // Finds the input connected to this label
```

This is how screen readers identify form fields, so it's both a good locator and an accessibility check.

**`getByPlaceholder()`** — When there's no label but there's placeholder text:

```java
page.getByPlaceholder("Enter your email");
```

**`getByText()`** — Finds by visible text content:

```java
page.getByText("Welcome back");         // Substring match by default
page.getByText("Welcome back", new Page.GetByTextOptions().setExact(true));  // Exact match
```

**`getByTestId()`** — The stable fallback. Developers add `data-testid` attributes specifically for testing:

```html
<div data-testid="product-card">...</div>
```

```java
page.getByTestId("product-card");
```

This survives refactoring because developers know not to change test IDs. It's a contract between devs and testers.

**`getByAltText()`** — For images:
```java
page.getByAltText("Company Logo");
```

**`getByTitle()`** — For elements with a title attribute:
```java
page.getByTitle("Close dialog");
```

#### Family 2: CSS/XPath Locators (The Fallback)

When user-facing locators don't work (no good labels, no roles, complex structures), you fall back to CSS selectors or XPath:

```java
page.locator("#email");                        // By ID
page.locator(".product-card");                 // By class
page.locator("input[name='username']");        // By attribute
page.locator("div.container > ul > li");       // By structure
page.locator("xpath=//div[@class='main']//button");  // XPath
```

These are more **brittle** because they depend on HTML structure. If a dev wraps the element in a new `<div>`, a structural CSS selector breaks. If they rename a class during a redesign, class-based selectors break.

### The Priority Ladder — How to Choose

When you need to locate an element, work down this list. Stop at the first one that works reliably:

```
1. getByRole()         --> Best: accessible, resilient, tests accessibility
2. getByLabel()        --> Great for form inputs
3. getByPlaceholder()  --> When there's no label
4. getByText()         --> For links, headings, content
5. getByTestId()       --> Stable fallback, needs dev cooperation
6. CSS selector        --> When nothing above works
7. XPath               --> Last resort, most fragile
```

**In practice:** About 70-80% of locators should be `getByRole()`, `getByLabel()`, or `getByText()`. About 15-20% might need `getByTestId()` or CSS. XPath should be rare (less than 5%).

### Narrowing Down — When Multiple Elements Match

What happens when `getByRole(AriaRole.BUTTON, ...)` finds 5 buttons? You need to narrow down.

**Method 1: filter() with hasText** — "Find X that contains this text"

```java
page.locator(".product-card")
    .filter(new Locator.FilterOptions().setHasText("Blue T-Shirt"))
    .getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add to Cart"));
```

Read this as: "Among all product cards, find the one that contains the text 'Blue T-Shirt', then find the 'Add to Cart' button inside it."

**Method 2: filter() with has()** — "Find X that contains this child element"

```java
page.getByRole(AriaRole.LISTITEM).filter(
    new Locator.FilterOptions().setHas(
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Jeans"))
    )
);
```

Read as: "Among all list items, find the one that has a link named 'Jeans' inside it."

**Method 3: Chaining locators** — scoping inside a parent

```java
page.locator("#sidebar").getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Settings"));
```

Read as: "Inside the sidebar, find the Settings link." This avoids matching a Settings link in the header or footer.

**Method 4: nth(), first(), last()** — Position-based (last resort)

```java
locator.first();     // First match
locator.last();      // Last match
locator.nth(2);      // Third match (0-indexed)
```

Avoid these unless the position is inherently meaningful (like "first item in a list").

### Interview Q&A

**Q: How do you choose locators in Playwright? What's your approach?**

A: "I follow a priority ladder. My first choice is always `getByRole()` because it's the most resilient — it finds elements by their accessibility role and visible name, like 'find the button named Login.' This doesn't break when devs change CSS classes or restructure HTML, because the button is still a button and still says Login. It also tests accessibility for free — if my locator can find the element, screen readers can too.

If `getByRole()` doesn't work cleanly, I try `getByLabel()` for form fields, `getByText()` for links and content, or `getByPlaceholder()` for inputs without labels. If none of those work — maybe the element has no good text or role — I use `getByTestId()`, which requires developers to add `data-testid` attributes. This is a contract between the dev and QA team.

CSS selectors and XPath are my last resort because they're tied to HTML structure and break during refactors. In a well-built application, I'd say 80% of my locators are user-facing ones like `getByRole` and `getByLabel`."

**Q: What's the difference between Playwright locators and Selenium's findElement?**

A: "The fundamental difference is that Selenium's `findElement` returns a reference to the actual DOM element at that exact moment. If the page re-renders — which happens constantly in modern React or Angular apps — that reference goes stale and you get `StaleElementReferenceException`. It's probably the most common Selenium bug.

Playwright locators work completely differently. When I write `page.locator('#submit')`, nothing happens at that point — it just stores the instructions for how to find the element. Every time I actually use that locator — click it, check its text, assert on it — it finds the element fresh from scratch. So even if the page re-renders between two operations, it doesn't matter. The locator just finds the current version of the element. Stale element exceptions literally cannot happen in Playwright. This is called lazy evaluation and it's one of Playwright's biggest architectural wins."

**Q: What do you do when multiple elements match your locator?**

A: "I narrow down using Playwright's filtering methods. The most common approach is `filter()` with `setHasText()` — for example, if there are multiple product cards each with an 'Add to Cart' button, I'd first filter to the product card that contains the text 'Blue T-Shirt', then find the button inside it. I can also use `filter()` with `setHas()` to filter by child elements — like 'find the list item that contains a link named Jeans.'

Another approach is chaining — I can scope a locator inside a parent, like `page.locator('#sidebar').getByRole(LINK, 'Settings')` to find the Settings link specifically in the sidebar.

I avoid position-based selection like `nth()` unless the position is inherently meaningful, because if someone adds a new element before mine, the index shifts and the test breaks."

**Q: What is getByRole and why is it preferred?**

A: "Every HTML element has an implicit ARIA role. A `<button>` has the role 'button', an `<a>` tag has the role 'link', an `<input type='text'>` has the role 'textbox', and so on. `getByRole()` finds elements by this role and their accessible name — which is usually their visible text.

It's preferred for three reasons. First, it's resilient — devs can change CSS classes, restructure HTML, move elements around, and as long as it's still a button that says 'Login', my locator works. Second, it mirrors how real users think — nobody clicks 'the element with class btn-primary', they click 'the Login button.' Third, it validates accessibility — if `getByRole()` can't find your element, screen readers probably can't either, which means you've found an accessibility bug."

---

## Topic 3: Actions — Interacting With the Page

### The Problem This Solves

You've found the element. Now what? You need to **do something with it** — click it, type into it, check it, select from it. Actions are how your code simulates what a human does with their hands.

### The Auto-Wait — Playwright's Superpower

Before we talk about individual actions, you need to understand the most important thing about ALL actions in Playwright: **every single action auto-waits before executing.**

What does that mean? Before Playwright clicks a button, it runs through a checklist:

```
1. ATTACHED        --> Does the element exist in the DOM?
2. VISIBLE         --> Can a human see it? (not display:none, not zero-size)
3. STABLE          --> Has it stopped moving? (no CSS animations in progress)
4. ENABLED         --> Is it clickable? (no "disabled" attribute)
5. RECEIVES EVENTS --> Is anything blocking it? (no overlay, no spinner on top)
```

Playwright checks all 5 conditions. If any fail, it **waits and retries** automatically — up to the configured timeout (default 30 seconds). Only when ALL 5 pass does it perform the action.

**Why is this revolutionary?** In Selenium, you'd write:

```java
// Selenium — manual waiting everywhere
WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
wait.until(ExpectedConditions.elementToBeClickable(By.id("submit")));
driver.findElement(By.id("submit")).click();
```

In Playwright:

```java
// Playwright — just click. It waits automatically.
page.locator("#submit").click();
```

No explicit waits, no `ExpectedConditions`, no `WebDriverWait`. The wait intelligence is baked into every action. This is the single biggest reason Playwright tests are less flaky.

### Clicking

**Basic click** — covers 95% of cases:
```java
locator.click();
```

**Double-click** — for text selection or special UI interactions:
```java
locator.dblclick();
```

**Right-click** — for context menus:
```java
locator.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
```

**Ctrl+Click** — for opening links in new tabs or multi-select:
```java
locator.click(new Locator.ClickOptions().setModifiers(Arrays.asList(KeyboardModifier.CONTROL)));
```

**Force click** — bypasses actionability checks (use VERY rarely):
```java
locator.click(new Locator.ClickOptions().setForce(true));
```

When would you force click? Almost never. Maybe when you know an element works but Playwright thinks it's covered by another element (like a transparent overlay). But if you're using `force(true)`, you should question whether your test is realistic.

### Typing — fill() vs pressSequentially()

This is a critical distinction that comes up in interviews.

**`fill()`** — The default choice:
```java
locator.fill("hello@test.com");
```

What `fill()` does internally:
1. Clears the field completely (like Ctrl+A, Delete)
2. Sets the value instantly (not character by character)
3. Triggers `input` and `change` events

It's fast and reliable. Use this for 90% of text inputs — login forms, search boxes, registration forms.

**`pressSequentially()`** — The character-by-character option:
```java
locator.pressSequentially("hello", new Locator.PressSequentiallyOptions().setDelay(100));
```

What `pressSequentially()` does:
1. Types each character one at a time
2. Fires `keydown`, `keypress`, `keyup` events for EACH character
3. Optionally adds a delay between characters

When do you need this? When the application **reacts to individual keystrokes**:
- Autocomplete/search suggestions that filter as you type
- OTP (one-time password) fields where each digit triggers validation
- Character counters ("140 characters remaining")
- Live validation ("username already taken" as you type)

**Note:** `type()` is the old name — it's deprecated. `pressSequentially()` replaced it with the same behavior.

**Decision:** Start with `fill()`. If the test doesn't work because the app needs keystroke events, switch to `pressSequentially()`.

### Keyboard Actions

```java
locator.press("Enter");            // Submit a form
locator.press("Tab");              // Move to next field
locator.press("Escape");           // Close a dropdown, dismiss a modal
locator.press("Control+a");        // Select all text
locator.press("Control+c");        // Copy
locator.press("Control+v");        // Paste
locator.press("ArrowDown");        // Navigate dropdowns, lists
locator.press("Backspace");        // Delete character
```

These are useful for keyboard-driven interactions. Some apps have keyboard shortcuts — you test them with `press()`.

### Checkboxes — check() vs click()

This is a subtle but important distinction:

```java
locator.check();              // Guarantees: checkbox will be CHECKED after this
locator.uncheck();            // Guarantees: checkbox will be UNCHECKED after this
locator.setChecked(true);     // Explicit version — same as check()
locator.setChecked(false);    // Explicit version — same as uncheck()
```

Why not just use `click()` on a checkbox? Because `click()` **toggles** — if the checkbox is already checked, clicking unchecks it. Your test doesn't know the starting state.

Imagine this scenario:
- Your test assumes a checkbox is unchecked
- But a previous test (or a page default) left it checked
- Your test calls `click()` to "check" it — but it actually unchecks it
- Test passes but the checkbox is in the wrong state

`check()` is **idempotent** — it checks the current state first. If already checked, it does nothing. If unchecked, it checks it. The result is always: checked. No surprises.

### Dropdowns

**Native `<select>` dropdowns:**
```java
locator.selectOption("india");                                      // By value attribute
locator.selectOption(new SelectOption().setLabel("India"));          // By visible text
locator.selectOption(new SelectOption().setIndex(3));                // By position (0-indexed)
```

**Custom dropdowns (div-based):** Most modern apps don't use native `<select>`. They build custom dropdowns with `<div>`, `<ul>`, `<li>`. For these:
```java
// Step 1: Click to open the dropdown
page.locator(".dropdown-trigger").click();
// Step 2: Click the option
page.getByText("India").click();
```

You can't use `selectOption()` on custom dropdowns — it only works on native `<select>` elements.

### Other Actions

```java
locator.hover();      // Mouse hover — for dropdown menus, tooltips
locator.clear();      // Clear an input field without typing anything new
locator.focus();      // Focus on element without clicking (triggers focus events)
```

`hover()` is important for testing dropdown menus that appear on mouse hover, or tooltips that show when you hover over an icon.

### Interview Q&A

**Q: Do you need to add waits before actions in Playwright?**

A: "No, and this is one of Playwright's biggest advantages. Every action — click, fill, check, select — automatically waits before executing. It checks five conditions: the element must be attached to the DOM, visible, stable (not animating), enabled, and not blocked by another element. It keeps retrying these checks up to the timeout, and only performs the action when all five pass.

This eliminates the need for `WebDriverWait` and `ExpectedConditions` that Selenium requires. In Selenium, forgetting an explicit wait is probably the most common cause of flaky tests. In Playwright, the wait logic is baked into every action, so tests are reliable by default."

**Q: What's the difference between fill() and type() in Playwright?**

A: "`fill()` clears the field and sets the entire value instantly. It triggers `input` and `change` events but not individual key events. It's fast and should be your default for any text input — forms, search boxes, login fields.

`pressSequentially()` — which replaced the deprecated `type()` method — types character by character, firing `keydown`, `keypress`, `keyup` for each character. I use it when the application reacts to individual keystrokes, like autocomplete fields that show suggestions as you type, or OTP fields where each digit triggers validation.

My rule is: start with `fill()`. If the test doesn't work because the app needs keystroke events, switch to `pressSequentially()`."

**Q: Why should you use check() instead of click() for checkboxes?**

A: "`click()` toggles the checkbox — if it's checked, clicking unchecks it, and vice versa. This makes your test dependent on the checkbox's starting state, which can vary. If another test or a page default leaves the checkbox in an unexpected state, your test does the opposite of what you intended.

`check()` is idempotent — it first checks the current state. If the checkbox is already checked, it does nothing. If it's unchecked, it checks it. The result is guaranteed: the checkbox will be checked after `check()` runs. `uncheck()` works the same way in reverse. This removes any dependency on starting state and makes tests more reliable."

**Q: How do you handle custom dropdowns that aren't native select elements?**

A: "Playwright's `selectOption()` only works on native HTML `<select>` elements. Most modern apps use custom dropdowns built with divs and JavaScript. For these, I simulate what a real user does: click the dropdown trigger to open it, then click the option I want. Something like `page.locator('.dropdown-trigger').click()` followed by `page.getByText('India').click()`. If the dropdown has a search filter, I'd fill the search input first. The key insight is to mirror the actual user interaction, which also tests the dropdown's JavaScript behavior."

---

## Topic 4: Assertions & Validations

### The Problem This Solves

Actions do things, but without assertions, your test is just clicking around without verifying anything. An assertion says "I expect THIS to be true. If it's not, the test failed." It's the **verdict** — the part that decides pass or fail.

A test without assertions is like a food inspector who visits restaurants but never writes a report. What's the point?

### Two Completely Different Assertion Systems

Playwright gives you two separate assertion systems, and understanding when to use which is critical.

#### System 1: Playwright's assertThat() — The Auto-Waiting Assertions

```java
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
```

These assertions **auto-retry**. When you write:
```java
assertThat(page.locator(".success-message")).isVisible();
```

Playwright doesn't check once and give up. It repeatedly checks — every few milliseconds — until either the element becomes visible or the timeout expires (default 5 seconds). This means if a success message appears after a 2-second API call, the assertion automatically waits for it.

**Use these for anything on the page** — element visibility, text content, page URL, page title, element state.

#### System 2: TestNG's Assert — The Instant Assertions

```java
import static org.testng.Assert.*;
```

These check **once, right now, instantly**. No waiting, no retrying.

```java
assertEquals(cartTotal, "$500.00");  // Checks immediately, pass or fail
```

**Use these for data comparisons** — string matching, number comparisons, null checks, collection sizes.

### The Critical Difference — Why It Matters

```java
// After clicking "Submit", success message appears after 2 seconds:

// WRONG — TestNG assertion, checks instantly, fails because message isn't there yet
assertTrue(page.locator(".success-message").isVisible());  // FAILS!

// RIGHT — Playwright assertion, waits up to 5s for message to appear
assertThat(page.locator(".success-message")).isVisible();   // PASSES after ~2s
```

The difference is one word — `assertTrue` vs `assertThat` — but the behavior is completely different. This is the #1 source of confusion and the #1 interview question about assertions.

**The rule:** If you're checking something about the **UI** (page, elements), use Playwright's `assertThat()`. If you're checking **data or logic** (comparing strings, numbers), use TestNG's assertions.

### Playwright Assertions — Complete Reference

#### Element Assertions

```java
// Visibility and state
assertThat(locator).isVisible();          // Can a human see it?
assertThat(locator).isHidden();           // Is it NOT visible?
assertThat(locator).isEnabled();          // Can you interact with it?
assertThat(locator).isDisabled();         // Is it greyed out / disabled?
assertThat(locator).isChecked();          // Is checkbox/radio checked?
assertThat(locator).isEditable();         // Can you type into it?

// Text content
assertThat(locator).hasText("Welcome");                           // Contains "Welcome" (substring)
assertThat(locator).hasText("Welcome", new AssertionOptions());   // Various options
assertThat(locator).hasText(Pattern.compile("Welcome.*User"));    // Regex match
assertThat(locator).containsText("Welcome");                      // Same as hasText (substring)

// Attributes and properties
assertThat(locator).hasAttribute("href", "/home");       // Attribute has exact value
assertThat(locator).hasClass("active");                   // Has this CSS class
assertThat(locator).hasCSS("color", "rgb(255, 0, 0)");  // Has this CSS property value
assertThat(locator).hasValue("test@email.com");           // Input field's value
assertThat(locator).hasCount(5);                          // Exactly 5 elements match this locator
```

#### Page Assertions

```java
assertThat(page).hasURL("https://example.com/dashboard");          // Exact URL match
assertThat(page).hasURL(Pattern.compile(".*dashboard.*"));         // URL matches pattern
assertThat(page).hasTitle("Dashboard - My App");                   // Exact title match
assertThat(page).hasTitle(Pattern.compile("Dashboard.*"));         // Title matches pattern
```

#### Negation — The not() Modifier

Every assertion has a negated version:
```java
assertThat(locator).not().isVisible();       // Element should NOT be visible
assertThat(locator).not().hasText("Error");  // Should NOT contain "Error"
assertThat(page).not().hasURL("/login");     // Should NOT be on login page
```

This is especially useful for verifying error messages disappeared, spinners are gone, or the user is NOT on the login page anymore.

### TestNG Assertions — For Data and Logic

```java
assertEquals(actual, expected);                       // Exact match
assertEquals(cartTotal, "$500.00", "Cart total mismatch");  // With failure message
assertNotEquals(newId, oldId);                         // Must be different
assertTrue(price > 0, "Price should be positive");     // Boolean condition
assertFalse(list.isEmpty(), "List shouldn't be empty");
assertNull(errorMessage);                              // Should be null
assertNotNull(response);                               // Should NOT be null
```

**The third parameter (message)** is your debugging lifeline. When a test fails at 2am in CI, the message tells you what went wrong without reading code. Always add meaningful messages to TestNG assertions.

### When to Use Which — The Decision Framework

Ask yourself: **Am I checking something on the PAGE, or am I checking DATA?**

```
Checking the PAGE (UI state)?
  - Element visible/hidden/enabled?      --> assertThat(locator).isVisible()
  - Page URL after navigation?           --> assertThat(page).hasURL(...)
  - Element's text content?              --> assertThat(locator).hasText(...)
  - Number of elements found?            --> assertThat(locator).hasCount(5)
  - Input field's current value?         --> assertThat(locator).hasValue(...)

Checking DATA (logic, values)?
  - Are two strings equal?               --> assertEquals(actual, expected)
  - Is a number positive?                --> assertTrue(price > 0)
  - Is a response not null?              --> assertNotNull(response)
  - Is a list the right size?            --> assertEquals(list.size(), 3)
```

### Soft Assertions — When You Want ALL Failures

Normal assertions are **hard** — they stop the test at the first failure:

```java
assertThat(locator1).isVisible();       // If this fails...
assertThat(locator2).hasText("Hello");  // ...this NEVER runs. You only know about the first problem.
```

Soft assertions **collect all failures** and report them together:

```java
SoftAssertions softly = SoftAssertions.create();
softly.assertThat(locator1).isVisible();       // Checked
softly.assertThat(locator2).hasText("Hello");  // Also checked, even if above failed
softly.assertThat(locator3).isEnabled();       // Also checked
softly.assertAll();  // NOW throw if any failed — reports ALL failures at once
```

**When to use soft assertions:** When you're verifying multiple independent things on one page. Imagine a dashboard with 5 widgets — you want to know which ones are broken, not just the first one. Without soft assertions, you'd have to fix one, re-run, discover the next, fix it, re-run... With soft assertions, you see all 5 problems in one run.

**TestNG also has its own SoftAssert** (for data assertions):
```java
SoftAssert softAssert = new SoftAssert();
softAssert.assertEquals(title, "Home");
softAssert.assertTrue(isLoggedIn);
softAssert.assertAll();  // Reports all failures
```

### Common Mistakes

| Mistake | Why It's Wrong | Fix |
|---|---|---|
| `assertTrue(locator.isVisible())` | `isVisible()` returns boolean instantly — no auto-wait. Fails on dynamic content. | `assertThat(locator).isVisible()` — auto-retries |
| No failure message on TestNG asserts | When tests fail in CI, you have no idea what went wrong | Add message: `assertEquals(a, b, "Cart total should be $500")` |
| `assertEquals(expected, actual)` | Arguments reversed — error messages will be confusing | TestNG order: `assertEquals(actual, expected)` |
| `Thread.sleep()` before assertion | Wasteful AND unreliable. Fixed wait. | Use Playwright `assertThat()` which auto-retries |

### Interview Q&A

**Q: How do assertions work in Playwright? How are they different from Selenium?**

A: "Playwright has two assertion systems. The first is Playwright's own `assertThat()` which is specifically for checking page and element state — visibility, text content, URL, title, etc. The key feature is that these assertions auto-retry. When I write `assertThat(locator).isVisible()`, Playwright doesn't check once and fail. It retries every few milliseconds up to the timeout — typically 5 seconds. So if an element appears after a 2-second API call, the assertion automatically waits for it and passes.

The second system is TestNG's standard assertions like `assertEquals`, `assertTrue`, `assertNull` — these are for data comparisons and check instantly, no retrying.

This is fundamentally different from Selenium, where all assertions are instant. In Selenium, if content hasn't loaded yet when you assert, you either get a failure or you have to manually add a wait before the assertion. Playwright's auto-retrying assertions eliminate an entire category of flaky tests."

**Q: What's the difference between assertThat(locator).isVisible() and assertTrue(locator.isVisible())?**

A: "They look almost identical but behave completely differently. `assertThat(locator).isVisible()` is Playwright's auto-retrying assertion — it keeps checking for up to 5 seconds, retrying every few milliseconds, and only fails if the element is still not visible after the timeout. `assertTrue(locator.isVisible())` calls `isVisible()` which is a plain boolean method — it checks once, right now, and returns true or false. If the element appears one millisecond later, too bad, the assertion already failed. This is the #1 source of flaky tests when people mix up these two approaches."

**Q: What are soft assertions and when would you use them?**

A: "Soft assertions collect all failures instead of stopping at the first one. Normally, if your first assertion fails, the test stops and you never know about problems with assertions 2, 3, and 4. With soft assertions, all checks run regardless of failures, and at the end when you call `assertAll()`, it reports every failure together.

I use them when verifying multiple independent things on a single page — like a dashboard where I'm checking five different widgets, or a form where I'm verifying all field validations. Without soft assertions, I'd have to fix one issue, re-run, discover the next issue, fix that, re-run — it's a slow feedback loop. With soft assertions, I see all problems in one test run."

**Q: When do you use Playwright's assertThat vs TestNG's assertEquals?**

A: "The rule is simple: if I'm checking something about the UI — is an element visible, what text does it show, what's the page URL — I use Playwright's `assertThat()` because it auto-retries and handles timing naturally. If I'm checking data — comparing two strings, verifying a calculation, checking if a list has the right size — I use TestNG's assertions because there's nothing to wait for; the data is already in my variable.

For example, after a login, I'd use `assertThat(page).hasURL(Pattern.compile('.*dashboard.*'))` to verify the redirect — that needs auto-waiting because the redirect takes time. But if I extracted the cart total as a string and want to compare it, I'd use `assertEquals(cartTotal, '$500.00')` — the data is already captured, no need to wait."

---

## Topic 5: Waits & Synchronization

### The Problem This Solves

The web is **asynchronous**. When you click a button, things don't happen instantly — API calls fire, animations play, elements appear and disappear, pages redirect. The gap between "I did something" and "the result is ready" is where tests break.

Imagine clicking "Add to Cart" and immediately checking the cart count. The API call to add the item takes 500ms, but your test checks in 10ms. Cart still shows "0" → test fails → but the app was working fine. This is a **flaky test** — it fails not because of a bug, but because of timing.

### Playwright's Philosophy: Auto-Wait First, Manual Wait Rarely

Playwright's approach is fundamentally different from Selenium:

**Selenium's approach:** "Nothing waits by default. The developer must add waits everywhere."
**Playwright's approach:** "Everything waits by default. The developer only adds waits for special cases."

Let's break down what auto-waits for you:

| What You Do | What Playwright Waits For Automatically |
|---|---|
| `locator.click()` | Element to be attached, visible, stable, enabled, not blocked |
| `locator.fill()` | Element to be attached, visible, enabled, editable |
| `assertThat(locator).isVisible()` | Retries until visible or timeout (default 5s) |
| `assertThat(page).hasURL(...)` | Retries until URL matches or timeout |
| `page.navigate(url)` | Page to reach the `load` state |
| `page.goBack()` / `goForward()` | Page to reach the `load` state |

**This means 80-90% of your timing issues are handled automatically.** You only need manual waits for special scenarios.

### The 5 Actionability Checks — Understanding Deeply

Before every action, Playwright runs these checks in order:

**1. ATTACHED** — Does the element exist in the DOM?
The element must be present in the HTML. If JavaScript hasn't created it yet, Playwright waits.

**2. VISIBLE** — Can a human see it?
Not `display: none`, not `visibility: hidden`, not zero width/height, not off-screen. If a loading spinner is covering the page and the button is behind it, Playwright waits.

**3. STABLE** — Has it stopped moving?
CSS animations, transitions, elements sliding into view. Playwright waits for the element to be in the same position for two consecutive animation frames. This prevents clicking a button while it's still sliding down from the top.

**4. ENABLED** — Is it interactive?
No `disabled` attribute. A "Submit" button that's greyed out while a form is incomplete — Playwright waits until it becomes enabled.

**5. RECEIVES EVENTS** — Is anything blocking it?
If another element is on top (a modal overlay, a cookie banner, a loading spinner), the click would hit the wrong element. Playwright detects this and waits.

**If any check fails, Playwright waits and retries.** It keeps checking every few milliseconds until all 5 pass or the timeout expires.

### When You Need Manual Waits

Auto-wait handles element interactions beautifully. But sometimes you need to wait for things that aren't about element actionability:

#### 1. `locator.waitFor()` — Wait for Element State

```java
// Wait for a loading spinner to disappear
page.locator(".loading-spinner").waitFor(
    new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN)
);
// NOW the page is loaded, proceed with assertions
```

The 4 states you can wait for:

| State | Meaning | Real-World Use |
|---|---|---|
| `VISIBLE` (default) | Element exists AND is visible | Wait for a success message to appear |
| `HIDDEN` | Element is invisible or doesn't exist | Wait for spinner/loader to disappear |
| `ATTACHED` | Exists in DOM (may be invisible) | Wait for a hidden element to be created |
| `DETACHED` | Completely removed from DOM | Wait for a temp element to be removed |

**Most common pattern:** Waiting for a **loading spinner to disappear** before proceeding. Almost every modern app has some form of loading indicator.

#### 2. `page.waitForURL()` — Wait for Navigation

```java
// After clicking Login, wait for redirect to dashboard
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();
page.waitForURL("**/dashboard");                              // Glob pattern
page.waitForURL(Pattern.compile(".*dashboard.*"));            // Regex pattern
```

Why not just use `assertThat(page).hasURL(...)`? You can! But `waitForURL` is specifically a "wait" step — it doesn't assert, it just pauses until the URL matches. Useful when you need to wait for navigation before doing more actions (not assertions).

#### 3. `page.waitForLoadState()` — Wait for Page Load

```java
page.waitForLoadState(LoadState.NETWORKIDLE);       // No network requests for 500ms
page.waitForLoadState(LoadState.DOMCONTENTLOADED);  // HTML parsed
page.waitForLoadState(LoadState.LOAD);              // Everything loaded (default)
```

**When to use:** After actions that trigger dynamic content loading. Most useful for SPAs (Single Page Applications) where clicking a link doesn't do a full page reload — it just fetches data via API and re-renders. `NETWORKIDLE` waits until all those API calls finish.

#### 4. `page.waitForResponse()` — Wait for API Response

```java
// Wait for a specific API call to complete
Response response = page.waitForResponse(
    resp -> resp.url().contains("/api/products") && resp.status() == 200,
    () -> {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Load Products")).click();
    }
);
```

The pattern is: **set up the listener, then trigger the action**. The lambda says "I'm waiting for a response where the URL contains '/api/products' and the status is 200." The second lambda is the action that triggers that API call.

**Real-world use:** Testing that clicking "Load More" actually fetches the next page of results. Or that submitting a form sends the right API request.

#### 5. `page.waitForTimeout()` — The Hard Wait (Avoid)

```java
page.waitForTimeout(2000);  // Wait exactly 2 seconds
```

This is Playwright's version of `Thread.sleep()`. It's there for **debugging purposes only** — when you want to pause and visually see what's happening. Never use it in real tests because:
- If the real wait is 3 seconds, your 2-second wait fails
- If the real wait is 0.5 seconds, you waste 1.5 seconds per test
- Multiply by hundreds of tests → hours of wasted CI time

### Why Thread.sleep() Is the Enemy

```java
// TERRIBLE — always waits 5 seconds, even if result appears in 200ms
page.click("#submit");
Thread.sleep(5000);
assertThat(page.locator(".success")).isVisible();

// GOOD — waits exactly as long as needed, up to timeout
page.click("#submit");
assertThat(page.locator(".success")).isVisible();  // Returns as soon as element appears
```

| | `Thread.sleep()` / `waitForTimeout()` | Playwright auto-wait |
|---|---|---|
| Wait time | Fixed — always the full duration | Dynamic — returns as soon as condition is met |
| Too short? | Test fails randomly (flaky) | Impossible — keeps retrying until timeout |
| Too long? | Wastes time | Impossible — returns immediately when ready |
| Debugging | No info on what failed | Clear error: "element not visible after 30s" |
| Test suite impact | 100 tests x 5s sleep = 8+ minutes wasted | Tests run as fast as the app responds |

### The Wait Decision Flowchart

```
Need to wait for something?
|
+-- Element needs to appear or disappear?
|   +-- For assertions: assertThat(locator).isVisible() / .isHidden()
|   +-- For continuing actions: locator.waitFor(HIDDEN / VISIBLE)
|
+-- Page navigation/redirect?
|   +-- page.waitForURL("**/dashboard")
|
+-- API call needs to complete?
|   +-- page.waitForResponse(predicate, triggeringAction)
|
+-- SPA/dynamic content still loading?
|   +-- page.waitForLoadState(NETWORKIDLE)
|
+-- New tab or popup?
|   +-- page.waitForPopup(triggeringAction)
|
+-- None of the above?
    +-- You probably don't need a manual wait.
        Rethink your approach — use Playwright's assertThat().
```

### Interview Q&A

**Q: How does Playwright handle waits compared to Selenium?**

A: "This is one of the biggest differences between the two. In Selenium, nothing waits by default — you have to add `WebDriverWait` with `ExpectedConditions` before almost every interaction, and forgetting one is the most common cause of flaky tests. In Playwright, every action auto-waits. Before clicking, filling, or checking, Playwright verifies five conditions — the element must be attached, visible, stable, enabled, and not blocked by another element. It keeps retrying these checks up to the timeout.

On top of that, Playwright's `assertThat()` assertions also auto-retry. So when I check `assertThat(locator).isVisible()`, it keeps checking every few milliseconds for up to 5 seconds. This means I rarely need manual waits — maybe 10-20% of the time, for things like waiting for a spinner to disappear, a URL to change after navigation, or an API call to complete. Those have specific methods like `waitFor()`, `waitForURL()`, and `waitForResponse()`."

**Q: When would you need manual waits in Playwright?**

A: "Even though Playwright auto-waits for element interactions, there are scenarios where I need manual waits. The most common is waiting for a loading spinner to disappear — I use `locator.waitFor()` with the `HIDDEN` state. Another common case is waiting for a URL change after form submission or login — I use `page.waitForURL()` with a pattern. For SPA applications where content loads dynamically after an action, I use `page.waitForLoadState(NETWORKIDLE)` which waits until there are no network requests for 500 milliseconds. And when I need to verify that a specific API call completed — like confirming a product was added to the cart via the backend — I use `page.waitForResponse()` with a predicate that matches the API endpoint and status code."

**Q: Why should you never use Thread.sleep() in tests?**

A: "Thread.sleep() is a fixed wait — it always waits the full duration regardless of whether the app is ready. This creates two problems. If you set it too short, the test fails randomly because sometimes the app takes longer — that's a flaky test. If you set it too long, every test wastes time waiting for nothing. Multiply that across hundreds of tests and you're wasting hours of CI pipeline time.

Playwright's waits are dynamic — they return the instant the condition is met, up to a maximum timeout. So if the success message appears in 200ms, the test moves on in 200ms. If it takes 3 seconds, the test waits 3 seconds. It's both faster AND more reliable. The only place I'd use a hard wait is during debugging when I need to pause and visually inspect the browser state, and I remove it before committing."

**Q: What's the default timeout in Playwright? How do you customize it?**

A: "The default timeout for actions like click and fill is 30 seconds — Playwright will retry actionability checks for up to 30 seconds. For `assertThat()` assertions, the default timeout is 5 seconds. Both are configurable.

You can set global timeouts when creating the browser context, or per-action timeouts for specific interactions. For example, if a particular page is known to be slow, I might increase the timeout for that specific assertion: `assertThat(locator).isVisible(new IsVisibleOptions().setTimeout(15000))`. But I avoid making timeouts too long globally because if a test legitimately fails, you don't want to wait 60 seconds to find out."

---

## Topic 6: Frames & Shadow DOM

### Part A: Frames (iframes)

#### The Problem This Solves

A frame (iframe) is a **web page embedded inside another web page**. It's like a TV showing a TV — the inner TV has its own channels, remote, and controls. The outer page **cannot see or interact** with elements inside the frame directly.

Real-world examples where you'll encounter iframes:
- **Payment forms** — Stripe, PayPal, Razorpay embed payment fields in iframes for security. The main site never touches your card number; it stays inside the iframe.
- **CAPTCHA** — Google reCAPTCHA is inside an iframe.
- **Embedded content** — YouTube players, Google Maps, social media widgets.
- **Ads** — Almost all ads are in iframes to isolate them from the main page.
- **Legacy applications** — Old enterprise apps using framesets.

#### Why Frames Are Tricky

The browser treats each frame as a **separate document** with its own DOM. When Playwright looks at the page, it only sees the main document's elements. Elements inside frames are invisible — like trying to read a book inside a locked glass box. You need to "reach into" the frame first.

#### How Playwright Handles Frames — frameLocator()

```java
FrameLocator paymentFrame = page.frameLocator("iframe[name='payment']");
paymentFrame.getByLabel("Card Number").fill("4242424242424242");
paymentFrame.getByLabel("Expiry").fill("12/30");
paymentFrame.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Pay")).click();
```

`frameLocator()` takes a CSS selector that identifies the iframe element, and returns a `FrameLocator` object. You then chain locator methods on it — everything you can do on `page`, you can do on a `FrameLocator`.

Ways to identify the iframe:
```java
page.frameLocator("iframe[name='payment']");      // By name attribute (most reliable)
page.frameLocator("#checkout-iframe");              // By ID
page.frameLocator("iframe[src*='stripe']");         // By URL pattern in src attribute
page.frameLocator("iframe").nth(0);                 // By position (fragile — avoid)
```

#### frameLocator() vs frame() — Know the Difference

Playwright has two ways to access frames:

```java
// frameLocator() — RECOMMENDED — acts like a locator, auto-waits
FrameLocator fl = page.frameLocator("#my-frame");
fl.getByText("Hello").click();  // Auto-waits for the frame to load AND the element to be ready

// frame() — Lower-level — returns the raw Frame object
Frame f = page.frame("frameName");
f.locator("#btn").click();  // No auto-wait for frame loading — might fail if frame hasn't loaded
```

**Always use `frameLocator()`** because it auto-waits for the frame to appear. `frame()` gives you the raw Frame object which might be null if the frame hasn't loaded yet.

#### Nested Frames (Frame Inside a Frame)

Sometimes a frame contains another frame (payment form inside a checkout iframe inside the main page). Just chain `frameLocator()` calls:

```java
page.frameLocator("#outer-frame")
    .frameLocator("#inner-frame")
    .getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit"))
    .click();
```

Each `frameLocator()` scopes one level deeper.

#### The Huge Advantage Over Selenium

In Selenium, frames require **switching**:

```java
// Selenium — stateful switching, error-prone
driver.switchTo().frame("payment");           // Switch INTO frame
driver.findElement(By.id("card")).sendKeys("4242...");
driver.switchTo().defaultContent();           // Switch BACK to main page
driver.findElement(By.id("confirm")).click(); // Now on main page
// Forgot to switch back? You're looking for "confirm" inside the frame — ERROR!
```

In Playwright — no switching at all:

```java
// Playwright — direct access, no state to manage
page.frameLocator("#payment").getByLabel("Card").fill("4242...");  // Inside frame
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirm")).click();  // Main page
// Both work simultaneously — no switching, no state, no mistakes
```

In Selenium, forgetting `switchTo().defaultContent()` is a classic bug — your code tries to find elements on the main page but Selenium is still looking inside the frame. In Playwright, this entire category of bugs doesn't exist.

### Part B: Shadow DOM

#### The Problem This Solves

Shadow DOM is a browser feature used by **Web Components**. It creates a private, encapsulated DOM tree inside an element. The main page's CSS can't style it, and regular DOM queries can't find elements inside it.

Think of it as a **locked room** inside an HTML element. The element's internal structure is hidden from the outside.

```html
<!-- Regular DOM — everything is visible -->
<div id="app">
    <button>Click me</button>        <!-- Easily found by any selector -->
</div>

<!-- Shadow DOM — internal structure is hidden -->
<custom-element>
    #shadow-root (open)              <!-- The "locked room" boundary -->
        <button>Click me</button>    <!-- Hidden from regular querySelector -->
</custom-element>
```

Common places you'll encounter Shadow DOM:
- Custom web components (`<custom-dropdown>`, `<date-picker>`)
- Material Design components
- Salesforce Lightning components
- Some modern UI frameworks

#### Playwright's Approach — Auto-Pierce

Here's the beautiful part: **Playwright automatically pierces through open Shadow DOM boundaries.** You don't need to do anything special.

```java
// Even if the button is deep inside a shadow DOM, this works:
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Click me")).click();

// CSS selectors also work across shadow boundaries:
page.locator("custom-element button").click();
```

Playwright's locator engine searches through shadow roots automatically. When you use `getByRole()`, `getByText()`, or CSS selectors, Playwright looks inside every open shadow root as if the boundary didn't exist.

#### Selenium's Pain With Shadow DOM

In Selenium, Shadow DOM requires explicit navigation:

```java
// Selenium — painful chain of shadow root lookups
WebElement host = driver.findElement(By.cssSelector("custom-element"));
SearchContext shadow = host.getShadowRoot();
WebElement button = shadow.findElement(By.cssSelector("button"));
button.click();

// Nested shadow DOMs? Even worse:
WebElement host1 = driver.findElement(By.cssSelector("outer-element"));
SearchContext shadow1 = host1.getShadowRoot();
WebElement host2 = shadow1.findElement(By.cssSelector("inner-element"));
SearchContext shadow2 = host2.getShadowRoot();
WebElement button = shadow2.findElement(By.cssSelector("button"));
```

In Playwright: `page.locator("button").click();` — done. The engine handles all the shadow root traversal.

#### Open vs Closed Shadow DOM

Shadow DOMs can be **open** (mode: "open") or **closed** (mode: "closed"):

- **Open** (99% of cases): Playwright auto-pierces. No special code needed.
- **Closed** (very rare): Even Playwright can't pierce it automatically. You'd need JavaScript evaluation:

```java
page.evaluate("document.querySelector('custom-element').shadowRoot.querySelector('button').click()");
```

In practice, closed shadow DOMs are extremely rare. Most component libraries use open shadow DOM.

### Interview Q&A

**Q: How do you handle iframes in Playwright?**

A: "I use `page.frameLocator()` with a CSS selector to identify the iframe — usually by name attribute, ID, or a pattern in the src URL. This gives me a FrameLocator object that I can chain any locator method on, just like I would with the main page. For example, to fill a payment form inside an iframe, I'd write `page.frameLocator('iframe[name=payment]').getByLabel('Card Number').fill('4242...')`.

The big advantage over Selenium is that there's no 'switching.' In Selenium, you have to `switchTo().frame()` to enter the frame and `switchTo().defaultContent()` to go back to the main page. If you forget to switch back, all your subsequent operations happen inside the frame and fail mysteriously. In Playwright, I can interact with frame elements and main page elements in any order without any switching. `frameLocator` directly scopes into the frame, and `page` always accesses the main page. It eliminates an entire category of bugs."

**Q: What's the difference between frame() and frameLocator()?**

A: "`frame()` returns the raw Frame object, which might be null if the frame hasn't loaded yet. There's no auto-waiting — you're responsible for making sure the frame exists. `frameLocator()` returns a FrameLocator that behaves like Playwright's other locators — it auto-waits for the frame to appear before interacting. I always use `frameLocator()` for the same reason I use locators instead of raw DOM references — it's more reliable and handles timing automatically."

**Q: How do you handle Shadow DOM in Playwright?**

A: "Honestly, in Playwright it's a non-issue. Playwright automatically pierces through open Shadow DOM boundaries. When I use `getByRole()`, `getByText()`, or even CSS selectors, Playwright's locator engine searches through shadow roots as if they don't exist. I don't need any special code.

This is a big advantage over Selenium, where you have to explicitly get the shadow root of each host element and chain lookups through each shadow boundary. With nested shadow DOMs in Selenium, the code becomes really ugly and fragile. In Playwright, `page.getByRole(BUTTON, 'Click me').click()` works whether the button is in the main DOM, inside a shadow root, or even inside nested shadow roots. The 99% case — open shadow DOM — is handled automatically."

**Q: How do you handle nested frames?**

A: "I chain `frameLocator()` calls. Each one scopes one level deeper. So if there's an iframe inside another iframe, I'd write `page.frameLocator('#outer').frameLocator('#inner').getByText('Hello').click()`. Playwright handles all the frame traversal and auto-waits at each level."

---

## Topic 7: File Upload, Download & Dialogs

### Part A: File Upload

#### The Problem This Solves

Testing file upload is tricky because you can't automate the operating system's file picker dialog. When you click "Choose File" on a website, your OS opens a native file browser — that's outside the browser's control. Playwright solves this by **bypassing the file picker entirely** and setting the file directly on the input element.

#### Method 1: setInputFiles() — The Standard Approach

Most file uploads use an `<input type="file">` element — even if it's hidden behind a pretty button:

```java
// Single file upload
page.locator("input[type='file']").setInputFiles(Paths.get("test-data/photo.jpg"));

// Multiple files
page.locator("input[type='file']").setInputFiles(new Path[] {
    Paths.get("test-data/photo1.jpg"),
    Paths.get("test-data/photo2.jpg")
});

// Clear file selection (remove previously selected files)
page.locator("input[type='file']").setInputFiles(new Path[] {});
```

`setInputFiles()` directly sets the file on the input element. No file picker dialog opens. The website sees the file as if the user selected it normally.

**Note on file paths:** Use relative paths from the project root, or absolute paths. Keep test files in a `test-data/` folder in your project.

#### Method 2: FileChooser — When Input Is Hidden

Some websites hide the `<input type="file">` and use a styled button or drag-and-drop area that triggers the hidden input via JavaScript. In these cases, you can't directly target the input. Instead, you catch the file chooser event:

```java
// Wait for the file chooser dialog, then set the file
FileChooser fileChooser = page.waitForFileChooser(() -> {
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Upload Photo")).click();
});
fileChooser.setFiles(Paths.get("test-data/photo.jpg"));
```

The pattern is: "Start listening for a file chooser event, THEN trigger the action that opens it." The `waitForFileChooser()` captures the event and gives you a `FileChooser` object to set files on.

**When to use which:**
- Can you find `input[type='file']` in the DOM? → Use `setInputFiles()` directly
- The input is hidden/styled and you can only click a button? → Use `waitForFileChooser()`

### Part B: File Download

#### The Problem This Solves

When testing downloads, you need to verify that clicking "Download Invoice" actually produces a file with the right name and content. Playwright intercepts the download before it hits the file system, giving you control over where it's saved and what it contains.

```java
// Wait for download triggered by a click
Download download = page.waitForDownload(() -> {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Download Invoice")).click();
});

// Inspect the download
String fileName = download.suggestedFilename();  // e.g., "invoice-2024.pdf"
String url = download.url();                      // The download URL

// Save to a specific location
download.saveAs(Paths.get("downloads/invoice.pdf"));

// Verify the download
assertEquals(download.suggestedFilename(), "invoice-2024.pdf");

// Read content directly (for small text files)
InputStream stream = download.createReadStream();
```

The pattern is the same as file upload: **set up the listener before triggering the action.** `waitForDownload()` captures the download in memory. You then decide where to save it with `saveAs()`.

**Key point:** Downloads don't automatically go to a folder. They're captured in memory by Playwright. You must explicitly save them.

### Part C: Dialogs (Browser Popups)

#### The Problem This Solves

JavaScript has three built-in dialog functions: `alert()`, `confirm()`, and `prompt()`. These create native browser popups — not HTML modals, but actual browser-level dialogs that block the page. You need to handle them or your test hangs.

#### The Three Types

**Alert** — Just a message with OK:
```
JavaScript: alert("Item added to cart!")
User sees:  [Item added to cart!] [OK]
Returns:    Nothing
```

**Confirm** — A question with OK/Cancel:
```
JavaScript: confirm("Are you sure you want to delete?")
User sees:  [Are you sure you want to delete?] [OK] [Cancel]
Returns:    true (OK) or false (Cancel)
```

**Prompt** — A text input with OK/Cancel:
```
JavaScript: prompt("Enter your name:")
User sees:  [Enter your name:] [text input] [OK] [Cancel]
Returns:    The entered text (OK) or null (Cancel)
```

#### Critical Rule: Listener BEFORE Action

Playwright **auto-dismisses dialogs by default**. If a dialog appears and you haven't set up a listener, Playwright dismisses it immediately (like clicking Cancel). You'll never see it and won't be able to read its message.

So you MUST set up the listener BEFORE the action that triggers the dialog:

```java
// Step 1: Register listener (does nothing yet — just waits)
page.onDialog(dialog -> {
    System.out.println("Dialog type: " + dialog.type());      // "alert", "confirm", or "prompt"
    System.out.println("Dialog message: " + dialog.message()); // The text in the dialog
    dialog.accept();   // Click OK
    // OR dialog.dismiss();  // Click Cancel
    // OR dialog.accept("John Doe");  // Enter text and click OK (for prompt)
});

// Step 2: NOW trigger the action that causes the dialog
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete")).click();
```

#### Common Patterns

```java
// Pattern 1: Accept an alert and verify its message
final String[] dialogMessage = {""};
page.onDialog(dialog -> {
    dialogMessage[0] = dialog.message();
    dialog.accept();
});
page.getByText("Show Alert").click();
assertEquals(dialogMessage[0], "Item added to cart!");

// Pattern 2: Dismiss a confirm (click Cancel)
page.onDialog(dialog -> dialog.dismiss());
page.getByText("Delete Account").click();
// Verify account was NOT deleted

// Pattern 3: Enter text in a prompt
page.onDialog(dialog -> dialog.accept("John Doe"));
page.getByText("Enter Name").click();
// Verify the page now shows "John Doe"
```

#### Dialog vs HTML Modal — The Critical Distinction

This is something interviewers love to ask because many candidates confuse them:

| | Native Browser Dialog | HTML Modal |
|---|---|---|
| Created by | `alert()`, `confirm()`, `prompt()` | HTML/CSS/JavaScript (`<div class="modal">`) |
| Appearance | Plain browser-style popup, can't be styled | Styled, looks like part of the website |
| How to detect | Can't see it in DevTools Elements tab | Visible in DevTools as a regular DOM element |
| How to automate | `page.onDialog()` listener | Regular locators: `page.locator(".modal")` |
| Auto-dismissed by Playwright? | Yes | No — it's just HTML |
| Can you inspect its content? | Only through the `dialog.message()` API | Yes — it's regular DOM |

**If an interviewer says "how do you handle popups" — ask:** "Do you mean a native browser dialog or an HTML modal?" They require completely different approaches.

### Interview Q&A

**Q: How do you handle file uploads in Playwright?**

A: "It depends on the implementation. If the page has a standard `<input type='file'>` element — even if it's hidden — I use `setInputFiles()` directly on that element and pass the file path. No file picker dialog opens; Playwright sets the file programmatically.

If the file input is completely hidden and triggered by a custom button or drag-and-drop area, I use `page.waitForFileChooser()`. I set up the listener first, then click the upload button. The listener captures the file chooser event and I can call `setFiles()` on it. The pattern is always 'listener before trigger' in Playwright. For multiple files, I pass an array of paths."

**Q: How do you verify file downloads in tests?**

A: "I use `page.waitForDownload()` with the triggering action as a callback — like clicking a download link. This returns a Download object which captures the download in memory. I can then verify the filename using `suggestedFilename()`, check the URL, and save the file to a specific location using `saveAs()`. For content verification, I can read the file using `createReadStream()`.

An important detail is that Playwright doesn't auto-save downloads to a folder like a real browser. The download is captured in memory, and you explicitly choose where to save it. This gives us full control for testing."

**Q: How do you handle JavaScript alerts, confirms, and prompts?**

A: "The key is that Playwright auto-dismisses dialogs by default, so I must register a `page.onDialog()` listener BEFORE triggering the action that causes the dialog. Inside the listener, I can access the dialog's type and message, then either accept it, dismiss it, or — for prompt dialogs — accept it with text.

For example, to test a delete confirmation, I'd set up a listener that captures the dialog message and calls `dialog.accept()`, then click the Delete button. After that, I can verify both that the dialog had the correct message AND that the deletion was performed. If I want to test the 'Cancel' path, I'd call `dialog.dismiss()` instead and verify the item was NOT deleted."

**Q: What's the difference between a browser dialog and an HTML modal?**

A: "A native browser dialog is created by JavaScript's `alert()`, `confirm()`, or `prompt()` functions. It's a plain browser-level popup that you can't style and can't see in DevTools. You handle it with Playwright's `page.onDialog()` listener.

An HTML modal is just regular HTML/CSS — a `div` with styling that looks like a popup. You can see it in DevTools, inspect its elements, and interact with it using regular Playwright locators like `page.locator('.modal')`.

The approach is completely different, so in an interview or when debugging, I always clarify which type we're dealing with. I've seen candidates waste time trying to use `page.onDialog()` for HTML modals — it will never work because there's no actual browser dialog."

---

## Topic 8: Window/Tab Handling

### The Problem This Solves

When a user clicks a link with `target="_blank"` or a button calls `window.open()`, a new tab opens. Your test needs to interact with that new tab — maybe verify its content, fill a form, then go back to the original tab. In Selenium, this is one of the most error-prone areas. In Playwright, it's straightforward.

### Playwright's Model — Every Tab Is a Page Object

This is the key concept: in Playwright, every tab is a **separate Page object**. You don't "switch" to a tab — you just have a reference to it and use it directly.

Think of it like having two TV remotes — one for the TV in the living room, one for the bedroom. You don't "switch" which TV you're controlling — you just pick up the right remote.

```java
// The original tab — you already have this
Page originalTab = page;

// A new tab opens — Playwright gives you a NEW Page object
Page newTab = page.waitForPopup(() -> {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Terms")).click();
});

// Now you have TWO independent Page objects:
// originalTab (or page) — still works, still usable
// newTab — the newly opened tab, also usable

// Use the new tab
newTab.waitForLoadState();
assertThat(newTab).hasTitle("Terms and Conditions");
String termsText = newTab.getByRole(AriaRole.HEADING).textContent();

// Go back to original — just use the original variable!
page.getByRole(AriaRole.CHECKBOX, new Page.GetByRoleOptions().setName("I agree")).check();

// Close the new tab when done
newTab.close();
```

**No switching.** Both Page objects are independently usable at any time. You can even interact with them in alternating fashion — click something on the original tab, then check something on the new tab, back and forth. This is impossible in Selenium without constantly switching.

### The waitForPopup() Pattern

```java
Page popup = page.waitForPopup(() -> {
    // The action that opens a new tab/window
    page.getByText("Open in New Tab").click();
});
```

This follows the same pattern as `waitForDownload()` and `waitForFileChooser()`: **set up the listener, then trigger the action.** Playwright starts listening for a new page, then your click opens the new tab, and Playwright captures it.

### Creating Tabs Programmatically

Sometimes you want to open multiple tabs yourself (not triggered by user clicks):

```java
// All tabs share the same context (same cookies, session, storage)
Page tab1 = context.newPage();
Page tab2 = context.newPage();
Page tab3 = context.newPage();

tab1.navigate("https://example.com/page1");
tab2.navigate("https://example.com/page2");
tab3.navigate("https://example.com/page3");
```

Since they're in the same BrowserContext, they share session state. If tab1 logs in, tab2 and tab3 are also logged in (because they share cookies).

### Multiple Users — Different Contexts

When you need truly independent sessions (two different users), create separate BrowserContexts:

```java
BrowserContext adminSession = browser.newContext();
BrowserContext userSession = browser.newContext();

Page adminPage = adminSession.newPage();
Page userPage = userSession.newPage();

// These are COMPLETELY independent — separate cookies, separate sessions
adminPage.navigate("https://app.com/admin");
userPage.navigate("https://app.com/user-dashboard");
```

**Use cases for multiple contexts:**
- Testing chat between two users
- Testing collaborative editing (Google Docs-style)
- Testing that User A's changes appear on User B's screen
- Testing role-based access (admin vs regular user)

### Getting All Pages in a Context

```java
List<Page> allPages = context.pages();
System.out.println("Open tabs: " + allPages.size());
```

### Playwright vs Selenium — Window Handling

| | Selenium | Playwright |
|---|---|---|
| How tabs are represented | Window handles (opaque strings like `CDwindow-ABC123`) | Page objects (real, usable references) |
| "Switching" | `driver.switchTo().window(handle)` — you can only talk to ONE window at a time | No switching — every Page is usable anytime |
| Risk | Forget to switch back → interacting with wrong window | No state → no risk |
| Identifying which window | Loop through handles, check titles/URLs | You already have the variable from `waitForPopup()` |
| Two windows simultaneously | Impossible — must switch | Natural — just use both variables |

This is one of Playwright's cleanest wins over Selenium. In Selenium, window handling code is consistently the ugliest part of any test framework.

### Interview Q&A

**Q: How do you handle new tabs/windows in Playwright?**

A: "In Playwright, every tab is a separate Page object. When a click opens a new tab, I use `page.waitForPopup()` with the triggering click as a callback. This gives me a new Page object for the new tab. Now I have two independent Page objects — the original and the new one — and I can interact with either one at any time without any switching.

For example, if clicking 'Terms & Conditions' opens a new tab, I'd capture it with `waitForPopup()`, verify its content using assertions on that new Page object, close it with `popup.close()`, and continue on the original page. The original page is always accessible — I never lose my reference to it.

This is completely different from Selenium where you deal with window handles and `switchTo()`. In Selenium, you can only interact with one window at a time, and forgetting to switch back is a common bug. In Playwright, there's no switching state to manage, so that category of bugs doesn't exist."

**Q: How do you test multi-user scenarios? Like testing a chat between two users?**

A: "I create separate BrowserContexts for each user. Each context is like an independent incognito session — separate cookies, separate storage, separate login. So I'd create `adminContext` and `userContext`, get a Page from each, and log in with different credentials.

Then I can simulate the interaction — User A sends a message, I switch to User B's page and verify the message appeared, User B replies, switch back to User A and verify. Since each context is independent, the sessions don't interfere. Both pages are usable simultaneously — no switching, just using different variables.

The same approach works for collaborative editing, testing permissions (admin vs regular user), or any scenario where two different users interact with the system at the same time."

**Q: How is Playwright's approach to windows better than Selenium's?**

A: "In Selenium, tabs are represented by window handles — opaque strings like 'CDwindow-ABC123'. You have to `switchTo().window(handle)` before you can interact with a tab, and you can only interact with one at a time. To find a specific tab, you often loop through all handles checking titles or URLs. And the most common bug is forgetting to switch back to the original window after closing a popup.

In Playwright, when a new tab opens, you get a real Page object. It's like having a TV remote for each screen — you just pick up the right one and use it. Both are usable simultaneously, there's no switching state, and you can never 'forget to switch back' because there's nothing to switch. It's a fundamentally simpler mental model."

---

## Topic 9: Page Object Model (POM)

### The Problem This Solves

Without POM, your test code is full of locators and raw interactions scattered everywhere:

```java
// Test 1: Login test
page.locator("#email").fill("user@test.com");
page.locator("#password").fill("pass123");
page.locator("#login-btn").click();

// Test 2: Login + add to cart test
page.locator("#email").fill("user@test.com");       // Same locator, copy-pasted
page.locator("#password").fill("pass123");           // Same locator, copy-pasted
page.locator("#login-btn").click();                  // Same locator, copy-pasted
page.locator(".add-to-cart").click();
```

Now imagine the developer changes the login button's ID from `#login-btn` to `#submit-btn`. You have to find and update every test that logs in. With 50 tests? That's 50 files to edit. Miss one? You get a mysterious failure.

POM solves this by creating a **single source of truth** for each page's elements and interactions.

### What POM Actually Is

Page Object Model is a design pattern where you create **one Java class per page** (or major component) of your application. That class contains:

1. **The page reference** — the connection to the browser
2. **Methods that represent user actions** — what a user CAN DO on this page

The tests then use these methods instead of interacting with raw locators.

```java
// LoginPage.java — the page object
public class LoginPage {
    private final Page page;

    public LoginPage(Page page) {
        this.page = page;
    }

    public HomePage login(String email, String password) {
        page.getByLabel("Email").fill(email);
        page.getByLabel("Password").fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();
        return new HomePage(page);
    }

    public LoginPage loginExpectingError(String email, String password) {
        page.getByLabel("Email").fill(email);
        page.getByLabel("Password").fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();
        return this;  // Stays on login page (login failed)
    }

    public String getErrorMessage() {
        return page.locator(".error-message").textContent();
    }
}
```

```java
// Test — clean, readable, maintainable
LoginPage loginPage = new LoginPage(page);
HomePage homePage = loginPage.login("user@test.com", "pass123");
assertThat(page).hasURL(Pattern.compile(".*dashboard.*"));
```

If the login button changes? Fix it in ONE place — `LoginPage.java`. All 50 tests automatically work.

### The Three Benefits (For Interviews)

**1. Maintainability** — UI changes require updates in one place, not hundreds. This is the #1 benefit.

**2. Readability** — Tests read like user stories:
```java
loginPage.login("user@test.com", "pass");
homePage.searchProduct("Blue Top");
productPage.addToCart();
cartPage.proceedToCheckout();
```
Even a non-technical person can understand what this test does.

**3. Reusability** — The `login()` method is written once and used in every test that needs login. `addToCart()` is used in cart tests, checkout tests, order history tests.

### Four Design Rules — Deep Understanding

#### Rule 1: Return the Next Page Object

When an action navigates to a different page, return a new instance of that page's class:

```java
public class LoginPage {
    public HomePage login(String email, String password) {
        // ... fill and click ...
        return new HomePage(page);  // After login, user lands on HomePage
    }
}

public class HomePage {
    public ProductPage goToProducts() {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Products")).click();
        return new ProductPage(page);
    }
}
```

This creates a **fluent chain** that mirrors the user journey:
```java
HomePage home = loginPage.login("user@test.com", "pass");
ProductPage products = home.goToProducts();
products.addToCart("Blue Top");
```

If the action stays on the same page (like an error case), return `this`:
```java
public LoginPage loginExpectingError(String email, String password) {
    // ... fill and click ...
    return this;  // Still on login page
}
```

**Why does this matter?** It makes the test self-documenting. When a method returns `HomePage`, anyone reading the code knows "after this action, the user is on the home page."

#### Rule 2: No Assertions Inside Page Objects

```java
// BAD — page object judges pass/fail
public class LoginPage {
    public void login(String email, String pass) {
        // ... fill and click ...
        assertThat(page).hasURL("/dashboard");  // DON'T — the page object shouldn't decide what's correct
    }
}

// GOOD — page object just does the action, test decides what to verify
public class LoginPage {
    public HomePage login(String email, String pass) {
        // ... fill and click ...
        return new HomePage(page);  // Just does the action, returns the next page
    }
}

// In the test:
loginPage.login("user@test.com", "pass123");
assertThat(page).hasURL(Pattern.compile(".*dashboard.*"));  // Test decides what to check
```

**Why?** Because different tests need different assertions after the same action. One test verifies the URL. Another test verifies the welcome message. Another test verifies the user's name in the header. If the assertion is inside the page object, you'd need separate login methods for each test. That defeats the purpose of reusability.

The page object is a **service** — it performs actions. The test is the **judge** — it decides what's correct.

**Exception:** Getter methods are fine. `getErrorMessage()`, `getCartTotal()`, `isLoggedIn()` — these return data. The test then asserts on the returned value.

#### Rule 3: Expose User Behavior, Not Element Details

```java
// BAD — exposes implementation details
public Locator getEmailField() { return page.locator("#email"); }
public Locator getPasswordField() { return page.locator("#password"); }
public Locator getLoginButton() { return page.locator("#login-btn"); }

// GOOD — exposes what users DO
public HomePage login(String email, String password) { ... }
public LoginPage loginExpectingError(String email, String password) { ... }
public String getErrorMessage() { ... }
```

Ask yourself: "Would a real user say this?" A user says "I want to log in," not "I want to get the email field and set its value." Model your methods after user behavior.

**Why does this matter beyond aesthetics?** If your tests call `getEmailField()` and the developer changes the email input to a different element, you need to update both the page object AND every test that calls `getEmailField()`. But if tests call `login()`, you only update the page object. The test never knew what `#email` was — it just said "log me in."

#### Rule 4: One Page Object Per Page/Component

```
pages/
├── HomePage.java           -- The main page
├── LoginPage.java           -- Login/signup page
├── ProductPage.java         -- Product listing page
├── ProductDetailPage.java   -- Individual product detail
├── CartPage.java            -- Shopping cart
├── CheckoutPage.java        -- Checkout flow
└── components/
    ├── HeaderComponent.java  -- Header (appears on every page)
    └── FooterComponent.java  -- Footer (appears on every page)
```

For shared components (header with search, navigation, user menu), create separate component classes. Page objects can compose them:

```java
public class HomePage {
    private final Page page;
    private final HeaderComponent header;

    public HomePage(Page page) {
        this.page = page;
        this.header = new HeaderComponent(page);
    }

    public ProductPage searchProduct(String term) {
        return header.search(term);
    }
}
```

### POM With BaseTest — The Full Picture

```java
// BaseTest.java — manages Playwright lifecycle (not a page object)
public class BaseTest {
    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    @BeforeClass
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @BeforeMethod
    void createContext() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterMethod
    void closeContext() { context.close(); }

    @AfterClass
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }
}

// LoginTest.java — uses page objects, extends BaseTest
public class LoginTest extends BaseTest {

    @Test
    void testSuccessfulLogin() {
        page.navigate(ConfigReader.getBaseUrl() + "/login");
        LoginPage loginPage = new LoginPage(page);

        HomePage homePage = loginPage.login("test@test.com", "pass123");
        assertThat(page).hasURL(Pattern.compile(".*dashboard.*"));
    }

    @Test
    void testInvalidLogin() {
        page.navigate(ConfigReader.getBaseUrl() + "/login");
        LoginPage loginPage = new LoginPage(page);

        loginPage.loginExpectingError("bad@email.com", "wrongpass");
        assertEquals(loginPage.getErrorMessage(), "Invalid credentials");
    }
}
```

Notice how each test gets a **fresh context and page** from BaseTest (clean state), creates a page object with that page, and uses high-level methods. The test reads like a story.

### POM in Playwright vs Selenium — One Key Difference

Selenium had **Page Factory** with `@FindBy` annotations:
```java
// Selenium Page Factory — NOT used in Playwright
@FindBy(id = "email")
WebElement emailField;  // Element found at class initialization, can go stale!
```

Playwright doesn't need Page Factory because locators are lazy — they find elements fresh every time. You just create locators inline in your methods:
```java
// Playwright — locators are lazy, always fresh
public void login(String email, String pass) {
    page.getByLabel("Email").fill(email);  // Finds fresh, fills
    page.getByLabel("Password").fill(pass); // Finds fresh, fills
}
```

No `@FindBy`, no `PageFactory.initElements()`, no stale elements. Simpler and more reliable.

### Interview Q&A

**Q: What is Page Object Model and why do you use it?**

A: "Page Object Model is a design pattern where each page of the application has a corresponding Java class that encapsulates all the interactions for that page. Instead of writing raw locators and actions in every test, I create methods like `login()`, `addToCart()`, `searchProduct()` that hide the implementation details.

I use it for three main reasons. First, maintainability — if the UI changes, like a button's ID changes or an element moves, I update one page class instead of every test that touches that element. Second, readability — my tests read like user stories: `loginPage.login()`, `homePage.searchProduct('shirt')`, `cartPage.checkout()`. Even someone who doesn't code can understand what the test does. Third, reusability — the `login()` method is written once and reused in every test that needs authentication."

**Q: Should page objects contain assertions?**

A: "No, and this is a design principle I feel strongly about. Page objects are services — they perform actions and return data. Tests are the judges — they decide what's correct.

The reason is reusability. Different tests need different assertions after the same action. One test checks the URL after login. Another checks the welcome message. Another checks the sidebar content. If I put assertions inside the page object's `login()` method, I'd need different login methods for each test. That defeats the whole purpose.

What I do instead is have methods that return values — `getErrorMessage()`, `getCartTotal()`, `getProductCount()` — and the test asserts on those returned values. The page object collects information; the test makes judgments."

**Q: How do you handle page navigation in POM?**

A: "Methods that trigger navigation return an instance of the next page's class. So `loginPage.login()` returns a `new HomePage(page)` because after successful login, the user lands on the home page. `homePage.goToProducts()` returns a `new ProductPage(page)`.

This creates a natural chain that mirrors the user journey. And it makes the code self-documenting — when I see a method return `HomePage`, I know the user ends up on the home page after that action. If an action stays on the same page, like a failed login, the method returns `this`."

**Q: What's the difference between POM and Page Factory?**

A: "Page Factory is a Selenium-specific pattern that uses `@FindBy` annotations to declare elements at the class level. They're initialized when the class is created. The problem is those references can go stale — if the page re-renders, the stored element reference is no longer valid.

In Playwright, Page Factory isn't needed and doesn't exist. Playwright locators are lazy — they don't find the element until you actually use them, and they find it fresh every time. So in my page objects, I just create locators inline in my methods. No annotations, no initialization step, and no stale element issues. It's simpler and more reliable."

**Q: How do you handle shared components like a header that appears on every page?**

A: "I create a separate component class — like `HeaderComponent` — that contains methods for header interactions: `search()`, `goToCart()`, `logout()`, etc. Then I compose it into page objects that have that header. The `HomePage`, `ProductPage`, and `CartPage` all contain a `HeaderComponent` instance. This way, header interactions are defined once and reused across all pages.

An alternative is inheritance — a `BasePage` class with common header methods that all page objects extend. But I prefer composition because it's more flexible. Not every page might have a header (like a login page), and some pages might have different versions of the header."

---

## Topic 10: TestNG Essentials

### The Problem This Solves

You've written test methods. But who decides which tests run, in what order, with what data, what happens before and after each test, and how results are reported? That's TestNG's job. It's the **orchestrator** — it manages everything around your test logic.

Without a test framework, you'd have to write a `main()` method, manually call each test, handle exceptions, track pass/fail, generate reports. TestNG does all of this (and more) with annotations.

### The Annotation Lifecycle — Deep Understanding

TestNG annotations are **hooks** — they let you run code at specific moments in the test lifecycle. Understanding the hierarchy is crucial:

```
@BeforeSuite     -- Once before ALL tests in the entire suite
  @BeforeTest    -- Once before each <test> group in testng.xml
    @BeforeClass -- Once before the first test METHOD in a class
      @BeforeMethod -- Before EVERY individual @Test method
        @Test        -- Your actual test
      @AfterMethod  -- After EVERY individual @Test method
    @AfterClass  -- Once after the last test method in a class
  @AfterTest     -- Once after each <test> group
@AfterSuite      -- Once after ALL tests in the entire suite
```

#### How This Maps to Playwright

The lifecycle maps perfectly to Playwright's resource model:

**@BeforeClass (static)** — Launch Browser
Browser is expensive (spawns a process). Do it once, share it across all tests in the class.

**@BeforeMethod** — Create BrowserContext + Page
Context is cheap but provides test isolation. Every test gets a fresh incognito-like session.

**@Test** — Your actual test logic. Navigate, interact, assert.

**@AfterMethod** — Close BrowserContext
Closes all pages in the context. Cleans up cookies, storage — ready for the next test.

**@AfterClass (static)** — Close Browser + Playwright
Release the browser process and kill the Node.js process.

```java
public class BaseTest {
    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    @BeforeClass
    static void setup() {
        playwright = Playwright.create();        // Start the engine
        browser = playwright.chromium().launch(); // Launch once, share everywhere
    }

    @BeforeMethod
    void createContext() {
        context = browser.newContext();  // Fresh isolation per test
        page = context.newPage();        // Fresh tab per test
    }

    @AfterMethod
    void tearDown() {
        context.close();  // Clean up — closes page too
    }

    @AfterClass
    static void cleanup() {
        browser.close();     // Release browser process
        playwright.close();  // Kill Node.js process
    }
}
```

**Why does this structure matter for interviews?** Because it shows you understand both Playwright's resource model AND TestNG's lifecycle. Interviewers want to know you can make architectural decisions, not just write code.

### @Test Annotation — The Options

The `@Test` annotation has several options that control how tests run:

```java
@Test(description = "Verify login with valid credentials")
// description — documents what the test does (shows in reports)

@Test(enabled = false)
// enabled — set to false to skip this test without deleting it

@Test(priority = 1)
// priority — controls execution order. Lower number = runs first.
// Default is 0. Tests with same priority run in alphabetical order.

@Test(dependsOnMethods = "testLogin")
// dependsOnMethods — this test only runs if testLogin PASSES.
// If testLogin fails, this test is SKIPPED (not failed).

@Test(expectedExceptions = IllegalArgumentException.class)
// expectedExceptions — test PASSES only if this exception is thrown.
// If no exception or a different exception → test FAILS.

@Test(timeOut = 10000)
// timeOut — test fails if it takes longer than 10 seconds (in milliseconds).

@Test(invocationCount = 3)
// invocationCount — runs this test 3 times. Useful for checking flakiness.

@Test(groups = {"smoke", "regression"})
// groups — tags for organizing tests. You can run specific groups.
```

#### Dependencies — When Order Matters

```java
@Test(priority = 1)
void testLogin() { ... }

@Test(priority = 2, dependsOnMethods = "testLogin")
void testAddToCart() { ... }

@Test(priority = 3, dependsOnMethods = "testAddToCart")
void testCheckout() { ... }
```

If `testLogin` fails → `testAddToCart` is **SKIPPED** (not failed) → `testCheckout` is also **SKIPPED**. The report shows 1 failure and 2 skips, which is more accurate than 3 failures.

**Warning:** Over-using dependencies creates fragile chains. If an early test fails, everything after it is skipped. Use dependencies only for truly sequential workflows (like login → add to cart → checkout in a single user journey test).

### DataProvider — Data-Driven Testing

#### The Concept

DataProvider separates **test logic** from **test data**. You write the test once, and DataProvider feeds it different sets of data. The test runs once per data row.

Think of it like a recipe (the test) and ingredients (the data). Same recipe, different ingredients, different dishes.

#### How It Works

```java
// Step 1: Create the DataProvider — returns a 2D array
@DataProvider(name = "loginData")
public Object[][] loginTestData() {
    return new Object[][] {
        // Each row = one test execution
        // { email,               password,    shouldPass }
        { "valid@test.com",       "pass123",   true  },    // Row 1 — valid login
        { "invalid@test.com",     "wrong",     false },    // Row 2 — invalid credentials
        { "",                     "pass123",   false },    // Row 3 — empty email
        { "valid@test.com",       "",          false },    // Row 4 — empty password
        { "sql@inject.com'--",   "hack",      false },    // Row 5 — SQL injection attempt
    };
}

// Step 2: Connect the test to the DataProvider
@Test(dataProvider = "loginData")
void testLogin(String email, String password, boolean shouldPass) {
    page.navigate(baseUrl + "/login");
    LoginPage loginPage = new LoginPage(page);

    loginPage.login(email, password);

    if (shouldPass) {
        assertThat(page).hasURL(Pattern.compile(".*dashboard.*"));
    } else {
        assertThat(page.locator(".error-message")).isVisible();
    }
}
// This test runs 5 times — once for each row
```

The `Object[][]` is a 2D array. The outer array is the list of test runs. Each inner array is one set of parameters. The test method's parameter names match the order of values in each row.

#### DataProvider in a Separate Class

For larger frameworks, centralize data providers:

```java
// DataProviders.java — separate class for all data providers
public class DataProviders {

    @DataProvider(name = "searchTerms")
    public static Object[][] searchTerms() {  // MUST be static when in separate class
        return new Object[][] {
            {"Blue Top"},
            {"Men Tshirt"},
            {"Dress"}
        };
    }

    @DataProvider(name = "loginData")
    public static Object[][] loginData() {
        return new Object[][] {
            {"valid@test.com", "pass123", true},
            {"invalid@test.com", "wrong", false}
        };
    }
}

// In test class — reference with dataProviderClass
@Test(dataProvider = "searchTerms", dataProviderClass = DataProviders.class)
void testSearch(String term) {
    homePage.searchProduct(term);
    assertThat(page.locator(".product-list")).isVisible();
}
```

**Note the `static` keyword** — when the DataProvider is in a different class, the method MUST be static. This is a common source of errors.

#### DataProvider From External Files

For real-world frameworks, data often comes from files:

```java
@DataProvider(name = "productData")
public Object[][] productData() throws Exception {
    // Read from JSON, CSV, Excel, or database
    String json = new String(Files.readAllBytes(Paths.get("test-data/products.json")));
    // Parse and return as Object[][]
}
```

### Groups — Organizing Tests

Groups let you tag tests and run subsets:

```java
@Test(groups = {"smoke"})
void testHomePageLoads() { }

@Test(groups = {"smoke", "regression"})
void testLogin() { }

@Test(groups = {"regression"})
void testComplexCheckoutFlow() { }

@Test(groups = {"regression", "payment"})
void testCreditCardPayment() { }
```

A test can belong to multiple groups. You control which groups run via testng.xml:

```xml
<test name="Smoke Tests">
    <groups>
        <run>
            <include name="smoke"/>        <!-- Only run smoke tests -->
            <exclude name="payment"/>      <!-- Exclude payment tests -->
        </run>
    </groups>
    <packages>
        <package name="tests.*"/>
    </packages>
</test>
```

**Common group strategy:**
- `smoke` — critical paths that must always work (login, homepage, core features). Run on every commit.
- `regression` — comprehensive tests. Run nightly or before releases.
- `api` — API tests that don't need a browser. Fast, run frequently.
- Feature-specific groups (`payment`, `search`, `auth`) — run when that feature changes.

### testng.xml — The Master Configuration

This is the central file that controls your entire test suite:

```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<suite name="AutomationExercise Suite" verbose="1" parallel="tests" thread-count="3">

    <test name="Smoke Tests">
        <classes>
            <class name="tests.smoke.NavigationSmokeTest"/>
            <class name="tests.smoke.LoginSmokeTest"/>
        </classes>
    </test>

    <test name="Regression - Products">
        <packages>
            <package name="tests.products.*"/>
        </packages>
    </test>

    <test name="Regression - Cart">
        <classes>
            <class name="tests.cart.CartTest"/>
            <class name="tests.cart.CartEdgeCaseTest"/>
        </classes>
    </test>

    <listeners>
        <listener class-name="utils.ExtentReportListener"/>
        <listener class-name="utils.RetryListener"/>
    </listeners>

</suite>
```

**Key attributes:**

`parallel` — How to parallelize:
- `"tests"` — each `<test>` block runs in its own thread
- `"classes"` — each test class runs in its own thread
- `"methods"` — each test method runs in its own thread (most aggressive)

`thread-count` — How many threads to use for parallel execution.

`verbose` — Logging level (0-10). Higher = more output.

### Listeners — Hooking Into Test Events

Listeners let you execute code when test events happen — before a test starts, after it passes, when it fails:

```java
public class TestListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        System.out.println("Starting: " + result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("PASSED: " + result.getName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.out.println("FAILED: " + result.getName());
        // This is where you'd:
        // 1. Take a screenshot
        // 2. Capture Playwright trace
        // 3. Log the error
        // 4. Attach evidence to report
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("SKIPPED: " + result.getName());
    }
}
```

**Most important use case: Screenshot on failure.** Inside `onTestFailure()`, you capture a screenshot with `page.screenshot()` and attach it to your report (Extent Reports, Allure). This gives you visual evidence of what the page looked like when the test failed — invaluable for debugging.

Register listeners in testng.xml or with `@Listeners` annotation on the test class.

### Retry Failed Tests

For legitimately flaky tests (network timeouts, third-party service issues), you can auto-retry:

```java
public class RetryAnalyzer implements IRetryAnalyzer {
    private int retryCount = 0;
    private static final int MAX_RETRY = 2;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY) {
            retryCount++;
            return true;   // Yes, retry this test
        }
        return false;      // No more retries, mark as failed
    }
}

// Attach to a test:
@Test(retryAnalyzer = RetryAnalyzer.class)
void testFlakyExternalService() { }
```

**Important caveat:** Retries are a band-aid, not a solution. If a test needs retries, investigate the root cause. Common causes:
- Timing issues → fix with proper waits
- Third-party scripts → block with route interception
- Shared test data → use unique data per test
- Environment issues → make the test environment more stable

### Soft Assertions (TestNG's Version)

TestNG has its own `SoftAssert` for data assertions (separate from Playwright's `SoftAssertions`):

```java
SoftAssert softAssert = new SoftAssert();
softAssert.assertEquals(title, "Home Page", "Title mismatch");
softAssert.assertTrue(isLoggedIn, "User should be logged in");
softAssert.assertEquals(cartCount, 3, "Cart should have 3 items");
softAssert.assertAll();  // Throws if ANY assertion failed, reports ALL failures
```

**When to use which:**
- Playwright's `SoftAssertions` → for page/element checks (auto-waits)
- TestNG's `SoftAssert` → for data/logic checks (instant)

### TestNG vs JUnit — Why TestNG for Automation

| Feature | TestNG | JUnit 5 |
|---|---|---|
| DataProvider | Built-in, powerful `Object[][]` | `@ParameterizedTest` with sources |
| Groups | `@Test(groups=...)`, XML control | `@Tag` |
| Parallel execution | Built-in via XML config | Requires `junit-platform.properties` |
| Test dependencies | `dependsOnMethods`, `dependsOnGroups` | `@Order` (weaker, no skip-on-fail) |
| Suite configuration | testng.xml — powerful, flexible | No direct equivalent |
| Listeners | Rich `ITestListener` API | Extensions API |
| Built-in reporting | HTML report out of the box | Needs plugin |
| Retry | `IRetryAnalyzer` built-in | Needs extension |

**Why TestNG is the standard for Java automation:** It was designed for end-to-end testing. DataProviders, XML-based suite management, built-in parallel execution, rich listener API, and dependency management make it more suitable for automation frameworks than JUnit, which was designed primarily for unit testing.

### Interview Q&A

**Q: What is TestNG and why do you use it over JUnit?**

A: "TestNG is a testing framework for Java that provides everything needed to manage test execution — annotations for setup and teardown, DataProviders for data-driven testing, test grouping, parallel execution, XML-based suite configuration, and a rich listener API for reporting.

I prefer TestNG over JUnit for automation frameworks because it was designed for end-to-end testing. DataProviders are more flexible and easier to use than JUnit's parameterized tests. The testng.xml file gives me fine-grained control over which tests run, in what order, and how many in parallel. The dependency feature lets me skip downstream tests when a prerequisite fails — so if login fails, the checkout test is skipped rather than failing with a confusing error. And the listener API makes it easy to add screenshots on failure, custom reporting, and retry logic."

**Q: Explain the TestNG annotation lifecycle and how it maps to Playwright.**

A: "TestNG has a hierarchy of annotations that run at different scopes. At the broadest level, `@BeforeSuite` and `@AfterSuite` run once for the entire suite. Then `@BeforeTest` and `@AfterTest` run for each test group defined in testng.xml. Then `@BeforeClass` and `@AfterClass` run once per test class. And `@BeforeMethod` and `@AfterMethod` run before and after every individual test method.

For Playwright, the mapping is natural. I create the Playwright instance and launch the browser in `@BeforeClass` — these are expensive operations that should happen once and be shared. In `@BeforeMethod`, I create a fresh BrowserContext and Page — this gives every test a clean, isolated environment with no shared cookies or state. After each test, `@AfterMethod` closes the context to clean up. And `@AfterClass` closes the browser and Playwright instance.

This maps perfectly because Browser is expensive (create once, share) and Context is cheap (create per test, provides isolation). The architecture gives you both performance and reliability."

**Q: What is DataProvider and how do you use it?**

A: "DataProvider is TestNG's way of doing data-driven testing. You create a method annotated with `@DataProvider` that returns a 2D `Object` array — each row is a set of parameters for one test execution. Then you connect your test method to it using `@Test(dataProvider = 'name')`.

For example, I have a login test that needs to run with valid credentials, invalid credentials, empty email, and empty password. Instead of writing four separate tests with the same logic, I write one test method that takes email, password, and expectedResult as parameters. The DataProvider feeds four rows of data, and the test runs four times — once per row.

For larger frameworks, I put DataProviders in a separate class and reference them with `dataProviderClass`. The methods must be static when they're in a different class. For complex scenarios, I might read data from JSON or CSV files inside the DataProvider."

**Q: How do you run tests in parallel with TestNG?**

A: "In testng.xml, I set `parallel` attribute on the suite tag and specify `thread-count`. `parallel='tests'` runs each `<test>` block in its own thread — this is the safest option for Playwright because each test block can have its own browser context. `parallel='classes'` runs classes in parallel, and `parallel='methods'` runs individual test methods in parallel — that's the most aggressive.

The key requirement for parallel Playwright tests is isolation. The static Browser can be shared across threads since Playwright handles that safely, but each thread needs its own BrowserContext and Page. Since I create these in `@BeforeMethod`, each parallel test automatically gets its own isolated context. No shared cookies, no shared state, no interference."

**Q: How do you take screenshots on test failure?**

A: "I implement TestNG's `ITestListener` interface and override the `onTestFailure()` method. Inside it, I capture a screenshot using `page.screenshot()` and save it to the reports directory, usually with the test name and timestamp as the filename. I also attach it to the Extent Report so it appears inline with the test failure.

The tricky part is accessing the `page` object from the listener, since the listener is separate from the test class. I usually store the Page reference in a ThreadLocal variable or use a utility method that the listener can call. This ensures each parallel thread captures the right screenshot."

**Q: What are test groups and how do you use them?**

A: "Groups are tags you attach to tests using `@Test(groups = {'smoke', 'regression'})`. A test can belong to multiple groups. Then in testng.xml, I control which groups to include or exclude when running the suite.

My typical strategy is: `smoke` group for critical paths like login, homepage, core features — these run on every commit in CI. `regression` group for comprehensive tests — these run nightly. Then feature-specific groups like `payment`, `search`, `auth` — these run when that feature's code changes.

This gives me flexibility. During development, I run just smoke tests for fast feedback. Before a release, I run the full regression suite. If I'm working on the payment feature, I run just the payment group."

**Q: Hard assert vs soft assert — when do you use each?**

A: "A hard assert — like `assertEquals()` — stops the test at the first failure. The test method exits immediately and moves to `@AfterMethod` cleanup. This is the default and is appropriate when subsequent steps depend on the assertion. For example, if I assert that login succeeded, there's no point continuing to test the dashboard if login failed.

A soft assert — using `SoftAssert` in TestNG or `SoftAssertions` in Playwright — collects all failures and only throws at the end when you call `assertAll()`. I use these when checking multiple independent things on one page. For example, on a product detail page, I might verify the name, price, description, image, and rating. These are all independent — knowing ALL failures at once is more useful than stopping at the first one. I'd have to run the test five times to discover five issues with hard asserts; with soft asserts, I discover all five in one run."

---

## Bonus: Framework Interview Questions

### "Tell Me About Your Test Automation Framework"

> "Our framework uses **Playwright with Java** and **TestNG**. We follow the **Page Object Model** pattern — each page of the application has a corresponding class that encapsulates locators and user actions. Tests use these page objects instead of raw locators, which makes them readable and maintainable.
>
> The **BaseTest** class manages the Playwright lifecycle. Browser is static and shared across tests for performance. Every test gets a fresh BrowserContext for isolation — separate cookies, storage, session — so tests never interfere with each other.
>
> **ConfigReader** loads environment-specific properties — we have different configs for local development and CI. Browser type, headless mode, base URL, and timeouts are all configurable without changing code.
>
> Tests are organized by category — smoke, regression, products, cart, checkout — using TestNG's group structure in **testng.xml**. We use **DataProviders** for data-driven tests, and **Extent Reports** for HTML reporting with screenshots on failure.
>
> One thing that makes our framework reliable is **route interception** in BaseTest — we block ad and tracking scripts that cause flaky tests. And we use Playwright's built-in tracing for debugging failures — it captures a timeline of actions, screenshots, and DOM snapshots that we can replay in the trace viewer."

### Common Follow-Up Questions

**Q: How do you handle flaky tests?**

A: "I approach flakiness at multiple levels. First, Playwright's auto-wait eliminates most timing-related flakiness — that's the biggest source in Selenium frameworks. Second, we block third-party scripts like ads and analytics using route interception, because those are the next biggest source of random failures. Third, we use unique test data per test — random emails and usernames from our TestDataGenerator — so tests don't conflict when running in parallel.

If a test is still flaky after all that, I investigate the root cause rather than just adding retries. We do have a RetryAnalyzer configured for max 2 retries, but that's a safety net, not a solution. Every flaky test gets a Jira ticket to investigate and fix the underlying issue."

**Q: How do you decide what to automate?**

A: "I use a value-based approach. First priority is smoke tests — critical user journeys like login, search, add to cart, checkout. These are high-value because they run frequently and catch the most impactful bugs. Second is repetitive regression tests — things that are tedious to test manually every sprint. Third is data-driven scenarios — like testing form validation with 20 different inputs, which is perfect for automation with DataProviders.

I don't automate everything. Things that change frequently (UI under active redesign), are tested once (migration scripts), or require human judgment (visual design, UX) are better left manual or addressed with other tools."

**Q: How do you handle test data?**

A: "We have a TestDataGenerator utility that creates random, unique data — random email addresses, names, passwords, and addresses with India-specific presets for our context. This ensures parallel tests don't conflict over shared data.

Configuration data like base URLs and browser settings come from properties files — `config.properties` for local, `config-ci.properties` for CI. Sensitive data like API keys are environment variables, never committed to git.

For data-driven tests, DataProviders feed structured data. For simple cases, the data is inline in the DataProvider method. For complex scenarios, we read from JSON files in the `test-data/` directory."

**Q: How does your framework integrate with CI/CD?**

A: "We detect the CI environment through an environment variable. When `CI=true`, the framework loads `config-ci.properties` which sets headless mode, shorter timeouts, and the CI-specific base URL. Tests run with `mvn clean test` which executes the testng.xml suite.

In the pipeline, tests run on every pull request. The Extent Reports HTML file and Playwright trace files are saved as build artifacts so anyone can review failures. For failing tests, the trace file is especially powerful — it shows a complete timeline of what the test did, with screenshots at each step and the DOM snapshot at the point of failure."

### Top 10 "Why Playwright Over Selenium?" — Quick Reference

| # | Area | Playwright Advantage |
|---|---|---|
| 1 | Waiting | Auto-waits on every action and assertion. No WebDriverWait needed. |
| 2 | Locators | Lazy (fresh every time). No StaleElementReferenceException. |
| 3 | Frames | `frameLocator()` — no switching. Both frame and main page accessible simultaneously. |
| 4 | Shadow DOM | Auto-pierces open shadow roots. No `getShadowRoot()` chains. |
| 5 | Tabs/Windows | Separate Page objects. No window handles, no switchTo(). |
| 6 | Speed | Direct browser protocol (CDP). No HTTP overhead like WebDriver protocol. |
| 7 | Browsers | Chromium, Firefox, WebKit from one API. No separate driver management. |
| 8 | Network | Built-in `page.route()` for interception, mocking, blocking. |
| 9 | Debugging | Built-in trace viewer — timeline, screenshots, DOM snapshots. |
| 10 | Assertions | Auto-retrying assertions that wait for conditions. |
