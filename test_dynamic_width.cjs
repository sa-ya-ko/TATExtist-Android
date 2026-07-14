const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setViewport({ width: 1200, height: 800 });
  await page.goto('file://' + __dirname + '/test_dynamic_width.html');
  
  await new Promise(r => setTimeout(r, 2000)); // wait for script
  
  await page.screenshot({ path: '/Users/sayako/.gemini/antigravity-ide/brain/ad18de42-9184-4400-87bd-fb2fa612f4ac/screenshot_dynamic.png' });
  
  await browser.close();
})();
