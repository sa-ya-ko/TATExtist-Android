import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  const res1 = await page.evaluate(async () => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '漢';
    
    const range = document.createRange();
    range.selectNodeContents(editor);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    document.dispatchEvent(new Event('selectionchange'));
    
    const btn = document.querySelector('button[title="フォントサイズを拡大"]');
    if (btn) {
      const mousedownEvent = new MouseEvent('mousedown', { bubbles: true, cancelable: true });
      btn.dispatchEvent(mousedownEvent);
    }
    
    await new Promise(r => setTimeout(r, 200));
    return editor.innerHTML;
  });
  
  console.log("Plus Button HTML:", res1);
  
  await browser.close();
})();
