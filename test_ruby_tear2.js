import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  await page.waitForSelector('[contenteditable="true"]');
  
  const res = await page.evaluate(async () => {
    const editor = document.querySelector('[contenteditable="true"]');
    editor.innerHTML = '<ruby>漢<rt>かん</rt></ruby>\u200B<ruby>字<rt>じ</rt></ruby>';
    
    // Select ZWSP and "字"
    const range = document.createRange();
    range.setStartBefore(editor.childNodes[1]); // ZWSP
    range.setEnd(editor.querySelectorAll('ruby')[1].childNodes[0], 1); // "字"
    
    // Attempt to extract
    const extracted = range.extractContents();
    
    // Wrap
    const span1 = document.createElement('span');
    span1.style.color = 'red';
    span1.appendChild(extracted.childNodes[0]); // ZWSP
    
    const span2 = document.createElement('span');
    span2.style.color = 'red';
    span2.appendChild(extracted.childNodes[0]); // <ruby>字</ruby>
    
    range.insertNode(span2);
    range.insertNode(span1);
    
    return editor.innerHTML;
  });
  
  console.log("DOM after tearing across nodes:", res);
  
  await browser.close();
})();
