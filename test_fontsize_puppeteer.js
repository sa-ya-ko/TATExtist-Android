import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  const html = await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '漢';
    
    // Select the text
    const range = document.createRange();
    range.selectNodeContents(editor);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    // Dispatch selectionchange to update Vue state
    document.dispatchEvent(new Event('selectionchange'));
    
    // Click the '+' button
    const btn = document.querySelector('button[title="フォントサイズを拡大"]');
    if (btn) {
      // Simulate mousedown.prevent
      const mousedownEvent = new MouseEvent('mousedown', { bubbles: true, cancelable: true });
      btn.dispatchEvent(mousedownEvent);
    }
    
    return new Promise(resolve => {
      setTimeout(() => {
        resolve(editor.innerHTML);
      }, 500);
    });
  });
  
  console.log("FINAL HTML:", html);
  await browser.close();
})();
