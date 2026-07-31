import puppeteer from 'puppeteer-core'
import chromium from '@sparticuz/chromium'
import { defineEventHandler, readBody, createError } from 'h3'

export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  const htmlContent = body.html || ''
  const paperSize = body.paperSize || 'A4'
  const margins = body.margins || { top: 30, right: 30, bottom: 30, left: 30 }

  const fullHtml = `
    <!DOCTYPE html>
    <html lang="ja">
    <head>
      <meta charset="UTF-8">
      <link href="https://fonts.googleapis.com/css2?family=Noto+Serif+JP:wght@400;700&display=swap" rel="stylesheet">
      <style>
        :root {
          /* エディタと完全に同じデフォルトカラーを定義 */
          --color-on-surface: #091d2e;
        }
        * {
          -webkit-print-color-adjust: exact;
          print-color-adjust: exact;
          box-sizing: border-box;
          -webkit-font-smoothing: antialiased;
          -moz-osx-font-smoothing: grayscale;
          text-rendering: geometricPrecision;
          font-synthesis: none; /* Prevent faux bold/italic which causes grey rasterization */
        }
        body {
          margin: 0;
          padding: 0;
          font-family: 'Noto Serif JP', serif;
          writing-mode: vertical-rl;
          line-height: 1.8;
          font-size: 16px;
          color: var(--color-on-surface); /* デフォルトを真っ黒ではなくエディタ色に */
        }
        .writing-vertical {
          writing-mode: vertical-rl;
        }
        p, blockquote { margin: 0; padding: 0; }
        
        /* 箇条書き: PDF出力時に確実にかつ自然に中点とテキストを配置 */
        ul, ol { 
          margin: 0; 
          padding: 0; 
          list-style: none;
          margin-block-start: 0.5em; 
          margin-block-end: 0.5em; 
        }
        li { 
          margin: 0; 
          padding: 0; 
          margin-inline-end: 0.5em; 
          list-style: none;
          display: inline-block;
          writing-mode: vertical-rl;
        }
        li div, li p, li span {
          display: inline;
          margin: 0;
          padding: 0;
        }
        li::before { 
          content: '・'; 
          display: inline;
          line-height: inherit;
        }

        h1, h2, h3, h4 { font-weight: bold; margin: 0; padding: 0; break-inside: avoid; page-break-inside: avoid; }
        h1 { font-size: 2em; margin-block-end: 0.67em; }
        h2 { font-size: 1.5em; margin-block-end: 0.83em; }
        h3 { font-size: 1.17em; margin-block-end: 1em; }
        h4 { font-size: 1em; margin-block-end: 1.33em; }
        
        p:empty::before { content: '\\200B'; }
        hr { border: none; margin: 0; padding: 0; }
        hr.manual-page-break { page-break-after: always; break-after: page; visibility: hidden; margin: 0; padding: 0; border: none; }
        
        /* エディタ（tailwind.css）と完全同一の装飾スタイル（!importantは使用しない） */
        u, [style*="underline"] { 
          text-decoration: underline;
          text-decoration-color: var(--color-on-surface);
        }
        ruby { ruby-align: center; }
        ruby > rt, ruby > rt span, ruby > rt font { 
          color: var(--color-on-surface);
          font-size: 0.5em; 
          font-family: inherit;
        }
        b, strong {
          font-weight: bold;
        }
        /* 斜体の傾きによる行崩れ・予期せぬ自動改行を防止 */
        i, em {
          font-style: italic;
          line-height: inherit;
        }
      </style>
    </head>
    <body>
      ${htmlContent}
    </body>
    </html>
  `

  let browser
  try {
    const isDev = process.env.NODE_ENV === 'development'

    browser = await puppeteer.launch({
      args: isDev ? [] : chromium.args,
      defaultViewport: chromium.defaultViewport,
      executablePath: isDev ? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' : await chromium.executablePath(),
      headless: isDev ? true : chromium.headless,
      ignoreHTTPSErrors: true
    })
    const page = await browser.newPage()
    await page.setContent(fullHtml, { waitUntil: 'networkidle0' })
    
    await page.evaluateHandle('document.fonts.ready')
    
    const pdfOptions: any = {
      printBackground: true,
      displayHeaderFooter: false,
      margin: {
        top: `${margins.top}mm`,
        right: `${margins.right}mm`,
        bottom: `${margins.bottom}mm`,
        left: `${margins.left}mm`
      }
    }
    
    if (paperSize === 'B5') {
      pdfOptions.width = '257mm'
      pdfOptions.height = '182mm'
    } else {
      pdfOptions.width = '297mm'
      pdfOptions.height = '210mm'
    }

    const pdfBuffer = await page.pdf(pdfOptions)

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
