import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '漢';
    const range = document.createRange();
    range.selectNodeContents(editor);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    document.dispatchEvent(new Event('selectionchange'));
  });
  
  // Click the input, type 24, press Enter
  await page.click('input[type="text"]'); // The only text input is fontSizeInput
  await page.keyboard.type('24');
  await page.keyboard.press('Enter');
  
  await new Promise(r => setTimeout(r, 200));
  
  const res = await page.evaluate(() => document.querySelector('[contenteditable="true"]').innerHTML);
  console.log("Direct Input HTML with real keyboard:", res);
  
  await browser.close();
})();
