<script setup lang="ts">
const props = defineProps(['editor'])
const { pageSize, selectedFont } = useEditorSettings()

// 編集コマンドの実行関数
const toggleBold = () => props.editor?.chain().focus().toggleBold().run()
const toggleHeading = () => props.editor?.chain().focus().toggleHeading({ level: 1 }).run()
</script>

<template>
  <div class="flex items-center gap-2 p-2 border-b bg-gray-50 overflow-x-auto">
    <USelectMenu v-model="pageSize" :options="['A4', 'A5', 'B6']" class="w-24" />
    <UDivider vertical />
    <UButtonGroup>
      <UButton 
        icon="i-heroicons-bold-20-solid" 
        :variant="editor?.isActive('bold') ? 'solid' : 'ghost'"
        @click="toggleBold" 
      />
      <UButton 
        icon="i-heroicons-h1-20-solid" 
        :variant="editor?.isActive('heading') ? 'solid' : 'ghost'"
        @click="toggleHeading" 
      />
    </UButtonGroup>
  </div>
</template>