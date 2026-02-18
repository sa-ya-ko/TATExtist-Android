<template>
  <div class="flex items-center gap-2 px-4 py-1.5 bg-[#f9fbfd] border-b overflow-x-auto shadow-sm">
    <template v-if="editor">
      <USelectMenu v-model="currentFormatValue" :items="formatOptions" value-key="value" label-key="label" class="w-36"
        size="sm" />

      <USeparator orientation="vertical" class="h-6 mx-1" />
      <USelectMenu v-model="pageSize" :items="pageSizeOptionsObj" value-key="value" label-key="label" class="w-24"
        size="sm" />

      <USeparator orientation="vertical" class="h-6 mx-1" />
      <USelectMenu v-model="selectedFont" :items="fontOptionsObj" value-key="value" label-key="label" class="w-44"
        size="sm" />

      <USeparator orientation="vertical" class="h-6 mx-1" />
      <div class="flex items-center gap-1">
        <UInputNumber v-model="fontSize" class="w-16" size="xs" :min="6" :max="72" />
        <UButton icon="i-heroicons-plus-small" variant="ghost" color="neutral" size="xs" />
      </div>

      <USeparator orientation="vertical" class="h-6 mx-1" />
      <UButton icon="i-heroicons-bold-20-solid" :color="editor?.isActive('bold') ? 'primary' : 'neutral'"
        variant="ghost" @click="editor?.chain().focus().toggleBold().run()" />
    </template>
  </div>
</template>

<script setup lang="ts">
const props = defineProps(['editor'])
const { pageSize, selectedFont, fontSize } = useEditorSettings()

const formatOptions = [
  { label: '標準テキスト', value: 'p' },
  { label: '見出し 1', value: 'h1' },
  { label: '見出し 2', value: 'h2' },
  { label: '見出し 3', value: 'h3' },
  { label: '見出し 4', value: 'h4' },
]

const pageSizeOptionsObj = [
  { label: 'A4', value: 'A4' },
  { label: 'A5', value: 'A5' },
  { label: 'B6', value: 'B6' },
]

const fontOptionsObj = [
  { label: 'Noto Serif JP', value: 'Noto Serif JP' },
  { label: 'Sawarabi Mincho', value: 'Sawarabi Mincho' },
]

const currentFormatValue = ref('p')
let isUpdatingFromEditor = false

const updateFontSize = (val: number) => {
  const clamped = Math.max(6, Math.min(72, val))
  fontSize.value = clamped
  props.editor?.chain().focus().setFontSize(`${clamped}pt`).run()
}

// 書式の変化を監視
watch(currentFormatValue, (val) => {
  if (isUpdatingFromEditor || !props.editor) return
  if (val === 'p') {
    props.editor.chain().focus().setParagraph().run()
  } else {
    const level = parseInt(val.replace('h', '')) as 1 | 2 | 3 | 4
    props.editor.chain().focus().toggleHeading({ level }).run()
  }
})

// エディターの状態を検知
watch(() => props.editor, (newEditor) => {
  if (!newEditor) return

  // 重複登録を避けるため、一旦オフにするか、外部で管理するのが望ましいが
  // Nuxt HMR 等での対応として簡易的な登録を行う
  newEditor.on('selectionUpdate', () => {
    isUpdatingFromEditor = true
    const level = ([1, 2, 3, 4] as const).find(l => newEditor.isActive('heading', { level: l }))
    currentFormatValue.value = level ? `h${level}` : 'p'

    const attrs = newEditor.getAttributes('textStyle')
    if (attrs.fontSize) {
      fontSize.value = parseInt(attrs.fontSize)
    }

    // マイクロタスク終了後にフラグを戻す
    Promise.resolve().then(() => {
      isUpdatingFromEditor = false
    })
  })
}, { immediate: true })
</script>