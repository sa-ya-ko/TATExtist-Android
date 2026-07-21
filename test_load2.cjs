const puppeteer = require('puppeteer');
const fs = require('fs');

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  
  // Accept confirm dialog BEFORE triggering
  page.on('dialog', async dialog => {
    await dialog.accept();
  });
  
  // Wait for editor
  await page.waitForSelector('div[contenteditable="true"]');
  
  // Type something
  await page.type('div[contenteditable="true"]', 'Hello Load Test');
  
  // Let's just create a mock tategaki file
  const mockData = {
    documentTitle: 'Test Doc',
    pages: [
      { id: 123, content: 'Loaded Content', isOverflowing: false, isWarningDismissed: false }
    ],
    paperSize: 'A4',
    margins: { top: 30, bottom: 30, left: 30, right: 30 },
    version: '1.0'
  };
  fs.writeFileSync('test.tategaki', JSON.stringify(mockData));
  
  // Upload file
  const fileInput = await page.$('input[type="file"]');
  await fileInput.uploadFile('test.tategaki');
  
  // Wait a bit
  await new Promise(r => setTimeout(r, 1000));
  
  const content = await page.evaluate(() => {
    const el = document.querySelector('div[contenteditable="true"]');
    return el ? el.innerHTML : 'No editor found';
  });
  
  console.log("Editor content after load:", content);

  await browser.close();
})();
