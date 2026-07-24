import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  await page.goto('about:blank');
  
  await page.setContent(`
    <style>
      h1 { font-size: 32px; font-weight: bold; }
    </style>
    <h1 id="test1">普通<ruby id="ruby1">漢字<rt id="rt1">かんじ</rt></ruby></h1>
  `);
  
  const styles = await page.evaluate(() => {
    const ruby = document.getElementById('ruby1');
    const rt = document.getElementById('rt1');
    return {
      rubyWeight: window.getComputedStyle(ruby).fontWeight,
      rtWeight: window.getComputedStyle(rt).fontWeight
    };
  });
  
  console.log("Weights:", styles);
  await browser.close();
})();
