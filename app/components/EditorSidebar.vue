<template>
  <aside class="print-hidden fixed right-0 top-toolbar-height h-[calc(100vh-56px)] w-sidebar-width flex flex-col bg-surface-container-low border-l border-outline-variant z-40">
      <div class="p-4 flex flex-col gap-lg">
          <div>
              <h2 class="font-sidebar-header text-sidebar-header text-on-surface mb-4">ページ設定</h2>
              <div class="space-y-6">
                  <!-- Paper Size -->
                  <div class="flex flex-col gap-sm">
                      <label class="text-[10px] uppercase font-bold text-outline">用紙サイズ</label>
                      <select v-model="paperSize" class="w-full bg-white border-b border-outline-variant focus:border-primary focus:ring-0 text-ui-label-md py-2 px-0">
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
                      <h3 class="font-sidebar-header text-sidebar-header text-on-surface mb-2">ドキュメントの構造</h3>
                      <div class="flex flex-col gap-sm">
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
          <div class="flex justify-around mt-4">
              <button class="flex flex-col items-center gap-1 group">
                  <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors" data-icon="help_outline">help_outline</span>
                  <span class="text-[10px] text-on-surface-variant">ヘルプ</span>
              </button>
              <button class="flex flex-col items-center gap-1 group">
                  <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors" data-icon="chat_bubble_outline">chat_bubble_outline</span>
                  <span class="text-[10px] text-on-surface-variant">フィードバック</span>
              </button>
          </div>
      </div>
  </aside>
</template>

<script setup lang="ts">
// Note: @change handlers for updateMargins and updatePaperSize were removed
// because Vue v-model already handles updating the reactive state, and the
// empty functions were causing warnings.
const { paperSize, margins } = useEditorState()
</script>
