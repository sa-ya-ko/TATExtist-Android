import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  
  await page.goto('http://localhost:3000');
  await page.waitForSelector('[contenteditable="true"]');
  
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
  
  // Find the exact input for font size. It's the one with value "12" by default
  const inputs = await page.$$('input[type="text"]');
  for (const input of inputs) {
    const val = await page.evaluate(el => el.value, input);
    if (val === '12') {
      await input.click();
      await page.keyboard.press('Backspace');
      await page.keyboard.press('Backspace');
      await page.keyboard.type('36');
      await page.keyboard.press('Enter');
      break;
    }
  }
  
  await new Promise(r => setTimeout(r, 500));
  
  const res = await page.evaluate(() => document.querySelector('[contenteditable="true"]').innerHTML);
  console.log("FINAL AFTER ENTER:", res);
  
  await browser.close();
})();
