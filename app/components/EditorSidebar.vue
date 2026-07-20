<template>
  <!-- Mobile Overlay -->
  <div v-if="isSidebarOpen" @click="toggleSidebar" class="fixed inset-0 bg-black/50 z-30 lg:hidden transition-opacity"></div>
  
  <aside class="print:hidden fixed right-0 top-toolbar-height h-[calc(100vh-56px)] w-[320px] flex flex-col bg-surface-container-low dark:bg-gray-900 border-l border-outline-variant dark:border-gray-700 z-40 transition-transform duration-300"
         :class="isSidebarOpen ? 'translate-x-0' : 'translate-x-full lg:translate-x-0'">
      <div class="p-4 flex flex-col gap-lg">
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
                      <select :value="layoutMode" @change="setLayoutMode($event.target.value)" class="w-full bg-white dark:bg-gray-800 dark:text-gray-100 border-b border-outline-variant dark:border-gray-700 focus:border-primary dark:focus:border-blue-400 focus:ring-0 text-ui-label-md py-2 pl-2 pr-8 transition-colors">
                          <option value="auto">横にページを追加(自動改ページ)</option>
                          <option value="manual">縦にページを追加(手動改ページ)</option>
                      </select>
                  </div>

                  <!-- Scroll Style (Only shown in auto mode) -->
                  <div v-if="layoutMode === 'auto'" class="flex flex-col gap-sm border-b border-outline-variant dark:border-gray-700 pb-4">
                      <label class="text-[10px] uppercase font-bold text-outline dark:text-gray-400">見た目</label>
                      <select v-model="scrollStyle" class="w-full bg-white dark:bg-gray-800 dark:text-gray-100 border-b border-outline-variant dark:border-gray-700 focus:border-primary dark:focus:border-blue-400 focus:ring-0 text-ui-label-md py-2 pl-2 pr-8 transition-colors">
                          <option value="continuous">巻物</option>
                          <option value="paged">分割ページ</option>
                      </select>
                  </div>

                  <!-- Paper Size -->
                  <div class="flex flex-col gap-sm">
                      <label class="text-[10px] uppercase font-bold text-outline dark:text-gray-400">用紙サイズ</label>
                      <select v-model="paperSize" class="w-full bg-white dark:bg-gray-800 dark:text-gray-100 border-b border-outline-variant dark:border-gray-700 focus:border-primary dark:focus:border-blue-400 focus:ring-0 text-ui-label-md py-2 pl-2 pr-8 transition-colors">
                          <option value="A4">A4 (210 x 297 mm)</option>
                          <option value="B5">B5 (182 x 257 mm)</option>
                      </select>
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
                          <div class="flex items-center gap-2 p-2 hover:bg-white dark:hover:bg-gray-800 rounded transition-colors cursor-pointer group">
                              <span class="material-symbols-outlined text-outline dark:text-gray-400 group-hover:text-primary dark:group-hover:text-blue-400" data-icon="account_tree">account_tree</span>
                              <span class="text-ui-label-md dark:text-gray-300">アウトラインを表示</span>
                          </div>
                          <div class="flex items-center gap-2 p-2 hover:bg-white dark:hover:bg-gray-800 rounded transition-colors cursor-pointer group">
                              <span class="material-symbols-outlined text-outline dark:text-gray-400 group-hover:text-primary dark:group-hover:text-blue-400" data-icon="rate_review">rate_review</span>
                              <span class="text-ui-label-md dark:text-gray-300">変更履歴を確認</span>
                          </div>
                      </div>
                  </div>
              </div>
          </div>
      </div>
      <div class="mt-auto p-4 border-t border-outline-variant dark:border-gray-700">
          <button class="w-full bg-primary dark:bg-blue-600 text-on-primary py-2.5 rounded font-ui-button text-ui-button flex items-center justify-center gap-2 shadow-sm hover:shadow-md transition-all">
              変更を適用
          </button>
          <div class="flex justify-center mt-4">
              <button @click="openHelpModal" class="flex flex-col items-center gap-1 group">
                  <span class="material-symbols-outlined text-on-surface-variant dark:text-gray-400 group-hover:text-primary dark:group-hover:text-blue-400 transition-colors" data-icon="help_outline">help_outline</span>
                  <span class="text-[10px] text-on-surface-variant dark:text-gray-400">ヘルプ</span>
              </button>
          </div>
      </div>
  </aside>
</template>

<script setup lang="ts">
// Note: @change handlers for updateMargins and updatePaperSize were removed
// because Vue v-model already handles updating the reactive state, and the
// empty functions were causing warnings.
import { useEditorState } from '../composables/useEditorState'

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
