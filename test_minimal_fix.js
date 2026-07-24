import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '<div><ul><li>Bullet 1</li></ul>Normal text</div>';
  });
  
  // Apply the getClosestBlockElement fix directly to the page's logic if possible.
  // We can't inject into the compiled Vue composition function easily.
  // Let's just simulate the exact logic.
  
  const html = await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    const textNode = editor.querySelector('div').lastChild; // "Normal text"
    const range = document.createRange();
    range.selectNodeContents(textNode);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    // Simulate our modified getBlocksInRange behavior for ONLY text selected
    let blocks = [];
    // getClosestBlockElement returns null because it's a dirty div!
    
    if (blocks.length === 0) {
      document.execCommand('formatBlock', false, 'DIV');
      // Re-get blocks (now it's a clean div!)
      blocks = [editor.querySelector('div:last-child')]; // just simulating
    }
    
    blocks.forEach(block => {
      block.style.paddingInlineStart = '2em';
    });
    
    return editor.innerHTML;
  });
  
  console.log("HTML structure after minimal fix:", html);
  
  await browser.close();
})();
