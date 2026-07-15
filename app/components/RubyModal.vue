<template>
  <div class="fixed inset-0 flex items-center justify-center bg-black/30 z-50">
    <div class="bg-white rounded-lg p-4 w-80 shadow-lg">
      <h2 class="text-lg font-medium mb-2">ルビを入力</h2>
      <input v-model="reading" class="border w-full p-2 mb-4" placeholder="例: ふりがな" />
      <div class="flex justify-end gap-2">
        <button @click="cancel" class="px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 rounded">キャンセル</button>
        <button @click="confirm" class="px-3 py-1 text-sm bg-primary text-on-primary rounded">OK</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, defineEmits } from 'vue'

const emit = defineEmits(['confirm', 'cancel'])
const reading = ref('')

function confirm() {
  emit('confirm', reading.value)
  reading.value = ''
}

function cancel() {
  emit('cancel')
  reading.value = ''
}
</script>

<style scoped>
/* Tailwind でレイアウト済み */
</style>
