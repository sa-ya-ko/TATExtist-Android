import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  
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
  
  await page.click('button[title="フォントサイズを拡大"]');
  await new Promise(r => setTimeout(r, 100));
  await page.click('button[title="フォントサイズを拡大"]');
  await new Promise(r => setTimeout(r, 100));
  await page.click('button[title="フォントサイズを拡大"]');
  
  await new Promise(r => setTimeout(r, 500));
  
  const res = await page.evaluate(() => document.querySelector('[contenteditable="true"]').innerHTML);
  console.log("FINAL AFTER 3 CLICKS:", res);
  
  await browser.close();
})();
