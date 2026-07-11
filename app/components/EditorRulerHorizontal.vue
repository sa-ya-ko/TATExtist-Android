<template>
  <div class="h-6 w-full bg-white border-b border-gray-300 flex justify-center sticky top-0 z-20 select-none overflow-hidden" v-if="showRulers">
    <div class="relative h-full" :style="{ width: pageWidthPx + 'px' }">
      <!-- Right Margin (Start of vertical text) -->
      <div class="absolute right-0 top-0 h-full bg-gray-100 border-l border-gray-300" :style="{ width: marginRightPx + 'px' }"></div>
      
      <!-- Left Margin (End of vertical text) -->
      <div class="absolute left-0 top-0 h-full bg-gray-100 border-r border-gray-300" :style="{ width: marginLeftPx + 'px' }"></div>
      
      <!-- Ticks -->
      <svg class="absolute inset-0 w-full h-full pointer-events-none">
        <g v-for="tick in ticks" :key="tick.pos">
          <line 
            :x1="pageWidthPx - tick.pos" 
            :y1="tick.isMajor ? 0 : 12" 
            :x2="pageWidthPx - tick.pos" 
            :y2="24" 
            stroke="#9ca3af" 
            :stroke-width="tick.isMajor ? 1 : 0.5" 
          />
          <text 
            v-if="tick.isMajor && tick.val > 0" 
            :x="pageWidthPx - tick.pos - 2" 
            y="10" 
            font-size="9" 
            fill="#6b7280"
            text-anchor="end"
          >{{ tick.val }}</text>
        </g>
      </svg>

      <!-- Right Handle -->
      <div 
        class="absolute top-0 h-full w-2 -translate-x-1 cursor-col-resize hover:bg-primary/50 z-30"
        :style="{ right: marginRightPx + 'px' }"
        @mousedown.prevent="startDrag('right', $event)"
      >
        <div class="w-0 h-0 border-l-[6px] border-l-transparent border-r-[6px] border-r-transparent border-t-[6px] border-t-primary absolute -bottom-1 -left-[5px]"></div>
      </div>

      <!-- Left Handle -->
      <div 
        class="absolute top-0 h-full w-2 -translate-x-1 cursor-col-resize hover:bg-primary/50 z-30"
        :style="{ left: marginLeftPx + 'px' }"
        @mousedown.prevent="startDrag('left', $event)"
      >
        <div class="w-0 h-0 border-l-[6px] border-l-transparent border-r-[6px] border-r-transparent border-t-[6px] border-t-primary absolute -bottom-1 -left-[5px]"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useEditorState } from '../composables/useEditorState'

const { paperSize, margins, showRulers, isDraggingRuler } = useEditorState()

const MM_TO_PX = 3.78
const paperDimensions = {
  'A4': { width: 297, height: 210 },
  'B5': { width: 257, height: 182 }
}

const pageWidthPx = computed(() => paperDimensions[paperSize.value].width * MM_TO_PX)
const marginRightPx = computed(() => margins.value.right * MM_TO_PX)
const marginLeftPx = computed(() => margins.value.left * MM_TO_PX)

// Generate ticks every 10mm (major) and 2mm (minor)
const ticks = computed(() => {
  const widthMm = paperDimensions[paperSize.value].width
  const res = []
  for (let mm = 0; mm <= widthMm; mm += 2) {
    res.push({
      pos: mm * MM_TO_PX,
      val: mm / 10,
      isMajor: mm % 10 === 0
    })
  }
  return res
})

let isDragging = false
let dragType = ''
let startX = 0
let startVal = 0

const startDrag = (type: string, e: MouseEvent) => {
  isDragging = true
  dragType = type
  isDraggingRuler.value = type as any
  startX = e.clientX
  startVal = type === 'right' ? margins.value.right : margins.value.left
  
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e: MouseEvent) => {
  if (!isDragging) return
  const diffPx = e.clientX - startX
  const diffMm = diffPx / MM_TO_PX
  
  if (dragType === 'right') {
    let newVal = startVal - diffMm
    if (newVal < 5) newVal = 5
    if (newVal > paperDimensions[paperSize.value].width - margins.value.left - 20) newVal = paperDimensions[paperSize.value].width - margins.value.left - 20
    margins.value.right = Math.round(newVal)
  } else if (dragType === 'left') {
    let newVal = startVal + diffMm
    if (newVal < 5) newVal = 5
    if (newVal > paperDimensions[paperSize.value].width - margins.value.right - 20) newVal = paperDimensions[paperSize.value].width - margins.value.right - 20
    margins.value.left = Math.round(newVal)
  }
}

const stopDrag = () => {
  isDragging = false
  dragType = ''
  isDraggingRuler.value = null
  
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}
</script>
