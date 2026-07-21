const puppeteer = require('puppeteer');
const fs = require('fs');

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  
  await page.goto('http://localhost:3000');
  
  page.on('dialog', async dialog => {
    await dialog.accept();
  });
  
  // Switch to mobile mode
  await page.evaluate(() => {
    document.querySelector('button[title="ページ表示"]').click(); // Actually let's assume auto mode is desktop
    // wait, auto mode is Desktop. Paginated is Mobile.
  });
  
  const mockData = {
    documentTitle: 'Test Doc',
    pages: [
      { id: 123, content: '<p>Loaded Content</p>', isOverflowing: false, isWarningDismissed: false }
    ],
    paperSize: 'A4',
    margins: { top: 30, bottom: 30, left: 30, right: 30 },
    version: '1.0'
  };
  fs.writeFileSync('test.tategaki', JSON.stringify(mockData));
  
  const fileInput = await page.$('input[type="file"]');
  await fileInput.uploadFile('test.tategaki');
  
  await new Promise(r => setTimeout(r, 1000));
  
  const content = await page.evaluate(() => {
    const el = document.querySelector('div[contenteditable="true"]');
    return el ? el.innerHTML : 'No editor found';
  });
  
  console.log("Editor content after load (desktop mode):", content);

  await browser.close();
})();
