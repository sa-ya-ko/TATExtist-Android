const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  
  await page.setViewport({ width: 767, height: 800 });
  await page.goto('http://localhost:3000');
  await page.waitForSelector('header');
  
  const metrics = await page.evaluate(() => {
    const header = document.querySelector('header');
    const rect = header.getBoundingClientRect();
    
    // Check heights of children
    const children = Array.from(header.querySelectorAll('*')).map(c => {
      const cr = c.getBoundingClientRect();
      return { tag: c.tagName, class: c.className, height: cr.height, top: cr.top };
    }).filter(c => c.height > rect.height);
    
    return {
      headerHeight: rect.height,
      headerScrollHeight: header.scrollHeight,
      overflowingChildren: children
    };
  });
  
  console.log("Metrics on 767px screen (Mobile mode):", metrics);
  await browser.close();
})();
