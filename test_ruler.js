import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  await page.setViewport({ width: 1200, height: 800 });
  await page.goto('http://localhost:3000');
  
  // Wait for editor
  await page.waitForSelector('.manuscript-paper');
  
  // Change to manual layout mode
  await page.evaluate(() => {
    // Open sidebar
    const btn = document.querySelector('button[title="メニュー"]');
    if (btn) btn.click();
  });
  await new Promise(r => setTimeout(r, 500));
  await page.evaluate(() => {
    const layoutBtn = Array.from(document.querySelectorAll('button')).find(b => b.innerText.includes('横にページを追加') || b.innerText.includes('縦にページを追加'));
    if (layoutBtn) layoutBtn.click();
  });
  await new Promise(r => setTimeout(r, 100));
  await page.evaluate(() => {
    const manualBtn = Array.from(document.querySelectorAll('li')).find(li => li.innerText.includes('縦にページを追加'));
    if (manualBtn) manualBtn.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
  });
  await new Promise(r => setTimeout(r, 500));
  
  // Add a second page
  await page.evaluate(() => {
    const addPageBtn = Array.from(document.querySelectorAll('button')).find(b => b.innerText.includes('新しいページを追加'));
    if (addPageBtn) addPageBtn.click();
  });
  await new Promise(r => setTimeout(r, 500));
  
  // Take screenshot to see what rulers look like
  await page.screenshot({ path: 'ruler_test.png' });
  
  console.log("Screenshot taken");
  
  await browser.close();
})();
