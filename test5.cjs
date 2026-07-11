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
      body { display: flex; direction: rtl; }
      .strip {
        width: max-content;
        height: 500px;
        background: repeating-linear-gradient(to left, white 0, white 200px, gray 200px, gray 250px);
      }
      .editor {
        writing-mode: vertical-rl;
        height: 100%;
      }
    </style>
    </head>
    <body>
      <div class="strip">
        <div class="editor" contenteditable="true">
          <script>
            for(let i=0; i<200; i++) {
              document.write('あいうえおかきくけこ');
            }
          </script>
        </div>
      </div>
    </body>
    </html>
  `);
  
  const width = await page.evaluate(() => {
    return document.querySelector('.strip').getBoundingClientRect().width;
  });
  
  console.log("Strip width:", width);
  await browser.close();
})();
