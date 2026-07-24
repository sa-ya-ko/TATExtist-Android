import { ref, computed } from 'vue'

export interface Page {
  id: number;
  content: string;
  isOverflowing?: boolean;
  isWarningDismissed?: boolean;
}

const pages = ref<Page[]>([])
// The document is stored as one continuous HTML stream.  Explicit page breaks
// are represented by markers in that stream; `pages` is only the editor view.
const documentContent = ref('')
const documentRevision = ref(0)
const MANUAL_BREAK = /<!--manual-page-break-->/g
const manualBreakMarker = '<!--manual-page-break-->'
const documentTitle = ref<string>('無題のドキュメント')
const editor = ref<HTMLElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

const isBold = ref(false)
const isItalic = ref(false)
const isUnderline = ref(false)
const isList = ref(false)
const rubyModalVisible = ref(false)
const helpModalVisible = ref(false)
const isDarkMode = ref(false)
const isSidebarOpen = ref(false)
const currentFontSize = ref(12)
const showFontSizeDropdown = ref(false)
const selectedColor = ref('#000000')
const currentEditorPageId = ref<string | null>(null)
const indentSize = ref(2)

const wordCountModalVisible = ref(false)
const showWordCountWhileTyping = ref(false)

const isRubyManagementMode = ref(false)
const rubyList = ref<Array<{id: string, text: string, rt: string, pageId: number, element: HTMLElement}>>([])

function toggleRubyManagementMode() {
  isRubyManagementMode.value = !isRubyManagementMode.value
  if (isRubyManagementMode.value) {
    refreshRubyList()
  }
}

function refreshRubyList() {
  rubyList.value = []
  
  // We need to query all rubies across all pages.
  // In auto mode, pages[0] might contain everything, but we should just query the whole document body or specific container
  // To keep it simple, we can query all `.writing-vertical` elements.
  const editors = document.querySelectorAll('.writing-vertical')
  editors.forEach(ed => {
    const rubies = ed.querySelectorAll('ruby')
    rubies.forEach((ruby, index) => {
      let id = ruby.getAttribute('data-ruby-id')
      if (!id) {
        id = `ruby-${index}-${Date.now()}`
        ruby.setAttribute('data-ruby-id', id)
      }
      
      const rtNode = ruby.querySelector('rt')
      const rtText = rtNode ? rtNode.textContent || '' : ''
      
      const clone = ruby.cloneNode(true) as HTMLElement
      const cloneRt = clone.querySelector('rt')
      if (cloneRt) cloneRt.remove()
      const baseText = clone.textContent || ''
      
      const pageId = Number(ed.getAttribute('data-page-id') || 0)
      
      rubyList.value.push({ id, text: baseText, rt: rtText, pageId, element: ruby })
    })
  })
}

function removeSpecificRuby(id: string) {
  const editors = document.querySelectorAll('.writing-vertical')
  let found = false
  editors.forEach(ed => {
    const rubyElement = ed.querySelector(`ruby[data-ruby-id="${id}"]`)
    if (rubyElement) {
      found = true
      const rt = rubyElement.querySelector('rt')
      if (rt) rt.remove()
      if (rubyElement.getAttribute('style')) {
        const span = document.createElement('span')
        span.setAttribute('style', rubyElement.getAttribute('style')!)
        while (rubyElement.firstChild) {
          span.appendChild(rubyElement.firstChild)
        }
        rubyElement.parentNode?.replaceChild(span, rubyElement)
      } else {
        const parent = rubyElement.parentNode
        while (rubyElement.firstChild) {
          parent?.insertBefore(rubyElement.firstChild, rubyElement)
        }
        parent?.removeChild(rubyElement)
      }
    }
  })
  if (found) {
    syncEditorToVue()
    onInput()
  }
}

function scrollToRuby(id: string) {
  const editors = document.querySelectorAll('.writing-vertical')
  editors.forEach(ed => {
    const rubyElement = ed.querySelector(`ruby[data-ruby-id="${id}"]`)
    if (rubyElement) {
      rubyElement.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' })
      const originalBg = (rubyElement as HTMLElement).style.backgroundColor
      ;(rubyElement as HTMLElement).style.backgroundColor = 'rgba(239, 68, 68, 0.4)'
      setTimeout(() => {
        ;(rubyElement as HTMLElement).style.backgroundColor = originalBg
      }, 1000)
    }
  })
}

const rawTextContent = computed(() => {
  if (typeof document === 'undefined') return ''
  const combinedHtml = pages.value.map(p => p.content).join(' ')
  const temp = document.createElement('div')
  
  let html = combinedHtml.replace(/<br\s*[\/]?>/gi, '\n')
  html = html.replace(/<\/div>/gi, '</div>\n')
  html = html.replace(/<\/p>/gi, '</p>\n')
  
  temp.innerHTML = html
  
  const rts = temp.querySelectorAll('rt')
  rts.forEach(rt => rt.remove())
  
  let text = temp.textContent || ''
  text = text.replace(/\u200B/g, '')
  return text.replace(/\n+$/, '')
})

const characterCountWithSpaces = computed(() => {
  return rawTextContent.value.length
})

const characterCount = computed(() => {
  return rawTextContent.value.replace(/\s+/g, '').length
})

function openWordCountModal() {
  wordCountModalVisible.value = true
}

function closeWordCountModal() {
  wordCountModalVisible.value = false
}

export const fontOptions = [
  { label: 'Noto Serif JP (明朝)', value: '"Noto Serif JP", serif' },
  { label: 'Noto Sans JP (ゴシック)', value: '"Noto Sans JP", sans-serif' },
  { label: 'しっぽり明朝', value: '"Shippori Mincho", serif' },
  { label: 'クレー (手書き風)', value: '"Klee One", cursive' },
  { label: 'Zen角ゴシック', value: '"Zen Kaku Gothic New", sans-serif' },
  { label: '游明朝 (標準明朝)', value: '"Yu Mincho", "YuMincho", serif' },
  { label: '游ゴシック (標準ゴシック)', value: '"Yu Gothic", "YuGothic", sans-serif' }
]
const currentFontFamily = ref('"Noto Serif JP", serif')
const currentHeading = ref('P')
const currentIndent = ref(0)

const layoutMode = ref<'auto' | 'manual'>('auto')
const scrollStyle = ref<'paged' | 'continuous'>('continuous')

function syncPagesFromDocument() {
  documentRevision.value++
  
  // ふりがな（rt）にカーソルが入り込む不具合を防ぐため、編集不可属性を追加
  const fixedContent = documentContent.value.replace(/<rt(?![^>]*contenteditable)/gi, '<rt contenteditable="false"');
  if (fixedContent !== documentContent.value) {
    documentContent.value = fixedContent;
  }
  
  const segments = documentContent.value.split(MANUAL_BREAK)
  pages.value = segments.map((content, index) => ({
    id: Date.now() + index + Math.random(),
    content
  }))
  if (pages.value.length === 0) pages.value = [{ id: Date.now(), content: '' }]
}

function setDocumentFromLegacyPages(legacyPages: Array<Pick<Page, 'content'>>) {
  documentContent.value = legacyPages.map(page => page.content).join(manualBreakMarker)
  syncPagesFromDocument()
}

function updatePageContent(pageId: number, content: string) {
  const index = pages.value.findIndex(page => page.id === pageId)
  if (index < 0) return
  const segments = documentContent.value.split(MANUAL_BREAK)
  segments[index] = content
  documentContent.value = segments.join(manualBreakMarker)
  pages.value[index].content = content
}

function addManualPage(afterPageId?: number) {
  const index = afterPageId === undefined
    ? pages.value.length - 1
    : pages.value.findIndex(page => page.id === afterPageId)
  const segments = documentContent.value.split(MANUAL_BREAK)
  const insertAt = index < 0 ? segments.length : index + 1
  segments.splice(insertAt, 0, '')
  documentContent.value = segments.join(manualBreakMarker)
  syncPagesFromDocument()
  persistDocument()
}

function deleteManualPage(pageId: number) {
  const index = pages.value.findIndex(page => page.id === pageId)
  if (index < 0 || pages.value.length <= 1) return
  const segments = documentContent.value.split(MANUAL_BREAK)
  segments.splice(index, 1)
  documentContent.value = segments.join(manualBreakMarker)
  syncPagesFromDocument()
  persistDocument()
}

function persistDocument() {
  localStorage.setItem('vertical-editor-document', JSON.stringify({
    version: '2.0',
    content: documentContent.value
  }))
}

const setLayoutMode = (mode: 'auto' | 'manual') => {
  if (layoutMode.value === mode) return

  if (mode === 'auto' && pages.value.length > 1) {
    documentContent.value = documentContent.value.replace(MANUAL_BREAK, '<br><br>')
    syncPagesFromDocument()
    persistDocument()
  }
  layoutMode.value = mode
  
  if (typeof window !== 'undefined') {
    localStorage.setItem('tategaki-layout-mode', mode)
  }
}

if (typeof window !== 'undefined') {
  const savedMode = localStorage.getItem('tategaki-layout-mode') as 'auto' | 'manual' | null
  if (savedMode && (savedMode === 'auto' || savedMode === 'manual')) {
    layoutMode.value = savedMode
  }
}

const showRulers = ref(true)

const paperSize = ref<'A4' | 'B5'>('A4')
const margins = ref({ top: 30, bottom: 30, left: 30, right: 30 })

const isDraggingRuler = ref<'left' | 'right' | 'top' | 'bottom' | null>(null)

const MM_TO_PX = 3.78
const paperDimensions = {
  'A4': { width: 297, height: 210 },
  'B5': { width: 257, height: 182 }
}

const paperStyle = computed(() => {
  const dim = paperDimensions[paperSize.value]

  const pageHeight = dim.height * MM_TO_PX
  const pageWidth = dim.width * MM_TO_PX
  const paddingTop = margins.value.top * MM_TO_PX
  const paddingBottom = margins.value.bottom * MM_TO_PX
  const paddingLeft = margins.value.left * MM_TO_PX
  const paddingRight = margins.value.right * MM_TO_PX

  const contentHeight = pageHeight - paddingTop - paddingBottom
  const contentWidth = pageWidth - paddingLeft - paddingRight

  return {
    '--page-height': `${pageHeight}px`,
    '--page-width': `${pageWidth}px`,
    '--margin-top': `${paddingTop}px`,
    '--margin-bottom': `${paddingBottom}px`,
    '--margin-left': `${paddingLeft}px`,
    '--margin-right': `${paddingRight}px`
  }
})

let savedRange: Range | null = null
let lastEditorRange: Range | null = null

function syncEditorToVue() {
  if (editor.value) {
    const idAttr = editor.value.getAttribute('data-page-id')
    if (idAttr) {
      const id = Number(idAttr)
      const page = pages.value.find(p => p.id === id)
      if (page) {
        page.content = editor.value.innerHTML
      }
    }
  }
}

function updateSelection() {
  if (document.activeElement?.tagName === 'INPUT') return;

  const sel = window.getSelection()
  if (sel && sel.rangeCount > 0) {
    let node = sel.anchorNode
    let isInside = false
    while (node) {
      if (node === editor.value) {
        isInside = true
        break
      }
      node = node.parentNode || null
    }
    if (isInside) {
      lastEditorRange = sel.getRangeAt(0).cloneRange()

      try {
        let anchor = sel.anchorNode
        if (anchor && anchor.nodeType === Node.ELEMENT_NODE && sel.anchorOffset < anchor.childNodes.length) {
          let selectedNode = anchor.childNodes[sel.anchorOffset]
          if (selectedNode) {
            anchor = selectedNode
          }
        }

        let parent = anchor?.nodeType === Node.TEXT_NODE ? anchor.parentElement : anchor as HTMLElement
        
        function getDeepestFirstChild(n: Node): Node {
          if (n.firstChild) return getDeepestFirstChild(n.firstChild)
          return n
        }
        let deepest = getDeepestFirstChild(parent)
        if (deepest.nodeType === Node.TEXT_NODE && deepest.parentElement) {
          parent = deepest.parentElement
        }

        if (parent && parent.nodeName.toUpperCase() === 'RT') {
          parent = parent.parentElement as HTMLElement
        }
        if (parent) {
          const comp = window.getComputedStyle(parent)
          isBold.value = comp.fontWeight === 'bold' || parseInt(comp.fontWeight) >= 700
          isItalic.value = comp.fontStyle === 'italic'
          isUnderline.value = comp.textDecoration.includes('underline') || document.queryCommandState('underline')
          isList.value = !!parent.closest('ul') || document.queryCommandState('insertUnorderedList')

          const fontSizeMatch = comp.fontSize.match(/(\d+(?:\.\d+)?)(px|pt)/)
          if (fontSizeMatch) {
            if (fontSizeMatch[2] === 'px') {
              currentFontSize.value = Math.round(parseFloat(fontSizeMatch[1]) * 0.75)
            } else {
              currentFontSize.value = Math.round(parseFloat(fontSizeMatch[1]))
            }
          }

          const colorMatch = comp.color.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/)
          if (colorMatch) {
            const r = parseInt(colorMatch[1]).toString(16).padStart(2, '0')
            const g = parseInt(colorMatch[2]).toString(16).padStart(2, '0')
            const b = parseInt(colorMatch[3]).toString(16).padStart(2, '0')
            selectedColor.value = `#${r}${g}${b}`
          }

          // Font Family
          if (comp.fontFamily) {
            const normalizedFont = comp.fontFamily.replace(/"/g, '').toLowerCase()
            const match = fontOptions.find(opt => opt.value.replace(/"/g, '').toLowerCase().includes(normalizedFont.split(',')[0].trim()))
            if (match) {
              currentFontFamily.value = match.value
            }
          }

          // Heading
          const block = parent.closest('h1, h2, h3, h4, p')
          if (block) {
            currentHeading.value = block.tagName.toUpperCase()
          } else {
            currentHeading.value = 'P'
          }

          // Indent
          const blockForIndent = parent.closest('div, p, h1, h2, h3, h4, blockquote, li')
          if (blockForIndent) {
            const padding = window.getComputedStyle(blockForIndent).paddingInlineStart
            currentIndent.value = parseFloat(padding || '0')
          } else {
            currentIndent.value = 0
          }
        }
      } catch (e) {
        isBold.value = document.queryCommandState('bold')
        isItalic.value = document.queryCommandState('italic')
        isUnderline.value = document.queryCommandState('underline')
        isList.value = document.queryCommandState('insertUnorderedList')
      }
    }
  }
  syncUnderlineColors()
  syncRubyStyles()
}

function syncRubyStyles() {
  if (!editor.value) return
  const rubies = editor.value.querySelectorAll('ruby')

  rubies.forEach(ruby => {
    const walker = document.createTreeWalker(ruby, NodeFilter.SHOW_TEXT, null)
    let node
    let commonColor: string | null = null
    let commonFontSize: string | null = null
    let allSameColor = true
    let allSameFontSize = true
    let hasText = false

    const rts = Array.from(ruby.querySelectorAll('rt'))

    while ((node = walker.nextNode())) {
      if (!node.textContent?.trim()) continue

      let isRt = false
      let curr = node.parentElement
      while (curr && curr !== ruby) {
        if (curr.nodeName.toUpperCase() === 'RT') {
          isRt = true
          break
        }
        curr = curr.parentElement
      }
      if (isRt) continue

      hasText = true
      const comp = window.getComputedStyle(node.parentElement!)
      const nodeColor = comp.color
      const nodeFontSize = comp.fontSize

      if (commonColor === null) {
        commonColor = nodeColor
      } else if (commonColor !== nodeColor) {
        allSameColor = false
      }

      if (commonFontSize === null) {
        commonFontSize = nodeFontSize
      } else if (commonFontSize !== nodeFontSize) {
        allSameFontSize = false
      }
    }

    if (hasText) {
      if (allSameColor && commonColor) {
        ruby.style.color = commonColor
      } else {
        ruby.style.color = ''
      }

      if (allSameFontSize && commonFontSize) {
        ruby.style.fontSize = commonFontSize
      } else {
        ruby.style.fontSize = ''
      }
    }
  })
}

function syncUnderlineColors() {
  if (!editor.value) return;
  const allColoredSpans = Array.from(editor.value.querySelectorAll('[style*="color"], font[color]')) as HTMLElement[];
  allColoredSpans.forEach(span => {
    if (span.style.textDecoration === 'underline' || span.style.textDecorationLine === 'underline') {
      span.style.textDecoration = '';
      if (span.getAttribute('style') === '') span.removeAttribute('style');
    }
  });

  const uTags = editor.value.querySelectorAll('u');
  uTags.forEach(u => {
    const walker = document.createTreeWalker(u, NodeFilter.SHOW_TEXT, null);
    let node;
    let commonColor: string | null = null;
    let allSame = true;
    let hasText = false;

    while (node = walker.nextNode()) {
      if (!node.textContent || node.textContent.trim() === '') continue;
      hasText = true;
      let nodeColor = null;
      if (node.parentElement) {
        nodeColor = window.getComputedStyle(node.parentElement).color;
      }
      if (!nodeColor) {
        allSame = false;
      } else if (commonColor === null) {
        commonColor = nodeColor;
      } else if (commonColor !== nodeColor) {
        allSame = false;
      }
    }

    if (hasText && allSame && commonColor) {
      u.style.color = commonColor;
      u.style.textDecorationColor = commonColor;
    } else if (hasText && !allSame) {
      u.style.color = '';
      u.style.textDecorationColor = '';
      const coloredChildren = Array.from(u.querySelectorAll('[style*="color"], font[color]')) as HTMLElement[];
      coloredChildren.forEach(child => {
        if (child.style.color || child.getAttribute('color')) {
          child.style.textDecoration = 'underline';
        }
      });
    }
  });
}

function execCommand(command: string, value: string | undefined = undefined) {
  if (lastEditorRange && document.activeElement !== editor.value) {
    editor.value?.focus()
    const sel = window.getSelection()
    sel?.removeAllRanges()
    sel?.addRange(lastEditorRange)
  }
  document.execCommand(command, false, value)
  editor.value?.focus()
  updateSelection()
}

function updateSelectionState() {
  updateSelection()
  onInput()
}

function getClosestBlockElement(node: Node | null): HTMLElement | null {
  if (!node) return null;
  let element = node.nodeType === Node.TEXT_NODE ? node.parentElement : (node as HTMLElement);
  while (element) {
    if (element.classList?.contains('bg-white') || element.getAttribute('contenteditable') === 'true') break;
    const tagName = element.tagName.toLowerCase();
    
    if (['p', 'li', 'h1', 'h2', 'h3', 'h4', 'blockquote'].includes(tagName)) {
      return element;
    }
    
    if (tagName === 'div') {
      const isDirty = Array.from(element.children).some(child => 
        ['p', 'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'blockquote', 'div'].includes(child.tagName.toLowerCase())
      );
      if (!isDirty) {
        return element;
      }
      break;
    }
    
    element = element.parentElement;
  }
  return null;
}

function getBlocksInRange(range: Range): HTMLElement[] {
  const blocks = new Set<HTMLElement>();

  if (range.collapsed) {
    const block = getClosestBlockElement(range.startContainer);
    if (block) blocks.add(block);
    return Array.from(blocks);
  }

  const walker = document.createTreeWalker(
    range.commonAncestorContainer,
    NodeFilter.SHOW_TEXT,
    {
      acceptNode: (node) => {
        return range.intersectsNode(node) ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
      }
    }
  );

  let currentNode = walker.currentNode;
  if (currentNode && currentNode.nodeType === Node.TEXT_NODE && range.intersectsNode(currentNode)) {
    const block = getClosestBlockElement(currentNode);
    if (block) blocks.add(block);
  }

  while (walker.nextNode()) {
    const block = getClosestBlockElement(walker.currentNode);
    if (block) blocks.add(block);
  }

  return Array.from(blocks);
}

function customIndent() {
  if (lastEditorRange) {
    editor.value?.focus()
    const sel = window.getSelection()
    sel?.removeAllRanges()
    sel?.addRange(lastEditorRange)
  }

  const selection = window.getSelection();
  if (!selection || selection.rangeCount === 0) return;

  let node = selection.anchorNode;
  let isInsideEditor = false;
  while (node) {
    if (node.nodeType === Node.ELEMENT_NODE && (node as Element).getAttribute('contenteditable') === 'true') {
      isInsideEditor = true;
      break;
    }
    node = node.parentNode || null;
  }
  if (!isInsideEditor) return;

  let range = selection.getRangeAt(0);
  let blocks = getBlocksInRange(range);

  if (blocks.length === 0) {
    document.execCommand('formatBlock', false, 'DIV');
    range = selection.getRangeAt(0);
    blocks = getBlocksInRange(range);
  }

  blocks.forEach(block => {
    const currentPadding = parseFloat(block.style.paddingInlineStart || '0');
    block.style.paddingInlineStart = `${currentPadding + indentSize.value}em`;
  });

  updateSelectionState();
}

function customOutdent() {
  if (lastEditorRange) {
    editor.value?.focus()
    const sel = window.getSelection()
    sel?.removeAllRanges()
    sel?.addRange(lastEditorRange)
  }

  const selection = window.getSelection();
  if (!selection || selection.rangeCount === 0) return;

  let node = selection.anchorNode;
  let isInsideEditor = false;
  while (node) {
    if (node.nodeType === Node.ELEMENT_NODE && (node as Element).getAttribute('contenteditable') === 'true') {
      isInsideEditor = true;
      break;
    }
    node = node.parentNode || null;
  }
  if (!isInsideEditor) return;

  let range = selection.getRangeAt(0);
  let blocks = getBlocksInRange(range);

  if (blocks.length === 0) {
    document.execCommand('formatBlock', false, 'DIV');
    range = selection.getRangeAt(0);
    blocks = getBlocksInRange(range);
  }

  blocks.forEach(block => {
    const currentPadding = parseFloat(block.style.paddingInlineStart || '0');
    const newPadding = Math.max(0, currentPadding - indentSize.value);
    if (newPadding > 0) {
      block.style.paddingInlineStart = `${newPadding}em`;
    } else {
      block.style.paddingInlineStart = '';
    }
  });

  updateSelectionState();
}

function applyHeading(blockTag: string) {
  if (lastEditorRange) {
    editor.value?.focus()
    const sel = window.getSelection()
    sel?.removeAllRanges()
    sel?.addRange(lastEditorRange)
  }

  const cleanUpBlock = (targetBlock: HTMLElement) => {
    const spansAndRubies = targetBlock.querySelectorAll('span, ruby')
    spansAndRubies.forEach(el => {
      (el as HTMLElement).style.removeProperty('font-size')
      if (el.getAttribute('style') === '') el.removeAttribute('style')
      if (el.tagName.toLowerCase() === 'span' && el.attributes.length === 0) {
        while(el.firstChild) el.parentNode?.insertBefore(el.firstChild, el)
        el.parentNode?.removeChild(el)
      }
    })
  }

  const sel = window.getSelection()
  if (sel && sel.rangeCount > 0) {
    const range = sel.getRangeAt(0)
    const blocks = getBlocksInRange(range)
    if (blocks.length > 0) {
      blocks.forEach(block => {
        if (block.tagName.toUpperCase() !== blockTag.toUpperCase()) {
          const newBlock = document.createElement(blockTag)
          Array.from(block.attributes).forEach(attr => newBlock.setAttribute(attr.name, attr.value))
          while (block.firstChild) newBlock.appendChild(block.firstChild)
          block.parentNode?.replaceChild(newBlock, block)
          cleanUpBlock(newBlock)
        }
      })
    } else {
      document.execCommand('formatBlock', false, blockTag)
      const newSel = window.getSelection()
      if (newSel && newSel.rangeCount > 0) {
        const newBlocks = getBlocksInRange(newSel.getRangeAt(0))
        newBlocks.forEach(block => cleanUpBlock(block))
      }
    }
  } else {
    document.execCommand('formatBlock', false, blockTag)
  }

  editor.value?.focus()
  syncEditorToVue()
  onInput()
}

function updateFakeSelection() {
  const cssObj = (window as any).CSS
  if (cssObj && cssObj.highlights) {
    const HighlightClass = (window as any).Highlight
    if (HighlightClass && lastEditorRange) {
      const highlight = new HighlightClass(lastEditorRange)
      cssObj.highlights.set('editor-fake-selection', highlight)
    }
  }
}

function removeFakeSelection() {
  const cssObj = (window as any).CSS
  if (cssObj && cssObj.highlights) {
    cssObj.highlights.delete('editor-fake-selection')
  }
}

function commitFontSize() {
  let val = currentFontSize.value.toString().replace(/[０-９]/g, (s: string) => String.fromCharCode(s.charCodeAt(0) - 0xFEE0))
  val = val.replace(/[^0-9]/g, '')
  let size = Number(val)
  if (!isNaN(size) && val !== '') {
    size = Math.max(8, Math.min(72, size))
    currentFontSize.value = size
    applyCustomFontSize(size.toString())
  } else {
    currentFontSize.value = 12
  }
}

function handleFontSizeEnter(e: KeyboardEvent) {
  if (e.isComposing) return
  showFontSizeDropdown.value = false
  commitFontSize()
  setTimeout(() => {
    editor.value?.focus()
    const sel = window.getSelection()
    if (lastEditorRange) {
      sel?.removeAllRanges()
      sel?.addRange(lastEditorRange)
    }
  }, 50)
}

function selectDropdownFontSize(size: number) {
  currentFontSize.value = size
  showFontSizeDropdown.value = false
  commitFontSize()
  editor.value?.focus()
  const sel = window.getSelection()
  if (lastEditorRange) {
    sel?.removeAllRanges()
    sel?.addRange(lastEditorRange)
  }
}

function changeFontSize(delta: number) {
  let val = currentFontSize.value.toString().replace(/[０-９]/g, (s: string) => String.fromCharCode(s.charCodeAt(0) - 0xFEE0))
  val = val.replace(/[^0-9]/g, '')
  let size = parseInt(val)
  if (isNaN(size)) size = 12
  size += delta
  if (size < 1) size = 1
  currentFontSize.value = size
  applyCustomFontSize(size.toString())
}

function applyInlineStyleSafe(styleName: string, styleValue: string) {
  let range: Range | null = null
  const sel = window.getSelection()
  if (lastEditorRange) {
    range = lastEditorRange
  } else if (sel && sel.rangeCount > 0) {
    range = sel.getRangeAt(0)
  }
  if (!range) return

  // Bug 4 & 2 Fix: Handle empty lines or collapsed selections by inserting a Zero-Width Space
  if (range.collapsed) {
    const zws = document.createTextNode('\u200B')
    const span = document.createElement('span')
    span.style.setProperty(styleName, styleValue)
    span.appendChild(zws)
    range.insertNode(span)
    const newRange = document.createRange()
    newRange.setStart(zws, 1)
    newRange.collapse(true)
    if (document.activeElement?.tagName !== 'INPUT') {
      editor.value?.focus()
      const sel = window.getSelection()
      sel?.removeAllRanges()
      sel?.addRange(newRange)
    }
    lastEditorRange = newRange.cloneRange()
    onInput()
    return
  }

  const extracted = range.extractContents()
  const elements = Array.from(extracted.querySelectorAll('*'))
  elements.forEach(node => {
    if (node instanceof HTMLElement) {
      if (styleName === 'font-size') {
        node.style.removeProperty('font-size')
        if (node.nodeName === 'FONT') node.removeAttribute('size')
      } else if (styleName === 'color') {
        node.style.removeProperty('color')
        if (node.nodeName === 'FONT') node.removeAttribute('color')
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
  })

  const fragment = document.createDocumentFragment()

  function wrapTextNodes(node: Node) {
    if (node.nodeType === Node.TEXT_NODE) {
      if (node.textContent && node.textContent.trim().length > 0 || node.textContent === '\u200B') {
        const span = document.createElement('span')
        span.style.setProperty(styleName, styleValue)
        const clone = node.cloneNode(true)
        span.appendChild(clone)
        node.parentNode?.replaceChild(span, node)
      }
    } else if (node instanceof HTMLElement) {
      // Bug 1 & 3: Do not wrap text inside <rt> to avoid styling ruby text directly
      if (node.tagName.toUpperCase() !== 'RT') {
        Array.from(node.childNodes).forEach(wrapTextNodes)
      }
    }
  }

  Array.from(extracted.childNodes).forEach(wrapTextNodes)
  fragment.appendChild(extracted)

  // Check if fragment is empty after wrapping to prevent errors
  if (!fragment.firstChild) {
    return
  }

  const firstChild = fragment.firstChild
  const lastChild = fragment.lastChild

  range.insertNode(fragment)

  if (firstChild && lastChild) {
    const newRange = document.createRange()
    
    // Bug 2 Fix: Safely select the inserted content without throwing IndexSizeError
    try {
      newRange.setStartBefore(firstChild)
      newRange.setEndAfter(lastChild)
    } catch (e) {
      // Fallback if elements were modified/moved
      console.warn("Could not set precise range, collapsing to end")
      newRange.setStartAfter(lastChild)
      newRange.collapse(true)
    }
    
    if (document.activeElement?.tagName !== 'INPUT') {
      editor.value?.focus()
      const sel = window.getSelection()
      sel?.removeAllRanges()
      sel?.addRange(newRange)
    }
    lastEditorRange = newRange.cloneRange()
  }

  if (document.activeElement && document.activeElement.tagName === 'INPUT') {
    updateFakeSelection()
  }

  syncEditorToVue()
  onInput()
  syncUnderlineColors()
  syncRubyStyles()
}

function applyCustomFontSize(ptSize: string) {
  applyInlineStyleSafe('font-size', `${ptSize}pt`)
}

function applyColor() {
  applyInlineStyleSafe('color', selectedColor.value)
}

function openHelpModal() {
  helpModalVisible.value = true
}

function closeHelpModal() {
  helpModalVisible.value = false
}

function toggleDarkMode() {
  isDarkMode.value = !isDarkMode.value
  if (isDarkMode.value) {
    document.documentElement.classList.add('dark')
    localStorage.setItem('vertical-editor-theme', 'dark')
  } else {
    document.documentElement.classList.remove('dark')
    localStorage.setItem('vertical-editor-theme', 'light')
  }
}

function toggleSidebar() {
  isSidebarOpen.value = !isSidebarOpen.value
}

async function downloadPDF() {
  try {
    const htmlContent = documentContent.value.replace(MANUAL_BREAK, '<hr style="page-break-after: always; visibility: hidden;">')
    
    // サーバーサイドAPIにHTMLを送信してPDFを生成
    const response = await fetch('/api/export-pdf', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ html: htmlContent })
    })

    if (!response.ok) {
      throw new Error('PDFの生成に失敗しました')
    }

    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${documentTitle.value || 'document'}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('PDF Export Error:', error)
    alert('PDFの書き出しに失敗しました。')
  }
}

function openRubyModal() {
  let sel = window.getSelection()
  let node = sel?.anchorNode
  let isInsideEditor = false
  while (node) {
    if (node === editor.value) {
      isInsideEditor = true
      break
    }
    node = node.parentNode || null
  }

  if (!isInsideEditor && lastEditorRange) {
    editor.value?.focus()
    sel?.removeAllRanges()
    sel?.addRange(lastEditorRange)
    sel = window.getSelection()
    isInsideEditor = true
  }

  if (sel && sel.rangeCount > 0 && isInsideEditor && sel.toString().length > 0) {
    const range = sel.getRangeAt(0)
    
    let currentAnchor = sel.anchorNode
    let currentFocus = sel.focusNode
    let hasRubyAncestor = false
    
    const checkRuby = (n: Node | null) => {
      while (n && n !== editor.value) {
        if (n.nodeName.toUpperCase() === 'RUBY') return true
        n = n.parentNode
      }
      return false
    }
    
    if (checkRuby(currentAnchor) || checkRuby(currentFocus)) {
      hasRubyAncestor = true
    }
    
    const container = document.createElement('div')
    container.appendChild(range.cloneContents())
    const hasRubyChild = container.querySelector('ruby') !== null

    if (hasRubyAncestor || hasRubyChild) {
      alert("すでにルビが振られているテキストが含まれています。新しいルビを振る前に、既存のルビを解除してください。")
      return
    }

    savedRange = range.cloneRange()
    rubyModalVisible.value = true
    updateFakeSelection()
    return
  }
  alert("ルビを振るテキストをエディタ内で選択してください。")
}

function closeRubyModal() {
  rubyModalVisible.value = false
  removeFakeSelection()
}

function applyRuby(reading: string) {
  rubyModalVisible.value = false
  removeFakeSelection()
  if (!savedRange) return

  const sel = window.getSelection()
  if (sel) {
    sel.removeAllRanges()
    sel.addRange(savedRange)
  }
  if (savedRange.collapsed) {
    savedRange = null
    return
  }

  const extracted = savedRange.extractContents()
  const ruby = document.createElement('ruby')
  ruby.appendChild(extracted)

  const rt = document.createElement('rt')
  rt.textContent = reading
  rt.setAttribute('contenteditable', 'false')
  ruby.appendChild(rt)
  savedRange.insertNode(ruby)

  const zwsp = document.createTextNode('\u200B')
  ruby.parentNode?.insertBefore(zwsp, ruby.nextSibling)

  sel?.removeAllRanges()
  const newRange = document.createRange()
  newRange.setStartAfter(zwsp)
  newRange.collapse(true)
  sel?.addRange(newRange)
  lastEditorRange = newRange.cloneRange()

  savedRange = null
  editor.value?.focus()
  syncEditorToVue()
  onInput()
  syncUnderlineColors()
  syncRubyStyles()
}

function removeRuby() {
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) {
    alert("ルビを解除するには、解除したいルビを選択してからボタンを押してください。")
    return
  }

  const range = sel.getRangeAt(0)
  let found = false
  const rubies = editor.value?.querySelectorAll('ruby')
  if (rubies) {
    rubies.forEach(rubyElement => {
      if (range.intersectsNode(rubyElement)) {
        found = true
        const rt = rubyElement.querySelector('rt')
        if (rt) rt.remove()
        if (rubyElement.getAttribute('style')) {
          const span = document.createElement('span')
          span.setAttribute('style', rubyElement.getAttribute('style')!)
          while (rubyElement.firstChild) {
            span.appendChild(rubyElement.firstChild)
          }
          rubyElement.parentNode?.replaceChild(span, rubyElement)
        } else {
          const parent = rubyElement.parentNode
          while (rubyElement.firstChild) {
            parent?.insertBefore(rubyElement.firstChild, rubyElement)
          }
          parent?.removeChild(rubyElement)
        }
      }
    })
  }

  if (found) {
    syncEditorToVue()
    onInput()
    syncUnderlineColors()
    syncRubyStyles()
  } else {
    alert("ルビを解除するには、解除したいルビを選択してからボタンを押してください。")
  }
}

export function initPages() {
  const savedTheme = localStorage.getItem('vertical-editor-theme')
  if (savedTheme === 'dark' || (!savedTheme && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    isDarkMode.value = true
    if (typeof document !== 'undefined') document.documentElement.classList.add('dark')
  } else {
    isDarkMode.value = false
    if (typeof document !== 'undefined') document.documentElement.classList.remove('dark')
  }

  const savedDocument = localStorage.getItem('vertical-editor-document')
  if (savedDocument) {
    try {
      const data = JSON.parse(savedDocument)
      if (data.version === '2.0' && typeof data.content === 'string') {
        documentContent.value = data.content
        syncPagesFromDocument()
        return
      }
    } catch {
      // Fall back to the legacy page-array format below.
    }
  }

  const saved = localStorage.getItem('vertical-editor-pages')
  if (saved) {
    try {
      const legacyPages = JSON.parse(saved)
      if (!Array.isArray(legacyPages) || legacyPages.length === 0) throw new Error()
      setDocumentFromLegacyPages(legacyPages)
      persistDocument()
    } catch {
      documentContent.value = ''
      syncPagesFromDocument()
    }
  } else {
    const oldSaved = localStorage.getItem('vertical-editor')
    if (oldSaved) {
      documentContent.value = oldSaved
      syncPagesFromDocument()
      persistDocument()
      localStorage.removeItem('vertical-editor')
    } else {
      documentContent.value = ''
      syncPagesFromDocument()
    }
  }
}

function onInput(e?: Event) {
  if (document.activeElement?.tagName !== 'INPUT') {
    const sel = window.getSelection()
    if (sel && sel.rangeCount > 0) {
      let node = sel.anchorNode
      let isInside = false
      while (node) {
        if (node === editor.value) {
          isInside = true
          break
        }
        node = node.parentNode || null
      }
      if (isInside) {
        lastEditorRange = sel.getRangeAt(0).cloneRange()
      }
    }
  }

  if (e && e.target) {
    const el = e.target as HTMLElement;
    if (el.textContent?.trim() === '') {
      el.innerHTML = '';
      currentFontSize.value = 12;
    }
  }

  if (editor.value) {
    const pageIdStr = editor.value.getAttribute('data-page-id')
    if (pageIdStr) {
      const pageId = Number(pageIdStr)
      updatePageContent(pageId, editor.value.innerHTML)
    }
  }
  persistDocument()
  if (isRubyManagementMode.value) {
    refreshRubyList()
  }
}

function triggerFileInput() {
  fileInput.value?.click()
}

function handleFileRead(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (confirm("現在の内容がすべて消去され、読み込んだファイルの内容で上書きされます。よろしいですか？")) {
    const reader = new FileReader()
    reader.onload = (e) => {
      let content = e.target?.result as string

      try {
        const data = JSON.parse(content)
        if (data.version === '2.0' && typeof data.content === 'string') {
          documentContent.value = data.content
          syncPagesFromDocument()
          if (data.documentTitle) documentTitle.value = data.documentTitle
          if (data.paperSize) paperSize.value = data.paperSize
          if (data.margins) margins.value = data.margins
          persistDocument()
          target.value = ''
          return
        }

        if (data.pages && data.version) {
          setDocumentFromLegacyPages(data.pages)
          if (data.documentTitle) documentTitle.value = data.documentTitle
          if (data.paperSize) paperSize.value = data.paperSize
          if (data.margins) margins.value = data.margins
          persistDocument()
          target.value = ''
          return
        }
      } catch (err) {
        // Not a JSON file, or not our format. Fall through to text fallback.
      }

      // Fallback for regular text files
      if (file.name.endsWith('.txt')) {
        content = content.replace(/\n/g, '<br>')
      }
      documentContent.value = content
      syncPagesFromDocument()
      persistDocument()

      target.value = ''
    }
    reader.readAsText(file)
  }
  target.value = ''
}

function saveDocumentLocal() {
  const data = {
    documentTitle: documentTitle.value,
    content: documentContent.value,
    paperSize: paperSize.value,
    margins: margins.value,
    version: '2.0'
  }

  let fileName = documentTitle.value || '無題のドキュメント'

  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${fileName}.tategaki`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

function exportDocument(format: 'print' | 'html' = 'print') {
  if (!editor.value) return
  if (format === 'print') {
    window.print()
  } else if (format === 'html') {
    const content = documentContent.value.replace(MANUAL_BREAK, '<hr>')
    const blob = new Blob([content], { type: 'text/html' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'document.html'
    a.click()
    URL.revokeObjectURL(url)
  }
}

function handleKeyDown(e: KeyboardEvent) {
  if (e.key === 'Tab') {
    if (e.isComposing) return;
    e.preventDefault();
    
    const selection = window.getSelection();
    const isCollapsed = selection ? selection.isCollapsed : true;
    
    if (e.shiftKey) {
      customOutdent();
    } else if (!isCollapsed) {
      customIndent();
    } else {
      document.execCommand('insertHTML', false, '　');
    }
    return;
  }

  if (e.key === 'Enter') {
    if (e.isComposing) return;
    
    // カーソルがルビ(ruby)の中にある場合の改行時の挙動調整
    let selection = window.getSelection();
    if (selection && selection.rangeCount > 0) {
      let range = selection.getRangeAt(0);
      let container = range.startContainer;
      if (container.nodeType === 3 && container.parentNode) {
        container = container.parentNode;
      }
      
      const rubyAncestor = (container instanceof HTMLElement) ? container.closest('ruby') : null;
      if (rubyAncestor) {
        // Shift+Enterの場合は後続の独自ロジックに任せる
        if (!e.shiftKey) {
          // 通常のEnterの場合、ルビが真っ二つに割れるのを防ぐため、カーソルを外に出す
          let isAtStart = false;
          if (range.startContainer.nodeType === 3 && range.startOffset === 0) {
            let prev = range.startContainer.previousSibling;
            isAtStart = true;
            while(prev) {
              if (prev.nodeType === 3 && prev.textContent && prev.textContent.length > 0) {
                isAtStart = false; break;
              }
              if (prev.nodeType === 1 && prev.nodeName !== 'RT') {
                isAtStart = false; break;
              }
              prev = prev.previousSibling;
            }
          } else if (range.startContainer === rubyAncestor && range.startOffset === 0) {
            isAtStart = true;
          }

          if (isAtStart) {
            range.setStartBefore(rubyAncestor);
          } else {
            range.setStartAfter(rubyAncestor);
          }
          range.collapse(true);
          selection.removeAllRanges();
          selection.addRange(range);
        }
      }
    }

    if (e.shiftKey) {
      e.preventDefault();
      
      let selection = window.getSelection();
      if (!selection || selection.rangeCount === 0) return;
      let range = selection.getRangeAt(0);
      
      let container = range.startContainer;
      if (container.nodeType === 3) container = container.parentNode;
      
      if (container instanceof HTMLElement && container.hasAttribute('contenteditable')) {
        document.execCommand('formatBlock', false, 'div');
        selection = window.getSelection();
        if (selection && selection.rangeCount > 0) {
          range = selection.getRangeAt(0);
        }
      }
      
      const br = document.createElement('br');
      range.deleteContents();
      range.insertNode(br);
      
      const rubyAncestor = br.parentElement?.closest('ruby');
      if (rubyAncestor) {
        let isAtStart = true;
        let prev = br.previousSibling;
        while (prev) {
          if (prev.nodeType === Node.TEXT_NODE && prev.textContent && prev.textContent.length > 0) {
            isAtStart = false; break;
          }
          if (prev.nodeType === Node.ELEMENT_NODE) {
            isAtStart = false; break;
          }
          prev = prev.previousSibling;
        }
        
        if (isAtStart) {
          rubyAncestor.parentNode?.insertBefore(br, rubyAncestor);
        } else {
          rubyAncestor.parentNode?.insertBefore(br, rubyAncestor.nextSibling);
        }
      }
      
      range.setStartAfter(br);
      range.setEndAfter(br);
      
      let current = br.nextSibling;
      let isAtEnd = true;
      while (current) {
        if (current.nodeName === 'BR' || (current.nodeType === Node.TEXT_NODE && current.textContent.length > 0) || current.nodeType === Node.ELEMENT_NODE) {
          isAtEnd = false;
          break;
        }
        current = current.nextSibling;
      }

      if (isAtEnd) {
        const dummyBr = document.createElement('br');
        br.parentNode?.insertBefore(dummyBr, br.nextSibling);
      }
      
      selection.removeAllRanges();
      selection.addRange(range);
      
      if (e.target) {
        e.target.dispatchEvent(new Event('input', { bubbles: true }));
      }
    } else {
      setTimeout(() => {
        const editorElement = e.target as HTMLElement;
        if (editorElement) {
          const rubies = editorElement.querySelectorAll('ruby');
          let changed = false;
          rubies.forEach(ruby => {
            if (ruby.textContent?.trim() === '') {
              const br = document.createElement('br');
              ruby.parentNode?.replaceChild(br, ruby);
              changed = true;
            }
          });
          if (changed) {
            editorElement.dispatchEvent(new Event('input', { bubbles: true }));
          }
        }
      }, 0);
    }
    return;
  }

  if (e.key === 'Backspace' && !e.isComposing) {
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0) return;
    const range = selection.getRangeAt(0);

    if (range.collapsed) {
      const ancestor = range.commonAncestorContainer;
      const block = getClosestBlockElement(ancestor);

      if (block) {
        const preCaretRange = range.cloneRange();
        preCaretRange.selectNodeContents(block);
        preCaretRange.setEnd(range.startContainer, range.startOffset);
        const textBefore = preCaretRange.toString();

        if (textBefore.length === 0) {
          if (block.tagName.toLowerCase() === 'li') {
            e.preventDefault();
            document.execCommand('outdent', false, undefined);
            updateSelectionState();
            return;
          } else {
            const currentPadding = parseFloat(block.style.paddingInlineStart || '0');
            if (currentPadding > 0) {
              e.preventDefault();
              const newPadding = Math.max(0, currentPadding - indentSize.value);
              if (newPadding > 0) {
                block.style.paddingInlineStart = `${newPadding}em`;
              } else {
                block.style.paddingInlineStart = '';
              }
              updateSelectionState();
              return;
            }
          }
        }
      }
    }
  }
}

export const useEditorState = () => {
  const pageWidthPx = computed(() => paperDimensions[paperSize.value].width * MM_TO_PX)

  return {
    editor,
    documentRevision,
    pages,
    documentContent,
    documentTitle,
    initPages,
    fileInput,
    isBold,
    isItalic,
    isUnderline,
    isList,
    rubyModalVisible,
    currentFontSize,
    showFontSizeDropdown,
    selectedColor,
    currentEditorPageId,
    indentSize,
    currentFontFamily,
    currentHeading,
    currentIndent,
    fontOptions,
    showRulers,
    paperSize,
    margins,
    paperStyle,
    pageWidthPx,
    updateSelection,
    execCommand,
    customIndent,
    customOutdent,
    applyHeading,
    wordCountModalVisible,
    showWordCountWhileTyping,
    characterCount,
    characterCountWithSpaces,
    openWordCountModal,
    closeWordCountModal,
    updateFakeSelection,
    removeFakeSelection,
    isDraggingRuler,
    commitFontSize,
    handleFontSizeEnter,
    selectDropdownFontSize,
    changeFontSize,
    applyColor,
    onInput,
    openRubyModal,
    closeRubyModal,
    helpModalVisible,
    openHelpModal,
    closeHelpModal,
    downloadPDF,
    applyRuby,
    removeRuby,
    triggerFileInput,
    handleFileRead,
    exportDocument,
    saveDocumentLocal,
    handleKeyDown,
    layoutMode,
    scrollStyle,
    setLayoutMode,
    updatePageContent,
    addManualPage,
    deleteManualPage,
    persistDocument,
    isDarkMode,
    isSidebarOpen,
    toggleDarkMode,
    toggleSidebar,
    MM_TO_PX,
    paperDimensions,
    isRubyManagementMode,
    rubyList,
    toggleRubyManagementMode,
    refreshRubyList,
    removeSpecificRuby,
    scrollToRuby
  }
}
