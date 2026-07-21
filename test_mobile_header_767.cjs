const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  
  await page.setViewport({ width: 767, height: 800 });
  
  await page.goto('http://localhost:3000');
  await page.waitForSelector('header');
  
  const metrics = await page.evaluate(() => {
    const header = document.querySelector('header');
    return {
      viewportWidth: window.innerWidth,
      headerWidth: header.getBoundingClientRect().width,
      headerScrollWidth: header.scrollWidth,
    };
  });
  
  console.log("Metrics on 767px screen (Mobile mode):", metrics);

  await page.setViewport({ width: 768, height: 800 });
  await new Promise(r => setTimeout(r, 500));
  
  const metricsDesktop = await page.evaluate(() => {
    const header = document.querySelector('header');
    return {
      viewportWidth: window.innerWidth,
      headerWidth: header.getBoundingClientRect().width,
      headerScrollWidth: header.scrollWidth,
    };
  });
  
  console.log("Metrics on 768px screen (Desktop mode):", metricsDesktop);

  await browser.close();
})();
