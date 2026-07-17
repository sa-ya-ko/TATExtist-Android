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
              <div class="relative flex items-center">
                  <button @click="showFileMenu = !showFileMenu" @blur="setTimeout(() => showFileMenu = false, 150)" class="px-2 py-1 flex items-center gap-1 rounded hover:bg-surface-container-low text-ui-label-md transition-colors whitespace-nowrap" title="ファイル">
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
                      <li @mousedown.prevent="loadFileListFromDrive(); showFileMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low cursor-pointer text-[#4285F4] m-0 flex items-center gap-2">
                          <span class="material-symbols-outlined text-[18px]">cloud_download</span> Driveから開く
                      </li>
                      <li @mousedown.prevent="saveToDrive(); showFileMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low cursor-pointer text-[#4285F4] m-0 flex items-center gap-2">
                          <span class="material-symbols-outlined text-[18px]">cloud_upload</span> Driveに保存
                      </li>
                      <div class="h-px bg-gray-200 my-1"></div>
                      <li @mousedown.prevent="exportDocument('pdf'); showFileMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low cursor-pointer text-on-surface m-0 flex items-center gap-2">
                          <span class="material-symbols-outlined text-[18px]">picture_as_pdf</span> PDFとして書き出し
                      </li>
                  </ul>
              </div>

              <!-- Tools Dropdown -->
              <div class="relative flex items-center">
                  <button @click="showToolsMenu = !showToolsMenu" @blur="setTimeout(() => showToolsMenu = false, 150)" class="px-2 py-1 flex items-center gap-1 rounded hover:bg-surface-container-low text-ui-label-md transition-colors whitespace-nowrap" title="ツール">
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
                  <div class="flex items-center gap-2">
                      <button @mousedown.prevent="openRubyModal" class="px-3 py-1.5 bg-blue-50 text-blue-700 hover:bg-blue-100 rounded text-ui-label-md transition-colors whitespace-nowrap shadow-sm font-medium" title="ルビ付与">ルビ付与</button>
                      <button @mousedown.prevent="removeRuby" class="px-3 py-1.5 bg-red-50 text-red-600 hover:bg-red-100 rounded text-ui-label-md transition-colors whitespace-nowrap shadow-sm font-medium" title="ルビ解除">ルビ解除</button>
                  </div>
              </nav>
          </div>
          <div class="flex items-center gap-md">
              <input type="file" ref="fileInput" @change="handleFileRead" accept=".tategaki,.txt" class="hidden" />
          </div>
      </div>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useGoogleDrive } from '../composables/useGoogleDrive'

const showToolsMenu = ref(false)
const showFileMenu = ref(false)

const {
  initGoogleDrive,
  saveToDrive,
  loadFileListFromDrive
} = useGoogleDrive()

onMounted(() => {
  // スクリプトの読み込み完了を待ってから初期化するため少し遅延させます
  setTimeout(initGoogleDrive, 1000)
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
  openWordCountModal,
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
