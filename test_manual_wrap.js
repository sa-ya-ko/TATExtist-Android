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
  
  await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    const textNode = editor.querySelector('div').lastChild; // "Normal text"
    
    // Manually wrap the text node
    const wrapper = document.createElement('div');
    textNode.parentNode.insertBefore(wrapper, textNode);
    wrapper.appendChild(textNode);
    wrapper.style.paddingInlineStart = '2em';
  });
  
  const html = await page.evaluate(() => {
    return document.querySelector('[contenteditable="true"]').innerHTML;
  });
  
  console.log("HTML structure after manual wrap:", html);
  
  await browser.close();
})();
