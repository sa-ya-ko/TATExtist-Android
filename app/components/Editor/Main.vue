<script setup lang="ts">
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'

const props = defineProps(['modelValue'])
const emit = defineEmits(['update:editor'])
const { paperDimensions, selectedFont } = useEditorSettings()

const editor = useEditor({
  extensions: [StarterKit],
  content: '<p>縦書きの世界へようこそ。</p>',
  editorProps: {
    attributes: { class: 'focus:outline-none tategaki-content' }
  },
  onUpdate: () => { /* 保存処理などをここに追加予定 */ },
  onCreate: ({ editor }) => {
    emit('update:editor', editor) // 親にeditorインスタンスを渡す
  }
})
</script>

<template>
  <div 
    class="editor-paper shadow-2xl mx-auto bg-white transition-all duration-500"
    :style="{ width: paperDimensions.width, height: paperDimensions.height }"
  >
    <editor-content :editor="editor" :class="selectedFont.replace(/\s+/g, '-').toLowerCase()" />
  </div>
</template>

<style scoped>
.editor-paper { padding: 20mm; overflow: hidden; }
:deep(.tategaki-content) {
  writing-mode: vertical-rl;
  height: 100%;
  width: 100%;
  line-height: 2.2;
}
.noto-serif-jp { font-family: 'Noto Serif JP', serif; }
</style>