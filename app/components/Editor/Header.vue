<template>
  <div class="flex items-center justify-between px-4 py-2 bg-white border-b select-none">
    <div class="flex items-center gap-3">
      <UButton icon="i-heroicons-document-text-20-solid" variant="ghost" color="primary" />
      <div class="flex flex-col">
        <input v-model="fileName"
          class="text-sm font-medium px-1 hover:bg-gray-100 rounded focus:bg-white focus:ring-1 focus:ring-blue-500 outline-none transition w-64" />
        <div class="flex gap-4 text-[10px] text-gray-500 px-1">
          <span class="hover:text-gray-800 cursor-pointer">ファイル</span>
          <span class="hover:text-gray-800 cursor-pointer">編集</span>
          <span class="hover:text-gray-800 cursor-pointer">表示</span>
          <span class="hover:text-gray-800 cursor-pointer">挿入</span>
          <span class="hover:text-gray-800 cursor-pointer">ツール</span>
          <span class="hover:text-gray-800 cursor-pointer">ヘルプ</span>
        </div>
      </div>
      <UIcon name="i-heroicons-cloud-arrow-up" class="w-4 h-4 text-gray-400" />
    </div>
    <div class="flex items-center gap-3">
      <UButton label="共有" icon="i-heroicons-share" size="xs" color="primary" variant="solid"
        class="rounded-full px-4" />
      <UAvatar src="https://avatars.githubusercontent.com/u/739984?v=4" size="sm" />
    </div>
  </div>
</template>

<script setup lang="ts">
const fileName = ref('無題の原稿')
</script>