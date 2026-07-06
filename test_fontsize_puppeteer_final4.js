import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  
  page.on('console', msg => console.log('PAGE LOG:', msg.text()));
  
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  await page.evaluate(() => {
    window.originalApplyCustomFontSize = window.applyCustomFontSize;
    
    // We can't easily hook Vue's internal functions without rebuilding, but we can hook into applyInlineStyleSafe if we exposed it. 
    // Instead, let's just observe what happens to the range.
    
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '漢';
    const range = document.createRange();
    range.selectNodeContents(editor);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    document.dispatchEvent(new Event('selectionchange'));
  });
  
  await page.click('input[type="text"]');
  await page.keyboard.press('Backspace');
  await page.keyboard.press('Backspace');
  await page.keyboard.type('24');
  await page.keyboard.press('Enter');
  
  await new Promise(r => setTimeout(r, 200));
  
  const res = await page.evaluate(() => document.querySelector('[contenteditable="true"]').innerHTML);
  console.log("Direct Input HTML:", res);
  
  await browser.close();
})();
