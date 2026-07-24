import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    // Raw text nodes with an explicit ruby font-size (simulating syncRubyStyles)
    editor.innerHTML = 'テスト<ruby style="font-size: 16px;">漢字<rt>かんじ</rt></ruby>です';
    editor.dispatchEvent(new Event('input', { bubbles: true }));
  });
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    const ruby = editor.querySelector('ruby');
    const range = document.createRange();
    range.selectNode(ruby);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    // Simulate applyHeading fallback
    document.execCommand('formatBlock', false, 'H1');
  });
  
  const html = await page.evaluate(() => {
    return document.querySelector('[contenteditable="true"]').innerHTML;
  });
  
  console.log("HTML:", html);
  
  await browser.close();
})();
