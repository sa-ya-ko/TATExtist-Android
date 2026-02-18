export const useEditorSettings = () => {
  const pageSize = useState<'A4' | 'A5' | 'B6'>('pageSize', () => 'A5')
  const selectedFont = useState('selectedFont', () => 'Noto Serif JP')
  const fontSize = useState('fontSize', () => 12)

  const paperDimensions = computed(() => {
    const dimensions = {
      A4: { width: '297mm', height: '210mm' },
      A5: { width: '210mm', height: '148mm' },
      B6: { width: '182mm', height: '128mm' }
    }
    return dimensions[pageSize.value]
  })

  return { pageSize, selectedFont, fontSize, paperDimensions }
}