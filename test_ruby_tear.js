import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  await page.waitForSelector('[contenteditable="true"]');
  
  const res = await page.evaluate(async () => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '<ruby>字<rt>じ</rt></ruby>';
    
    // Select ONLY the base text "字"
    const range = document.createRange();
    range.selectNodeContents(editor.querySelector('ruby').childNodes[0]); 
    
    // Attempt to extract
    const extracted = range.extractContents();
    const span = document.createElement('span');
    span.style.color = 'red';
    span.appendChild(extracted);
    range.insertNode(span);
    
    return editor.innerHTML;
  });
  
  console.log("DOM after extracting partial ruby:", res);
  
  await browser.close();
})();
