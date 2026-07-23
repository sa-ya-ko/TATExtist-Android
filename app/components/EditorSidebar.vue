<template>
  <!-- Mobile Overlay -->
  <div v-if="isSidebarOpen" @click="toggleSidebar" class="fixed inset-0 bg-black/50 z-[100] lg:hidden transition-opacity"></div>
  
  <aside class="print:hidden fixed right-0 top-0 h-[100dvh] w-[320px] flex flex-col bg-surface-container-low dark:bg-gray-900 border-l border-outline-variant dark:border-gray-700 z-[110] lg:z-40 transition-transform duration-300 lg:top-toolbar-height lg:h-[calc(100dvh-56px)]"
         :class="isSidebarOpen ? 'translate-x-0' : 'translate-x-full lg:translate-x-0'">
      <div class="p-4 flex-1 overflow-y-auto pb-16 flex flex-col gap-lg">
          <div>
              <h2 class="font-sidebar-header text-sidebar-header text-on-surface dark:text-gray-100 mb-4 flex justify-between items-center">
                ページ設定
                <button @click="toggleSidebar" class="lg:hidden p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors">
                  <span class="material-symbols-outlined text-outline dark:text-gray-300" data-icon="close">close</span>
                </button>
              </h2>
              <div class="space-y-6">
                   <!-- Layout Mode -->
                   <div class="flex flex-col gap-sm border-b border-outline-variant dark:border-gray-700 pb-4">
                       <label class="text-[10px] uppercase font-bold text-outline dark:text-gray-400">レイアウト</label>
                       <div class="relative w-full" ref="layoutMenuContainer">
                           <button @click="showLayoutMenu = !showLayoutMenu" class="w-full bg-white dark:bg-gray-800 dark:text-gray-100 border-b border-outline-variant dark:border-gray-700 text-ui-label-md py-2 pl-2 pr-2 text-left flex justify-between items-center transition-colors rounded">
                               <span>{{ layoutMode === 'auto' ? '横にページを追加(自動改ページ)' : '縦にページを追加(手動改ページ)' }}</span>
                               <span class="material-symbols-outlined text-[18px]">expand_more</span>
                           </button>
                           <ul v-if="showLayoutMenu" class="absolute top-full left-0 w-full bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded shadow-lg z-50 list-none p-0 m-0 py-1 transition-colors mt-1">
                               <li @mousedown.prevent="setLayoutMode('auto'); showLayoutMenu = false" 
                                   class="px-3 py-2 text-sm cursor-pointer text-on-surface dark:text-gray-200 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700"
                                   :class="layoutMode === 'auto' ? 'bg-gray-100 dark:bg-gray-700 font-medium' : ''">
                                   横にページを追加(自動改ページ)
                               </li>
                               <li @mousedown.prevent="setLayoutMode('manual'); showLayoutMenu = false" 
                                   class="px-3 py-2 text-sm cursor-pointer text-on-surface dark:text-gray-200 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700"
                                   :class="layoutMode === 'manual' ? 'bg-gray-100 dark:bg-gray-700 font-medium' : ''">
                                   縦にページを追加(手動改ページ)
                               </li>
                           </ul>
                       </div>
                   </div>

                   <!-- Scroll Style (Only shown in auto mode) -->
                   <div v-if="layoutMode === 'auto'" class="flex flex-col gap-sm border-b border-outline-variant dark:border-gray-700 pb-4">
                       <label class="text-[10px] uppercase font-bold text-outline dark:text-gray-400">見た目</label>
                       <div class="relative w-full" ref="scrollStyleMenuContainer">
                           <button @click="showScrollStyleMenu = !showScrollStyleMenu" class="w-full bg-white dark:bg-gray-800 dark:text-gray-100 border-b border-outline-variant dark:border-gray-700 text-ui-label-md py-2 pl-2 pr-2 text-left flex justify-between items-center transition-colors rounded">
                               <span>{{ scrollStyle === 'continuous' ? '巻物' : '分割ページ' }}</span>
                               <span class="material-symbols-outlined text-[18px]">expand_more</span>
                           </button>
                           <ul v-if="showScrollStyleMenu" class="absolute top-full left-0 w-full bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded shadow-lg z-50 list-none p-0 m-0 py-1 transition-colors mt-1">
                               <li @mousedown.prevent="scrollStyle = 'continuous'; showScrollStyleMenu = false" 
                                   class="px-3 py-2 text-sm cursor-pointer text-on-surface dark:text-gray-200 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700"
                                   :class="scrollStyle === 'continuous' ? 'bg-gray-100 dark:bg-gray-700 font-medium' : ''">
                                   巻物
                               </li>
                               <li @mousedown.prevent="scrollStyle = 'paged'; showScrollStyleMenu = false" 
                                   class="px-3 py-2 text-sm cursor-pointer text-on-surface dark:text-gray-200 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700"
                                   :class="scrollStyle === 'paged' ? 'bg-gray-100 dark:bg-gray-700 font-medium' : ''">
                                   分割ページ
                               </li>
                           </ul>
                       </div>
                   </div>

                   <!-- Paper Size -->
                   <div class="flex flex-col gap-sm">
                       <label class="text-[10px] uppercase font-bold text-outline dark:text-gray-400">用紙サイズ</label>
                       <div class="relative w-full" ref="paperSizeMenuContainer">
                           <button @click="showPaperSizeMenu = !showPaperSizeMenu" class="w-full bg-white dark:bg-gray-800 dark:text-gray-100 border-b border-outline-variant dark:border-gray-700 text-ui-label-md py-2 pl-2 pr-2 text-left flex justify-between items-center transition-colors rounded hover:border-primary dark:hover:border-blue-400">
                               <span>{{ paperSize === 'A4' ? 'A4 (210 x 297 mm)' : 'B5 (182 x 257 mm)' }}</span>
                               <span class="material-symbols-outlined text-[18px]">expand_more</span>
                           </button>
                           <ul v-if="showPaperSizeMenu" class="absolute top-full left-0 w-full bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded shadow-lg z-50 list-none p-0 m-0 py-1 transition-colors mt-1">
                               <li @mousedown.prevent="paperSize = 'A4'; showPaperSizeMenu = false" 
                                   class="px-3 py-2 text-sm cursor-pointer text-on-surface dark:text-gray-200 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700"
                                   :class="paperSize === 'A4' ? 'bg-gray-100 dark:bg-gray-700 font-medium' : ''">
                                   A4 (210 x 297 mm)
                               </li>
                               <li @mousedown.prevent="paperSize = 'B5'; showPaperSizeMenu = false" 
                                   class="px-3 py-2 text-sm cursor-pointer text-on-surface dark:text-gray-200 transition-colors hover:bg-gray-100 dark:hover:bg-gray-700"
                                   :class="paperSize === 'B5' ? 'bg-gray-100 dark:bg-gray-700 font-medium' : ''">
                                   B5 (182 x 257 mm)
                               </li>
                           </ul>
                       </div>
                   </div>
                  <!-- Margins -->
                  <div class="flex flex-col gap-sm">
                      <label class="text-[10px] uppercase font-bold text-outline dark:text-gray-400">余白 (mm)</label>
                      <div class="grid grid-cols-2 gap-md">
                          <div class="flex flex-col gap-1">
                              <span class="text-[10px] text-on-surface-variant dark:text-gray-300">上</span>
                              <input v-model.number="margins.top" class="bg-white dark:bg-gray-800 dark:text-gray-100 border-b border-outline-variant dark:border-gray-700 focus:border-primary dark:focus:border-blue-400 focus:ring-0 text-ui-label-md py-1 transition-colors" type="number" min="0">
                          </div>
                          <div class="flex flex-col gap-1">
                              <span class="text-[10px] text-on-surface-variant dark:text-gray-300">下</span>
                              <input v-model.number="margins.bottom" class="bg-white dark:bg-gray-800 dark:text-gray-100 border-b border-outline-variant dark:border-gray-700 focus:border-primary dark:focus:border-blue-400 focus:ring-0 text-ui-label-md py-1 transition-colors" type="number" min="0">
                          </div>
                          <div class="flex flex-col gap-1">
                              <span class="text-[10px] text-on-surface-variant dark:text-gray-300">左</span>
                              <input v-model.number="margins.left" class="bg-white dark:bg-gray-800 dark:text-gray-100 border-b border-outline-variant dark:border-gray-700 focus:border-primary dark:focus:border-blue-400 focus:ring-0 text-ui-label-md py-1 transition-colors" type="number" min="0">
                          </div>
                          <div class="flex flex-col gap-1">
                              <span class="text-[10px] text-on-surface-variant dark:text-gray-300">右</span>
                              <input v-model.number="margins.right" class="bg-white dark:bg-gray-800 dark:text-gray-100 border-b border-outline-variant dark:border-gray-700 focus:border-primary dark:focus:border-blue-400 focus:ring-0 text-ui-label-md py-1 transition-colors" type="number" min="0">
                          </div>
                      </div>
                  </div>
                  <!-- Structure Section -->
                  <div class="pt-4 border-t border-outline-variant dark:border-gray-700">
                      <h3 class="font-sidebar-header text-sidebar-header text-on-surface dark:text-gray-100 mb-2">表示設定</h3>
                      <div class="flex flex-col gap-sm">
                          <label class="flex items-center gap-2 p-2 rounded transition-colors"
                                 :class="layoutMode === 'auto' ? 'opacity-50 cursor-not-allowed' : 'hover:bg-white dark:hover:bg-gray-800 cursor-pointer group'">
                              <input type="checkbox" v-model="showRulers" :disabled="layoutMode === 'auto'" 
                                     class="rounded border-outline-variant text-primary dark:text-blue-500 focus:ring-primary dark:focus:ring-blue-500 dark:bg-gray-900"
                                     :class="{'cursor-not-allowed': layoutMode === 'auto'}">
                              <span class="text-ui-label-md dark:text-gray-300">ルーラーを表示</span>
                          </label>
                          <div class="flex items-center gap-2 p-2 hover:bg-white dark:hover:bg-gray-800 rounded transition-colors group">
                              <label class="flex items-center gap-2 flex-1">
                                  <span class="text-ui-label-md dark:text-gray-300">字下げ幅</span>
                              </label>
                              <div class="flex items-center gap-1">
                                  <input type="number" v-model.number="indentSize" min="0" max="10" class="w-12 bg-white dark:bg-gray-900 dark:text-gray-100 border-b border-outline-variant dark:border-gray-700 focus:border-primary dark:focus:border-blue-400 focus:ring-0 text-ui-label-md py-0.5 px-1 text-center transition-colors">
                                  <span class="text-[10px] text-outline dark:text-gray-400">文字</span>
                              </div>
                          </div>
                      </div>
                  </div>
              </div>
          </div>
      </div>
      <div class="mt-auto p-4 border-t border-outline-variant dark:border-gray-700">
          <div class="flex justify-center gap-8">
              <button @click="openHelpModal" class="flex flex-col items-center gap-1 group">
                  <span class="material-symbols-outlined text-on-surface-variant dark:text-gray-400 group-hover:text-primary dark:group-hover:text-blue-400 transition-colors" data-icon="help_outline">help_outline</span>
                  <span class="text-[10px] text-on-surface-variant dark:text-gray-400">ヘルプ</span>
              </button>
              <NuxtLink to="/terms" target="_blank" class="flex flex-col items-center gap-1 group">
                  <span class="material-symbols-outlined text-on-surface-variant dark:text-gray-400 group-hover:text-primary dark:group-hover:text-blue-400 transition-colors" data-icon="description">description</span>
                  <span class="text-[10px] text-on-surface-variant dark:text-gray-400">利用規約</span>
              </NuxtLink>
              <NuxtLink to="/privacy" target="_blank" class="flex flex-col items-center gap-1 group">
                  <span class="material-symbols-outlined text-on-surface-variant dark:text-gray-400 group-hover:text-primary dark:group-hover:text-blue-400 transition-colors" data-icon="security">security</span>
                  <span class="text-[10px] text-on-surface-variant dark:text-gray-400">ポリシー</span>
              </NuxtLink>
          </div>
      </div>
  </aside>
</template>

<script setup lang="ts">
// Note: @change handlers for updateMargins and updatePaperSize were removed
// because Vue v-model already handles updating the reactive state, and the
// empty functions were causing warnings.
import { ref, onMounted, onUnmounted } from 'vue'
import { useEditorState } from '../composables/useEditorState'

const showLayoutMenu = ref(false)
const showScrollStyleMenu = ref(false)
const showPaperSizeMenu = ref(false)

const layoutMenuContainer = ref<HTMLElement | null>(null)
const scrollStyleMenuContainer = ref<HTMLElement | null>(null)
const paperSizeMenuContainer = ref<HTMLElement | null>(null)

const closeSidebarMenus = (e: MouseEvent) => {
  if (showLayoutMenu.value && layoutMenuContainer.value && !layoutMenuContainer.value.contains(e.target as Node)) showLayoutMenu.value = false
  if (showScrollStyleMenu.value && scrollStyleMenuContainer.value && !scrollStyleMenuContainer.value.contains(e.target as Node)) showScrollStyleMenu.value = false
  if (showPaperSizeMenu.value && paperSizeMenuContainer.value && !paperSizeMenuContainer.value.contains(e.target as Node)) showPaperSizeMenu.value = false
}

onMounted(() => {
  document.addEventListener('mousedown', closeSidebarMenus)
})

onUnmounted(() => {
  document.removeEventListener('mousedown', closeSidebarMenus)
})

const {
  paperSize,
  margins,
  showRulers,
  layoutMode,
  scrollStyle,
  setLayoutMode,
  indentSize,
  openHelpModal,
  isSidebarOpen,
  toggleSidebar
} = useEditorState()
</script>
