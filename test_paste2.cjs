const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setViewport({ width: 1200, height: 800 });
  await page.goto('http://localhost:3000');
  
  await new Promise(r => setTimeout(r, 1000));
  
  // Set to auto mode
  await page.select('select', 'auto');
  await new Promise(r => setTimeout(r, 500));
  
  // Paste large text
  await page.evaluate(() => {
    const editor = document.querySelector('.writing-vertical');
    let text = '';
    for(let i=0; i<5000; i++) text += 'あいうえお';
    editor.innerHTML = text;
    editor.dispatchEvent(new Event('input', { bubbles: true }));
  });
  
  await new Promise(r => setTimeout(r, 1000));
  await page.screenshot({ path: 'artifacts/screenshot_paste_overflow.png' });
  
  await browser.close();
})();
