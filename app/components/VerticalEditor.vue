<template>
  <div class="flex flex-col h-screen bg-surface">
    <!-- Header Toolbar -->
    <EditorHeader />

    <!-- Main Content -->
    <main class="flex flex-1 pt-toolbar-height overflow-hidden">
      <!-- Sidebar Right (Page Setup) -->
      <EditorSidebar />

      <!-- Editor Canvas (Vertical Scroll) -->
      <div class="flex-1 flex flex-col items-center py-page-margin-y pr-sidebar-width pl-12 overflow-y-auto bg-surface">
        
        <!-- Render each page -->
        <div v-for="page in pages" :key="page.id" class="flex flex-col items-center mr-12 mb-12">
          
          <!-- Content -->
          <div class="manuscript-paper relative transition-all duration-300 bg-white border border-gray-300 shadow-[0_4px_20px_rgba(0,0,0,0.15)] overflow-hidden" 
               :class="{ 'ring-4 ring-red-500 !shadow-[0_0_15px_rgba(239,68,68,0.6)]': page.isOverflowing }"
               :style="paperStyle">
            <div :ref="el => setEditorRef(page.id, el)"
                 class="writing-vertical outline-none"
                 contenteditable="true"
                 dir="ltr"
                 :data-page-id="page.id"
                 @focus="handleFocus($event.target)"
                 @input="handleInput(page.id, $event)"
                 @keydown="handleKeyDown"
                 style="font-family: serif; font-size: 16px; line-height: 1.8;">
            </div>
          </div>

          <!-- Page Controls (Warning & Delete Button) -->
          <div class="w-full flex justify-between items-start mt-6" style="max-width: var(--page-width);">
            <!-- Warning Message -->
            <div :class="{ 'opacity-100 translate-y-0': page.isOverflowing, 'opacity-0 -translate-y-4 pointer-events-none': !page.isOverflowing }" 
                 class="text-red-600 font-bold flex items-center gap-2 transition-all duration-300 text-sm bg-red-50 px-4 py-3 rounded-lg border border-red-200 shadow-sm">
              <span>⚠️</span>
              <span>このページに入力できる文字数を超えました。｢新しいページを追加｣して続きを書いてください。</span>
            </div>
            
            <!-- Delete Button -->
            <button @click="deletePage(page.id)" 
                    :disabled="pages.length === 1"
                    class="flex items-center gap-2 text-sm px-4 py-2 rounded-lg border transition-colors flex-shrink-0"
                    :class="pages.length === 1 ? 'text-gray-400 border-gray-200 bg-gray-50 cursor-not-allowed' : 'text-red-500 border-red-200 bg-white hover:bg-red-50 hover:text-red-700 shadow-sm'">
              <span class="i-heroicons-trash"></span>
              ページを削除
            </button>
          </div>

        </div>

        <button @click="addPage" class="mb-12 px-6 py-3 bg-white border border-gray-300 rounded-lg shadow hover:bg-gray-50 flex items-center gap-2 text-gray-700 transition-colors">
          <span class="i-heroicons-plus text-xl"></span>
          新しいページを追加
        </button>
      </div>
    </main>

    <!-- Ruby Modal -->
    <RubyModal v-if="rubyModalVisible" @confirm="applyRuby" @cancel="closeRubyModal" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import EditorHeader from './EditorHeader.vue'
import EditorSidebar from './EditorSidebar.vue'
import RubyModal from './RubyModal.vue'

const {
  editor,
  pages,
  initPages,
  paperStyle,
  rubyModalVisible,
  pageWidthPx,
  margins,
  onInput,
  handleKeyDown,
  applyRuby,
  removeRuby,
  updateSelection
} = useEditorState()

const editorRefs = ref<Record<number, HTMLElement>>({})

const setEditorRef = (id: number, el: any) => {
  if (el) {
    editorRefs.value[id] = el
    if (!el.getAttribute('data-initialized')) {
      const page = pages.value.find(p => p.id === id)
      if (page) el.innerHTML = page.content
      el.setAttribute('data-initialized', 'true')
      
      // Auto focus newly added page
      if (id === pages.value[pages.value.length - 1].id) {
        editor.value = el
      }
    }
  }
}

const checkOverflow = (id: number, el: HTMLElement) => {
  const page = pages.value.find(p => p.id === id)
  if (!page) return
  
  const range = document.createRange()
  range.selectNodeContents(el)
  const rect = range.getBoundingClientRect()
  
  const MM_TO_PX = 3.78
  const availableWidth = pageWidthPx.value - (margins.value.left + margins.value.right) * MM_TO_PX
  
  page.isOverflowing = rect.width > availableWidth + 2
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
  height: var(--page-height);
  width: var(--page-width);
  background-image: none !important;
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
    margin: 2cm; /* Set print margins natively so they apply to all printed pages */
  }

  /* 印刷時のみ発動する強力な非表示設定 */
  .print-hidden, body .print-hidden {
    display: none !important;
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
  
  /* エディタ親要素のパディング（サイドバーやツールバー分の余白）をリセット */
  body main {
    padding-top: 0 !important;
  }
  body .pr-sidebar-width {
    padding-right: 0 !important;
  }
  body .py-page-margin-y {
    padding-top: 0 !important;
    padding-bottom: 0 !important;
  }
  
  /* 影や枠線を消し、紙に綺麗に収める */
  body .manuscript-paper {
    background-image: none !important;
    padding: 0 !important;
    height: auto !important;
    min-height: 0 !important;
    width: auto !important;
    min-width: 0 !important;
    box-shadow: none !important;
    border: none !important;
  }
  
  /* 段組みを解除して自然な改ページを促す */
  body .writing-vertical {
    height: auto !important;
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

/* Fix ruby alignment */
ruby {
  ruby-align: center;
}
rt {
  font-size: 0.5em;
}
</style>
