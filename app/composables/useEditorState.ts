import { ref, computed } from 'vue'
import DOMPurify from 'dompurify'

export function sanitizeHTML(html: string): string {
  if (typeof window !== 'undefined') {
    return DOMPurify.sanitize(html, {
      ADD_ATTR: ['contenteditable', 'data-ruby-id', 'data-page-id', 'style', 'class'],
      ADD_TAGS: ['ruby', 'rt', 'rp', 'hr']
    })
  }
  return html
}

export interface Page {
  id: number;
  content: string;
  isOverflowing?: boolean;
  isWarningDismissed?: boolean;
}

const pages = ref<Page[]>([])

let nextId = Date.now()
function generateId() {
  return nextId++
}

// The document is stored as one continuous HTML stream.  Explicit page breaks
// are represented by markers in that stream; `pages` is only the editor view.
const documentContent = ref('')
const documentRevision = ref(0)
const MANUAL_BREAK = '<hr class="manual-page-break" style="display:none;">'
const manualBreakMarker = '<hr class="manual-page-break" style="display:none;">'

function splitPages(content: string): string[] {
  if (!content) return ['']
  return content.split(/<!--manual-page-break-->|<hr[^>]*class="manual-page-break"[^>]*>/gi)
}
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
const rubyList = ref<Array<{ id: string, text: string, rt: string, pageId: number, element: HTMLElement }>>([])

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
        ; (rubyElement as HTMLElement).style.backgroundColor = 'rgba(239, 68, 68, 0.4)'
      setTimeout(() => {
        ; (rubyElement as HTMLElement).style.backgroundColor = originalBg
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

  const segments = splitPages(documentContent.value)
  if (layoutMode.value === 'manual') {
    pages.value = segments.map((content, i) => ({
      id: pages.value[i]?.id || generateId(),
      content
    }))
  } else {
    pages.value = segments.map((content, index) => ({
      id: generateId() + index,
      content
    }))
  }
  if (pages.value.length === 0) pages.value = [{ id: generateId(), content: '' }]
}

export function setDocumentFromLegacyPages(legacyPages: Array<Pick<Page, 'content'>>) {
  documentContent.value = legacyPages.map(page => page.content).join(manualBreakMarker)
  syncPagesFromDocument()
}

function updatePageContent(pageId: number, content: string) {
  const index = pages.value.findIndex(page => page.id === pageId)
  if (index < 0) return
  const segments = splitPages(documentContent.value)
  segments[index] = content
  documentContent.value = segments.join(manualBreakMarker)
  pages.value[index].content = content
}

function addManualPage(afterPageId?: number) {
  const index = afterPageId === undefined
    ? pages.value.length - 1
    : pages.value.findIndex(page => page.id === afterPageId)
  const segments = splitPages(documentContent.value)
  const insertAt = index < 0 ? segments.length : index + 1

  // 原稿データを分割して新しいページを挿入
  segments.splice(insertAt, 0, '')
  // Vueの表示用配列（pages）も同時に更新し、新IDを割り振ることでズレを防ぐ
  pages.value.splice(insertAt, 0, { id: generateId(), content: '' })

  documentContent.value = segments.join(manualBreakMarker)
  documentRevision.value++
  persistDocument()
}

function deleteManualPage(pageId: number) {
  const index = pages.value.findIndex(page => page.id === pageId)
  if (index < 0 || pages.value.length <= 1) return
  const segments = splitPages(documentContent.value)

  // 原稿データからページを削除
  segments.splice(index, 1)
  // Vueの表示用配列からも同時に削除し、残ったページのIDを正しく維持する
  pages.value.splice(index, 1)

  documentContent.value = segments.join(manualBreakMarker)
  documentRevision.value++
  persistDocument()
}

export function persistDocument() {
  localStorage.setItem('vertical-editor-document', JSON.stringify({
    version: '2.0',
    title: documentTitle.value,
    content: documentContent.value
  }))
}

const setLayoutMode = (mode: 'auto' | 'manual') => {
  if (layoutMode.value === mode) return

  if (mode === 'auto' && pages.value.length > 1) {
    documentContent.value = documentContent.value.replace(new RegExp('<!--manual-break-->', 'g'), '<br><br>')
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

// Use exact browser physical conversion ratio to prevent micro-overflows in print (1in = 96px = 25.4mm)
const MM_TO_PX = 96 / 25.4
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

  return {
    '--page-width': `${pageWidth}px`,
    '--page-height': `${pageHeight}px`,
    // エディタ側の余白をあえて0.2mm広く（印字領域を狭く）することで、
    // エディタ上で収まったテキストが印刷時に確実にはみ出ないようにする安全マージン
    '--margin-top': `${(margins.value.top + 0.2) * MM_TO_PX}px`,
    '--margin-bottom': `${(margins.value.bottom + 0.2) * MM_TO_PX}px`,
    '--margin-right': `${(margins.value.right + 0.2) * MM_TO_PX}px`,
    '--margin-left': `${(margins.value.left + 0.2) * MM_TO_PX}px`,
    fontSize: '16px',
    lineHeight: '1.8'
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
    let editorEl: HTMLElement | null = null
    while (node) {
      if ((node as HTMLElement).hasAttribute && (node as HTMLElement).hasAttribute('data-page-id')) {
        editorEl = node as HTMLElement
        break
      }
      node = node.parentNode || null
    }

    if (editorEl) {
      editor.value = editorEl
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
  repairSplitRubies()
  syncUnderlineColors()
  syncRubyStyles()
}

function repairSplitRubies() {
  if (!editor.value) return;
  const rubies = Array.from(editor.value.querySelectorAll('ruby'));

  // Go backwards so we can merge right-to-left safely
  for (let i = rubies.length - 1; i > 0; i--) {
    const right = rubies[i];
    const left = rubies[i - 1];

    // Check if they are exactly adjacent (ignoring empty text nodes)
    let isAdjacent = false;
    let curr: Node | null = left.nextSibling;
    while (curr) {
      if (curr === right) {
        isAdjacent = true;
        break;
      }
      if (curr.nodeType === Node.TEXT_NODE && !curr.textContent?.trim()) {
        curr = curr.nextSibling;
        continue;
      }
      break;
    }

    if (isAdjacent) {
      const leftRt = left.querySelector('rt');
      const rightRt = right.querySelector('rt');

      // If one of them has an <rt> and the other doesn't, it was likely split by execCommand
      if ((leftRt && !rightRt) || (!leftRt && rightRt)) {
        // Merge right into left
        while (right.firstChild) {
          left.appendChild(right.firstChild);
        }
        right.remove();

        // Ensure ALL <rt> elements are moved to the very end of the combined ruby
        const rts = Array.from(left.querySelectorAll('rt'));
        rts.forEach(rt => left.appendChild(rt));
      }
    }
  }
}

function syncRubyStyles() {
  if (!editor.value) return
  const rubies = editor.value.querySelectorAll('ruby')
  rubies.forEach(ruby => {
    // インラインカラーの上書きを解除し、テーマ色を保持する
    ruby.style.color = ''
  })
}

function syncUnderlineColors() {
  if (!editor.value) return
  const uTags = editor.value.querySelectorAll('u')
  uTags.forEach(u => {
    // インラインカラーの上書きを解除し、テーマ色を保持する
    u.style.color = ''
    u.style.textDecorationColor = ''
  })
}

function execCommand(command: string, value: string | undefined = undefined) {
  if (lastEditorRange) {
    const container = lastEditorRange.commonAncestorContainer
    const targetElem = container.nodeType === Node.ELEMENT_NODE ? (container as HTMLElement) : container.parentElement
    const currentEditable = targetElem?.closest('[contenteditable="true"]') as HTMLElement | null

    if (currentEditable && document.activeElement !== currentEditable) {
      currentEditable.focus({ preventScroll: true })
      const sel = window.getSelection()
      sel?.removeAllRanges()
      sel?.addRange(lastEditorRange)
    }
  }
  document.execCommand(command, false, value)
  updateSelection()
}

function toggleUnorderedList() {
  let targetEditor: HTMLElement | null = null

  if (lastEditorRange) {
    const container = lastEditorRange.commonAncestorContainer
    const targetElem = container.nodeType === Node.ELEMENT_NODE ? (container as HTMLElement) : container.parentElement
    targetEditor = targetElem?.closest('[contenteditable="true"]') as HTMLElement | null
  }

  if (!targetEditor && editor.value) {
    targetEditor = editor.value
  }

  if (targetEditor && document.activeElement !== targetEditor) {
    targetEditor.focus({ preventScroll: true })
    if (lastEditorRange) {
      const sel = window.getSelection()
      sel?.removeAllRanges()
      sel?.addRange(lastEditorRange)
    }
  }

  const sel = window.getSelection()
  let blocks: HTMLElement[] = []

  if (sel && sel.rangeCount > 0) {
    const range = sel.getRangeAt(0)
    blocks = getBlocksInRange(range)
  }

  if (blocks.length === 0 && targetEditor) {
    const firstBlock = targetEditor.querySelector('div, p, li, h1, h2, h3, h4') as HTMLElement | null
    if (firstBlock) {
      blocks = [firstBlock]
    } else {
      const newDiv = document.createElement('div')
      newDiv.innerHTML = '<br>'
      targetEditor.appendChild(newDiv)
      blocks = [newDiv]
    }
  }

  if (blocks.length > 0) {
    const isAllLi = blocks.every(b => b.tagName.toUpperCase() === 'LI' || b.closest('li') !== null)

    if (isAllLi) {
      // 既に箇条書きの場合は解除してDIVに戻す
      blocks.forEach(block => {
        const li = block.tagName.toUpperCase() === 'LI' ? block : block.closest('li')
        if (li) {
          const ul = li.parentElement
          const newDiv = document.createElement('div')
          while (li.firstChild) newDiv.appendChild(li.firstChild)
          if (!newDiv.textContent) newDiv.innerHTML = '<br>'
          if (ul?.parentNode) {
            ul.parentNode.insertBefore(newDiv, ul)
          }
          li.remove()
          if (ul && ul.children.length === 0) ul.remove()
        }
      })
    } else {
      // 箇条書きでない場合はUL > LIでラップする
      blocks.forEach(block => {
        const targetBlock = block.closest('li') ? block.closest('li')! : block
        if (targetBlock.tagName.toUpperCase() !== 'LI') {
          const ul = document.createElement('ul')
          const li = document.createElement('li')
          const innerDiv = document.createElement('div')
          innerDiv.style.display = 'inline'

          while (targetBlock.firstChild) innerDiv.appendChild(targetBlock.firstChild)
          if (!innerDiv.textContent) innerDiv.textContent = '\u200B'

          li.appendChild(innerDiv)
          ul.appendChild(li)
          targetBlock.parentNode?.replaceChild(ul, targetBlock)
        }
      })
    }
    syncEditorToVue()
    updateSelectionState()
  }
}

function updateSelectionState() {
  updateSelection()
  onInput()
}

function getClosestBlockElement(node: Node | null): HTMLElement | null {
  if (!node) return null;
  let element = node.nodeType === Node.TEXT_NODE ? node.parentElement : (node as HTMLElement);
  while (element) {
    if (element.getAttribute('contenteditable') === 'true' || element.classList?.contains('writing-vertical')) {
      break;
    }
    const tagName = element.tagName.toLowerCase();
    if (['p', 'li', 'h1', 'h2', 'h3', 'h4', 'blockquote', 'div'].includes(tagName)) {
      return element;
    }
    element = element.parentElement;
  }

  let directChild = node.nodeType === Node.TEXT_NODE ? node.parentElement : (node as HTMLElement);
  while (directChild && directChild.parentElement && directChild.parentElement.getAttribute('contenteditable') !== 'true' && !directChild.parentElement.classList?.contains('writing-vertical')) {
    directChild = directChild.parentElement;
  }
  return directChild;
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
    editor.value?.focus({ preventScroll: true })
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
    editor.value?.focus({ preventScroll: true })
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
    editor.value?.focus({ preventScroll: true })
    const sel = window.getSelection()
    sel?.removeAllRanges()
    sel?.addRange(lastEditorRange)
  }

  const cleanUpBlock = (targetBlock: HTMLElement) => {
    const spans = targetBlock.querySelectorAll('span')
    spans.forEach(el => {
      (el as HTMLElement).style.removeProperty('font-size')
      if (el.getAttribute('style') === '') el.removeAttribute('style')
      if (el.tagName.toLowerCase() === 'span' && el.attributes.length === 0) {
        while (el.firstChild) el.parentNode?.insertBefore(el.firstChild, el)
        el.parentNode?.removeChild(el)
      }
    })
  }

  const sel = window.getSelection()
  if (sel && sel.rangeCount > 0) {
    const range = sel.getRangeAt(0)
    const blocks = getBlocksInRange(range)
    if (blocks.length > 0) {
      blocks.forEach(rawBlock => {
        let block = rawBlock;
        if (['RUBY', 'RT'].includes(block.tagName.toUpperCase())) {
          const parentBlock = block.closest('p, div, h1, h2, h3, h4, li');
          if (parentBlock) {
            block = parentBlock;
          } else {
            const p = document.createElement('p');
            block.parentNode?.insertBefore(p, block);
            p.appendChild(block);
            block = p;
          }
        }
        const isSameTag = block.tagName.toUpperCase() === blockTag.toUpperCase();
        if (!isSameTag) {
          const isTargetDivOrP = blockTag.toUpperCase() === 'DIV' || blockTag.toUpperCase() === 'P';

          if (block.tagName.toUpperCase() === 'LI') {
            if (!isTargetDivOrP) {
              const innerBlock = document.createElement(blockTag)
              while (block.firstChild) innerBlock.appendChild(block.firstChild)
              block.appendChild(innerBlock)
              cleanUpBlock(innerBlock)
            }
          } else if (isTargetDivOrP && block.parentNode?.nodeName.toUpperCase() === 'LI') {
            while (block.firstChild) block.parentNode.insertBefore(block.firstChild, block)
            block.parentNode.removeChild(block)
          } else {
            const newBlock = document.createElement(blockTag)
            Array.from(block.attributes).forEach(attr => newBlock.setAttribute(attr.name, attr.value))
            while (block.firstChild) newBlock.appendChild(block.firstChild)
            block.parentNode?.replaceChild(newBlock, block)
            cleanUpBlock(newBlock)
          }
        }
      })
    } else {
      const caretNode = range.startContainer
      const caretBlock = getClosestBlockElement(caretNode)
      if (caretBlock && caretBlock.tagName.toUpperCase() !== blockTag.toUpperCase()) {
        const newBlock = document.createElement(blockTag)
        Array.from(caretBlock.attributes).forEach(attr => newBlock.setAttribute(attr.name, attr.value))
        while (caretBlock.firstChild) newBlock.appendChild(caretBlock.firstChild)
        caretBlock.parentNode?.replaceChild(newBlock, caretBlock)
        cleanUpBlock(newBlock)
      }
    }
  } else {
    // fallback if no selection
  }

  editor.value?.focus({ preventScroll: true })
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
    editor.value?.focus({ preventScroll: true })
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
  editor.value?.focus({ preventScroll: true })
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

function applyCustomFontSize(ptSize: string) {
  const sel = window.getSelection();
  if (lastEditorRange && document.activeElement?.tagName !== 'INPUT') {
    editor.value?.focus({ preventScroll: true });
    sel?.removeAllRanges();
    sel?.addRange(lastEditorRange);
  }

  // Use native execCommand for robust selection boundary handling (avoids extractContents corruption)
  document.execCommand('fontSize', false, '7');

  if (editor.value) {
    const fonts = Array.from(editor.value.querySelectorAll('font[size="7"], [style*="xxx-large"]'));
    const modifiedElements: HTMLElement[] = [];

    fonts.forEach((font: Element) => {
      const el = font as HTMLElement;
      el.style.fontSize = `${ptSize}pt`;
      let finalEl = el;

      if (el.tagName === 'FONT') {
        el.removeAttribute('size');
        // Convert to span if it only has styles now
        if (el.attributes.length === 1 && el.hasAttribute('style')) {
          const span = document.createElement('span');
          span.setAttribute('style', el.getAttribute('style')!);
          while (el.firstChild) span.appendChild(el.firstChild);
          el.parentNode?.replaceChild(span, el);
          finalEl = span;
        }
      }
      // Ensure <rt> inside ruby retains CSS 0.5em font size
      finalEl.querySelectorAll('rt').forEach(rtElem => (rtElem as HTMLElement).style.removeProperty('font-size'));
      modifiedElements.push(finalEl);
    });

    if (modifiedElements.length > 0) {
      try {
        const newRange = document.createRange();
        newRange.setStartBefore(modifiedElements[0]);
        newRange.setEndAfter(modifiedElements[modifiedElements.length - 1]);
        sel?.removeAllRanges();
        sel?.addRange(newRange);
        lastEditorRange = newRange.cloneRange();
      } catch (e) {
        console.warn("Could not perfectly restore selection", e);
      }
    }
  }

  onInput();
  syncUnderlineColors();
  syncRubyStyles();
}

function applyColor() {
  const sel = window.getSelection();
  if (lastEditorRange && document.activeElement?.tagName !== 'INPUT') {
    editor.value?.focus({ preventScroll: true });
    sel?.removeAllRanges();
    sel?.addRange(lastEditorRange);
  }

  const tempColor = '#fefefe';
  document.execCommand('foreColor', false, tempColor);

  if (editor.value) {
    const fonts = Array.from(editor.value.querySelectorAll(`font[color="${tempColor}"], [style*="rgb(254, 254, 254)"], [style*="#fefefe"]`));
    const modifiedElements: HTMLElement[] = [];

    fonts.forEach((font: Element) => {
      const el = font as HTMLElement;
      el.style.color = selectedColor.value;
      let finalEl = el;

      if (el.tagName === 'FONT') {
        el.removeAttribute('color');
        if (el.attributes.length === 1 && el.hasAttribute('style')) {
          const span = document.createElement('span');
          span.setAttribute('style', el.getAttribute('style')!);
          while (el.firstChild) span.appendChild(el.firstChild);
          el.parentNode?.replaceChild(span, el);
          finalEl = span;
        }
      }
      modifiedElements.push(finalEl);
    });

    if (modifiedElements.length > 0) {
      try {
        const newRange = document.createRange();
        newRange.setStartBefore(modifiedElements[0]);
        newRange.setEndAfter(modifiedElements[modifiedElements.length - 1]);
        sel?.removeAllRanges();
        sel?.addRange(newRange);
        lastEditorRange = newRange.cloneRange();
      } catch (e) {
        console.warn("Could not perfectly restore selection", e);
      }
    }
  }

  onInput();
  syncUnderlineColors();
  syncRubyStyles();
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
    // 各ページの末尾にある不要な空行やbrタグを削除して余分な白紙ページが生成されるのを防ぐ
    const segments = documentContent.value.split(MANUAL_BREAK);
    const cleanSegments = segments.map(seg => seg.replace(/(<div[^>]*>\s*<br\s*\/?>\s*<\/div>|<p[^>]*>\s*<br\s*\/?>\s*<\/p>|<br\s*\/?>|\s)+$/i, ''));
    const htmlContent = cleanSegments.join('<hr style="page-break-after: always; visibility: hidden; margin: 0; padding: 0; border: none;">');

    // サーバーサイドAPIにHTMLを送信してPDFを生成
    const response = await fetch('/api/export-pdf', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        html: htmlContent,
        paperSize: paperSize.value,
        margins: margins.value
      })
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
    editor.value?.focus({ preventScroll: true })
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
  const blockElements = extracted.querySelectorAll('h1, h2, h3, h4, p, div, section, article')
  blockElements.forEach(blk => {
    while (blk.firstChild) blk.parentNode?.insertBefore(blk.firstChild, blk)
    blk.parentNode?.removeChild(blk)
  })

  // Ensure ruby is inserted inside the current block element (p or h1), not wrapping it
  const ruby = document.createElement('ruby')
  ruby.appendChild(extracted)

  const rt = document.createElement('rt')
  rt.textContent = reading
  ruby.appendChild(rt)

  const activeBlock = getClosestBlockElement(savedRange.startContainer)
  if (activeBlock && activeBlock.contains(savedRange.startContainer)) {
    savedRange.insertNode(ruby)
  } else {
    savedRange.insertNode(ruby)
  }

  sel?.removeAllRanges()
  const newRange = document.createRange()
  newRange.setStartAfter(ruby)
  newRange.collapse(true)
  sel?.addRange(newRange)
  lastEditorRange = newRange.cloneRange()
  lastEditorRange = newRange.cloneRange()

  savedRange = null
  editor.value?.focus({ preventScroll: true })
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
  if (typeof window === 'undefined') return
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
        if (data.title) {
          documentTitle.value = data.title
        }
        documentContent.value = sanitizeHTML(data.content)
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

    // 漢字テキストが削除され<rt>ふりがなだけ残った孤立ルビを自動消去
    const rubies = el.querySelectorAll('ruby');
    rubies.forEach(ruby => {
      const rt = ruby.querySelector('rt');
      const rtText = rt?.textContent || '';
      const baseText = ruby.textContent?.replace(rtText, '').trim() || '';
      if (baseText === '') {
        ruby.remove();
      }
    });

    // <rt>タグの編集保護属性を解除し、通常削除ができるようにクリーンアップ
    const rts = el.querySelectorAll('rt');
    rts.forEach(rt => {
      if (rt.hasAttribute('contenteditable')) {
        rt.removeAttribute('contenteditable');
      }
    });
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
          documentContent.value = sanitizeHTML(data.content)
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
      documentRevision.value++
      persistDocument()
      repairSplitRubies()
      syncRubyStyles()

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
    const iframe = document.createElement('iframe')
    iframe.style.position = 'fixed'
    iframe.style.right = '0'
    iframe.style.bottom = '0'
    iframe.style.width = '0'
    iframe.style.height = '0'
    iframe.style.border = 'none'
    document.body.appendChild(iframe)

    const doc = iframe.contentWindow?.document
    if (doc) {
      const pageSizeCss = paperSize.value === 'A4' ? '297mm 210mm' : '257mm 182mm';
      const marginCss = `${margins.value.top}mm ${margins.value.right}mm ${margins.value.bottom}mm ${margins.value.left}mm`;
      // Fix Bug 1: Do not use overflow:hidden as it risks clipping text.
      // Instead, strip trailing empty line breaks (<br>, <div><br></div>, etc.) from the very end of EACH page.
      const segments = documentContent.value.split(MANUAL_BREAK);
      const cleanSegments = segments.map(seg => seg.replace(/(<div[^>]*>\s*<br\s*\/?>\s*<\/div>|<p[^>]*>\s*<br\s*\/?>\s*<\/p>|<br\s*\/?>|\s)+$/i, ''));
      const htmlBody = cleanSegments.join('<hr style="page-break-after: always; visibility: hidden; margin: 0; padding: 0; border: none;">');

      doc.open()
      doc.write(`
        <!DOCTYPE html>
        <html lang="ja">
        <head>
          <meta charset="UTF-8">
          <link href="https://fonts.googleapis.com/css2?family=Noto+Serif+JP:wght@400;700&display=swap" rel="stylesheet">
          <style>
            @page {
              size: ${pageSizeCss};
              margin: ${marginCss};
            }
            /* Tailwind reset to match editor DOM */
            * {
              margin: 0;
              padding: 0;
              box-sizing: border-box;
            }
            body {
              font-family: 'Noto Serif JP', serif;
              writing-mode: vertical-rl;
              line-height: 1.8;
              font-size: 16px;
              color: black;
            }
            .writing-vertical {
              writing-mode: vertical-rl;
            }
            h1, h2, h3, h4 { font-weight: bold; }
            h1 { font-size: 2em; margin-bottom: 0.67em; }
            h2 { font-size: 1.5em; margin-bottom: 0.83em; }
            h3 { font-size: 1.17em; margin-bottom: 1em; }
            h4 { font-size: 1em; margin-bottom: 1.33em; }
            p:empty::before { content: '\\200B'; }
            ruby > rt {
              font-size: 0.5em;
            }
            b, strong { font-weight: bold; }
            i, em { font-style: italic; }
            u, [style*="underline"] { text-decoration: underline; text-decoration-color: black; }
            img { max-width: 100%; height: auto; }
            table { border-collapse: collapse; }
            th, td { border: 1px solid black; padding: 0.5em; }
          </style>
        </head>
        <body>
          ${htmlBody}
        </body>
        </html>
      `)
      doc.close()

      const win = iframe.contentWindow
      if (win) {
        win.document.fonts.ready.then(() => {
          win.print()
          document.body.removeChild(iframe)
        })
      }
    }
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

function isCursorAtStartOfRuby(range: Range, rubyElement: Element) {
  const container = range.startContainer;
  const offset = range.startOffset;

  if (container === rubyElement && offset === 0) return true;

  if (container.nodeType === 1 && container.contains(rubyElement)) {
    const children = Array.from(container.childNodes);
    const rubyIdx = children.indexOf(rubyElement as ChildNode);
    if (offset <= rubyIdx) return true;
  }

  if (container.nodeType === 3) {
    let textBeforeInRuby = container.textContent?.substring(0, offset) || '';
    if (textBeforeInRuby.trim().length > 0) return false;

    let prev = container.previousSibling;
    while (prev) {
      if (prev.nodeName !== 'RT' && prev.textContent && prev.textContent.trim().length > 0) {
        return false;
      }
      prev = prev.previousSibling;
    }
    return true;
  }

  return false;
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
      if (selection && selection.rangeCount > 0) {
        const range = selection.getRangeAt(0);
        let container = range.startContainer;
        let targetRuby: Element | null = null;

        if (container.nodeType === 3) {
          const ruby = container.parentElement?.closest('ruby');
          if (ruby && isCursorAtStartOfRuby(range, ruby)) {
            targetRuby = ruby;
          }
        } else if (container.nodeType === 1) {
          const child = container.childNodes[range.startOffset];
          if (child && child.nodeName === 'RUBY') {
            targetRuby = child as Element;
          }
        }

        if (targetRuby) {
          const space = document.createTextNode('　');
          targetRuby.parentNode?.insertBefore(space, targetRuby);

          range.setStartAfter(space);
          range.collapse(true);
          selection.removeAllRanges();
          selection.addRange(range);

          if (editor.value) {
            editor.value.dispatchEvent(new Event('input', { bubbles: true }));
          }
          return;
        }
      }
      document.execCommand('insertHTML', false, '　');
    }
    return;
  }

  if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(e.key) && !e.isComposing) {
    const sel = window.getSelection();
    if (sel && sel.rangeCount > 0) {
      const r = sel.getRangeAt(0);
      const container = r.startContainer;
      const rt = (container.nodeType === 3)
        ? container.parentElement?.closest('rt')
        : (container instanceof HTMLElement ? container.closest('rt') : null);

      if (rt) {
        const ruby = rt.closest('ruby');
        if (ruby) {
          e.preventDefault();
          const newRange = document.createRange();
          if (e.key === 'ArrowUp' || e.key === 'ArrowRight') {
            newRange.setStartBefore(ruby);
          } else {
            newRange.setStartAfter(ruby);
          }
          newRange.collapse(true);
          sel.removeAllRanges();
          sel.addRange(newRange);
          return;
        }
      }
    }
  }

  if (e.key === 'Enter') {
    if (e.isComposing) return;

    const selection = window.getSelection();
    if (selection && selection.rangeCount > 0) {
      const range = selection.getRangeAt(0);
      const activeBlock = getClosestBlockElement(range.startContainer);

      if (activeBlock) {
        const rubyElement = (range.startContainer.nodeType === 3)
          ? range.startContainer.parentElement?.closest('ruby')
          : (range.startContainer instanceof HTMLElement ? range.startContainer.closest('ruby') : null);

        if (rubyElement || ['h1', 'h2', 'h3', 'h4'].includes(activeBlock.tagName.toLowerCase())) {
          e.preventDefault();
          const isAtStart = rubyElement
            ? isCursorAtStartOfRuby(range, rubyElement)
            : (range.startOffset === 0);

          const newP = document.createElement('p');
          newP.innerHTML = '<br>';

          if (isAtStart) {
            activeBlock.parentNode?.insertBefore(newP, activeBlock);
          } else {
            activeBlock.parentNode?.insertBefore(newP, activeBlock.nextSibling);
            const newRange = document.createRange();
            newRange.selectNodeContents(newP);
            newRange.collapse(true);
            selection.removeAllRanges();
            selection.addRange(newRange);
          }
          updateSelectionState();
          return;
        }
      }
    }

    if (e.shiftKey) {
      e.preventDefault();
      document.execCommand('insertLineBreak');
      onInput(e);
      return;
    }
  }

  if ((e.key === 'Backspace' || e.key === 'Delete') && !e.isComposing) {
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0) return;
    const range = selection.getRangeAt(0);

    if (range.collapsed) {
      const ancestor = range.commonAncestorContainer;
      const block = getClosestBlockElement(ancestor);

      if (block) {
        if (e.key === 'Backspace') {
          let textBefore = '';
          try {
            const preCaretRange = range.cloneRange();
            preCaretRange.selectNodeContents(block);
            preCaretRange.setEnd(range.startContainer, range.startOffset);
            textBefore = preCaretRange.toString();
          } catch {
            textBefore = 'non-empty';
          }

          if (textBefore.length === 0) {
            const tag = block.tagName.toLowerCase();
            const prevBlock = block.previousElementSibling as HTMLElement | null;

            if (!prevBlock || prevBlock.textContent?.trim() === '') {
              // 【パターンA: 前に文字がない場合】元のスタイル（見出し・文字サイズ）をそのまま100%維持して引き上げる
              if (prevBlock) prevBlock.remove();
              e.preventDefault();
              updateSelectionState();
              return;
            } else {
              // 【パターンB: 前に文字があり結合する場合】移動先（前行）のフォントサイズ・スタイルに自動変更して末尾へ合体
              e.preventDefault();
              const prevRange = document.createRange();
              prevRange.selectNodeContents(prevBlock);
              prevRange.collapse(false);

              while (block.firstChild) {
                const child = block.firstChild;
                if (child.nodeType === 1) {
                  const elem = child as HTMLElement;
                  elem.style.removeProperty('font-size');
                  elem.querySelectorAll('*').forEach(inner => (inner as HTMLElement).style.removeProperty('font-size'));
                }
                prevBlock.appendChild(child);
              }
              block.remove();
              const sel = window.getSelection();
              if (sel) {
                sel.removeAllRanges();
                sel.addRange(prevRange);
              }
              updateSelectionState();
              return;
            }
          }
          if (tag === 'li') {
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
        } else if (e.key === 'Delete') {
          let textAfter = '';
          try {
            const postCaretRange = range.cloneRange();
            postCaretRange.selectNodeContents(block);
            postCaretRange.setStart(range.endContainer, range.endOffset);
            textAfter = postCaretRange.toString();
          } catch {
            textAfter = 'non-empty';
          }

          if (textAfter.length === 0) {
            const nextBlock = block.nextElementSibling as HTMLElement | null;
            if (nextBlock) {
              const nextTag = nextBlock.tagName.toLowerCase();
              if (['h1', 'h2', 'h3', 'h4'].includes(nextTag)) {
                // 下の見出し行を現在行へ結合させ、文字の膨張を防ぐ
                e.preventDefault();
                while (nextBlock.firstChild) {
                  block.appendChild(nextBlock.firstChild);
                }
                nextBlock.remove();
                updateSelectionState();
                return;
              }
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
    toggleUnorderedList,
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
