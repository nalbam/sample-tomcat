// Browser check for GET / of a running sample-tomcat.
// Loads the page at a large and a small viewport and fails on new network,
// console or page errors, unrendered JSTL/EL, missing icons, unstyled
// buttons or horizontal overflow. Screenshots go to $OUT (default target/browser).
//
// Usage: NODE_PATH=<dir>/node_modules node scripts/browser-check.cjs <page_url> [expected_text ...]
// Needs Playwright with Chromium: npm i playwright && npx playwright install chromium
const fs = require("fs");
const path = require("path");
const { chromium } = require("playwright");

// Known failure before this check existed: the external counter script returns 404.
// It is reported, but it does not fail the check.
const KNOWN_FAILURES = ["https://nalbam.com/static/sample/counter.js"];

const VIEWPORTS = [
  { name: "large", width: 1280, height: 800 },
  { name: "small", width: 375, height: 667 },
];

const pageUrl = process.argv[2];
const expected = process.argv.slice(3);
if (!pageUrl) {
  console.error("usage: node scripts/browser-check.cjs <page_url> [expected_text ...]");
  process.exit(2);
}
const out = process.env.OUT || "target/browser";
fs.mkdirSync(out, { recursive: true });

const isKnown = (url) => KNOWN_FAILURES.includes(url);

async function check(browser, vp) {
  const errors = [];
  const known = [];
  const page = await browser.newPage({ viewport: { width: vp.width, height: vp.height } });

  page.on("response", (res) => {
    if (res.status() >= 400) {
      (isKnown(res.url()) ? known : errors).push(`HTTP ${res.status()} ${res.url()}`);
    }
  });
  page.on("requestfailed", (req) => {
    (isKnown(req.url()) ? known : errors).push(`request failed ${req.url()}: ${req.failure().errorText}`);
  });
  page.on("console", (msg) => {
    if (msg.type() === "error" && !isKnown(msg.location().url)) {
      errors.push(`console: ${msg.text()} (${msg.location().url})`);
    }
  });
  page.on("pageerror", (err) => errors.push(`page error: ${err.message}`));

  await page.goto(pageUrl, { waitUntil: "networkidle" });
  await page.evaluate(() => document.fonts.ready);

  const r = await page.evaluate(() => {
    const css = (el, prop, pseudo) => getComputedStyle(el, pseudo || null).getPropertyValue(prop);
    const box = (el) => el.getBoundingClientRect();
    const title = document.querySelector(".hello-title");
    const buttons = [...document.querySelectorAll("main button")];
    const icons = [...document.querySelectorAll("main button i")];
    const logo = document.querySelector("main img");
    return {
      text: document.body.innerText,
      html: document.body.innerHTML,
      host: title ? title.textContent.trim() : "",
      titleColor: title ? css(title, "color") : "",
      overflow: document.documentElement.scrollWidth - window.innerWidth,
      logo: logo ? logo.naturalWidth : 0,
      buttons: buttons.map((b) => ({
        bg: css(b, "background-color"),
        visible: box(b).width > 0 && box(b).right <= window.innerWidth,
      })),
      icons: icons.map((i) => ({
        content: css(i, "content", "::before"),
        family: css(i, "font-family", "::before"),
        width: box(i).width,
      })),
      faLoaded: [...document.fonts].some((f) => f.family.includes("Font Awesome") && f.status === "loaded"),
      localAssets: [...document.querySelectorAll("link[href], script[src]")]
        .map((e) => e.getAttribute("href") || e.getAttribute("src"))
        .filter((u) => u.startsWith("/")),
      jquery: window.jQuery ? window.jQuery.fn.jquery : null,
      bootstrap: window.bootstrap && window.bootstrap.Tooltip ? window.bootstrap.Tooltip.VERSION : null,
    };
  });

  if (r.html.includes("<c:") || r.text.includes("${")) errors.push("unrendered JSTL/EL");
  if (!r.host) errors.push("host is empty");
  for (const t of expected) if (!r.text.includes(t)) errors.push(`text not found: ${t}`);
  if (r.titleColor !== "rgb(0, 100, 0)") errors.push(`main.css not applied: .hello-title color ${r.titleColor}`);
  if (r.overflow > 0) errors.push(`horizontal overflow ${r.overflow}px`);
  if (r.logo === 0) errors.push("logo image not loaded");
  if (r.buttons.length !== 2) errors.push(`expected 2 buttons, found ${r.buttons.length}`);
  r.buttons.forEach((b, n) => {
    if (b.bg === "rgba(0, 0, 0, 0)") errors.push(`button ${n} has no Bootstrap style`);
    if (!b.visible) errors.push(`button ${n} is outside the viewport`);
  });
  if (!r.faLoaded) errors.push("Font Awesome font not loaded");
  r.icons.forEach((i, n) => {
    if (["none", "normal", ""].includes(i.content) || !i.family.includes("Font Awesome") || i.width === 0) {
      errors.push(`icon ${n} not rendered: content=${i.content} font=${i.family}`);
    }
  });
  const base = new URL(pageUrl).pathname.replace(/\/$/, "");
  for (const u of r.localAssets) {
    if (!u.startsWith(`${base}/static/`)) errors.push(`local asset outside context ${base || "/"}: ${u}`);
  }

  await page.screenshot({ path: path.join(out, `${vp.name}.png`), fullPage: true });
  await page.close();

  console.log(`[${vp.name} ${vp.width}x${vp.height}] jquery=${r.jquery} bootstrap=${r.bootstrap} assets=${r.localAssets.join(",")}`);
  known.forEach((k) => console.log(`  known failure (not a pass): ${k}`));
  errors.forEach((e) => console.log(`  ERROR ${e}`));
  return errors.length;
}

(async () => {
  const browser = await chromium.launch();
  let failed = 0;
  try {
    for (const vp of VIEWPORTS) failed += await check(browser, vp);
  } finally {
    await browser.close();
  }
  console.log(failed ? `BROWSER FAIL: ${failed} error(s)` : "BROWSER OK");
  process.exit(failed ? 1 : 0);
})();
