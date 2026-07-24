import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = 'テスト<ruby style="font-size: 16px;">漢字<rt>かんじ</rt></ruby>です';
    editor.dispatchEvent(new Event('input', { bubbles: true }));
  });
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    const ruby = editor.querySelector('ruby');
    const range = document.createRange();
    range.selectNodeContents(ruby);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    // Simulate fallback
    document.execCommand('formatBlock', false, 'H1');
    
    // NOW find the new block and strip styles!
    const newSel = window.getSelection();
    if (newSel && newSel.rangeCount > 0) {
      let node = newSel.anchorNode;
      let block = node.nodeType === 3 ? node.parentElement : node;
      while (block && block !== editor) {
        if (['P', 'H1', 'H2', 'H3', 'H4'].includes(block.tagName)) {
          const spansAndRubies = block.querySelectorAll('span, ruby');
          spansAndRubies.forEach(el => {
            el.style.removeProperty('font-size');
            if (el.getAttribute('style') === '') el.removeAttribute('style');
            if (el.tagName.toLowerCase() === 'span' && el.attributes.length === 0) {
              while(el.firstChild) el.parentNode.insertBefore(el.firstChild, el);
              el.parentNode.removeChild(el);
            }
          });
          break;
        }
        block = block.parentElement;
      }
    }
  });
  
  const html = await page.evaluate(() => {
    return document.querySelector('[contenteditable="true"]').innerHTML;
  });
  
  console.log("HTML:", html);
  
  await browser.close();
})();
