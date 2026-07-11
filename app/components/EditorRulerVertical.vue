<template>
  <div class="w-6 h-full bg-surface-container-lowest border-l border-outline-variant relative select-none overflow-hidden" v-if="showRulers">
    
    <div v-for="pos in pagePositions" :key="pos.id" class="absolute w-full bg-white" :style="{ top: pos.top + 'px', height: pageHeightPx + 'px' }">
      <!-- Top Margin -->
      <div class="absolute top-0 w-full bg-gray-100 border-b border-gray-300" :style="{ height: marginTopPx + 'px' }"></div>
      
      <!-- Bottom Margin -->
      <div class="absolute bottom-0 w-full bg-gray-100 border-t border-gray-300" :style="{ height: marginBottomPx + 'px' }"></div>
      
      <!-- Ticks -->
      <svg class="absolute inset-0 w-full h-full pointer-events-none">
        <g v-for="tick in ticks" :key="tick.pos">
          <line 
            :x1="tick.isMajor ? 0 : 12" 
            :y1="tick.pos" 
            :x2="24" 
            :y2="tick.pos" 
            stroke="#9ca3af" 
            :stroke-width="tick.isMajor ? 1 : 0.5" 
          />
          <text 
            v-if="tick.isMajor && tick.val > 0" 
            x="10" 
            :y="tick.pos + 3" 
            font-size="9" 
            fill="#6b7280"
            text-anchor="end"
          >{{ tick.val }}</text>
        </g>
      </svg>

      <!-- Top Handle -->
      <div 
        class="absolute left-0 w-full h-2 -translate-y-1 cursor-row-resize hover:bg-primary/50 z-30"
        :style="{ top: marginTopPx + 'px' }"
        @mousedown.prevent="startDrag('top', $event)"
      >
        <div class="w-0 h-0 border-b-[6px] border-b-transparent border-t-[6px] border-t-transparent border-l-[6px] border-l-primary absolute -top-[5px] left-0"></div>
      </div>

      <!-- Bottom Handle -->
      <div 
        class="absolute left-0 w-full h-2 -translate-y-1 cursor-row-resize hover:bg-primary/50 z-30"
        :style="{ bottom: marginBottomPx + 'px' }"
        @mousedown.prevent="startDrag('bottom', $event)"
      >
        <div class="w-0 h-0 border-b-[6px] border-b-transparent border-t-[6px] border-t-transparent border-l-[6px] border-l-primary absolute -bottom-1 left-0"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useEditorState } from '../composables/useEditorState'

const { paperSize, margins, showRulers, pages, isDraggingRuler } = useEditorState()

const props = defineProps<{
  scrollContainer?: HTMLElement | null
}>()

const MM_TO_PX = 3.78
const paperDimensions = {
  'A4': { width: 297, height: 210 },
  'B5': { width: 257, height: 182 }
}

const pageHeightPx = computed(() => paperDimensions[paperSize.value].height * MM_TO_PX)
const marginTopPx = computed(() => margins.value.top * MM_TO_PX)
const marginBottomPx = computed(() => margins.value.bottom * MM_TO_PX)

const ticks = computed(() => {
  const heightMm = paperDimensions[paperSize.value].height
  const res = []
  for (let mm = 0; mm <= heightMm; mm += 2) {
    res.push({
      pos: mm * MM_TO_PX,
      val: mm / 10,
      isMajor: mm % 10 === 0
    })
  }
  return res
})

const pagePositions = ref<{ id: number, top: number }[]>([])

const updatePositions = () => {
  if (!props.scrollContainer) return
  const containerRect = props.scrollContainer.getBoundingClientRect()
  const paperElements = props.scrollContainer.querySelectorAll('.manuscript-paper')
  
  const newPositions: { id: number, top: number }[] = []
  
  paperElements.forEach((el, index) => {
    const rect = el.getBoundingClientRect()
    // Calculate top relative to the scroll container's viewport
    const relativeTop = rect.top - containerRect.top
    
    // We can assume they match the pages array order
    if (pages.value[index]) {
      newPositions.push({
        id: pages.value[index].id,
        top: relativeTop
      })
    }
  })
  
  pagePositions.value = newPositions
}

let animFrame: number
const loop = () => {
  updatePositions()
  animFrame = requestAnimationFrame(loop)
}

onMounted(() => {
  loop()
})

onUnmounted(() => {
  cancelAnimationFrame(animFrame)
})

let isDragging = false
let dragType = ''
let startY = 0
let startVal = 0

const startDrag = (type: string, e: MouseEvent) => {
  isDragging = true
  dragType = type
  isDraggingRuler.value = type
  startY = e.clientY
  startVal = type === 'top' ? margins.value.top : margins.value.bottom
  
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e: MouseEvent) => {
  if (!isDragging) return
  const diffPx = e.clientY - startY
  const diffMm = diffPx / MM_TO_PX
  
  if (dragType === 'top') {
    let newVal = startVal + diffMm
    if (newVal < 5) newVal = 5
    if (newVal > paperDimensions[paperSize.value].height - margins.value.bottom - 20) newVal = paperDimensions[paperSize.value].height - margins.value.bottom - 20
    margins.value.top = Math.round(newVal)
  } else if (dragType === 'bottom') {
    // Moving handle down (diffPx > 0) means decreasing bottom margin
    let newVal = startVal - diffMm
    if (newVal < 5) newVal = 5
    if (newVal > paperDimensions[paperSize.value].height - margins.value.top - 20) newVal = paperDimensions[paperSize.value].height - margins.value.top - 20
    margins.value.bottom = Math.round(newVal)
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
