import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  const html = await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '<ruby>漢<rt>かん</rt></ruby>\u200B字';
    
    // Select the RUBY
    const range = document.createRange();
    range.selectNode(editor.childNodes[0]);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    // Dispatch selectionchange
    document.dispatchEvent(new Event('selectionchange'));
    
    // Click the '+' button
    const btn = document.querySelector('button[title="フォントサイズを拡大"]');
    if (btn) {
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
