import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  await page.goto('about:blank');
  
  await page.setContent(`
    <style>
      h1 { font-size: 32px; }
      p { font-size: 16px; }
      rt { font-size: 0.5em; }
    </style>
    <h1 id="test1">普通<ruby id="ruby1">漢字<rt>かんじ</rt></ruby></h1>
  `);
  
  const size = await page.evaluate(() => {
    const ruby = document.getElementById('ruby1');
    return window.getComputedStyle(ruby).fontSize;
  });
  
  console.log("Ruby font size:", size);
  await browser.close();
})();
