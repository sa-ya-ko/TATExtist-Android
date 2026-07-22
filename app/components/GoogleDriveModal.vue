<template>
  <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" @click.self="closeModal">
    <div class="bg-surface w-full max-w-md rounded-lg shadow-xl flex flex-col max-h-[80vh]">
      <div class="p-6 border-b border-outline-variant flex justify-between items-center">
        <h2 class="text-xl font-bold text-on-surface">Google Driveから開く</h2>
        <button @click="closeModal" class="text-on-surface-variant hover:bg-surface-container-low p-2 rounded-full transition-colors">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
      
      <div class="p-6 overflow-y-auto flex-1">
        <div v-if="isLoadingFiles" class="flex justify-center items-center py-8">
          <span class="material-symbols-outlined animate-spin text-primary text-3xl" style="animation: spin 1s linear infinite;">sync</span>
        </div>
        <div v-else-if="driveFiles.length === 0" class="text-center py-8 text-on-surface-variant">
          保存されているファイルが見つかりません。
        </div>
        <div v-else class="flex flex-col gap-2">
          <button 
            v-for="file in driveFiles" 
            :key="file.id"
            @click="loadFromDrive(file.id)"
            class="flex items-center gap-3 p-3 rounded-md hover:bg-surface-container-low transition-colors text-left border border-outline-variant hover:border-primary"
          >
            <span class="material-symbols-outlined text-primary" style="font-size: 24px;">description</span>
            <div class="flex-1 min-w-0">
              <div class="text-ui-label-md text-on-surface truncate">{{ file.name }}</div>
              <div class="text-xs text-on-surface-variant">{{ formatDate(file.modifiedTime) }}</div>
            </div>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useGoogleDrive } from '../composables/useGoogleDrive'

const { isDriveModalOpen, driveFiles, isLoadingFiles, loadFromDrive } = useGoogleDrive()

const closeModal = () => {
  isDriveModalOpen.value = false
}

const formatDate = (dateString: string) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
