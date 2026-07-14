const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setViewport({ width: 1200, height: 800 });
  await page.goto('file://' + __dirname + '/test_rtl_minwidth_bug.html');
  
  await new Promise(r => setTimeout(r, 2000));
  
  await page.screenshot({ path: '/Users/sayako/.gemini/antigravity-ide/brain/ad18de42-9184-4400-87bd-fb2fa612f4ac/screenshot_rtl_minwidth.png' });
  await browser.close();
})();
