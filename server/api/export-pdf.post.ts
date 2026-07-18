import puppeteer from 'puppeteer'
import { defineEventHandler, readBody, createError } from 'h3'

export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  const htmlContent = body.html || ''

  const fullHtml = `
    <!DOCTYPE html>
    <html lang="ja">
    <head>
      <meta charset="UTF-8">
      <link href="https://fonts.googleapis.com/css2?family=Noto+Serif+JP:wght@400;700&display=swap" rel="stylesheet">
      <style>
        body {
          margin: 0;
          padding: 0;
          font-family: 'Noto Serif JP', serif;
          writing-mode: vertical-rl;
          line-height: 1.8;
          font-size: 16px;
        }
        .writing-vertical {
          writing-mode: vertical-rl;
        }
        h1, h2, h3, h4 { font-weight: bold; }
        h1 { font-size: 2em; margin-bottom: 0.67em; }
        h2 { font-size: 1.5em; margin-bottom: 0.83em; }
        h3 { font-size: 1.17em; margin-bottom: 1em; }
        h4 { font-size: 1em; margin-bottom: 1.33em; }
        p:empty::before { content: '\\200B'; }
        hr { display: none; }
      </style>
    </head>
    <body>
      ${htmlContent}
    </body>
    </html>
  `

  let browser
  try {
    browser = await puppeteer.launch({
      headless: true,
      args: ['--no-sandbox', '--disable-setuid-sandbox']
    })
    const page = await browser.newPage()
    await page.setContent(fullHtml, { waitUntil: 'networkidle0' })
    
    // PDF化
    const pdfBuffer = await page.pdf({
      format: 'A4',
      printBackground: true,
      margin: { top: '30mm', right: '30mm', bottom: '30mm', left: '30mm' }
    })

    // レスポンスヘッダーの設定
    event.node.res.setHeader('Content-Type', 'application/pdf')
    event.node.res.setHeader('Content-Disposition', 'attachment; filename="document.pdf"')
    
    return pdfBuffer

  } catch (error) {
    console.error('PDF generation error:', error)
    throw createError({
      statusCode: 500,
      statusMessage: 'Failed to generate PDF'
    })
  } finally {
    if (browser) {
      await browser.close()
    }
  }
})
