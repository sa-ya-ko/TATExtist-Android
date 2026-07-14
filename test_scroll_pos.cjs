const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setViewport({ width: 1200, height: 800 });
  await page.goto('file://' + __dirname + '/test_rtl_scroll2.html');
  
  // Get initial scroll left
  let scrollPos = await page.evaluate(() => {
    return document.getElementById('scroll').scrollLeft;
  });
  console.log('Initial scrollLeft:', scrollPos);
  
  // Add a lot of text
  await page.evaluate(() => {
    const ed = document.getElementById('ed');
    let text = '';
    for(let i=0; i<100; i++) text += 'あいうえお<br>かきくけこ<br>';
    ed.innerHTML = text;
  });
  await new Promise(r => setTimeout(r, 500));
  
  // Get new scroll left
  scrollPos = await page.evaluate(() => {
    return document.getElementById('scroll').scrollLeft;
  });
  console.log('ScrollLeft after text:', scrollPos);
  
  let rect = await page.evaluate(() => {
    return document.getElementById('ed').getBoundingClientRect();
  });
  console.log('Editor rect:', rect);
  
  await browser.close();
})();
