const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('file:///Users/sayako/tategaki/tategaki_editor/public/test_columns3.html');
  
  const rects = await page.evaluate(() => {
    const el = document.querySelector('.editor');
    const range = document.createRange();
    range.selectNodeContents(el);
    const rects = Array.from(range.getClientRects());
    return rects.map(r => ({ x: Math.round(r.x), y: Math.round(r.y), width: Math.round(r.width), height: Math.round(r.height) }));
  });
  
  console.log("Total text nodes:", rects.length);
  console.log("First 5:", JSON.stringify(rects.slice(0, 5), null, 2));
  console.log("Last 5:", JSON.stringify(rects.slice(-5), null, 2));
  await browser.close();
})();
