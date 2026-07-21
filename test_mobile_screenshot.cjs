const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  
  await page.setViewport({ width: 375, height: 667, isMobile: true, hasTouch: true });
  await page.goto('http://localhost:3000');
  await page.waitForSelector('header');
  
  // Wait a bit for fonts and layout
  await new Promise(r => setTimeout(r, 2000));
  
  const metrics = await page.evaluate(() => {
    return {
      windowInnerWidth: window.innerWidth,
      documentBodyScrollWidth: document.body.scrollWidth,
      headerScrollWidth: document.querySelector('header').scrollWidth,
      row2ScrollWidth: document.querySelector('header > div > div:nth-child(2)').scrollWidth
    };
  });
  console.log(metrics);

  await page.screenshot({ path: '/Users/sayako/tategaki/tategaki_editor/mobile_screenshot.png' });
  
  await browser.close();
})();
