const puppeteer = require('puppeteer');
const fs = require('fs');

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  
  page.on('console', msg => console.log('BROWSER LOG:', msg.text()));
  
  await page.goto('http://localhost:3000');
  
  // Accept confirm dialog BEFORE triggering
  page.on('dialog', async dialog => {
    await dialog.accept();
  });
  
  // Inject some logging into handleFileRead or setEditorRef
  await page.evaluate(() => {
    // We can't easily inject into Vue components, but we can override FileReader
    const OriginalFileReader = window.FileReader;
    window.FileReader = function() {
      const fr = new OriginalFileReader();
      const origRead = fr.readAsText;
      fr.readAsText = function(file) {
        console.log("FileReader reading file:", file.name);
        return origRead.call(this, file);
      };
      return fr;
    };
  });
  
  // Wait for editor
  await page.waitForSelector('div[contenteditable="true"]');
  
  // Type something
  await page.type('div[contenteditable="true"]', 'Hello Load Test');
  
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
