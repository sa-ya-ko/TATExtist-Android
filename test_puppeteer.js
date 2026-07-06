import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');
  
  // Wait for the editor to load
  await page.waitForSelector('[contenteditable="true"]');
  
  // Get the initial DOM inside the editor
  const html = await page.evaluate(() => {
    const editor = document.querySelector('[contenteditable="true"]');
    // Simulate user action:
    editor.innerHTML = '<span style="color: rgb(255, 0, 0);"><span style="color: rgb(0, 0, 255);">漢</span><span style="color: rgb(255, 255, 0);">字</span></span>';
    
    // We need to trigger the sync manually or fake the selection
    const ruby1 = document.createElement('ruby');
    ruby1.innerHTML = '<span style="color: rgb(0, 0, 255);">漢</span><rt>かん</rt>';
    
    const ruby2 = document.createElement('ruby');
    ruby2.innerHTML = '<span style="color: rgb(255, 255, 0);">字</span><rt>じ</rt>';
    
    editor.innerHTML = `<span style="color: rgb(255, 0, 0);">${ruby1.outerHTML}${ruby2.outerHTML}</span>`;
    
    // Now trigger sync
    const vueApp = editor.__vue_app__; 
    // We can just call syncRubyColors if we can reach it, but it's local.
    // Let's dispatch selectionchange
    document.dispatchEvent(new Event('selectionchange'));
    
    return new Promise(resolve => {
      setTimeout(() => {
        resolve(editor.innerHTML);
      }, 500);
    });
  });
  
  console.log("FINAL HTML:", html);
  await browser.close();
})();
