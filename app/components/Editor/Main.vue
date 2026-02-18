<script setup lang="ts">
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { TextStyle, FontSize } from '@tiptap/extension-text-style'

const emit = defineEmits(['update:editor'])
const { paperDimensions, selectedFont } = useEditorSettings()

const editor = useEditor({
  extensions: [
    StarterKit,
    TextStyle,
    FontSize,
  ],
  content: '<p>縦書きの世界へようこそ。</p>',
  editorProps: {
    attributes: { class: 'focus:outline-none tategaki-content' }
  },
  onUpdate: ({ editor }) => {
    // コンテンツが更新された際の処理（必要に応じて）
  },
  onCreate: ({ editor: editorInstance }) => {
    emit('update:editor', editorInstance)
  }
})
</script>

<template>
  <div class="editor-paper shadow-2xl mx-auto bg-white transition-all duration-500 overflow-hidden" :style="{
    width: paperDimensions.width,
    height: paperDimensions.height,
    fontFamily: selectedFont,
  }">
    <!-- editor が準備できるまでガード -->
    <editor-content v-if="editor" :editor="editor" />
  </div>
</template>

<style scoped>
.editor-paper {
  padding: 20mm;
}

:deep(.tategaki-content) {
  writing-mode: vertical-rl;
  height: 100%;
  width: 100%;
  line-height: 2.2;
}

/* Tiptap フォーカス時の輪郭を消す */
:deep(.ProseMirror:focus) {
  outline: none;
}
</style>