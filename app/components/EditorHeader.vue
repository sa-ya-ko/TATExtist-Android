<template>
  <header class="print:hidden relative w-full flex flex-col md:flex-row md:items-center z-50 bg-surface dark:bg-gray-900 border-b border-outline-variant dark:border-gray-700 transition-colors duration-300 flex-shrink-0 md:h-[56px] md:overflow-visible">
      <div class="flex flex-col md:flex-row md:items-center justify-between px-4 py-2 gap-2 min-h-[56px] md:min-h-full md:py-0 md:pr-4 md:flex-shrink-0">
          <!-- 1段目 (スマホ) / 左側 (PC): ロゴ ＆ ドキュメントタイトル -->
          <div class="flex items-center gap-3 w-full md:w-auto overflow-hidden">
              <!-- ロゴとサイト名 -->
              <div class="flex items-center gap-2 shrink-0">
                  <img src="/logo.webp" alt="TATExtist" class="w-10 h-10 object-contain" />
                  <div class="flex flex-col justify-center">
                      <span class="text-[9px] text-gray-500 dark:text-gray-400 leading-none mb-0.5">縦書き専用エディタ</span>
                      <span class="text-sm font-bold text-gray-800 dark:text-gray-100 leading-none">TATExtist</span>
                  </div>
              </div>
              <!-- 区切り線 -->
              <div class="w-px h-6 bg-gray-300 dark:bg-gray-700 shrink-0"></div>

              <!-- 既存部分：汎用アイコンとタイトル入力欄 -->
              <div class="flex items-center gap-1.5 flex-1 min-w-0">
                  <span class="material-symbols-outlined text-primary dark:text-blue-400 text-[18px] shrink-0" data-icon="description">description</span>
                  <input v-model="documentTitle" class="bg-transparent border-none p-0 focus:ring-0 font-medium text-on-surface-variant dark:text-gray-200 flex-1 md:w-40 transition-colors outline-none truncate text-sm" type="text" placeholder="無題のドキュメント">
              </div>
          </div>

          <!-- 2段目 (スマホ) / 中央 (PC): ツール類 -->
          <div class="flex flex-wrap items-center justify-between md:justify-start w-full max-w-full md:w-auto gap-1 md:gap-2">
              <!-- File Dropdown -->
              <div class="relative flex items-center" ref="fileMenuContainer">
                  <button @click="showFileMenu = !showFileMenu" class="px-1 md:px-2 py-1 flex items-center gap-0.5 md:gap-1 rounded hover:bg-surface-container-low dark:hover:bg-gray-800 text-xs md:text-sm dark:text-gray-200 transition-colors whitespace-nowrap" title="ファイル">
                      ファイル
                  </button>
                  <ul v-if="showFileMenu" class="absolute top-full left-0 mt-1 w-48 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded shadow-lg z-50 list-none p-0 m-0 py-1 transition-colors">
                      <li @mousedown.prevent="triggerFileInput(); showFileMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-on-surface dark:text-gray-200 m-0 flex items-center gap-2 transition-colors">
                          <span class="material-symbols-outlined text-[18px]">folder_open</span> パソコンから開く
                      </li>
                      <li @mousedown.prevent="saveDocumentLocal(); showFileMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-on-surface dark:text-gray-200 m-0 flex items-center gap-2 transition-colors">
                          <span class="material-symbols-outlined text-[18px]">save</span> パソコンに保存
                      </li>
                      <div class="h-px bg-gray-200 dark:bg-gray-700 my-1"></div>
                      <li @mousedown.prevent="loadFileListFromDrive(); showFileMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-[#4285F4] m-0 flex items-center gap-2 transition-colors">
                          <span class="material-symbols-outlined text-[18px]">cloud_download</span> Driveから開く
                      </li>
                      <li @mousedown.prevent="saveToDrive(); showFileMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-[#4285F4] m-0 flex items-center gap-2 transition-colors">
                          <span class="material-symbols-outlined text-[18px]">cloud_upload</span> Driveに保存
                      </li>
                      <li v-if="isAuthenticated" @mousedown.prevent="handleSignoutClick(); showFileMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-red-500 m-0 flex items-center gap-2 transition-colors">
                          <span class="material-symbols-outlined text-[18px]">logout</span> Drive連携解除
                      </li>
                      <div class="h-px bg-gray-200 dark:bg-gray-700 my-1"></div>
                      <li @mousedown.prevent="downloadPDF(); showFileMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-on-surface dark:text-gray-200 m-0 flex items-center gap-2 transition-colors">
                          <span class="material-symbols-outlined text-[18px]">download</span> PDFとしてダウンロード
                      </li>
                      <li @mousedown.prevent="exportDocument('print'); showFileMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-on-surface dark:text-gray-200 m-0 flex items-center gap-2 transition-colors">
                          <span class="material-symbols-outlined text-[18px]">print</span> 印刷する
                      </li>
                  </ul>
              </div>

              <!-- Tools Dropdown -->
              <div class="relative flex items-center" ref="toolsMenuContainer">
                  <button @click="showToolsMenu = !showToolsMenu" class="px-1 md:px-2 py-1 flex items-center gap-0.5 md:gap-1 rounded hover:bg-surface-container-low dark:hover:bg-gray-800 text-xs md:text-sm dark:text-gray-200 transition-colors whitespace-nowrap" title="ツール">
                      ツール
                  </button>
                  <ul v-if="showToolsMenu" class="absolute top-full left-0 mt-1 w-40 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded shadow-lg z-50 list-none p-0 m-0 py-1 transition-colors">
                      <li @mousedown.prevent="openWordCountModal(); showToolsMenu = false" class="px-4 py-2 text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-on-surface dark:text-gray-200 m-0 transition-colors">
                          文字カウント
                      </li>
                  </ul>
              </div>

              <div class="h-6 w-[1px] bg-outline-variant dark:bg-gray-700 mx-1 hidden md:block"></div>

              <!-- Undo / Redo -->
              <button @mousedown.prevent="execCommand('undo')" class="p-1 md:p-2 hover:bg-surface-container-low dark:hover:bg-gray-800 transition-colors rounded text-on-surface dark:text-gray-200 flex items-center justify-center" title="元に戻す (Undo)">
                  <span class="material-symbols-outlined" data-icon="undo">undo</span>
              </button>
              <button @mousedown.prevent="execCommand('redo')" class="p-1 md:p-2 hover:bg-surface-container-low dark:hover:bg-gray-800 transition-colors rounded text-on-surface dark:text-gray-200 flex items-center justify-center" title="やり直す (Redo)">
                  <span class="material-symbols-outlined" data-icon="redo">redo</span>
              </button>

              <div class="h-6 w-[1px] bg-outline-variant dark:bg-gray-700 mx-1 hidden md:block"></div>

              <!-- Right Side (Dark mode, Hamburger) -->
              <button @click="toggleDarkMode" class="md:hidden p-1 md:p-2 hover:bg-surface-container-low dark:hover:bg-gray-800 rounded transition-colors flex items-center justify-center" title="ダークモード切替">
                  <span class="material-symbols-outlined text-on-surface-variant dark:text-gray-300">{{ isDarkMode ? 'light_mode' : 'dark_mode' }}</span>
              </button>
              <button @click="toggleSidebar" class="md:hidden p-1 md:p-2 hover:bg-surface-container-low dark:hover:bg-gray-800 rounded transition-colors flex items-center justify-center" title="メニュー">
                  <span class="material-symbols-outlined text-on-surface-variant dark:text-gray-300">menu</span>
              </button>
              <input type="file" ref="fileInput" @change="handleFileRead" accept=".tategaki,.txt" class="hidden" />
          </div>
      </div>

      <!-- Format Controls (Mobile Bottom Slider, Desktop Inline) -->
      <nav class="fixed bottom-0 left-0 w-full min-h-14 md:static md:w-auto md:min-h-0 md:flex-1 flex items-center gap-1 text-on-surface dark:text-gray-200 bg-surface dark:bg-gray-900 md:bg-transparent border-t md:border-t-0 border-outline-variant dark:border-gray-700 px-2 pt-2 pb-[calc(0.5rem+env(safe-area-inset-bottom,0px))] md:p-0 overflow-x-auto md:overflow-visible [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none] z-50 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)] md:shadow-none transition-colors duration-300 md:pl-0 md:pr-4" style="-webkit-overflow-scrolling: touch;">
          <!-- Font Name -->
          <div class="relative flex items-center mx-1" ref="fontMenuContainer">
              <button @click="showFontFamilyDropdown = !showFontFamilyDropdown" class="flex items-center gap-1 bg-transparent border-none text-sm font-medium focus:ring-0 px-2 py-1 outline-none hover:bg-surface-container-low dark:hover:bg-gray-800 rounded transition-colors whitespace-nowrap">
                  <span>{{ fontOptions.find(f => f.value === currentFontFamily)?.label || 'フォント' }}</span>
                  <span class="material-symbols-outlined text-[16px] pointer-events-none">arrow_drop_down</span>
              </button>
              <ul v-if="showFontFamilyDropdown" class="fixed bottom-[calc(3.5rem+env(safe-area-inset-bottom,0px))] left-0 w-full md:absolute md:bottom-auto md:top-full md:mb-0 md:mt-1 md:left-1/2 md:-translate-x-1/2 md:w-max md:min-w-[160px] bg-white dark:bg-gray-800 border-t md:border border-gray-200 dark:border-gray-700 md:rounded shadow-[0_-4px_10px_-1px_rgba(0,0,0,0.1)] md:shadow-lg z-[100] max-h-[50vh] overflow-y-auto list-none p-0 m-0">
                  <li v-for="font in fontOptions" :key="font.value" @mousedown.prevent="() => { applyCustomFontFamily(font.value); showFontFamilyDropdown = false }" class="px-4 py-4 md:px-2 md:py-1.5 text-center text-lg md:text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-on-surface dark:text-gray-200 m-0 transition-colors whitespace-nowrap border-b border-gray-100 dark:border-gray-700 md:border-none" :style="{ fontFamily: font.value }">
                      {{ font.label }}
                  </li>
              </ul>
          </div>
          
          <div class="h-6 w-[1px] bg-outline-variant dark:bg-gray-700 mx-1"></div>
          
          <!-- Heading -->
          <div class="relative flex items-center mx-1" ref="headingMenuContainer">
              <button @click="showHeadingDropdown = !showHeadingDropdown" class="flex items-center gap-1 bg-transparent border-none text-sm font-medium focus:ring-0 px-2 py-1 outline-none hover:bg-surface-container-low dark:hover:bg-gray-800 rounded transition-colors whitespace-nowrap dark:text-gray-200">
                  <span>{{ 
                      currentHeading === 'H1' ? 'タイトル' : 
                      currentHeading === 'H2' ? '見出し 1' : 
                      currentHeading === 'H3' ? '見出し 2' : 
                      currentHeading === 'H4' ? '見出し 3' : '標準テキスト' 
                  }}</span>
                  <span class="material-symbols-outlined text-[16px] pointer-events-none">arrow_drop_down</span>
              </button>
              <ul v-if="showHeadingDropdown" class="fixed bottom-[calc(3.5rem+env(safe-area-inset-bottom,0px))] left-0 w-full md:absolute md:bottom-auto md:top-full md:mb-0 md:mt-1 md:left-1/2 md:-translate-x-1/2 md:w-max md:min-w-[160px] bg-white dark:bg-gray-800 border-t md:border border-gray-200 dark:border-gray-700 md:rounded shadow-[0_-4px_10px_-1px_rgba(0,0,0,0.1)] md:shadow-lg z-[100] max-h-[50vh] overflow-y-auto list-none p-0 m-0">
                  <li @mousedown.prevent="() => { applyHeading('P'); currentHeading = 'P'; showHeadingDropdown = false }" class="px-4 py-4 md:px-2 md:py-1.5 text-center text-lg md:text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-on-surface dark:text-gray-200 m-0 transition-colors whitespace-nowrap border-b border-gray-100 dark:border-gray-700 md:border-none">標準テキスト</li>
                  <li @mousedown.prevent="() => { applyHeading('H1'); currentHeading = 'H1'; showHeadingDropdown = false }" class="px-4 py-4 md:px-2 md:py-1.5 text-center text-lg md:text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-on-surface dark:text-gray-200 m-0 transition-colors whitespace-nowrap border-b border-gray-100 dark:border-gray-700 md:border-none font-bold">タイトル</li>
                  <li @mousedown.prevent="() => { applyHeading('H2'); currentHeading = 'H2'; showHeadingDropdown = false }" class="px-4 py-4 md:px-2 md:py-1.5 text-center text-lg md:text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-on-surface dark:text-gray-200 m-0 transition-colors whitespace-nowrap border-b border-gray-100 dark:border-gray-700 md:border-none font-bold">見出し 1</li>
                  <li @mousedown.prevent="() => { applyHeading('H3'); currentHeading = 'H3'; showHeadingDropdown = false }" class="px-4 py-4 md:px-2 md:py-1.5 text-center text-lg md:text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-on-surface dark:text-gray-200 m-0 transition-colors whitespace-nowrap border-b border-gray-100 dark:border-gray-700 md:border-none font-bold">見出し 2</li>
                  <li @mousedown.prevent="() => { applyHeading('H4'); currentHeading = 'H4'; showHeadingDropdown = false }" class="px-4 py-4 md:px-2 md:py-1.5 text-center text-lg md:text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-on-surface dark:text-gray-200 m-0 transition-colors whitespace-nowrap border-b border-gray-100 dark:border-gray-700 md:border-none font-bold">見出し 3</li>
              </ul>
          </div>
          
          <div class="h-6 w-[1px] bg-outline-variant dark:bg-gray-700 mx-1"></div>
          
          <!-- Font Size Control -->
          <div class="flex items-center">
              <button @mousedown.prevent="changeFontSize(-1)" class="p-1 hover:bg-surface-container-low dark:hover:bg-gray-800 rounded transition-colors flex items-center justify-center" title="フォントサイズを縮小">
                  <span class="material-symbols-outlined pointer-events-none" style="font-size: 18px;">remove</span>
              </button>
              
              <div class="relative flex items-center mx-1" ref="fontSizeMenuContainer">
                  <input 
                      type="text" 
                      v-model="currentFontSize" 
                      @change="commitFontSize" 
                      @focus="() => { updateFakeSelection(); showFontSizeDropdown = true }"
                      @blur="removeFakeSelection"
                      @keydown.enter.prevent="handleFontSizeEnter"
                      class="w-12 md:w-10 h-8 md:h-7 text-center text-sm border border-outline-variant dark:border-gray-600 rounded hover:border-outline focus:border-primary focus:ring-1 focus:ring-primary p-0 bg-white dark:bg-gray-800 dark:text-gray-200 outline-none transition-colors" 
                  />
                  <ul v-if="showFontSizeDropdown" class="fixed bottom-[calc(3.5rem+env(safe-area-inset-bottom,0px))] left-0 w-full md:absolute md:bottom-auto md:top-full md:mb-0 md:mt-1 md:left-1/2 md:-translate-x-1/2 md:w-16 bg-white dark:bg-gray-800 border-t md:border border-gray-200 dark:border-gray-700 md:rounded shadow-[0_-4px_10px_-1px_rgba(0,0,0,0.1)] md:shadow-lg z-[100] max-h-[50vh] overflow-y-auto list-none p-0 m-0 grid grid-cols-4 md:grid-cols-1 md:block">
                      <li v-for="size in [8, 10, 11, 12, 14, 18, 24, 36]" :key="size" @mousedown.prevent="selectDropdownFontSize(size)" class="px-4 py-4 md:px-2 md:py-1.5 text-center text-lg md:text-sm hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-on-surface dark:text-gray-200 m-0 transition-colors border-b md:border-none border-r border-gray-100 dark:border-gray-700">{{ size }}</li>
                  </ul>
              </div>

              <button @mousedown.prevent="changeFontSize(1)" class="p-1 hover:bg-surface-container-low dark:hover:bg-gray-800 rounded transition-colors flex items-center justify-center" title="フォントサイズを拡大">
                  <span class="material-symbols-outlined pointer-events-none" style="font-size: 18px;">add</span>
              </button>
          </div>
          
          <div class="h-6 w-[1px] bg-outline-variant dark:bg-gray-700 mx-1"></div>
          
          <!-- Formatting Options -->
          <button @mousedown.prevent="execCommand('bold')" :class="['p-1 md:p-2 transition-colors rounded', isBold ? 'bg-primary/10 text-primary' : 'hover:bg-surface-container-low dark:hover:bg-gray-800']" title="太字">
              <span class="material-symbols-outlined" data-icon="format_bold">format_bold</span>
          </button>
          <button @mousedown.prevent="execCommand('italic')" :class="['p-1 md:p-2 transition-colors rounded', isItalic ? 'bg-primary/10 text-primary' : 'hover:bg-surface-container-low dark:hover:bg-gray-800']" title="斜体">
              <span class="material-symbols-outlined" data-icon="format_italic">format_italic</span>
          </button>
          <button @mousedown.prevent="execCommand('underline')" :class="['p-1 md:p-2 transition-colors rounded', isUnderline ? 'bg-primary/10 text-primary' : 'hover:bg-surface-container-low dark:hover:bg-gray-800']" title="傍線（下線）">
              <span class="material-symbols-outlined" data-icon="format_underlined">format_underlined</span>
          </button>
          
          <!-- 文字色パレット (プリセットカラー) -->
          <div class="relative flex items-center" ref="colorMenuContainer">
              <button @mousedown.prevent="showColorMenu = !showColorMenu" class="p-1 md:p-2 hover:bg-surface-container-low dark:hover:bg-gray-800 transition-colors rounded cursor-pointer flex items-center justify-center" title="文字色">
                  <div class="flex flex-col items-center justify-center w-6 h-6 pointer-events-none">
                      <span class="font-sans font-bold text-[16px] text-on-surface dark:text-gray-200 leading-none h-[16px]">A</span>
                      <div class="w-[18px] h-[4px] mt-[2px] rounded-sm" :style="{ backgroundColor: selectedColor || '#000000' }"></div>
                  </div>
              </button>

              <!-- カラーパレットポップオーバー -->
              <div v-if="showColorMenu" class="fixed bottom-[calc(3.5rem+env(safe-area-inset-bottom,0px))] left-1/2 -translate-x-1/2 md:absolute md:bottom-auto md:top-full md:mb-0 md:mt-1 md:left-1/2 md:-translate-x-1/2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg z-[100] p-2 transition-colors">
                  <div class="text-[11px] text-gray-500 dark:text-gray-400 mb-1.5 px-0.5 font-medium whitespace-nowrap">文字色を選択</div>
                  <!-- プリセット10色 -->
                  <div class="grid grid-cols-5 gap-1.5 w-36">
                      <button v-for="color in presetColors" :key="color.value"
                              @mousedown.prevent="selectPresetColor(color.value)"
                              class="w-5 h-5 rounded-full border border-gray-300 dark:border-gray-600 transition-transform hover:scale-110 flex items-center justify-center shadow-sm"
                              :style="{ backgroundColor: color.value }"
                              :title="color.label">
                      </button>
                  </div>
                  <!-- 区切り線 -->
                  <div class="h-px bg-gray-200 dark:bg-gray-700 my-2"></div>
                  <!-- カスタムカラーピッカー -->
                  <label class="flex items-center gap-1.5 px-1 py-1 rounded hover:bg-surface-container-low dark:hover:bg-gray-700 cursor-pointer text-xs text-on-surface dark:text-gray-200 transition-colors">
                      <div class="relative w-5 h-5 rounded-full border border-gray-300 dark:border-gray-600 overflow-hidden flex items-center justify-center bg-gradient-to-br from-red-500 via-green-500 to-blue-500 shadow-sm">
                          <input type="color" :value="selectedColor || '#000000'" @input="applyCustomColor" class="absolute inset-0 opacity-0 cursor-pointer w-full h-full" />
                      </div>
                      <span class="font-medium text-[11px]">カスタム色...</span>
                  </label>
              </div>
          </div>
          
          <div class="w-px h-6 bg-outline-variant dark:bg-gray-700 mx-1"></div>
          
          <button @mousedown.prevent="toggleUnorderedList" :class="['p-1 md:p-2 transition-colors rounded', isList ? 'bg-primary/10 text-primary' : 'hover:bg-surface-container-low dark:hover:bg-gray-800']" title="箇条書き">
              <span class="material-symbols-outlined" data-icon="format_list_bulleted">format_list_bulleted</span>
          </button>
          <button @mousedown.prevent="customOutdent" class="p-1 md:p-2 transition-colors rounded hover:bg-surface-container-low dark:hover:bg-gray-800" title="インデントを減らす（字上げ）">
              <span class="material-symbols-outlined" data-icon="format_indent_decrease">format_indent_decrease</span>
          </button>
          <button @mousedown.prevent="customIndent" class="p-1 md:p-2 transition-colors rounded hover:bg-surface-container-low dark:hover:bg-gray-800" title="インデントを増やす（字下げ）">
              <span class="material-symbols-outlined" data-icon="format_indent_increase">format_indent_increase</span>
          </button>
          
          <div class="h-6 w-[1px] bg-outline-variant dark:bg-gray-700 mx-1"></div>
          
          <button @mousedown.prevent="openRubyModal" class="px-2 py-1 border dark:border-gray-600 rounded hover:bg-surface-container-low dark:hover:bg-gray-800 text-sm transition-colors whitespace-nowrap" title="ルビ付与">ルビ付与</button>
          <button @mousedown.prevent="removeRuby" class="px-2 py-1 border dark:border-gray-600 rounded hover:bg-surface-container-low dark:hover:bg-gray-800 text-sm text-red-500 transition-colors whitespace-nowrap" title="ルビ解除">ルビ解除</button>
      </nav>
      
      <!-- PC Only Dark Mode Button (Far Right) -->
      <button @click="toggleDarkMode" class="hidden md:flex p-1 md:p-2 hover:bg-surface-container-low dark:hover:bg-gray-800 rounded transition-colors items-center justify-center ml-auto flex-shrink-0" title="ダークモード切替">
          <span class="material-symbols-outlined text-on-surface-variant dark:text-gray-300">{{ isDarkMode ? 'light_mode' : 'dark_mode' }}</span>
      </button>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useEditorState, persistDocument } from '../composables/useEditorState'
import { useGoogleDrive } from '../composables/useGoogleDrive'

const showToolsMenu = ref(false)
const showFileMenu = ref(false)
const showFontFamilyDropdown = ref(false)
const showHeadingDropdown = ref(false)

const fileMenuContainer = ref<HTMLElement | null>(null)
const toolsMenuContainer = ref<HTMLElement | null>(null)
const fontMenuContainer = ref<HTMLElement | null>(null)
const headingMenuContainer = ref<HTMLElement | null>(null)
const fontSizeMenuContainer = ref<HTMLElement | null>(null)

const showColorMenu = ref(false)
const colorMenuContainer = ref<HTMLElement | null>(null)

const {
  isAuthenticated,
  initGoogleDrive,
  saveToDrive,
  loadFileListFromDrive,
  handleSignoutClick
} = useGoogleDrive()

const presetColors = [
  // グレースケール（5段階）
  { label: '漆黒', value: '#000000' },
  { label: '墨色', value: '#1F2937' },
  { label: '濃灰', value: '#4B5563' },
  { label: '中灰', value: '#9CA3AF' },
  { label: '薄灰', value: '#D1D5DB' },
  // 有彩色（5色：赤と橙の間にピンク）
  { label: '赤', value: '#E62112' },
  { label: 'ピンク', value: '#E95098' },
  { label: '橙', value: '#ED6A00' },
  { label: '緑', value: '#0097A1' },
  { label: '青', value: '#100D63' }
]

const selectPresetColor = (colorValue: string) => {
  selectedColor.value = colorValue
  applyColor()
  showColorMenu.value = false
}

const applyCustomColor = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target && target.value) {
    selectedColor.value = target.value
    applyColor()
  }
}

const closeMenus = (e: MouseEvent) => {
  if (showFileMenu.value && fileMenuContainer.value && !fileMenuContainer.value.contains(e.target as Node)) showFileMenu.value = false
  if (showToolsMenu.value && toolsMenuContainer.value && !toolsMenuContainer.value.contains(e.target as Node)) showToolsMenu.value = false
  if (showFontFamilyDropdown.value && fontMenuContainer.value && !fontMenuContainer.value.contains(e.target as Node)) showFontFamilyDropdown.value = false
  if (showHeadingDropdown.value && headingMenuContainer.value && !headingMenuContainer.value.contains(e.target as Node)) showHeadingDropdown.value = false
  if (showFontSizeDropdown.value && fontSizeMenuContainer.value && !fontSizeMenuContainer.value.contains(e.target as Node)) showFontSizeDropdown.value = false
  if (showColorMenu.value && colorMenuContainer.value && !colorMenuContainer.value.contains(e.target as Node)) showColorMenu.value = false
}

onMounted(() => {
  document.addEventListener('mousedown', closeMenus)
  // スクリプトの読み込み完了を待ってから初期化するため少し遅延させます
  setTimeout(initGoogleDrive, 1000)
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
  toggleUnorderedList,
  applyHeading,
  changeFontSize,
  commitFontSize,
  handleFontSizeEnter,
  selectDropdownFontSize,
  applyCustomFontFamily,
  applyColor,
  openRubyModal,
  removeRuby,
  openWordCountModal,
  openHelpModal,
  downloadPDF,
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
  fontOptions,
  isDarkMode,
  toggleDarkMode,
  toggleSidebar
} = useEditorState()

watch(documentTitle, () => {
  persistDocument()
})
</script>
<!-- HMR trigger -->
