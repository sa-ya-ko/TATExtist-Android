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

  // Set to continuous
  await page.evaluate(() => {
    const selects = document.querySelectorAll('select');
    if (selects.length >= 2) {
      selects[1].value = 'continuous';
      selects[1].dispatchEvent(new Event('change'));
    }
  });
  await new Promise(r => setTimeout(r, 500));
  
  await page.evaluate(() => {
    const editor = document.querySelector('.writing-vertical');
    editor.innerHTML = 'あいうえお<br>かきくけこ<br>さしすせそ<br>たちつてと';
    editor.dispatchEvent(new Event('input', { bubbles: true }));
  });
  
  await new Promise(r => setTimeout(r, 1000));
  await page.screenshot({ path: '/Users/sayako/.gemini/antigravity-ide/brain/ad18de42-9184-4400-87bd-fb2fa612f4ac/screenshot_overlap2.png' });
  
  await browser.close();
})();
