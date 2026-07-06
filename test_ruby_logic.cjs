const jsdom = require("jsdom");
const { JSDOM } = jsdom;

const dom = new JSDOM(`
  <html>
    <body>
      <div id="editor">
        <span style="color: red;">
          <ruby><span style="color: blue;">漢</span><rt>かん</rt></ruby>
          <ruby><span style="color: yellow;">字</span><rt>じ</rt></ruby>
        </span>
      </div>
    </body>
  </html>
`);

const document = dom.window.document;
const NodeFilter = dom.window.NodeFilter;
const editor = document.getElementById('editor');

function syncRubyColors() {
  const rubies = editor.querySelectorAll('ruby');
  rubies.forEach(ruby => {
    const walker = document.createTreeWalker(ruby, NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_TEXT, {
      acceptNode: function(node) {
        if (node.nodeName === 'RT' || (node.parentElement && node.parentElement.closest('rt'))) {
          return NodeFilter.FILTER_REJECT;
        }
        if (node.nodeType === 3) {
          return NodeFilter.FILTER_ACCEPT;
        }
        return NodeFilter.FILTER_SKIP;
      }
    });

    let node;
    let commonColor = null;
    let allSame = true;
    let hasText = false;
    let hasUncoloredText = false;

    while (node = walker.nextNode()) {
      if (!node.textContent || node.textContent.trim() === '') continue;
      hasText = true;
      
      let ancestor = node.parentElement;
      let nodeColor = null;
      while (ancestor && ancestor !== ruby) {
        if (ancestor.style.color || ancestor.getAttribute('color')) {
          nodeColor = ancestor.style.color || ancestor.getAttribute('color');
          break;
        }
        ancestor = ancestor.parentElement;
      }
      
      if (!nodeColor) {
        hasUncoloredText = true;
        allSame = false;
      } else if (commonColor === null) {
        commonColor = nodeColor;
      } else if (commonColor !== nodeColor) {
        allSame = false;
      }
    }

    if (hasText && allSame && commonColor && !hasUncoloredText) {
      ruby.style.color = commonColor;
    } else {
      ruby.style.color = '';
    }
  });
}

syncRubyColors();
console.log("Ruby 1 color:", editor.querySelectorAll('ruby')[0].style.color);
console.log("Ruby 2 color:", editor.querySelectorAll('ruby')[1].style.color);
