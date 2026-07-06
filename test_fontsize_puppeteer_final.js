import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  // Test direct input
  const res1 = await page.evaluate(async () => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '漢';
    
    const range = document.createRange();
    range.selectNodeContents(editor);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    document.dispatchEvent(new Event('selectionchange'));
    
    // Simulate setting fontSizeInput to 24
    const input = document.querySelector('input[type="text"][class*="w-16"]'); // rough selector for font size input
    if (input) {
      input.value = '24';
      input.dispatchEvent(new Event('input', { bubbles: true }));
      
      const enterEvent = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
      input.dispatchEvent(enterEvent);
    }
    
    await new Promise(r => setTimeout(r, 200));
    return editor.innerHTML;
  });
  
  // Test ruby size sync
  const res2 = await page.evaluate(async () => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '<ruby><span style="font-size: 36pt;">字</span><rt>じ</rt></ruby>';
    
    const range = document.createRange();
    range.selectNodeContents(editor);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    document.dispatchEvent(new Event('selectionchange')); // This should trigger syncRubyStyles!
    
    await new Promise(r => setTimeout(r, 200));
    return editor.innerHTML;
  });
  
  console.log("Direct Input HTML:", res1);
  console.log("Ruby Sync HTML:", res2);
  
  await browser.close();
})();
