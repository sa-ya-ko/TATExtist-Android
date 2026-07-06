import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  
  page.on('console', msg => console.log('LOG:', msg.text()));
  
  await page.goto('http://localhost:3000');
  
  await page.evaluate(() => {
    // Inject console logs into applyInlineStyleSafe
    const oldApply = window.applyInlineStyleSafe || window.applyCustomFontSize;
    // We can't inject directly if it's bundled in setup script unless we redefine it or add an event listener.
    // Instead, let's just listen to the DOM mutations.
    const observer = new MutationObserver(muts => {
      console.log("Mutation:", muts.length);
    });
    observer.observe(document.querySelector('[contenteditable="true"]'), { childList: true, subtree: true, characterData: true });
  });
  
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
  await page.click('input[type="text"]');
  await page.keyboard.press('Backspace');
  await page.keyboard.press('Backspace');
  await page.keyboard.type('24');
  await page.keyboard.press('Enter');
  
  await new Promise(r => setTimeout(r, 500));
  
  const res = await page.evaluate(() => document.querySelector('[contenteditable="true"]').innerHTML);
  console.log("FINAL:", res);
  
  await browser.close();
})();
