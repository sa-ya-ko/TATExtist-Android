const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  
  // Set narrow mobile viewport
  await page.setViewport({ width: 320, height: 568 });
  
  await page.goto('http://localhost:3000');
  await page.waitForSelector('header');
  
  const metrics = await page.evaluate(() => {
    const header = document.querySelector('header');
    const headerRect = header.getBoundingClientRect();
    const bodyRect = document.body.getBoundingClientRect();
    const nav = document.querySelector('nav');
    const navRect = nav.getBoundingClientRect();
    
    // Check if any child of header is wider than the header itself
    let widestChildWidth = 0;
    let widestChildHTML = '';
    
    const children = header.querySelectorAll('*');
    children.forEach(c => {
      const rect = c.getBoundingClientRect();
      if (rect.width > widestChildWidth && rect.width > headerRect.width && c.tagName !== 'NAV') {
        widestChildWidth = rect.width;
        widestChildHTML = c.outerHTML.substring(0, 100);
      }
    });

    return {
      viewportWidth: window.innerWidth,
      bodyWidth: bodyRect.width,
      headerWidth: headerRect.width,
      headerScrollWidth: header.scrollWidth,
      navWidth: navRect.width,
      widestChildWidth,
      widestChildHTML
    };
  });
  
  console.log("Metrics on 320px screen:");
  console.log(metrics);
  
  // Also check 375px (iPhone X)
  await page.setViewport({ width: 375, height: 812 });
  await new Promise(r => setTimeout(r, 500));
  
  const metrics375 = await page.evaluate(() => {
    const header = document.querySelector('header');
    return {
      headerWidth: header.getBoundingClientRect().width,
      headerScrollWidth: header.scrollWidth,
    };
  });
  
  console.log("\nMetrics on 375px screen:");
  console.log(metrics375);

  await browser.close();
})();
