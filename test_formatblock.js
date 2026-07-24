import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    // Raw text nodes inside editor
    editor.innerHTML = 'テスト<ruby>漢字<rt>かんじ</rt></ruby>です';
  });
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    const ruby = editor.querySelector('ruby');
    const range = document.createRange();
    range.selectNode(ruby);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    document.execCommand('formatBlock', false, 'H1');
  });
  
  const html = await page.evaluate(() => {
    return document.querySelector('[contenteditable="true"]').innerHTML;
  });
  
  console.log("HTML:", html);
  
  await browser.close();
})();
