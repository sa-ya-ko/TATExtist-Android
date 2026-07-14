const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setContent(`
    <!DOCTYPE html>
    <html lang="ja">
    <head>
    <meta charset="UTF-8">
    <style>
      body { margin: 0; }
      .auto-pagination-strip {
        height: 700px;
        background: repeating-linear-gradient(to left, white 0, white 1000px, gray 1000px, gray 1050px);
        min-width: 1000px;
        padding: 50px;
        box-sizing: border-box;
      }
      .writing-vertical {
        writing-mode: vertical-rl;
        height: 100%;
        outline: none;
      }
    </style>
    </head>
    <body>
      <div id="strip" class="auto-pagination-strip" style="width: 1000px;">
        <div id="editor" class="writing-vertical" contenteditable="true">
          初期テキスト
        </div>
      </div>
      <script>
        const strip = document.getElementById('strip');
        const editor = document.getElementById('editor');
        const ro = new ResizeObserver(entries => {
          for(let entry of entries) {
            const textW = entry.target.scrollWidth;
            const contentWidth = 900;
            const fullWidth = 1000;
            const gap = 50;
            const pageCount = Math.max(1, Math.ceil(textW / contentWidth));
            const totalWidth = pageCount * fullWidth + (pageCount - 1) * gap;
            strip.style.width = totalWidth + 'px';
          }
        });
        ro.observe(editor);
        
        // Simulate paste
        setTimeout(() => {
          let hugeText = '';
          for(let i=0; i<1000; i++) hugeText += 'あいうえお<br>';
          editor.innerHTML = hugeText;
        }, 100);
      </script>
    </body>
    </html>
  `);
  
  await new Promise(r => setTimeout(r, 500));
  
  const stripWidth = await page.evaluate(() => document.getElementById('strip').style.width);
  const editorScrollWidth = await page.evaluate(() => document.getElementById('editor').scrollWidth);
  
  console.log("Strip width:", stripWidth);
  console.log("Editor scrollWidth:", editorScrollWidth);
  await browser.close();
})();
