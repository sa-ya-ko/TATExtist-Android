import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '<p id="test-p">テスト<ruby>漢字<rt>かんじ</rt></ruby>です</p>';
  });
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    const ruby = editor.querySelector('ruby');
    const range = document.createRange();
    
    // Select the entire ruby element
    range.selectNode(ruby);
    
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    editor.dispatchEvent(new Event('focus'));
    document.dispatchEvent(new Event('selectionchange'));
  });
  
  await new Promise(r => setTimeout(r, 500));
  
  await page.evaluate(() => {
    const buttons = Array.from(document.querySelectorAll('button'));
    const headingBtn = buttons.find(b => b.textContent.includes('標準テキスト'));
    if (headingBtn) headingBtn.click();
  });
  
  await new Promise(r => setTimeout(r, 500));
  
  await page.evaluate(() => {
    const lis = Array.from(document.querySelectorAll('li'));
    const h2Li = lis.find(li => li.textContent.includes('見出し 1'));
    if (h2Li) {
      const event = new MouseEvent('mousedown', { bubbles: true, cancelable: true });
      h2Li.dispatchEvent(event);
    }
  });
  
  const html = await page.evaluate(() => {
    return document.querySelector('[contenteditable="true"]').innerHTML;
  });
  
  console.log("HTML:", html);
  
  await browser.close();
})();
