<template>
  <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-[100] transition-opacity" @click="closeWordCountModal">
    <div class="bg-surface rounded-xl shadow-lg w-full max-w-sm overflow-hidden flex flex-col transform transition-transform" @click.stop>
      <!-- Header -->
      <div class="px-6 py-4 border-b border-outline-variant flex items-center justify-between bg-surface-container-low">
        <h3 class="text-ui-title-md font-ui-title-md text-on-surface m-0">文字カウント</h3>
        <button @click="closeWordCountModal" class="text-on-surface-variant hover:text-on-surface transition-colors p-1 rounded-full hover:bg-surface-container">
          <span class="material-symbols-outlined block" style="font-size: 20px;">close</span>
        </button>
      </div>
      
      <!-- Body -->
      <div class="px-6 py-4 flex flex-col gap-4">
        <div class="flex justify-between items-center py-2 border-b border-outline-variant">
          <span class="text-on-surface text-ui-body-md">文字数（スペースなし）</span>
          <span class="text-on-surface font-bold">{{ characterCount }}</span>
        </div>
        <div class="flex justify-between items-center py-2 border-b border-outline-variant">
          <span class="text-on-surface text-ui-body-md">文字数（スペースあり）</span>
          <span class="text-on-surface font-bold">{{ characterCountWithSpaces }}</span>
        </div>

        <label class="flex items-center gap-2 mt-4 cursor-pointer">
          <input type="checkbox" v-model="showWordCountWhileTyping" class="w-4 h-4 text-primary bg-surface border-outline-variant rounded focus:ring-primary focus:ring-2">
          <span class="text-on-surface text-ui-body-md">入力中に文字数を表示する</span>
        </label>
      </div>

      <!-- Footer -->
      <div class="px-6 py-4 flex justify-end gap-3 bg-surface-container-low">
        <button @click="closeWordCountModal" class="px-5 py-2 bg-primary text-on-primary rounded-full hover:bg-primary/90 transition-colors font-ui-label-md">
          OK
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useEditorState } from '../composables/useEditorState'

const {
  characterCount,
  characterCountWithSpaces,
  showWordCountWhileTyping,
  closeWordCountModal
} = useEditorState()
</script>
