import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  
  await page.goto('http://localhost:3000');
  await page.waitForSelector('[contenteditable="true"]');
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '<ruby>字<rt>じ</rt></ruby>';
    const range = document.createRange();
    range.selectNodeContents(editor);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    document.dispatchEvent(new Event('selectionchange'));
  });
  
  await page.click('button[title="フォントサイズを拡大"]');
  await new Promise(r => setTimeout(r, 200));
  const res1 = await page.evaluate(() => document.querySelector('[contenteditable="true"]').innerHTML);
  const size1 = await page.evaluate(() => document.querySelector('input.w-10').value);
  console.log("AFTER 1 CLICK:", res1, "Input size:", size1);
  
  await page.click('button[title="フォントサイズを拡大"]');
  await new Promise(r => setTimeout(r, 200));
  const res2 = await page.evaluate(() => document.querySelector('[contenteditable="true"]').innerHTML);
  const size2 = await page.evaluate(() => document.querySelector('input.w-10').value);
  console.log("AFTER 2 CLICKS:", res2, "Input size:", size2);
  
  await browser.close();
})();
