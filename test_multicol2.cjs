const puppeteer = require('puppeteer');
(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setViewport({ width: 1200, height: 800 });
  await page.goto('file://' + __dirname + '/test_multicol2.html');
  await page.screenshot({ path: '/Users/sayako/.gemini/antigravity-ide/brain/ad18de42-9184-4400-87bd-fb2fa612f4ac/screenshot_multicol2.png' });
  await browser.close();
})();
