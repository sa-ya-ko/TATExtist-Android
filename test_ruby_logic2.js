import { JSDOM } from "jsdom";

const dom = new JSDOM(`
  <html>
    <body>
      <div id="editor">
        <span style="color: rgb(255, 0, 0);">
          <ruby><span style="color: rgb(0, 0, 255);">漢</span><rt>かん</rt></ruby>
          <ruby><span style="color: rgb(255, 255, 0);">字</span><rt>じ</rt></ruby>
        </span>
      </div>
    </body>
  </html>
`);

const window = dom.window;
const document = window.document;
const NodeFilter = window.NodeFilter;
const editor = document.getElementById('editor');

function syncRubyColors() {
  const rubies = editor.querySelectorAll('ruby');
  rubies.forEach(ruby => {
    const walker = document.createTreeWalker(ruby, NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT, {
      acceptNode: function(node) {
        if (node.nodeName === 'RT' || (node.parentElement && node.parentElement.closest('rt'))) {
          return NodeFilter.FILTER_REJECT;
        }
        if (node.nodeType === 3) { // Node.TEXT_NODE
          return NodeFilter.FILTER_ACCEPT;
        }
        return NodeFilter.FILTER_SKIP;
      }
    });

    let node;
    let commonColor = null;
    let allSame = true;
    let hasText = false;

    while (node = walker.nextNode()) {
      if (!node.textContent || node.textContent.trim() === '') continue;
      hasText = true;
      
      let nodeColor = null;
      if (node.parentElement) {
        // jsdom doesn't fully support computedStyle inheritance from nested elements perfectly, 
        // but we'll try to emulate the exact code we have
        const comp = window.getComputedStyle(node.parentElement);
        nodeColor = comp.color;
      }
      
      if (!nodeColor || nodeColor === '') {
        allSame = false;
      } else if (commonColor === null) {
        commonColor = nodeColor;
      } else if (commonColor !== nodeColor) {
        allSame = false;
      }
    }

    if (hasText && allSame && commonColor) {
      ruby.style.color = commonColor;
    } else {
      ruby.style.color = '';
    }
  });
  console.log("Ruby 1 color:", rubies[0].style.color);
  console.log("Ruby 2 color:", rubies[1].style.color);
}

syncRubyColors();
