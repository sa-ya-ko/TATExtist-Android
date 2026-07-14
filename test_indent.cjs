const puppeteer = require('puppeteer');
(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  page.on('console', msg => console.log('BROWSER LOG:', msg.text()));
  await page.goto('file://' + __dirname + '/test_indent.html');
  await new Promise(r => setTimeout(r, 1000));
  await browser.close();
})();
