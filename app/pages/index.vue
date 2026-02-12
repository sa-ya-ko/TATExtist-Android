<template>
  <div class="h-screen flex flex-col overflow-hidden bg-gray-100">
    <header class="bg-white border-b p-2 flex flex-col gap-2">
      <div class="flex items-center justify-between px-4">
        <h1 class="text-sm font-bold">無題のドキュメント</h1>
        <div class="flex gap-2">
          <UButton icon="i-heroicons-cloud-arrow-up" variant="ghost" color="gray" />
          <UButton label="共有" color="primary" variant="solid" size="xs" />
        </div>
      </div>
      
      <div class="flex items-center gap-1 border-t pt-2 overflow-x-auto">
        <UButtonGroup size="sm" variant="ghost">
          <UButton icon="i-heroicons-bold-20-solid" color="gray" />
          <UButton icon="i-heroicons-italic-20-solid" color="gray" />
          <UButton icon="i-heroicons-language-20-solid" color="gray" /> </UButtonGroup>
        <UDivider vertical />
        <USelectMenu v-model="selectedFont" :options="fonts" class="w-32" />
      </div>
    </header>

    <main class="flex-1 overflow-y-auto p-10 flex justify-center">
      <ClientOnly>
        <TheEditor />
      </ClientOnly>
    </main>
  </div>
</template>
<script lang="ts" setup>

</script>

<style>

</style>