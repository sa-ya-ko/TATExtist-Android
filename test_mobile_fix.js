import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  await page.setViewport({ width: 375, height: 812, isMobile: true, hasTouch: true });
  
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('button[title="ファイル"]');
  
  // Apply fix in browser memory to test it
  await page.evaluate(() => {
    const btn = document.querySelector('button[title="ファイル"]');
    const container = btn.closest('.overflow-x-auto');
    container.classList.remove('overflow-x-auto', 'flex-nowrap', '[&::-webkit-scrollbar]:hidden');
    container.classList.add('flex-wrap');
  });
  
  const btn = await page.$('button[title="ファイル"]');
  await btn.tap();
  
  await new Promise(r => setTimeout(r, 500));
  
  const isMenuVisible = await page.evaluate(() => {
    const ul = document.querySelector('ul.absolute');
    if (!ul) return false;
    const rect = ul.getBoundingClientRect();
    return rect.height > 50; // ensure it's not clipped to 32px
  });
  
  console.log("Menu properly visible?", isMenuVisible);
  
  await browser.close();
})();
