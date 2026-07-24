import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({ headless: true });
  const page = await browser.newPage();
  
  await page.goto('http://localhost:3000');
  
  await page.waitForSelector('[contenteditable="true"]');
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '<div><h1>Heading</h1><ul><li>Bullet 1</li></ul>Normal text</div>';
  });
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    const textNode = editor.querySelector('div').lastChild; // "Normal text"
    
    // We only select the specific naked text node!
    const range = document.createRange();
    range.selectNodeContents(textNode);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
    
    document.execCommand('formatBlock', false, 'DIV');
  });
  
  const html = await page.evaluate(() => {
    return document.querySelector('[contenteditable="true"]').innerHTML;
  });
  
  console.log("HTML structure after isolated formatBlock DIV:", html);
  
  await browser.close();
})();
