import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  await page.waitForSelector('[contenteditable="true"]');
  
  const res = await page.evaluate(async () => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '<ruby>字<rt>じ</rt></ruby>';
    
    const range = document.createRange();
    range.selectNodeContents(editor.querySelector('ruby').childNodes[0]); 
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    document.dispatchEvent(new Event('selectionchange'));
    
    const colorInput = document.querySelector('input[type="color"]');
    if (colorInput) {
      colorInput.value = '#ff0000';
      colorInput.dispatchEvent(new Event('input', { bubbles: true }));
    }
    
    await new Promise(r => setTimeout(r, 200));
    return editor.innerHTML;
  });
  
  console.log("Color applied to base text:", res);
  
  await browser.close();
})();
