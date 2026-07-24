import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  await page.setViewport({ width: 1200, height: 800 });
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('.auto-strip-paper', { timeout: 10000 });
  
  // Open sidebar
  await page.evaluate(() => {
    const btn = document.querySelector('button[title="メニュー"]');
    if (btn) btn.click();
  });
  await new Promise(r => setTimeout(r, 500));
  
  // Click layout menu
  await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('button'));
    const layoutBtn = btns.find(b => b.innerText.includes('横にページを追加') || b.innerText.includes('縦にページを追加'));
    if (layoutBtn) layoutBtn.click();
  });
  await new Promise(r => setTimeout(r, 100));
  
  // Select manual mode
  await page.evaluate(() => {
    const manualBtn = Array.from(document.querySelectorAll('li')).find(li => li.innerText.includes('縦にページを追加'));
    if (manualBtn) manualBtn.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
  });
  await new Promise(r => setTimeout(r, 500));
  
  // Add a new page
  await page.evaluate(() => {
    const addPageBtn = Array.from(document.querySelectorAll('button')).find(b => b.innerText.includes('新しいページを追加'));
    if (addPageBtn) addPageBtn.click();
  });
  await new Promise(r => setTimeout(r, 1000)); // wait for animation
  
  // Check the HTML of the vertical ruler
  const html = await page.evaluate(() => {
    const ruler = document.querySelector('.w-6.h-full.bg-surface-container-lowest');
    return ruler ? ruler.outerHTML : 'No ruler found';
  });
  
  console.log("Ruler HTML:\n", html);
  
  await browser.close();
})();
