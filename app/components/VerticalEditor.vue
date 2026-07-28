<template>
  <div class="flex h-[100dvh] min-h-0 flex-col overflow-hidden bg-surface dark:bg-gray-900 transition-colors duration-300">
    <!-- Header Toolbar -->
    <EditorHeader />

    <!-- Main Content -->
    <main class="flex min-h-0 flex-1 overflow-hidden pb-[calc(3.5rem+env(safe-area-inset-bottom,0px))] print:block print:overflow-visible print:pt-0 lg:pb-0">
      <!-- Sidebar Right (Page Setup) -->
      <EditorSidebar class="print:hidden" />

      <div class="relative flex min-h-0 min-w-0 flex-1 overflow-hidden transition-all duration-300" :class="{ 'lg:mr-[320px]': true }">

        <!-- Editor Canvas (Vertical Scroll for manual, Horizontal for auto) -->
        <div ref="scrollContainer" 
             class="min-h-0 min-w-0 flex-1 bg-surface dark:bg-gray-900 print:overflow-visible print:block print:p-0 print:bg-white px-2 pb-[calc(5rem+env(safe-area-inset-bottom,0px))] transition-colors duration-300 w-full max-w-full md:px-6 md:pb-0"
             :class="[layoutMode === 'auto' ? 'overflow-x-auto overflow-y-hidden flex items-start' : 'overflow-y-auto overflow-x-hidden block', { 'ruby-management-mode': isRubyManagementMode }]"
             :dir="layoutMode === 'auto' ? 'rtl' : 'ltr'">
          
          <!-- MANUAL MODE -->
          <div v-if="layoutMode === 'manual'" class="print:block min-h-full min-w-0 print:min-h-0 print:min-w-0 w-full h-auto md:min-h-max md:min-w-max">
            
            <!-- Horizontal Ruler (Perfectly aligned with pages in manual mode) -->
            <EditorRulerHorizontal class="print:hidden mx-auto w-full md:w-max" />
            
            <div class="py-page-margin-y print:py-0 print:pr-0 w-full md:w-auto md:h-auto flex flex-col md:block">
              <!-- Render each page -->
              <div v-for="(page, index) in pages" :key="page.id" 
                   class="flex flex-col items-center mb-8 print:m-0 print:block relative mx-auto w-full md:mb-12 md:w-max h-auto"
                 :class="{'print:break-after-page': index < pages.length - 1}">
          
          <!-- Content -->
          <div class="manuscript-paper relative transition-all duration-300 bg-white dark:bg-gray-800 dark:border-gray-600 border border-gray-300 shadow-[0_4px_20px_rgba(0,0,0,0.15)] dark:shadow-none overflow-visible print:shadow-none print:border-none print:ring-0 print:m-0 print:mx-auto print:overflow-hidden print:break-inside-avoid flex-none w-[var(--page-width)] h-[var(--page-height)] max-w-full"
               :style="paperStyle">
               
            <!-- Margin Guide Lines -->
            <div v-if="isDraggingRuler" class="absolute inset-0 pointer-events-none z-10 print:hidden">
              <div v-if="isDraggingRuler === 'right'" class="absolute top-0 bottom-0 border-r-2 border-red-400 border-dashed right-[var(--margin-right)]"></div>
              <div v-if="isDraggingRuler === 'left'" class="absolute top-0 bottom-0 border-l-2 border-red-400 border-dashed left-[var(--margin-left)]"></div>
              <div v-if="isDraggingRuler === 'top'" class="absolute left-0 right-0 border-t-2 border-red-400 border-dashed top-[var(--margin-top)]"></div>
              <div v-if="isDraggingRuler === 'bottom'" class="absolute left-0 right-0 border-b-2 border-red-400 border-dashed bottom-[var(--margin-bottom)]"></div>
            </div>

            <!-- Overflow Boundary Line -->
            <div v-if="page.isOverflowing && !page.isWarningDismissed" class="absolute inset-0 pointer-events-none z-10 print:hidden">
              <div class="absolute top-0 bottom-0 border-l-2 border-red-500 border-dashed left-[var(--margin-left)]"></div>
            </div>

            <div
                 :ref="el => setEditorRef(page.id, el)"
                 class="writing-vertical outline-none text-[16px] leading-[1.8] font-['Noto_Serif_JP',_serif] dark:text-gray-100"
                 contenteditable="true"
                 dir="ltr"
                 :data-page-id="page.id"
                 @click="handleEditorClick"
                 @focus="handleFocus($event.target)"
                 @input="handleInput(page.id, $event)"
                 @keydown="handleKeyDown">
             </div>
             <!-- HMR trigger 37 -->
          </div>

          <!-- Page Controls -->
          <div class="w-full flex-none flex flex-col gap-3 mt-4 mb-8 print:hidden max-w-[var(--page-width)] md:mt-6 md:mb-12" dir="ltr">
            <!-- Primary actions -->
            <div class="flex w-full justify-between items-center gap-3">
              <button @click="addPage(page.id)" class="flex items-center gap-2 text-sm px-4 py-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg shadow dark:shadow-none hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-200 transition-colors print:hidden flex-shrink-0 md:px-6 md:py-2.5 md:text-base">
                <span class="i-heroicons-plus text-xl"></span>
                新しいページを追加
              </button>

              <button @click="deletePage(page.id)" 
                      :disabled="pages.length === 1"
                      class="flex items-center gap-2 text-sm px-3 py-1.5 rounded-lg border transition-colors flex-shrink-0 md:px-4 md:py-2"
                      :class="pages.length === 1 ? 'text-gray-400 border-gray-200 bg-gray-50 dark:bg-gray-800 dark:border-gray-700 cursor-not-allowed' : 'text-red-500 border-red-200 bg-white dark:bg-gray-800 dark:border-red-900 hover:bg-red-50 dark:hover:bg-red-900/30 hover:text-red-700 shadow-sm'">
                <span class="i-heroicons-trash"></span>
                ページを削除
              </button>
            </div>

            <!-- Overflow warning appears below both primary actions. -->
            <div v-if="page.isOverflowing" 
                 class="w-fit text-left text-red-600 dark:text-red-400 font-bold flex items-start gap-2 text-sm bg-red-50 dark:bg-red-900/30 p-3 rounded-lg border border-red-200 dark:border-red-800 shadow-sm relative">
              <span class="flex-shrink-0 mt-0.5">⚠️</span>
              <span class="flex-1 pr-2 leading-relaxed">このページに入力できる文字数を超えました。｢新しいページを追加｣して続きを書いてください。</span>
            </div>
          </div>
          </div>
          </div>
          </div>
          
          <div v-else-if="layoutMode === 'auto' && pages[0]" 
               :key="'auto-' + pages[0].id"
               class="auto-pagination-strip relative ml-auto mr-0 md:mx-auto w-max min-w-full md:min-w-[var(--strip-min-width)] h-full md:h-[var(--strip-height)] md:mt-[var(--strip-margin-top)] md:mb-[var(--strip-margin-bottom)]"
               :class="scrollStyle === 'paged' ? 'shadow-[0_4px_20px_rgba(0,0,0,0.15)]' : 'shadow-none'"
               :style="autoStripStyle"
               dir="rtl">
            
            <!-- BACKGROUND PAGES LAYER -->
            <div class="absolute right-0 top-0 bottom-0 pointer-events-none transition-colors duration-300"
                 :class="[scrollStyle === 'paged' ? 'bg-paged dark:!bg-gray-800' : 'bg-continuous dark:!bg-gray-800']"
                 :style="{ width: `${autoStripStyle.minWidth}` }"></div>

            <!-- Page separators stay above the editable text so page boundaries remain visible. -->
            <div v-if="scrollStyle === 'paged'" class="absolute right-0 top-0 bottom-0 z-30 pointer-events-none page-separators"
                 :style="{ width: `${autoStripStyle.minWidth}` }"></div>
            
            <!-- Margin Guide Lines for Auto Mode -->
            <div v-if="isDraggingRuler" class="absolute right-0 top-0 bottom-0 pointer-events-none z-10 print:hidden overflow-hidden" :style="{ width: `${autoStripStyle.minWidth}` }">
              <div v-if="isDraggingRuler === 'top'" class="absolute left-0 right-0 border-t-2 border-red-400 border-dashed" :style="{ top: `var(--margin-top)` }"></div>
              <div v-if="isDraggingRuler === 'bottom'" class="absolute left-0 right-0 border-b-2 border-red-400 border-dashed" :style="{ bottom: `var(--margin-bottom)` }"></div>
            </div>

            <!-- TEXT EDITOR LAYER -->
            <div
                 :ref="el => setEditorRef(pages[0].id, el)"
                 class="writing-vertical outline-none relative z-20 dark:text-gray-100"
                 contenteditable="true"
                 dir="ltr"
                 :data-page-id="pages[0].id"
                 @click="handleEditorClick"
                 @focus="handleFocus($event.target)"
                 @input="handleInput(pages[0].id, $event)"
                 @keydown="handleKeyDown"
                 :style="autoEditorStyle">
            </div>
          </div>

        </div>

      </div>
    </main>

    <!-- Ruby Modal -->
    <RubyModal v-if="rubyModalVisible" @confirm="applyRuby" @cancel="closeRubyModal" />
    <!-- Modals -->
    <WordCountModal v-if="wordCountModalVisible" />
    <GoogleDriveModal v-if="isDriveModalOpen" />
    <HelpModal v-if="helpModalVisible" />

    <!-- Live Word Count Widget -->
    <div v-if="showWordCountWhileTyping" 
         class="fixed bottom-4 left-4 bg-surface px-4 py-2 rounded-full shadow-md border border-outline-variant z-40 flex items-center gap-2 text-ui-label-md text-on-surface print:hidden cursor-pointer hover:bg-surface-container-low transition-colors"
         @click="openWordCountModal"
         title="クリックして詳細を表示">
      <span class="material-symbols-outlined text-[22px] font-bold">123</span>
      <span>{{ characterCount }} 文字</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch, computed } from 'vue'
import EditorHeader from './EditorHeader.vue'
import EditorSidebar from './EditorSidebar.vue'
import WordCountModal from './WordCountModal.vue'
import GoogleDriveModal from './GoogleDriveModal.vue'
import RubyModal from './RubyModal.vue'
import HelpModal from './HelpModal.vue'
import { useEditorState } from '../composables/useEditorState'
import { useGoogleDrive } from '../composables/useGoogleDrive'
import EditorRulerHorizontal from './EditorRulerHorizontal.vue'

const scrollContainer = ref<HTMLElement | null>(null)
const autoTextWidthPx = ref(0)
let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  if (typeof window !== 'undefined') {
    resizeObserver = new ResizeObserver((entries) => {
      for (let entry of entries) {
        if (entry.target.scrollWidth !== autoTextWidthPx.value) {
          autoTextWidthPx.value = entry.target.scrollWidth
        }
      }
    })

  }
})

onUnmounted(() => {
  if (resizeObserver) resizeObserver.disconnect()
})

const {
  editor,
  pages,
  initPages,
  paperStyle,
  rubyModalVisible,
  helpModalVisible,
  pageWidthPx,
  margins,
  isDraggingRuler,
  layoutMode,
  scrollStyle,
  paperDimensions,
  paperSize,
  isSidebarOpen,
  MM_TO_PX,
  onInput,
  handleKeyDown,
  applyRuby,
  removeRuby,
  closeRubyModal,
  updateSelection,
  currentFontFamily,
  wordCountModalVisible,
  showWordCountWhileTyping,
  characterCount,
  documentRevision,
  isRubyManagementMode,
  removeSpecificRuby,
  deleteManualPage,
  addManualPage
} = useEditorState()

const { isDriveModalOpen } = useGoogleDrive()

const editorRefs = ref<Record<number, HTMLElement>>({})

watch(() => documentRevision?.value, () => {
  pages.value.forEach(page => {
    const el = editorRefs.value[page.id]
    if (el && typeof document !== 'undefined' && document.body.contains(el)) {
      if (el.innerHTML !== page.content) {
        el.innerHTML = page.content
      }
    }
  })
}, { flush: 'post' })

const setEditorRef = (id: number, el: any) => {
  if (el) {
    editorRefs.value[id] = el
    const page = pages.value.find(p => p.id === id)
    // Only update innerHTML on initial mount of the editor element
    if (page && el.innerHTML === '' && page.content !== '') {
      el.innerHTML = page.content
      
      // Auto focus newly added page
      if (id === pages.value[pages.value.length - 1].id) {
        editor.value = el
      }
    } else if (page && id === pages.value[pages.value.length - 1].id && el.innerHTML === '') {
      // Auto focus for empty new page
      editor.value = el
    }
    
    if (layoutMode.value === 'auto' && id === pages.value[0]?.id && resizeObserver) {
      resizeObserver.observe(el as HTMLElement)
      if (el.scrollWidth !== autoTextWidthPx.value) {
        autoTextWidthPx.value = el.scrollWidth
      }
    }
  } else {
    delete editorRefs.value[id]
  }
}

// Styles for Auto Mode Strip (Container)
const autoStripStyle = computed(() => {
  const dim = paperDimensions[paperSize.value]
  const fullWidth = dim.width * MM_TO_PX
  const fullHeight = dim.height * MM_TO_PX
  
  const contentWidth = fullWidth - (margins.value.right * MM_TO_PX) - (margins.value.left * MM_TO_PX)
  const textW = autoTextWidthPx.value || contentWidth
  const pageCount = Math.max(1, Math.ceil(textW / contentWidth))
  const totalWidth = pageCount * fullWidth
  
  return {
    writingMode: 'horizontal-tb',
    position: 'relative',
    display: 'block',
    '--strip-height': `${fullHeight}px`,
    '--strip-margin-top': 'calc(24px + var(--page-margin-y))',
    '--strip-margin-bottom': 'var(--page-margin-y)',
    '--strip-min-width': `${totalWidth}px`,
    minWidth: `${totalWidth}px`,
    width: 'max-content',
    
    '--paper-full-width': `${fullWidth}px`,
  }
})

// Styles for the editable content inside Auto Mode Strip (Text Layer)
const autoEditorStyle = computed(() => {
  const dim = paperDimensions[paperSize.value]
  const fullHeight = dim.height * MM_TO_PX
  
  return {
    fontFamily: 'serif',
    fontSize: '16px',
    lineHeight: '1.8',
    height: '100%',
    width: 'max-content',
    direction: 'ltr',
    position: 'relative',
    paddingTop: `${margins.value.top * MM_TO_PX}px`,
    paddingRight: `${margins.value.right * MM_TO_PX}px`,
    paddingBottom: `${margins.value.bottom * MM_TO_PX}px`,
    paddingLeft: `${margins.value.left * MM_TO_PX}px`,
  }
})

const checkOverflow = (id: number, el: HTMLElement) => {
  const page = pages.value.find(p => p.id === id)
  if (!page) return
  
  // 基準となる絶対に大きさが変わらない「原稿用紙本体」を取得
  const paperEl = el.closest('.manuscript-paper') as HTMLElement
  if (!paperEl) return

  const paperRect = paperEl.getBoundingClientRect()
  
  if (el.innerText.trim() === '') {
    page.isOverflowing = false
    return
  }

  // ルビ(<rt>)を除外した、本文の最も左端の座標を探す
  let minLeft = Infinity;
  const range = document.createRange();

  // エディタ内のすべてのテキストノードを順番にチェックする
  const treeWalker = document.createTreeWalker(
    el,
    NodeFilter.SHOW_TEXT,
    {
      acceptNode: (node) => {
        // 親をたどって <rt>（ルビ）の中にある文字なら除外(無視)する
        let parent = node.parentElement;
        while (parent && parent !== el) {
          if (parent.tagName.toLowerCase() === 'rt') {
            return NodeFilter.FILTER_REJECT;
          }
          parent = parent.parentElement;
        }
        // 空白のみの文字も無視する
        if (!node.textContent || node.textContent.trim() === '') {
          return NodeFilter.FILTER_SKIP;
        }
        return NodeFilter.FILTER_ACCEPT;
      }
    }
  );

  let currentNode = treeWalker.nextNode();
  while (currentNode) {
    range.selectNodeContents(currentNode);
    const rects = range.getClientRects();
    for (let i = 0; i < rects.length; i++) {
      if (rects[i].width > 0 && rects[i].height > 0) {
        if (rects[i].left < minLeft) {
          minLeft = rects[i].left;
        }
      }
    }
    currentNode = treeWalker.nextNode();
  }
  
  // もしテキストが見つからなかった場合（改行のみなど）は、従来の方法（ルビ含む）で妥協する
  if (minLeft === Infinity) {
    range.selectNodeContents(el);
    const contentRect = range.getBoundingClientRect();
    if (contentRect.width === 0) {
      page.isOverflowing = false
      return
    }
    minLeft = contentRect.left;
  }

  // 左の限界線（赤い点線）の絶対座標 = 原稿用紙の左端 + 左の余白
  const limitLeftX = paperRect.left + (margins.value.left * MM_TO_PX)

  // 縦書き（右から左）なので、文字の一番左端が限界線より左(小さい)にいけばはみ出し
  // 行間などの透明な余白による誤差を吸収するための猶予(10px)を持たせる
  const tolerance = 10
  page.isOverflowing = minLeft < (limitLeftX - tolerance)
}

watch(() => margins.value, () => {
  setTimeout(() => {
    pages.value.forEach(page => {
      const el = editorRefs.value[page.id]
      if (el) {
        checkOverflow(page.id, el)
      }
    })
  }, 0)
}, { deep: true })

watch(() => layoutMode.value, async (newMode) => {
  await nextTick()
  if (scrollContainer.value) {
    scrollContainer.value.scrollLeft = 0
  }
}, { immediate: true })


const handleInput = (id: number, e: Event) => {
  const page = pages.value.find(p => p.id === id)
  if (page) {
    page.content = (e.target as HTMLElement).innerHTML
  }
  onInput(e as any)
  checkOverflow(id, e.target as HTMLElement)
}

const handleFocus = (target: any) => {
  editor.value = target
}

const addPage = (id: number) => {
  addManualPage(id)
}

const deletePage = (id: number) => {
  if (pages.value.length <= 1) return
  
  if (confirm("このページを削除してもよろしいですか？")) {
    deleteManualPage(id)
  }
}

const handleEditorClick = (e: MouseEvent) => {
  if (isRubyManagementMode.value) {
    const target = e.target as HTMLElement
    const ruby = target.closest('ruby')
    if (ruby) {
      e.preventDefault()
      const id = ruby.getAttribute('data-ruby-id')
      if (id) {
        // Ensure we extract the base text correctly for the prompt
        const clone = ruby.cloneNode(true) as HTMLElement
        const rt = clone.querySelector('rt')
        if (rt) rt.remove()
        if (confirm(`「${clone.textContent || ''}」のルビを削除しますか？`)) {
          removeSpecificRuby(id)
        }
      }
    }
  }
}

onMounted(() => {
  initPages()
  document.addEventListener('selectionchange', updateSelection)
})

onUnmounted(() => {
  document.removeEventListener('selectionchange', updateSelection)
})
</script>

<style>
.manuscript-paper {
  /* Dimensions are now handled via Tailwind classes in the template for responsiveness */
}

.writing-vertical {
  writing-mode: vertical-rl;
  -webkit-writing-mode: vertical-rl;
  -ms-writing-mode: tb-rl;
  
  padding-top: var(--margin-top);
  padding-bottom: var(--margin-bottom);
  padding-left: var(--margin-left);
  padding-right: var(--margin-right);
  
  height: 100%;
  width: 100%;
}

@media print {
  @page {
    margin: 0; /* Remove browser margins so our manuscript-paper fills the page exactly */
  }

  /* スクロールバーや画面固定を解除し、全ページ印刷できるようにする */
  html, body, body #__nuxt, body .h-screen, body .min-h-screen, body .flex-1, body .overflow-y-auto {
    height: 100%;
    min-height: 0;
    overflow: visible;
    background-color: white;
    margin: 0;
    padding: 0;
  }

  /* ページサイズのはみ出しバグを防ぎ、用紙にピッタリ収める */
  .manuscript-paper {
    width: 100vw !important;
    height: 100vh !important;
    max-width: none !important;
    max-height: none !important;
    margin: 0 !important;
    padding: 0 !important;
  }

  /* 印刷時はベースを黒にする（ただしカスタムカラー指定があればそちらが優先される） */
  .writing-vertical {
    color: #000;
  }

  /* 印刷時に色が間引かれてかすれるのを防ぐ（ユーザーが指定したカスタム色は維持する） */
  .writing-vertical,
  .writing-vertical * {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  /* 傍線やルビの色を、その文字自体に指定された色（カスタムカラー）に合わせる */
  .writing-vertical u,
  .writing-vertical [style*="underline"] {
    text-decoration-color: currentColor;
  }
  .writing-vertical ruby > rt,
  .writing-vertical ruby > rt span,
  .writing-vertical ruby > rt font {
    color: currentColor;
  }
}

/* Base heading styles inside editor */
.writing-vertical h1, .writing-vertical h2, .writing-vertical h3, .writing-vertical h4 {
  font-weight: bold;
  break-inside: avoid;
}
.writing-vertical h1 { font-size: 2em; margin-bottom: 0; margin-block-end: 0.67em; }
.writing-vertical h2 { font-size: 1.5em; margin-bottom: 0; margin-block-end: 0.83em; }
.writing-vertical h3 { font-size: 1.17em; margin-bottom: 0; margin-block-end: 1em; }
.writing-vertical h4 { font-size: 1em; margin-bottom: 0; margin-block-end: 1.33em; }

/* Remove blockquote styling that might interfere */
.writing-vertical blockquote {
  margin: 0;
  padding: 0;
}

/* Ensure empty paragraphs have some height */
.writing-vertical p:empty::before {
  content: '\200B'; /* Zero-width space */
}

/* List styling (印刷時・表示時の両方でインデントと中点が確実に出るようにする) */
.writing-vertical ul {
  list-style-type: none;
  padding: 0;
  margin-block-start: 0.5em;
  margin-block-end: 0.5em;
}
.writing-vertical li {
  padding-top: 1.2em; /* 2行目以降のインデント */
  text-indent: -1.2em; /* 1行目を上に引き上げ（中点配置スペース） */
  margin-inline-end: 0.2em;
}
.writing-vertical li::before {
  content: '・';
}

/* Fix ruby alignment */
ruby {
  ruby-align: center;
}
rt {
  font-size: 0.5em;
}

/* CSS for the Auto Pagination Backgrounds */
.bg-paged {
  background-color: white;
  background-image: repeating-linear-gradient(
    to left,
    transparent 0,
    transparent calc(var(--paper-full-width) - 1px),
    #e2e8f0 calc(var(--paper-full-width) - 1px), /* 1px very light slate-200 border */
    #e2e8f0 var(--paper-full-width)
  );
  background-position: right top;
}

/* Reserve a slim visual gutter at each page boundary.  It is layered above
   the editable text so a separator remains readable even near a page edge. */
.page-separators {
  background-image: repeating-linear-gradient(
    to left,
    transparent 0,
    transparent calc(var(--paper-full-width) - 6px),
    rgba(255, 255, 255, 0.96) calc(var(--paper-full-width) - 6px),
    rgba(255, 255, 255, 0.96) calc(var(--paper-full-width) - 1px),
    #cbd5e1 calc(var(--paper-full-width) - 1px),
    #cbd5e1 var(--paper-full-width)
  );
  background-position: right top;
}

.dark .bg-paged {
  background-color: transparent;
  background-image: repeating-linear-gradient(
    to left,
    transparent 0,
    transparent calc(var(--paper-full-width) - 1px),
    #4b5563 calc(var(--paper-full-width) - 1px), /* gray-600 */
    #4b5563 var(--paper-full-width)
  );
}

.dark .page-separators {
  background-image: repeating-linear-gradient(
    to left,
    transparent 0,
    transparent calc(var(--paper-full-width) - 6px),
    rgba(31, 41, 55, 0.96) calc(var(--paper-full-width) - 6px),
    rgba(31, 41, 55, 0.96) calc(var(--paper-full-width) - 1px),
    #4b5563 calc(var(--paper-full-width) - 1px),
    #4b5563 var(--paper-full-width)
  );
  background-position: right top;
}

.bg-continuous {
  background-color: white;
}

/* Ruby Management Mode Styles */
.ruby-management-mode ruby {
  background-color: rgba(253, 224, 71, 0.7); /* 目に優しい落ち着いた黄色 (yellow-300) */
  cursor: pointer;
  border-radius: 3px;
  transition: background-color 0.2s;
}
.ruby-management-mode ruby:hover {
  background-color: rgba(250, 204, 21, 0.9); /* ホバー時は少し濃くはっきりと (yellow-400) */
}
.dark .ruby-management-mode ruby {
  background-color: rgba(180, 83, 9, 0.6); /* ダークモード用: 暗めの琥珀色 (amber-700) */
}
.dark .ruby-management-mode ruby:hover {
  background-color: rgba(180, 83, 9, 0.9);
}
</style>
