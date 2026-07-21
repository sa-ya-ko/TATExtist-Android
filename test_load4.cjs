const puppeteer = require('puppeteer');
const fs = require('fs');

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  
  page.on('console', msg => console.log('BROWSER LOG:', msg.text()));
  
  await page.goto('http://localhost:3000');
  
  page.on('dialog', async dialog => {
    await dialog.accept();
  });
  
  // Inject MutationObserver to see if editor content is overwritten
  await page.evaluate(() => {
    window.editorMutations = [];
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.target.getAttribute && mutation.target.getAttribute('contenteditable') === 'true') {
          window.editorMutations.push({
            type: mutation.type,
            html: mutation.target.innerHTML
          });
          console.log("Editor Mutated! HTML:", mutation.target.innerHTML);
        }
      });
    });
    observer.observe(document.body, { childList: true, subtree: true, characterData: true });
  });
  
  await page.waitForSelector('div[contenteditable="true"]');
  await page.type('div[contenteditable="true"]', 'Hello Load Test');
  
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
  
  console.log("Editor content after load:", content);

  await browser.close();
})();
