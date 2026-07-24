import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '<div><ul><li>Bullet 1</li></ul>Normal text</div>';
  });
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    const textNode = editor.querySelector('div').lastChild; // "Normal text"
    const range = document.createRange();
    range.selectNodeContents(textNode);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    // Attempt to isolate it by formatting as P
    document.execCommand('formatBlock', false, 'P');
  });
  
  const html = await page.evaluate(() => {
    return document.querySelector('[contenteditable="true"]').innerHTML;
  });
  
  console.log("HTML structure after formatBlock P:", html);
  
  await browser.close();
})();
