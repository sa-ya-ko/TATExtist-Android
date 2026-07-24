import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  await page.goto('http://localhost:3000');
  
  await page.setContent(`
    <div id="editor" contenteditable="true">
      <div>
        <ul><li>Bullet 1</li></ul>
        Normal text
        <ruby>漢字<rt>かんじ</rt></ruby>
      </div>
      Naked text root
    </div>
  `);
  
  await page.evaluate(() => {
    function cleanUpDirtyBlock(block) {
      let currentWrapper = null;
      const childNodes = Array.from(block.childNodes);
      for (const node of childNodes) {
        if (node.nodeType === Node.ELEMENT_NODE) {
          const tagName = node.tagName.toLowerCase();
          if (['p', 'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'blockquote', 'div'].includes(tagName)) {
            currentWrapper = null;
            continue;
          }
        }
        
        if (node.nodeType === Node.TEXT_NODE && (!node.textContent || node.textContent.trim() === '')) {
          if (!currentWrapper) continue;
        }
        
        if (!currentWrapper) {
          currentWrapper = document.createElement('div');
          block.insertBefore(currentWrapper, node);
        }
        currentWrapper.appendChild(node);
      }
    }
    
    const editor = document.getElementById('editor');
    // Clean editor root
    cleanUpDirtyBlock(editor);
    // Clean child dirty div
    const dirtyDiv = editor.querySelector('div');
    if (dirtyDiv) cleanUpDirtyBlock(dirtyDiv);
  });
  
  const html = await page.evaluate(() => {
    return document.getElementById('editor').innerHTML;
  });
  
  console.log("HTML structure after cleanup:", html);
  
  await browser.close();
})();
