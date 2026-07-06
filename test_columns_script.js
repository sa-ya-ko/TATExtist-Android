const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('file:///Users/sayako/tategaki/tategaki_editor/public/test_columns.html');
  await page.screenshot({ path: 'test_columns.png' });
  await browser.close();
})();
