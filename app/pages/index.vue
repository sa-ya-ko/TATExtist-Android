<template>
  <div class="h-screen flex flex-col bg-gray-200">
    <EditorHeader />
    <EditorToolbar :editor="activeEditor" />
    <main class="flex-1 overflow-y-auto p-12">
      <ClientOnly>
        <EditorMain @update:editor="activeEditor = $event" />
      </ClientOnly>
    </main>
  </div>
</template>

<script setup lang="ts">
const activeEditor = ref(null)
</script>