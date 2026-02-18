<template>
  <div class="h-screen flex flex-col overflow-hidden font-sans">
    <ClientOnly>
      <EditorHeader />
      <EditorToolbar :editor="activeEditor" />
      <EditorMain @update:editor="(e) => activeEditor = e" />
    </ClientOnly>
  </div>
</template>

<script setup lang="ts">
const activeEditor = ref<any>(null)
</script>