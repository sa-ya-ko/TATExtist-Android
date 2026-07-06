import { ref, computed } from 'vue'

export interface Page {
  id: number;
  content: string;
  isOverflowing?: boolean;
  isWarningDismissed?: boolean;
}

const pages = ref<Page[]>([])
const editor = ref<HTMLElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

const isBold = ref(false)
const isItalic = ref(false)
const isUnderline = ref(false)
const rubyModalVisible = ref(false)
const currentFontSize = ref(12)
const showFontSizeDropdown = ref(false)
const selectedColor = ref('#000000')

const paperSize = ref<'A4' | 'B5'>('A4')
const margins = ref({ top: 30, bottom: 30, left: 30, right: 30 })

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

function updateSelection() {
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
        if (parent && parent.nodeName.toUpperCase() === 'RT') {
          parent = parent.parentElement as HTMLElement
        }
        if (parent) {
          const comp = window.getComputedStyle(parent)
          isBold.value = comp.fontWeight === 'bold' || parseInt(comp.fontWeight) >= 700
          isItalic.value = comp.fontStyle === 'italic'
          isUnderline.value = comp.textDecoration.includes('underline') || document.queryCommandState('underline')
          
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
        }
      } catch (e) {
        isBold.value = document.queryCommandState('bold')
        isItalic.value = document.queryCommandState('italic')
        isUnderline.value = document.queryCommandState('underline')
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
        rts.forEach(rt => rt.style.color = commonColor as string)
      } else {
        ruby.style.color = ''
        rts.forEach(rt => rt.style.color = '')
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

function applyHeading(blockTag: string) {
  if (lastEditorRange) {
    editor.value?.focus()
    const sel = window.getSelection()
    sel?.removeAllRanges()
    sel?.addRange(lastEditorRange)
  }
  
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) return
  
  let node = sel.anchorNode
  let blockNode: Element | null = null
  
  while (node && node !== editor.value) {
    if (node instanceof Element && /^(DIV|P|H[1-6])$/i.test(node.nodeName)) {
      blockNode = node
      break
    }
    node = node.parentNode || null
  }
  
  if (blockNode && blockNode !== editor.value) {
    const newBlock = document.createElement(blockTag)
    const innerElements = blockNode.querySelectorAll('*')
    innerElements.forEach(el => {
      if (el instanceof HTMLElement) {
        el.style.removeProperty('font-size')
        el.style.removeProperty('font-weight')
        if (el.nodeName === 'FONT') el.removeAttribute('size')
        if (el.getAttribute('style') === '') el.removeAttribute('style')
      }
    })
    
    while (blockNode.firstChild) {
      newBlock.appendChild(blockNode.firstChild)
    }
    blockNode.parentNode?.replaceChild(newBlock, blockNode)
    
    const newRange = document.createRange()
    newRange.selectNodeContents(newBlock)
    sel.removeAllRanges()
    sel.addRange(newRange)
    if (sel.rangeCount > 0) {
      lastEditorRange = sel.getRangeAt(0).cloneRange()
    }
  } else {
    document.execCommand('formatBlock', false, blockTag)
  }
  
  editor.value?.focus()
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
  if (!range || range.collapsed) return

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
  const blockTags = /^(DIV|P|H[1-6]|BLOCKQUOTE|UL|OL|LI|TABLE)$/i
  
  while (extracted.firstChild) {
    const child = extracted.firstChild
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
  const firstChild = fragment.firstChild
  const lastChild = fragment.lastChild
  
  range.insertNode(fragment)
  
  if (firstChild && lastChild) {
    const newRange = document.createRange()
    newRange.setStartBefore(firstChild)
    newRange.setEndAfter(lastChild)
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
    savedRange = sel.getRangeAt(0).cloneRange()
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
    onInput()
    syncUnderlineColors()
    syncRubyStyles()
  } else {
    alert("ルビを解除するには、解除したいルビを選択してからボタンを押してください。")
  }
}

export function initPages() {
  const saved = localStorage.getItem('vertical-editor-pages')
  if (saved) {
    try {
      pages.value = JSON.parse(saved)
      if (pages.value.length === 0) throw new Error()
    } catch {
      pages.value = [{ id: Date.now(), content: '' }]
    }
  } else {
    const oldSaved = localStorage.getItem('vertical-editor')
    if (oldSaved) {
      pages.value = [{ id: Date.now(), content: oldSaved }]
      localStorage.removeItem('vertical-editor')
    } else {
      pages.value = [{ id: Date.now(), content: '' }]
    }
  }
}

function onInput() {
  if (editor.value) {
    const pageIdStr = editor.value.getAttribute('data-page-id')
    if (pageIdStr) {
      const pageId = Number(pageIdStr)
      const page = pages.value.find(p => p.id === pageId)
      if (page) {
        page.content = editor.value.innerHTML
      }
    }
  }
  localStorage.setItem('vertical-editor-pages', JSON.stringify(pages.value))
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
      if (file.name.endsWith('.txt')) {
        content = content.replace(/\n/g, '<br>')
      }
      pages.value = [{ id: Date.now(), content }]
      localStorage.setItem('vertical-editor-pages', JSON.stringify(pages.value))
    }
    reader.readAsText(file)
  }
  target.value = ''
}

function exportDocument(format: 'pdf' | 'html' = 'pdf') {
  if (!editor.value) return
  if (format === 'pdf') {
    window.print()
  } else if (format === 'html') {
    const content = pages.value.map(p => p.content).join('<hr>')
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
  if (e.key === 'Enter') {
    if (e.isComposing) return
    e.preventDefault()
    document.execCommand('insertText', false, '\n')
  }
}

export const useEditorState = () => {
  const pageWidthPx = computed(() => paperDimensions[paperSize.value].width * MM_TO_PX)

  return {
    editor,
    pages,
    initPages,
    fileInput,
    isBold,
    isItalic,
    isUnderline,
    rubyModalVisible,
    currentFontSize,
    showFontSizeDropdown,
    selectedColor,
    paperSize,
    margins,
    paperStyle,
    pageWidthPx,
    updateSelection,
    execCommand,
    applyHeading,
    updateFakeSelection,
    removeFakeSelection,
    commitFontSize,
    handleFontSizeEnter,
    selectDropdownFontSize,
    changeFontSize,
    applyColor,
    onInput,
    openRubyModal,
    closeRubyModal,
    applyRuby,
    removeRuby,
    triggerFileInput,
    handleFileRead,
    exportDocument,
    handleKeyDown
  }
}
