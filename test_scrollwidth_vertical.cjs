const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setContent(`
    <!DOCTYPE html>
    <html>
    <head>
    <style>
      .editor {
        writing-mode: vertical-rl;
        height: 500px;
        display: inline-block;
        background: yellow;
      }
    </style>
    </head>
    <body>
      <div class="editor" id="ed"></div>
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
  let rect = await page.evaluate(() => {
    return document.getElementById('ed').getBoundingClientRect().width;
  });
  console.log("scrollWidth:", w);
  console.log("getBoundingClientRect().width:", rect);
  
  await browser.close();
})();
