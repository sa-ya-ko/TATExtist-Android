import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  await page.setViewport({ width: 375, height: 812, isMobile: true, hasTouch: true });
  
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('button[title="ファイル"]');
  
  const btn = await page.$('button[title="ファイル"]');
  await btn.tap();
  
  await new Promise(r => setTimeout(r, 500));
  
  // Check the height of the overflow container
  const overflowContainerHeight = await page.evaluate(() => {
    const btn = document.querySelector('button[title="ファイル"]');
    const container = btn.closest('.overflow-x-auto');
    return container.clientHeight;
  });
  
  console.log("Overflow container height:", overflowContainerHeight);
  
  // Screenshot to see if it's clipped
  await page.screenshot({ path: 'clip_test.png' });
  
  await browser.close();
})();
