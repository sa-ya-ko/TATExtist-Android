package com.tatextist.utils

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import com.tatextist.ui.PaperSize

object PrintHelper {

    /**
     * PaperSizeに対応するMediaSizeの識別子（テストおよびCSS照合用）
     */
    fun getMediaSizeId(paperSize: PaperSize): String {
        return when (paperSize) {
            PaperSize.A4 -> "ISO_A4"
            PaperSize.B5 -> "JIS_B5"
            PaperSize.FIT -> "UNKNOWN_LANDSCAPE"
        }
    }

    /**
     * PaperSizeに対応するMediaSizeを取得する
     */
    fun getMediaSizeForPaper(paperSize: PaperSize): PrintAttributes.MediaSize {
        return when (paperSize) {
            PaperSize.A4 -> PrintAttributes.MediaSize.ISO_A4.asLandscape()
            PaperSize.B5 -> PrintAttributes.MediaSize.JIS_B5.asLandscape()
            PaperSize.FIT -> PrintAttributes.MediaSize.UNKNOWN_LANDSCAPE
        }
    }

    /**
     * 用紙サイズに応じたPrintAttributesを構築する
     */
    fun buildPrintAttributes(
        paperSize: PaperSize,
        builder: PrintAttributes.Builder = PrintAttributes.Builder()
    ): PrintAttributes {
        val mediaSize = getMediaSizeForPaper(paperSize)

        return builder
            .setMediaSize(mediaSize)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
    }

    /**
     * WebViewの縦書き原稿をシステム印刷ダイアログ（PDF出力）へ送信する
     */
    fun print(
        context: Context,
        webView: WebView,
        documentTitle: String,
        paperSize: PaperSize
    ): Boolean {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return false
        val jobName = if (documentTitle.isNotBlank() && documentTitle != "無題のドキュメント") {
            "TATExtist - $documentTitle"
        } else {
            "TATExtist - 原稿"
        }

        val printAdapter = webView.createPrintDocumentAdapter(jobName)
        val printAttributes = buildPrintAttributes(paperSize)

        printManager.print(jobName, printAdapter, printAttributes)
        return true
    }
}
