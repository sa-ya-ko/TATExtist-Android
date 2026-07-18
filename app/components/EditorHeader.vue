<template>
  <header class="print:hidden fixed top-0 left-0 w-full h-toolbar-height flex flex-col z-50 bg-surface border-b border-outline-variant">
      <div class="flex items-center justify-between px-4 h-full">
          <div class="flex items-center gap-md">
              <span class="material-symbols-outlined text-primary" data-icon="description">description</span>
              <div class="flex flex-col">
                  <input v-model="documentTitle" class="bg-transparent border-none p-0 focus:ring-0 font-ui-label-md text-on-surface-variant w-40" type="text" placeholder="無題のドキュメント">
              </div>
              <div class="h-6 w-[1px] bg-outline-variant mx-2"></div>


              <!-- File Dropdown -->
              <div class="relative flex items-center" ref="fileMenuContainer">
                  <button @click="showFileMenu = !showFileMenu" class="px-2 py-1 flex items-center gap-1 rounded hover:bg-surface-container-low text-ui-label-md transition-colors whitespace-nowrap" title="ファイル">
                      ファイル
                  </button>
                  <ul v-if="showFileMenu" class="absolute top-full left-0 mt-1 w-48 bg-white border border-gray-200 rounded shadow-lg z-50 list-none p-0 m-0 py-1">
                      <li @mousedown.prevent="triggerFileInput(); showFileMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low cursor-pointer text-on-surface m-0 flex items-center gap-2">
                          <span class="material-symbols-outlined text-[18px]">folder_open</span> パソコンから開く
                      </li>
                      <li @mousedown.prevent="saveDocumentLocal(); showFileMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low cursor-pointer text-on-surface m-0 flex items-center gap-2">
                          <span class="material-symbols-outlined text-[18px]">save</span> パソコンに保存
                      </li>

                      <div class="h-px bg-gray-200 my-1"></div>
                      <li @mousedown.prevent="exportDocument('print'); showFileMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low cursor-pointer text-on-surface m-0 flex items-center gap-2">
                          <span class="material-symbols-outlined text-[18px]">print</span> 印刷する
                      </li>
                  </ul>
              </div>

              <!-- Tools Dropdown -->
              <div class="relative flex items-center" ref="toolsMenuContainer">
                  <button @click="showToolsMenu = !showToolsMenu" class="px-2 py-1 flex items-center gap-1 rounded hover:bg-surface-container-low text-ui-label-md transition-colors whitespace-nowrap" title="ツール">
                      ツール
                  </button>
                  <ul v-if="showToolsMenu" class="absolute top-full left-0 mt-1 w-40 bg-white border border-gray-200 rounded shadow-lg z-50 list-none p-0 m-0 py-1">
                      <li @mousedown.prevent="openWordCountModal(); showToolsMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low cursor-pointer text-on-surface m-0">
                          文字カウント
                      </li>
                  </ul>
              </div>

              <div class="h-6 w-[1px] bg-outline-variant mx-2"></div>

              <!-- Format Controls -->
              <nav class="flex items-center gap-sm">
                  <button @mousedown.prevent="execCommand('undo')" class="p-2 hover:bg-surface-container-low transition-colors rounded" title="元に戻す (Undo)">
                      <span class="material-symbols-outlined" data-icon="undo">undo</span>
                  </button>
                  <button @mousedown.prevent="execCommand('redo')" class="p-2 hover:bg-surface-container-low transition-colors rounded" title="やり直す (Redo)">
                      <span class="material-symbols-outlined" data-icon="redo">redo</span>
                  </button>
                  <div class="h-6 w-[1px] bg-outline-variant mx-1"></div>
                  <!-- Font Name -->
                  <select v-model="currentFontFamily" @change="execCommand('fontName', ($event.target as HTMLSelectElement).value)" @focus="updateFakeSelection" @blur="removeFakeSelection" class="bg-transparent border-none text-ui-label-md font-ui-label-md focus:ring-0 pr-8">
                    <option v-for="font in fontOptions" :key="font.value" :value="font.value" :style="{ fontFamily: font.value }">
                        {{ font.label }}
                    </option>
                  </select>
                  <div class="h-6 w-[1px] bg-outline-variant mx-1"></div>
                  <!-- Heading -->
                  <select v-model="currentHeading" @change="applyHeading(($event.target as HTMLSelectElement).value)" @focus="updateFakeSelection" @blur="removeFakeSelection" class="bg-transparent border-none text-ui-label-md font-ui-label-md focus:ring-0 pr-8">
                      <option value="P">標準テキスト</option>
                      <option value="H1">タイトル</option>
                      <option value="H2">見出し 1</option>
                      <option value="H3">見出し 2</option>
                      <option value="H4">見出し 3</option>
                  </select>
                  <div class="h-6 w-[1px] bg-outline-variant mx-1"></div>
                  <!-- Font Size Control -->
                  <div class="flex items-center">
                      <button @mousedown.prevent="changeFontSize(-1)" class="p-1 hover:bg-surface-container-low rounded transition-colors flex items-center justify-center" title="フォントサイズを縮小">
                          <span class="material-symbols-outlined pointer-events-none" style="font-size: 18px;">remove</span>
                      </button>
                      
                      <div class="relative flex items-center mx-1">
                          <input 
                              type="text" 
                              v-model="currentFontSize" 
                              @change="commitFontSize" 
                              @focus="() => { updateFakeSelection(); showFontSizeDropdown = true }"
                              @blur="() => { removeFakeSelection(); setTimeout(() => showFontSizeDropdown = false, 150) }"
                              @keydown.enter.prevent="handleFontSizeEnter"
                              class="w-10 h-7 text-center text-ui-label-md border border-outline-variant rounded hover:border-outline focus:border-primary focus:ring-1 focus:ring-primary p-0 bg-white outline-none transition-colors" 
                          />
                          <ul v-if="showFontSizeDropdown" class="absolute top-full left-1/2 -translate-x-1/2 mt-1 w-16 bg-white border border-gray-200 rounded shadow-lg z-50 max-h-48 overflow-y-auto list-none p-0 m-0">
                              <li v-for="size in [8, 10, 11, 12, 14, 18, 24, 36]" :key="size" @mousedown.prevent="selectDropdownFontSize(size)" class="px-2 py-1.5 text-center text-sm hover:bg-surface-container-low cursor-pointer text-on-surface m-0">{{ size }}</li>
                          </ul>
                      </div>

                      <button @mousedown.prevent="changeFontSize(1)" class="p-1 hover:bg-surface-container-low rounded transition-colors flex items-center justify-center" title="フォントサイズを拡大">
                          <span class="material-symbols-outlined pointer-events-none" style="font-size: 18px;">add</span>
                      </button>
                  </div>
                  <div class="h-6 w-[1px] bg-outline-variant mx-1"></div>
                  <button @mousedown.prevent="execCommand('bold')" :class="['p-2 transition-colors rounded', isBold ? 'bg-primary/10 text-primary' : 'hover:bg-surface-container-low']" title="太字">
                      <span class="material-symbols-outlined" data-icon="format_bold">format_bold</span>
                  </button>
                  <button @mousedown.prevent="execCommand('italic')" :class="['p-2 transition-colors rounded', isItalic ? 'bg-primary/10 text-primary' : 'hover:bg-surface-container-low']" title="斜体">
                      <span class="material-symbols-outlined" data-icon="format_italic">format_italic</span>
                  </button>
                  <button @mousedown.prevent="execCommand('underline')" :class="['p-2 transition-colors rounded', isUnderline ? 'bg-primary/10 text-primary' : 'hover:bg-surface-container-low']" title="傍線（下線）">
                      <span class="material-symbols-outlined" data-icon="format_underlined">format_underlined</span>
                  </button>
                  <div class="w-px h-6 bg-outline-variant mx-1"></div>
                  <button @mousedown.prevent="execCommand('insertUnorderedList')" :class="['p-2 transition-colors rounded', isList ? 'bg-primary/10 text-primary' : 'hover:bg-surface-container-low']" title="箇条書き">
                      <span class="material-symbols-outlined" data-icon="format_list_bulleted">format_list_bulleted</span>
                  </button>
                  <button @mousedown.prevent="customOutdent" class="p-2 transition-colors rounded hover:bg-surface-container-low" title="インデントを減らす（字上げ）">
                      <span class="material-symbols-outlined" data-icon="format_indent_decrease">format_indent_decrease</span>
                  </button>
                  <button @mousedown.prevent="customIndent" class="p-2 transition-colors rounded hover:bg-surface-container-low" title="インデントを増やす（字下げ）">
                      <span class="material-symbols-outlined" data-icon="format_indent_increase">format_indent_increase</span>
                  </button>
                  <div class="relative flex items-center p-2 hover:bg-surface-container-low transition-colors rounded cursor-pointer" title="文字色">
                      <span class="material-symbols-outlined" :style="{ color: selectedColor }" data-icon="format_color_text">format_color_text</span>
                      <input type="color" v-model="selectedColor" @input="applyColor" class="absolute inset-0 opacity-0 cursor-pointer w-full h-full" />
                  </div>
                  <div class="h-6 w-[1px] bg-outline-variant mx-1"></div>
                  <button @mousedown.prevent="openRubyModal" class="px-2 py-1 border rounded hover:bg-surface-container-low text-ui-label-md transition-colors whitespace-nowrap" title="ルビ付与">ルビ付与</button>
                  <button @mousedown.prevent="removeRuby" class="px-2 py-1 border rounded hover:bg-surface-container-low text-ui-label-md text-red-500 transition-colors whitespace-nowrap" title="ルビ解除">ルビ解除</button>
              </nav>
          </div>
          <div class="flex items-center gap-md">
              <button @click="saveDocumentLocal" class="px-3 py-1.5 border border-primary text-primary hover:bg-primary/5 rounded font-ui-button text-ui-button flex items-center gap-1 transition-colors" title="パソコンに保存">
                  <span class="material-symbols-outlined text-[18px]" data-icon="save">save</span>
                  保存
              </button>
              <button @click="triggerFileInput" class="px-3 py-1.5 border border-primary text-primary hover:bg-primary/5 rounded font-ui-button text-ui-button flex items-center gap-1 transition-colors" title="ファイルを開く">
                  <span class="material-symbols-outlined text-[18px]" data-icon="folder_open">folder_open</span>
                  開く
              </button>
              <input type="file" ref="fileInput" @change="handleFileRead" accept=".tategaki,.txt" class="hidden" />
              <button @click="exportDocument('pdf')" class="bg-primary text-on-primary px-4 py-1.5 rounded-full font-ui-button text-ui-button flex items-center gap-2 hover:opacity-90 transition-opacity" title="PDF書き出し">
                  <span class="material-symbols-outlined" data-icon="picture_as_pdf" style="font-size: 18px;">picture_as_pdf</span>
                  PDF書き出し
              </button>
          </div>
      </div>
  </header>
</template>

<script setup lang="ts">

import { ref, onMounted, onUnmounted } from 'vue'

const showToolsMenu = ref(false)
const showFileMenu = ref(false)

const fileMenuContainer = ref<HTMLElement | null>(null)
const toolsMenuContainer = ref<HTMLElement | null>(null)

const closeMenus = (e: MouseEvent) => {
  if (showFileMenu.value && fileMenuContainer.value && !fileMenuContainer.value.contains(e.target as Node)) {
    showFileMenu.value = false
  }
  if (showToolsMenu.value && toolsMenuContainer.value && !toolsMenuContainer.value.contains(e.target as Node)) {
    showToolsMenu.value = false
  }
}

onMounted(() => {
  document.addEventListener('mousedown', closeMenus)

})

onUnmounted(() => {
  document.removeEventListener('mousedown', closeMenus)
})


const {
  isBold,
  isItalic,
  isUnderline,
  isList,
  currentFontSize,
  showFontSizeDropdown,
  selectedColor,
  documentTitle,
  fileInput,
  execCommand,
  applyHeading,
  changeFontSize,
  commitFontSize,
  handleFontSizeEnter,
  selectDropdownFontSize,
  applyColor,
  openRubyModal,
  removeRuby,
  triggerFileInput,
  handleFileRead,
  exportDocument,
  saveDocumentLocal,
  updateFakeSelection,
  removeFakeSelection,
  customIndent,
  customOutdent,
  currentFontFamily,
  currentHeading,
  currentIndent,
  fontOptions
} = useEditorState()
</script>
