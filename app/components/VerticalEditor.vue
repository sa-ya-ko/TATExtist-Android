<template>
  <div class="flex h-[100dvh] min-h-0 flex-col overflow-hidden bg-surface dark:bg-gray-900 transition-colors duration-300">
    <!-- Header Toolbar -->
    <EditorHeader />

    <!-- Main Content -->
    <main class="flex min-h-0 flex-1 overflow-hidden pb-[calc(3.5rem+env(safe-area-inset-bottom,0px))] print:block print:overflow-visible print:pt-0 lg:pb-0">
      <!-- Sidebar Right (Page Setup) -->
      <EditorSidebar class="print:hidden" />

      <!-- Editor Canvas Area -->
      <div class="relative flex min-h-0 min-w-0 flex-1 overflow-hidden transition-all duration-300" :class="{ 'lg:mr-[320px]': true }">
        <!-- Vertical Ruler (Fixed to the right of the canvas, left of the sidebar, manual mode only) -->
        <EditorRulerVertical v-if="layoutMode === 'manual'" :scroll-container="scrollContainer" class="absolute top-0 bottom-0 right-0 z-40 print:hidden shadow-sm" />

        <!-- Editor Canvas (Vertical Scroll for manual, Horizontal for auto) -->
        <div ref="scrollContainer" 
             class="min-h-0 min-w-0 flex-1 bg-surface dark:bg-gray-900 print:overflow-visible print:block print:p-0 print:bg-white px-2 pb-[calc(5rem+env(safe-area-inset-bottom,0px))] transition-colors duration-300 w-full max-w-full md:px-6 md:pb-0"
             :class="layoutMode === 'auto' ? 'overflow-x-auto overflow-y-hidden flex items-start' : 'overflow-y-auto overflow-x-hidden md:overflow-auto flex flex-col items-center md:block'"
             :dir="layoutMode === 'auto' ? 'rtl' : 'ltr'">
          
          <!-- MANUAL MODE -->
          <div v-if="layoutMode === 'manual'" class="print:block min-h-full min-w-0 print:min-h-0 print:min-w-0 w-full h-auto md:min-h-max md:min-w-max">
            
            <!-- Horizontal Ruler (Perfectly aligned with pages in manual mode) -->
            <EditorRulerHorizontal class="print:hidden mx-auto w-full md:w-max" />
            
            <div class="py-page-margin-y print:py-0 print:pr-0 w-full min-h-full md:w-auto md:h-auto flex flex-col md:block">
              <!-- Render each page -->
              <div v-for="(page, index) in pages" :key="page.id" 
                   class="flex flex-col items-center mb-8 print:m-0 print:block relative mx-auto w-full min-h-full md:mb-12 md:w-max md:h-auto"
                 :class="{'print:break-after-page': index < pages.length - 1}">
          
          <!-- Content -->
          <div class="manuscript-paper relative transition-all duration-300 bg-white dark:bg-gray-800 dark:border-gray-600 border border-gray-300 shadow-[0_4px_20px_rgba(0,0,0,0.15)] dark:shadow-none overflow-visible print:shadow-none print:border-none print:ring-0 print:m-0 print:mx-auto print:overflow-hidden print:break-inside-avoid flex-1 w-full h-full md:w-[var(--page-width)] md:h-[var(--page-height)]"
               :class="{ 'ring-4 ring-red-500 !shadow-[0_0_15px_rgba(239,68,68,0.6)]': page.isOverflowing && !page.isWarningDismissed }"
               :style="paperStyle">
               
            <!-- Margin Guide Lines -->
            <div v-if="isDraggingRuler" class="absolute inset-0 pointer-events-none z-10 print:hidden">
              <div v-if="isDraggingRuler === 'right'" class="absolute top-0 bottom-0 border-r-2 border-red-400 border-dashed right-[var(--margin-right)]"></div>
              <div v-if="isDraggingRuler === 'left'" class="absolute top-0 bottom-0 border-l-2 border-red-400 border-dashed left-[var(--margin-left)]"></div>
              <div v-if="isDraggingRuler === 'top'" class="absolute left-0 right-0 border-t-2 border-red-400 border-dashed top-[var(--margin-top)]"></div>
              <div v-if="isDraggingRuler === 'bottom'" class="absolute left-0 right-0 border-b-2 border-red-400 border-dashed bottom-[var(--margin-bottom)]"></div>
            </div>

            <div
                 :ref="el => setEditorRef(page.id, el)"
                 class="writing-vertical outline-none text-[16px] leading-[1.8] font-['Noto_Serif_JP',_serif] dark:text-gray-100"
                 contenteditable="true"
                 dir="ltr"
                 :data-page-id="page.id"
                 @focus="handleFocus($event.target)"
                 @input="handleInput(page.id, $event)"
                 @keydown="handleKeyDown">
             </div>
             <!-- HMR trigger 37 -->
          </div>

          <!-- Page Controls -->
          <div class="w-full flex-none flex flex-col gap-3 mt-4 mb-8 print:hidden max-w-[var(--page-width)] md:mt-6 md:mb-12">
            <!-- Primary actions -->
            <div class="flex w-full justify-between items-center gap-3">
              <button @click="deletePage(page.id)" 
                      :disabled="pages.length === 1"
                      class="flex items-center gap-2 text-sm px-3 py-1.5 rounded-lg border transition-colors flex-shrink-0 md:px-4 md:py-2"
                      :class="pages.length === 1 ? 'text-gray-400 border-gray-200 bg-gray-50 dark:bg-gray-800 dark:border-gray-700 cursor-not-allowed' : 'text-red-500 border-red-200 bg-white dark:bg-gray-800 dark:border-red-900 hover:bg-red-50 dark:hover:bg-red-900/30 hover:text-red-700 shadow-sm'">
                <span class="i-heroicons-trash"></span>
                ページを削除
              </button>

              <button @click="addPage" class="flex items-center gap-2 text-sm px-4 py-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg shadow dark:shadow-none hover:bg-gray-50 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-200 transition-colors print:hidden flex-shrink-0 md:px-6 md:py-2.5 md:text-base">
                <span class="i-heroicons-plus text-xl"></span>
                新しいページを追加
              </button>
            </div>

            <!-- Overflow warning appears below both primary actions. -->
            <div v-if="page.isOverflowing && !page.isWarningDismissed" 
                 class="w-full text-red-600 font-bold flex items-center gap-2 text-sm bg-red-50 pl-4 pr-8 py-1.5 rounded-lg border border-red-200 shadow-sm relative">
              <span>⚠️</span>
              <span>このページに入力できる文字数を超えました。｢新しいページを追加｣して続きを書いてください。</span>
              <button @click="dismissWarning(page.id)" class="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-red-700 bg-red-200 hover:bg-red-300 rounded transition-colors" title="警告を閉じる">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 block" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>
          </div>
          </div>
          </div>
          
          <div v-else-if="layoutMode === 'auto' && pages[0]" 
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
    <HelpModal v-if="helpModalVisible" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch, computed } from 'vue'
import EditorHeader from './EditorHeader.vue'
import EditorSidebar from './EditorSidebar.vue'
import RubyModal from './RubyModal.vue'
import HelpModal from './HelpModal.vue'
import EditorRulerHorizontal from './EditorRulerHorizontal.vue'
import EditorRulerVertical from './EditorRulerVertical.vue'

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
  currentFontFamily
} = useEditorState()

const editorRefs = ref<Record<number, HTMLElement>>({})

const setEditorRef = (id: number, el: any) => {
  if (el) {
    editorRefs.value[id] = el
    const page = pages.value.find(p => p.id === id)
    if (page && el.innerHTML !== page.content) {
      el.innerHTML = page.content
      
      // Auto focus newly added page
      if (id === pages.value[pages.value.length - 1].id) {
        editor.value = el
      }
    }
    
    if (layoutMode.value === 'auto' && id === pages.value[0]?.id && resizeObserver) {
      resizeObserver.observe(el as HTMLElement)
      if (el.scrollWidth !== autoTextWidthPx.value) {
        autoTextWidthPx.value = el.scrollWidth
      }
    }
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
  
  const range = document.createRange()
  range.selectNodeContents(el)
  const rect = range.getBoundingClientRect()
  
  const availableWidth = pageWidthPx.value - (margins.value.left + margins.value.right) * MM_TO_PX
  
  const isNowOverflowing = rect.width > availableWidth + 2
  
  if (page.isOverflowing && !isNowOverflowing) {
    page.isWarningDismissed = false
  }
  
  page.isOverflowing = isNowOverflowing
}

watch(() => margins.value, () => {
  pages.value.forEach(page => {
    const el = editorRefs.value[page.id]
    if (el) {
      checkOverflow(page.id, el)
    }
  })
}, { deep: true })

watch(() => layoutMode.value, async (newMode) => {
  await nextTick()
  if (scrollContainer.value) {
    scrollContainer.value.scrollLeft = 0
  }
}, { immediate: true })

const dismissWarning = (id: number) => {
  const page = pages.value.find(p => p.id === id)
  if (page) {
    page.isWarningDismissed = true
  }
}

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

const addPage = () => {
  pages.value.push({ id: Date.now(), content: '' })
}

const deletePage = (id: number) => {
  if (pages.value.length <= 1) return
  
  if (confirm("このページを削除してもよろしいですか？")) {
    pages.value = pages.value.filter(p => p.id !== id)
    onInput(new Event('input') as any) // 保存処理を実行
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
    height: auto;
    min-height: 0;
    overflow: visible;
    background-color: white;
    margin: 0;
    padding: 0;
  }
}

/* Base heading styles inside editor */
.writing-vertical h1, .writing-vertical h2, .writing-vertical h3, .writing-vertical h4 {
  font-weight: bold;
}
.writing-vertical h1 { font-size: 2em; margin-bottom: 0.67em; }
.writing-vertical h2 { font-size: 1.5em; margin-bottom: 0.83em; }
.writing-vertical h3 { font-size: 1.17em; margin-bottom: 1em; }
.writing-vertical h4 { font-size: 1em; margin-bottom: 1.33em; }

/* Remove blockquote styling that might interfere */
.writing-vertical blockquote {
  margin: 0;
  padding: 0;
}

/* Ensure empty paragraphs have some height */
.writing-vertical p:empty::before {
  content: '\200B'; /* Zero-width space */
}

/* List styling */
.writing-vertical ul {
  list-style-type: disc;
  padding-inline-start: 2em; /* Indent from top */
  margin-block-start: 0.5em;
  margin-block-end: 0.5em;
}
.writing-vertical li {
  margin-inline-end: 0.2em;
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
</style>
