import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '<p id="test-p">テスト<ruby>漢字<rt>かんじ</rt></ruby>です</p>';
    editor.dispatchEvent(new Event('input', { bubbles: true }));
  });
  
  // Select the ruby
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    const ruby = editor.querySelector('ruby');
    const range = document.createRange();
    range.selectNode(ruby);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    editor.dispatchEvent(new Event('focus', { bubbles: true }));
    document.dispatchEvent(new Event('selectionchange'));
  });
  
  await new Promise(r => setTimeout(r, 500));
  
  // Click the Heading dropdown button
  // The button has text "標準テキスト" inside a span
  await page.evaluate(() => {
    const spans = Array.from(document.querySelectorAll('span'));
    const headingBtnSpan = spans.find(s => s.textContent === '標準テキスト');
    if (headingBtnSpan) headingBtnSpan.parentElement.click();
  });
  
  await new Promise(r => setTimeout(r, 500));
  
  // Click H2 ("見出し 1")
  await page.evaluate(() => {
    const lis = Array.from(document.querySelectorAll('li'));
    const h2Li = lis.find(li => li.textContent === '見出し 1');
    if (h2Li) {
      const event = new MouseEvent('mousedown', { bubbles: true, cancelable: true });
      h2Li.dispatchEvent(event);
    }
  });
  
  await new Promise(r => setTimeout(r, 1000));
  
  const html = await page.evaluate(() => {
    return document.querySelector('[contenteditable="true"]').innerHTML;
  });
  
  console.log("HTML:", html);
  
  await browser.close();
})();
