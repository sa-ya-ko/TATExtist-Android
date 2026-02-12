<template>
  <div class="flex items-center justify-between px-4 py-1 bg-white">
    <div class="flex items-center gap-2">
      <UIcon name="i-heroicons-document-text" class="w-6 h-6 text-blue-600" />
      <input 
        v-model="fileName" 
        class="text-lg px-2 py-1 rounded hover:bg-gray-100 focus:bg-white focus:ring-2 focus:ring-blue-500 outline-none transition"
      />
      <UBadge v-if="isSaving" variant="soft" color="gray" size="xs">保存中...</UBadge>
    </div>
    <div class="flex items-center gap-2">
      <UButton label="共有" color="primary" icon="i-heroicons-share" size="xs" />
      <UAvatar src="https://avatars.githubusercontent.com/u/739984?v=4" size="sm" />
    </div>
  </div>
</template>

<script setup>
const fileName = ref('無題のドキュメント')
const isSaving = ref(false)
</script>