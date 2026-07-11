const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('file:///Users/sayako/tategaki/tategaki_editor/public/test_columns3.html');
  
  const rects = await page.evaluate(() => {
    // Get the bounding rect of the editor itself
    const el = document.querySelector('.editor');
    // Get all text nodes and their positions
    const range = document.createRange();
    range.selectNodeContents(el);
    const rects = Array.from(range.getClientRects());
    return rects.map(r => ({ x: r.x, y: r.y, width: r.width, height: r.height }));
  });
  
  console.log(JSON.stringify(rects.slice(0, 5), null, 2));
  console.log(JSON.stringify(rects.slice(-5), null, 2));
  await browser.close();
})();
