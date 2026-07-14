const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setContent(`
    <!DOCTYPE html>
    <html>
    <head>
    <style>
      .strip {
        writing-mode: horizontal-tb;
        height: 500px;
        column-width: 400px;
        column-gap: 50px;
        direction: rtl;
        background: #ccc;
      }
      .editor {
        writing-mode: vertical-rl;
        height: 100%;
        direction: ltr;
        background: yellow;
      }
    </style>
    </head>
    <body>
      <div class="strip">
        <div class="editor" id="ed"></div>
      </div>
    </body>
    </html>
  `);
  
  await page.evaluate(() => {
    const ed = document.getElementById('ed');
    let text = '';
    for(let i=0; i<100; i++) text += 'あいうえお<br>かきくけこ<br>';
    ed.innerHTML = text;
  });
  await new Promise(r => setTimeout(r, 500));
  
  let w = await page.evaluate(() => {
    return document.getElementById('ed').scrollWidth;
  });
  console.log("Editor scrollWidth inside multicol:", w);
  
  let stripW = await page.evaluate(() => {
    return document.querySelector('.strip').scrollWidth;
  });
  console.log("Strip scrollWidth:", stripW);
  
  await browser.close();
})();
