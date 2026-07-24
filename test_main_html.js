import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  await page.goto('http://localhost:3000');
  await new Promise(r => setTimeout(r, 2000));
  
  const html = await page.evaluate(() => {
    const main = document.querySelector('main');
    return main ? main.outerHTML : 'No main found';
  });
  console.log("MAIN HTML:", html);
  
  await browser.close();
})();
