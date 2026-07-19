<template>
  <aside class="print-hidden fixed right-0 top-toolbar-height h-[calc(100vh-56px)] w-sidebar-width flex flex-col bg-surface-container-low border-l border-outline-variant z-40">
      <div class="p-4 flex flex-col gap-lg">
          <div>
              <h2 class="font-sidebar-header text-sidebar-header text-on-surface mb-4">ページ設定</h2>
              <div class="space-y-6">
                  <!-- Layout Mode -->
                  <div class="flex flex-col gap-sm border-b border-outline-variant pb-4">
                      <label class="text-[10px] uppercase font-bold text-outline">レイアウト</label>
                      <select :value="layoutMode" @change="setLayoutMode($event.target.value)" class="w-full bg-white border-b border-outline-variant focus:border-primary focus:ring-0 text-ui-label-md py-2 pl-2 pr-8">
                          <option value="auto">横にページを追加(自動改ページ)</option>
                          <option value="manual">縦にページを追加(手動改ページ)</option>
                      </select>
                  </div>

                  <!-- Scroll Style (Only shown in auto mode) -->
                  <div v-if="layoutMode === 'auto'" class="flex flex-col gap-sm border-b border-outline-variant pb-4">
                      <label class="text-[10px] uppercase font-bold text-outline">見た目</label>
                      <select v-model="scrollStyle" class="w-full bg-white border-b border-outline-variant focus:border-primary focus:ring-0 text-ui-label-md py-2 pl-2 pr-8">
                          <option value="continuous">巻物</option>
                          <option value="paged">分割ページ</option>
                      </select>
                  </div>

                  <!-- Paper Size -->
                  <div class="flex flex-col gap-sm">
                      <label class="text-[10px] uppercase font-bold text-outline">用紙サイズ</label>
                      <select v-model="paperSize" class="w-full bg-white border-b border-outline-variant focus:border-primary focus:ring-0 text-ui-label-md py-2 pl-2 pr-8">
                          <option value="A4">A4 (210 x 297 mm)</option>
                          <option value="B5">B5 (182 x 257 mm)</option>
                      </select>
                  </div>
                  <!-- Margins -->
                  <div class="flex flex-col gap-sm">
                      <label class="text-[10px] uppercase font-bold text-outline">余白 (mm)</label>
                      <div class="grid grid-cols-2 gap-md">
                          <div class="flex flex-col gap-1">
                              <span class="text-[10px] text-on-surface-variant">上</span>
                              <input v-model.number="margins.top" class="bg-white border-b border-outline-variant focus:border-primary focus:ring-0 text-ui-label-md py-1" type="number" min="0">
                          </div>
                          <div class="flex flex-col gap-1">
                              <span class="text-[10px] text-on-surface-variant">下</span>
                              <input v-model.number="margins.bottom" class="bg-white border-b border-outline-variant focus:border-primary focus:ring-0 text-ui-label-md py-1" type="number" min="0">
                          </div>
                          <div class="flex flex-col gap-1">
                              <span class="text-[10px] text-on-surface-variant">左</span>
                              <input v-model.number="margins.left" class="bg-white border-b border-outline-variant focus:border-primary focus:ring-0 text-ui-label-md py-1" type="number" min="0">
                          </div>
                          <div class="flex flex-col gap-1">
                              <span class="text-[10px] text-on-surface-variant">右</span>
                              <input v-model.number="margins.right" class="bg-white border-b border-outline-variant focus:border-primary focus:ring-0 text-ui-label-md py-1" type="number" min="0">
                          </div>
                      </div>
                  </div>
                  <!-- Structure Section -->
                  <div class="pt-4 border-t border-outline-variant">
                      <h3 class="font-sidebar-header text-sidebar-header text-on-surface mb-2">表示設定</h3>
                      <div class="flex flex-col gap-sm">
                          <label class="flex items-center gap-2 p-2 rounded transition-colors"
                                 :class="layoutMode === 'auto' ? 'opacity-50 cursor-not-allowed' : 'hover:bg-white cursor-pointer group'">
                              <input type="checkbox" v-model="showRulers" :disabled="layoutMode === 'auto'" 
                                     class="rounded border-outline-variant text-primary focus:ring-primary"
                                     :class="{'cursor-not-allowed': layoutMode === 'auto'}">
                              <span class="text-ui-label-md">ルーラーを表示</span>
                          </label>
                          <div class="flex items-center gap-2 p-2 hover:bg-white rounded transition-colors group">
                              <label class="flex items-center gap-2 flex-1">
                                  <span class="text-ui-label-md">字下げ幅</span>
                              </label>
                              <div class="flex items-center gap-1">
                                  <input type="number" v-model.number="indentSize" min="0" max="10" class="w-12 bg-white border-b border-outline-variant focus:border-primary focus:ring-0 text-ui-label-md py-0.5 px-1 text-center">
                                  <span class="text-[10px] text-outline">文字</span>
                              </div>
                          </div>
                          <div class="flex items-center gap-2 p-2 hover:bg-white rounded transition-colors cursor-pointer group">
                              <span class="material-symbols-outlined text-outline group-hover:text-primary" data-icon="account_tree">account_tree</span>
                              <span class="text-ui-label-md">アウトラインを表示</span>
                          </div>
                          <div class="flex items-center gap-2 p-2 hover:bg-white rounded transition-colors cursor-pointer group">
                              <span class="material-symbols-outlined text-outline group-hover:text-primary" data-icon="rate_review">rate_review</span>
                              <span class="text-ui-label-md">変更履歴を確認</span>
                          </div>
                      </div>
                  </div>
              </div>
          </div>
      </div>
      <div class="mt-auto p-4 border-t border-outline-variant">
          <button class="w-full bg-primary text-on-primary py-2.5 rounded font-ui-button text-ui-button flex items-center justify-center gap-2 shadow-sm hover:shadow-md transition-all">
              変更を適用
          </button>
          <div class="flex justify-center mt-4">
              <button @click="openHelpModal" class="flex flex-col items-center gap-1 group">
                  <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors" data-icon="help_outline">help_outline</span>
                  <span class="text-[10px] text-on-surface-variant">ヘルプ</span>
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
  openHelpModal
} = useEditorState()
</script>
