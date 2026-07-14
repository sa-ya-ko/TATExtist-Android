const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setViewport({ width: 1200, height: 800 });
  await page.goto('file://' + __dirname + '/test_scroll.html');
  await page.screenshot({ path: '/Users/sayako/.gemini/antigravity-ide/brain/ad18de42-9184-4400-87bd-fb2fa612f4ac/screenshot_scroll.png' });
  
  // Add a lot of text to see if it grows leftwards
  await page.evaluate(() => {
    const ed = document.getElementById('ed');
    let text = '';
    for(let i=0; i<100; i++) text += 'あいうえお<br>かきくけこ<br>';
    ed.innerHTML = text;
  });
  await new Promise(r => setTimeout(r, 500));
  await page.screenshot({ path: '/Users/sayako/.gemini/antigravity-ide/brain/ad18de42-9184-4400-87bd-fb2fa612f4ac/screenshot_scroll_grown.png' });
  
  await browser.close();
})();
