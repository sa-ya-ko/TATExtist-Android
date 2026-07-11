const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setContent(`
    <!DOCTYPE html>
    <html lang="ja">
    <head>
    <meta charset="UTF-8">
    <style>
      .scroll-container {
        writing-mode: horizontal-tb;
        height: 500px;
        direction: rtl; 
      }
      .editor {
        writing-mode: vertical-rl;
        height: 100%;
      }
    </style>
    </head>
    <body>
      <div class="scroll-container">
        <div class="editor" contenteditable="true">
          <script>
            for(let i=0; i<100; i++) {
              document.write('あいうえおかきくけこ');
            }
          </script>
        </div>
      </div>
    </body>
    </html>
  `);
  
  const rects = await page.evaluate(() => {
    const el = document.querySelector('.editor');
    const range = document.createRange();
    range.selectNodeContents(el);
    const rects = Array.from(range.getClientRects());
    return rects.map(r => ({ x: Math.round(r.x), y: Math.round(r.y), width: r.width, height: r.height }));
  });
  
  console.log(JSON.stringify(rects.slice(0, 5), null, 2));
  console.log(JSON.stringify(rects.slice(-5), null, 2));
  await browser.close();
})();
