import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  // Emulate mobile
  await page.setViewport({ width: 375, height: 812, isMobile: true, hasTouch: true });
  
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('button[title="ファイル"]');
  
  // Tap the File button
  const btn = await page.$('button[title="ファイル"]');
  await btn.tap();
  
  await new Promise(r => setTimeout(r, 500));
  
  const isMenuVisible = await page.evaluate(() => {
    const ul = document.querySelector('ul.absolute');
    return ul ? ul.innerText.includes('パソコンから開く') : false;
  });
  
  console.log("File menu visible after tap?", isMenuVisible);
  
  await browser.close();
})();
