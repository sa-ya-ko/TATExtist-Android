<template>
  <div class="min-h-screen bg-gray-100">
    
    <VerticalEditor />
  </div>
</template>

<script setup lang="ts">
import VerticalEditor from '@/components/VerticalEditor.vue'

useSchemaOrg([
  defineWebSite({
    name: 'TATExtist'
  }),
  defineWebPage({
    name: '縦書きエディタ'
  }),
  defineSoftwareApp({
    name: 'TATExtist',
    applicationCategory: 'BrowserApplication',
    operatingSystem: 'Any',
    offers: {
      price: '0',
      priceCurrency: 'JPY'
    }
  })
])
</script>
