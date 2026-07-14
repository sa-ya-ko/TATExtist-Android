const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setViewport({ width: 1200, height: 800 });
  await page.goto('file://' + __dirname + '/test_decoupled.html');
  
  await new Promise(r => setTimeout(r, 1000));
  
  // Type into the editor to trigger Vue reactivity
  await page.evaluate(() => {
    const ed = document.querySelector('.editor');
    ed.focus();
    let text = '';
    for(let i=0; i<100; i++) text += 'あいうえお<br>かきくけこ<br>';
    ed.innerHTML = text;
    ed.dispatchEvent(new Event('input', { bubbles: true }));
  });
  
  await new Promise(r => setTimeout(r, 1000));
  await page.screenshot({ path: '/Users/sayako/.gemini/antigravity-ide/brain/ad18de42-9184-4400-87bd-fb2fa612f4ac/screenshot_decoupled.png' });
  await browser.close();
})();
