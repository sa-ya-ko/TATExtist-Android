import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.focus();
    // Type a bulleted list
    document.execCommand('insertUnorderedList');
    document.execCommand('insertText', false, 'Bullet 1');
    
    // Press enter twice to exit list
    const p1 = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
    editor.dispatchEvent(p1);
    
    // In chrome, we just execCommand insertParagraph or something
  });
  
  await page.keyboard.press('Enter');
  await page.keyboard.press('Enter');
  
  await page.evaluate(() => {
    document.execCommand('insertText', false, 'Normal text');
  });
  
  const html = await page.evaluate(() => {
    return document.querySelector('[contenteditable="true"]').innerHTML;
  });
  
  console.log("HTML structure after bullet and enter:", html);
  
  await browser.close();
})();
