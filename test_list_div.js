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
    range.setStart(textNode, 5);
    range.collapse(true);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    editor.dispatchEvent(new Event('focus', { bubbles: true }));
    document.dispatchEvent(new Event('selectionchange'));
  });
  
  await new Promise(r => setTimeout(r, 500));
  
  // Click Indent button
  await page.evaluate(() => {
    const btns = Array.from(document.querySelectorAll('button'));
    const indentBtn = btns.find(b => b.title.includes('インデントを増やす'));
    if (indentBtn) {
      const event = new MouseEvent('mousedown', { bubbles: true, cancelable: true });
      indentBtn.dispatchEvent(event);
    }
  });
  
  await new Promise(r => setTimeout(r, 1000));
  
  const html = await page.evaluate(() => {
    return document.querySelector('[contenteditable="true"]').innerHTML;
  });
  
  console.log("HTML structure after indent:", html);
  
  await browser.close();
})();
