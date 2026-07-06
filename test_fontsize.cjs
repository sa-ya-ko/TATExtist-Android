const { JSDOM } = require('jsdom');
const dom = new JSDOM('<!DOCTYPE html><html><body><div contenteditable="true" id="editor"><span style="color: blue"><ruby>漢<rt>かん</rt></ruby></span>&#8203;字</div></body></html>', {
  runScripts: "dangerously"
});
const window = dom.window;
const document = window.document;
const Node = window.Node;
const HTMLElement = window.HTMLElement;

const editor = document.getElementById('editor');

// fake selection over the ruby
const range = document.createRange();
range.setStartBefore(editor.firstChild);
range.setEndAfter(editor.firstChild);

const sel = window.getSelection();
sel.removeAllRanges();
sel.addRange(range);

const styleName = 'font-size';
const styleValue = '24pt';

const extracted = range.extractContents();
const elements = Array.from(extracted.querySelectorAll('*'));
elements.forEach(node => {
  if (node instanceof HTMLElement) {
    if (styleName === 'font-size') {
      node.style.removeProperty('font-size')
      if (node.nodeName === 'FONT') node.removeAttribute('size')
    }
    if (node.getAttribute('style') === '') {
      node.removeAttribute('style')
    }
    if (node.nodeName === 'SPAN' && node.attributes.length === 0) {
      while (node.firstChild) {
        node.parentNode?.insertBefore(node.firstChild, node)
      }
      node.parentNode?.removeChild(node)
    }
  }
});

const fragment = document.createDocumentFragment();
const blockTags = /^(DIV|P|H[1-6]|BLOCKQUOTE|UL|OL|LI|TABLE)$/i;

while (extracted.firstChild) {
  const child = extracted.firstChild;
  if (child.nodeType === Node.TEXT_NODE) {
    const span = document.createElement('span')
    span.style.setProperty(styleName, styleValue)
    span.appendChild(child)
    fragment.appendChild(span)
  } else if (child instanceof HTMLElement) {
    if (blockTags.test(child.nodeName)) {
      child.style.setProperty(styleName, styleValue)
      fragment.appendChild(child)
    } else {
      const span = document.createElement('span')
      span.style.setProperty(styleName, styleValue)
      span.appendChild(child)
      fragment.appendChild(span)
    }
  } else {
    fragment.appendChild(child)
  }
}

range.insertNode(fragment);
console.log(editor.innerHTML);
