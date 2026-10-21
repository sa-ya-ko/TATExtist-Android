package com.tatextist.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView
import com.tatextist.utils.HtmlTextConverter

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VerticalEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    rubyRequest: String? = null,
    onRubyApplied: () -> Unit = {},
    formatCommand: String? = null,
    onFormatApplied: () -> Unit = {},
    fontSize: Int = 20,
    fontFamily: EditorFontFamily = EditorFontFamily.SERIF,
    fontApplyRequest: FontApplyRequest? = null,
    onFontApplyConsumed: () -> Unit = {},
    onStyleDetected: (EditorFontFamily, Int) -> Unit = { _, _ -> },
    onFormatStateDetected: (Boolean, Boolean, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
    onColorDetected: (String?) -> Unit = {},
    isRubyListActive: Boolean = false,
    targetRubyIndex: Int? = null,
    onRubyJumpConsumed: () -> Unit = {},
    indentRequest: Int? = null,
    onIndentConsumed: () -> Unit = {},
    indentWidth: Float = 0.0f,
    paperSize: PaperSize = PaperSize.FIT,
    margins: EditorMargins = EditorMargins(),
    documentTitle: String = "無題のドキュメント",
    printRequest: Long? = null,
    onPrintConsumed: () -> Unit = {},
    paragraphStyleRequest: ParagraphStyle? = null,
    onParagraphStyleConsumed: () -> Unit = {},
    onParagraphStyleDetected: (String?) -> Unit = {},
    showFontSettingsBottomSheet: Boolean = false,
    modifier: Modifier = Modifier
) {

    val surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
    val onSurfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val surfaceColorCss = String.format("#%06X", 0xFFFFFF and surfaceColor.toArgb())
    val textColorCss = String.format("#%06X", 0xFFFFFF and onSurfaceColor.toArgb())
    val fontFamilyCss = if (fontFamily == EditorFontFamily.SERIF) "serif, 'Noto Serif JP'" else "sans-serif, 'Noto Sans JP'"
    val paperClass = when (paperSize) {
        PaperSize.A4 -> "paper-a4"
        PaperSize.B5 -> "paper-b5"
        PaperSize.FIT -> "paper-fit"
    }
    val printPageSizeCss = when (paperSize) {
        PaperSize.A4 -> "A4 landscape"
        PaperSize.B5 -> "B5 landscape"
        PaperSize.FIT -> "auto landscape"
    }

    val lastHandledPrintIdRef = remember { arrayOf<Long?>(null) }
    val lastThemeSignatureRef = remember { arrayOf<String?>(null) }
    val lastBottomSheetOpenRef = remember { arrayOf(false) }

    // WebViewとComposeのテキスト状態を同期するための変数（ループ防止用）
    // mutableStateOfを使うと不要な再描画が走り、IMEの漢字変換（コンポジション）が強制終了されるため、配列で状態を持たせます
    val lastKnownTextRef = remember { arrayOf(value.text) }
    val lastHandledRequestIdRef = remember { arrayOf<Long?>(null) }
    val lastHandledRubyRequestRef = remember { arrayOf<String?>(null) }
    val isInternalUpdateRef = remember { arrayOf(false) }

    // 3. ComposeとWebViewの通信（ブリッジ）
    val jsBridge = remember(onValueChange, onStyleDetected, onFormatStateDetected, onColorDetected) {
        object {
            // メインスレッド（UI操作担当）に処理を委譲するためのハンドラー
            private val handler = android.os.Handler(android.os.Looper.getMainLooper())
            
            @JavascriptInterface
            fun onTextChanged(newHtml: String) {
                // WebViewの裏方スレッドから呼ばれるため、handlerを使ってメインスレッドで実行させる
                handler.post {
                    val plainText = HtmlTextConverter.convertFromHtml(newHtml)
                    lastKnownTextRef[0] = plainText // WebViewからの更新を記録（再描画はトリガーしない）
                    isInternalUpdateRef[0] = true
                    onValueChange(TextFieldValue(text = plainText))
                }
            }

            @JavascriptInterface
            fun onSelectionStyle(familyStr: String, sizePx: Int) {
                handler.post {
                    val family = if (familyStr.contains("sans-serif")) EditorFontFamily.SANS_SERIF else EditorFontFamily.SERIF
                    onStyleDetected(family, sizePx)
                }
            }

            @JavascriptInterface
            fun onFormatState(isBold: Boolean, isItalic: Boolean, isUnderline: Boolean, isBullet: Boolean) {
                handler.post {
                    onFormatStateDetected(isBold, isItalic, isUnderline, isBullet)
                }
            }

            @JavascriptInterface
            fun onSelectionColor(hexColor: String?) {
                handler.post {
                    val color = if (hexColor.isNullOrEmpty() || hexColor == "null") null else hexColor
                    onColorDetected(color)
                }
            }

            @JavascriptInterface
            fun onParagraphStyle(tagName: String?) {
                handler.post {
                    onParagraphStyleDetected(tagName)
                }
            }
        }
    }

    // 2. WebView用エディタの実装
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                // WebViewのデバッグモードを有効にする（ChromeのInspect用）
                WebView.setWebContentsDebuggingEnabled(true)
                
                // 背景色をComposeのsurfaceColorと同期
                setBackgroundColor(surfaceColor.toArgb())
                
                settings.apply {
                    javaScriptEnabled = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    domStorageEnabled = true
                }
                
                isFocusable = true
                isFocusableInTouchMode = true
                
                addJavascriptInterface(jsBridge, "AndroidBridge")
                webViewClient = WebViewClient()
                
                // 縦書きエディタ用のHTMLテンプレート
                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                        <style>
                            /* 共通スタイル: 文字装飾・ルビ */
                            rt {
                                font-weight: normal;
                                font-style: normal;
                                text-decoration: none;
                                -webkit-user-select: none;
                                user-select: none;
                                pointer-events: none;
                            }

                            /* 画面表示時のレイアウト基本設定 */
                            @media screen {
                                html, body {
                                    margin: 0;
                                    padding: 0;
                                    width: 100vw;
                                    overflow: hidden;
                                }
                                body {
                                    display: flex;
                                    justify-content: center;
                                    align-items: center;
                                    height: 100vh;
                                    box-sizing: border-box;
                                }
                                #editor {
                                    writing-mode: vertical-rl;
                                    box-sizing: border-box;
                                    font-size: ${fontSize}px;
                                    line-height: 2;
                                    font-family: $fontFamilyCss;
                                    outline: none;
                                    overflow-x: auto;
                                    overflow-y: hidden;
                                    word-break: break-all;
                                    white-space: pre-wrap;
                                    transition: background-color 0.25s ease-out, color 0.25s ease-out;
                                }
                                #editor.paper-fit {
                                    width: 100vw;
                                    height: 100vh;
                                    box-shadow: none;
                                    border: none;
                                    border-radius: 0;
                                }
                                #editor.paper-a4 {
                                    height: calc(100vh - 32px);
                                    aspect-ratio: 297 / 210;
                                    border-radius: 6px;
                                    border: 1px solid rgba(128, 128, 128, 0.25);
                                    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
                                }
                                #editor.paper-b5 {
                                    height: calc(100vh - 32px);
                                    aspect-ratio: 257 / 182;
                                    border-radius: 6px;
                                    border: 1px solid rgba(128, 128, 128, 0.25);
                                    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
                                }
                                .highlight-all-rubies ruby {
                                    background-color: rgba(255, 235, 59, 0.45);
                                    border-radius: 4px;
                                    box-shadow: 0 0 0 2px rgba(255, 235, 59, 0.45);
                                }
                                #editor ruby rt {
                                    color: $textColorCss;
                                }
                                #editor u {
                                    text-decoration-color: $textColorCss;
                                }
                                #editor .bullet-line {
                                    padding-top: 2.5em;
                                    text-indent: -1em;
                                }
                                #editor h1, #editor h2, #editor h3, #editor h4 {
                                    margin: 0;
                                    padding: 0;
                                    font-weight: bold;
                                    display: block;
                                }
                                #editor h1 { font-size: 1.75em; line-height: 1.6; }
                                #editor h2 { font-size: 1.4em; line-height: 1.6; }
                                #editor h3 { font-size: 1.2em; line-height: 1.7; }
                                #editor h4 { font-size: 1.1em; line-height: 1.8; }
                                #editor h1 ruby, #editor h2 ruby, #editor h3 ruby, #editor h4 ruby {
                                    font-size: inherit;
                                }
                            }
                        </style>
                        <style id="dynamic-editor-style">
                            @media screen {
                                html, body {
                                    background-color: $surfaceColorCss;
                                }
                                #editor {
                                    background-color: $surfaceColorCss;
                                    color: $textColorCss;
                                    padding: ${margins.top}px ${margins.right}px ${margins.bottom}px ${margins.left}px;
                                }
                            }
                            @media print {
                                @page {
                                    size: $printPageSizeCss;
                                    margin: ${margins.top}px ${margins.right}px ${margins.bottom}px ${margins.left}px;
                                }
                                html, body {
                                    display: block;
                                    writing-mode: vertical-rl;
                                    direction: ltr;
                                    margin: 0;
                                    padding: 0;
                                    width: 100%;
                                    height: 100%;
                                    background-color: #ffffff;
                                    overflow: visible;
                                }
                                #editor {
                                    display: block;
                                    writing-mode: vertical-rl;
                                    direction: ltr;
                                    box-sizing: border-box;
                                    margin: 0;
                                    padding: 0;
                                    width: auto;
                                    height: 100%;
                                    background-color: #ffffff;
                                    color: #000000;
                                    font-family: $fontFamilyCss;
                                    font-size: ${fontSize}px;
                                    line-height: 2;
                                    outline: none;
                                    border: none;
                                    border-radius: 0;
                                    box-shadow: none;
                                    overflow: visible;
                                    word-break: break-all;
                                    white-space: pre-wrap;
                                    -webkit-print-color-adjust: exact;
                                    print-color-adjust: exact;
                                }
                                #editor .bullet-line {
                                    padding-top: 2.5em;
                                    text-indent: -1em;
                                }
                                #editor h1, #editor h2, #editor h3, #editor h4 {
                                    margin: 0;
                                    padding: 0;
                                    font-weight: bold;
                                    display: block;
                                }
                                #editor h1 { font-size: 1.75em; line-height: 1.6; }
                                #editor h2 { font-size: 1.4em; line-height: 1.6; }
                                #editor h3 { font-size: 1.2em; line-height: 1.7; }
                                #editor h4 { font-size: 1.1em; line-height: 1.8; }
                                #editor h1 ruby, #editor h2 ruby, #editor h3 ruby, #editor h4 ruby {
                                    font-size: inherit;
                                }
                            }
                        </style>
                    </head>
                    <body>
                        <div id="editor" class="$paperClass" contenteditable="true">${HtmlTextConverter.convertToHtml(value.text)}</div>
                        <script>
                            const editor = document.getElementById('editor');
                            
                            // 高さをブラウザの物理ピクセルに合わせて絶対固定する関数（Flexboxバグ回避用）
                            function setHeight() {
                                const h = window.innerHeight;
                                document.body.style.height = h + 'px';
                                if (editor.classList.contains('paper-a4') || editor.classList.contains('paper-b5')) {
                                    editor.style.height = Math.max(100, h - 32) + 'px';
                                } else {
                                    editor.style.height = h + 'px';
                                }
                            }
                            // 起動時とリサイズ（キーボード表示等）時に高さを再計算
                            setHeight();
                            window.addEventListener('resize', setHeight);

                            editor.addEventListener('input', function(e) {
                                // 親文字（漢字部分）が消去されて空になった孤立 ruby タグをクリーンアップ
                                var rubies = editor.querySelectorAll('ruby');
                                rubies.forEach(function(rb) {
                                    var clone = rb.cloneNode(true);
                                    var rts = clone.querySelectorAll('rt');
                                    rts.forEach(function(rt) { rt.remove(); });
                                    var baseText = clone.textContent.replace(/\u200B/g, '').trim();
                                    if (baseText.length === 0) {
                                        rb.remove();
                                    }
                                });

                                // 文字を消して中身が空になった装飾タグ（u, b, i等）をクリーンアップ
                                var emptyTags = editor.querySelectorAll('u, b, i, strong, em');
                                emptyTags.forEach(function(tag) {
                                    var text = tag.textContent.replace(/\u200B/g, '').trim();
                                    if (text.length === 0) {
                                        tag.remove();
                                    }
                                });

                                AndroidBridge.onTextChanged(editor.innerHTML);
                            });

                            // インデント幅に応じたスペース文字列（全角＋半角）を生成する関数
                            function getIndentSpaces(w) {
                                if (w <= 0) return '';
                                var full = Math.floor(w);
                                var hasHalf = (w - full) >= 0.4;
                                return "　".repeat(full) + (hasHalf ? " " : "");
                            }

                            // キャレットが行（editor直下またはdiv要素内）の先頭にあるかを正確に判定する関数
                            function isCaretAtLineStart(node, offset) {
                                if (!node) return false;
                                if (node === editor) {
                                    return offset === 0;
                                }
                                // 要素ノードの場合（空のdivや改行直後のdiv内など）
                                if (node.nodeType === 1) {
                                    if (offset === 0) return true;
                                    var textBefore = (node.textContent || '').substring(0, offset);
                                    return textBefore.trim().length === 0;
                                }
                                // テキストノードの場合
                                if (node.nodeType === 3) {
                                    var textBefore = (node.nodeValue || '').substring(0, offset);
                                    if (textBefore.trim().length > 0) return false;
                                    
                                    // このテキストノードの前に兄弟要素やテキストがないか親行要素まで遡る
                                    var cur = node;
                                    while (cur && cur.parentElement !== editor && cur.parentElement) {
                                        var prev = cur.previousSibling;
                                        while (prev) {
                                            if ((prev.textContent || '').trim().length > 0) return false;
                                            prev = prev.previousSibling;
                                        }
                                        cur = cur.parentElement;
                                    }
                                    if (cur && cur.parentElement === editor) {
                                        var prev = cur.previousSibling;
                                        while (prev) {
                                            if (prev.nodeName === 'DIV' || prev.nodeName === 'P' || prev.nodeName === 'BR') break;
                                            if ((prev.textContent || '').trim().length > 0) return false;
                                            prev = prev.previousSibling;
                                        }
                                    }
                                    return true;
                                }
                                return false;
                            }

                            // スマホのIME（ソフトウェアキーボード）でのスペース入力を行頭で捕捉
                            editor.addEventListener('beforeinput', function(e) {
                                var width = (window.indentWidth !== undefined) ? window.indentWidth : 0;
                                if (width > 0 && (e.data === ' ' || e.data === '　')) {
                                    var sel = window.getSelection();
                                    if (sel.rangeCount > 0 && sel.isCollapsed) {
                                        var range = sel.getRangeAt(0);
                                        var isAtLineStart = isCaretAtLineStart(range.startContainer, range.startOffset);
                                        if (isAtLineStart) {
                                            e.preventDefault();
                                            var spaces = getIndentSpaces(width);
                                            document.execCommand('insertText', false, spaces);
                                            AndroidBridge.onTextChanged(editor.innerHTML);
                                        }
                                    }
                                }
                            });
                            
                            // ユーザーがエディタを手動タップした時：選択範囲が存在しない（完全に1点カーソルの）時のみ静かに解除
                            editor.addEventListener('pointerdown', function() {
                                // pointerdown直後はブラウザがまだ新しい選択範囲を確定していないため、
                                // ここで無理に frozenSelection を null にせず、selectionchange に委ねる
                            });

                            // ユーザーが文字選択を終えて指を離した瞬間に最新の選択範囲を確定・保存
                            editor.addEventListener('pointerup', function() {
                                var sel = window.getSelection();
                                if (sel && sel.rangeCount > 0 && !sel.isCollapsed) {
                                    var r = sel.getRangeAt(0);
                                    window.frozenSelection = r.cloneRange();
                                    window.lastSelectionRange = r.cloneRange();
                                    window.savedRange = r.cloneRange();
                                }
                            });

                            window.savedRange = null;
                            window.lastSelectionRange = null;
                            window.ignoreSelectionChange = false;
                            window.isFormatting = false;

                            window.checkAndReportFormatState = function(force) {
                                if (!force && (window.ignoreSelectionChange || window.isFormatting)) return;
                                var sel = window.getSelection();
                                var range = (sel && sel.rangeCount > 0) ? sel.getRangeAt(0) : (window.savedRange || window.lastCaretRange);
                                if (!range) return;

                                var node = range.startContainer;
                                var el = (node.nodeType === 3) ? node.parentElement : node;

                                var isBold = false;
                                var isItalic = false;
                                var isUnderline = false;
                                var isBullet = false;
                                if (el && editor.contains(el)) {
                                    isBold = !!el.closest('b, strong');
                                    isItalic = !!el.closest('i, em');
                                    isUnderline = !!el.closest('u');

                                    var curLine = null;
                                    if (el === editor) {
                                        var childIdx = Math.min(range.startOffset, editor.childNodes.length - 1);
                                        curLine = (childIdx >= 0) ? editor.childNodes[childIdx] : null;
                                    } else {
                                        curLine = el;
                                        while (curLine && curLine.parentElement !== editor && curLine.parentElement) {
                                            curLine = curLine.parentElement;
                                        }
                                    }

                                    var lineText = curLine ? (curLine.textContent || '') : '';
                                    isBullet = /^[ 　]*・/.test(lineText);

                                    var curTag = (curLine && curLine.tagName) ? curLine.tagName.toLowerCase() : 'p';
                                    AndroidBridge.onParagraphStyle(curTag);
                                }
                                AndroidBridge.onFormatState(isBold, isItalic, isUnderline, isBullet);

                                if (sel && sel.rangeCount > 0) {
                                    window.lastCaretRange = range.cloneRange();
                                    if (!sel.isCollapsed) {
                                        window.lastSelectionRange = range.cloneRange();
                                        window.frozenSelection = range.cloneRange();
                                    } else {
                                        window.lastSelectionRange = null;
                                        window.frozenSelection = null;
                                    }
                                    window.savedRange = range.cloneRange();
                                }

                                if (el && editor.contains(el)) {
                                    // フォント判定: 親要素に明示的な sans-serif 指定があるかチェック
                                    var sansEl = el.closest('[style*="sans-serif"], [face*="sans-serif"]');
                                    var family = sansEl ? 'sans-serif' : 'serif';
                                    var comp = window.getComputedStyle(el);
                                    var size = parseInt(comp.fontSize, 10) || 20;
                                    AndroidBridge.onSelectionStyle(family, size);

                                    // 文字色判定: 親要素に明示的な color 指定があるかチェック
                                    var colorEl = el.closest('[style*="color"], font[color]');
                                    var detectedHex = null;
                                    if (colorEl && colorEl !== editor) {
                                        var rawCol = colorEl.style.color || (colorEl.tagName.toLowerCase() === 'font' ? colorEl.getAttribute('color') : null);
                                        if (rawCol) {
                                            var match = rawCol.match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/i);
                                            if (match) {
                                                var r = parseInt(match[1], 10).toString(16).padStart(2, '0');
                                                var g = parseInt(match[2], 10).toString(16).padStart(2, '0');
                                                var b = parseInt(match[3], 10).toString(16).padStart(2, '0');
                                                detectedHex = ('#' + r + g + b).toUpperCase();
                                            } else if (rawCol.startsWith('#')) {
                                                detectedHex = rawCol.toUpperCase();
                                            }
                                        }
                                    }
                                    AndroidBridge.onSelectionColor(detectedHex);
                                }
                            };

                            document.addEventListener('selectionchange', function() {
                                window.checkAndReportFormatState();
                            });

                            // 文字の入力・削除（Backspace等）の直後にも即座に箇条書きや装飾状態を再判定
                            editor.addEventListener('input', function() {
                                window.checkAndReportFormatState();
                            });


                            window.indentWidth = $indentWidth;
                            window.bulletIndentWidth = 1.5;

                            // キーボードによる字下げ・字上げ（Tab, Shift+Tab, Backspace, Delete, Enter）
                            editor.addEventListener('keydown', function(e) {
                                var sel = window.getSelection();
                                if (!sel.rangeCount) return;
                                var width = (window.indentWidth !== undefined) ? window.indentWidth : 0;
                                var bIndent = 1.5;


                                if (e.key === 'Enter') {
                                    e.preventDefault();
                                    // 改行時は過去のテキスト選択を完全にリセット
                                    window.frozenSelection = null;
                                    window.lastSelectionRange = null;
                                    var range = sel.getRangeAt(0);
                                    var node = range.startContainer;

                                    // 現在行のテキストブロックと箇条書き有無を判定
                                    var curLine = (node.nodeType === 3 ? node.parentElement : node);
                                    while (curLine && curLine.parentElement !== editor && curLine.parentElement) {
                                        curLine = curLine.parentElement;
                                    }
                                    if (!curLine || curLine === editor) {
                                        curLine = (node.nodeType === 3 ? node.parentElement : node);
                                    }

                                    var lineText = curLine ? (curLine.textContent || '') : '';
                                    var isCurrentBullet = /^[ 　]*・/.test(lineText);
                                    var trimmedLine = lineText.trim();

                                    // 箇条書き記号「・」だけで中身が空の場合、箇条書きを終了して通常の空行に戻す
                                    if (isCurrentBullet && (trimmedLine === '・' || trimmedLine.length === 0)) {
                                        if (curLine && curLine !== editor) {
                                            curLine.classList.remove('bullet-line');
                                            curLine.textContent = '';
                                            curLine.appendChild(document.createElement('br'));
                                        } else {
                                            document.execCommand('delete', false, null);
                                        }
                                        AndroidBridge.onTextChanged(editor.innerHTML);
                                        window.checkAndReportFormatState();
                                        return;
                                    }

                                    // 新しい段落DIV要素を作成
                                    var newDiv = document.createElement('div');
                                    var width = (window.indentWidth !== undefined) ? window.indentWidth : 0;

                                    if (isCurrentBullet) {
                                        newDiv.classList.add('bullet-line');
                                        var bulletPrefixStr = '・';
                                        
                                        // 前の行の文字色スタイルがあれば継承
                                        var curColorEl = curLine.closest('[style*="color"], font[color]') || curLine.querySelector('[style*="color"], font[color]');
                                        var curColor = curColorEl ? (curColorEl.style.color || curColorEl.getAttribute('color')) : null;
                                        if (curColor) {
                                            newDiv.innerHTML = '<span style="color: ' + curColor + '">' + bulletPrefixStr + '</span>';
                                        } else {
                                            newDiv.textContent = bulletPrefixStr;
                                        }
                                    } else if (width > 0) {
                                        newDiv.textContent = getIndentSpaces(width);
                                    } else {
                                        newDiv.appendChild(document.createElement('br'));
                                    }

                                    // 現在の行の直後に新しい段落を挿入（必ずeditor直下に限定してHierarchyRequestErrorを防止）
                                    var targetLine = (curLine && curLine.parentElement === editor) ? curLine : null;
                                    if (targetLine && targetLine.nextSibling) {
                                        editor.insertBefore(newDiv, targetLine.nextSibling);
                                    } else {
                                        editor.appendChild(newDiv);
                                    }

                                    // 新しい行へキャレットを確実に移動
                                    var newRange = document.createRange();
                                    newRange.selectNodeContents(newDiv);
                                    newRange.collapse(false);
                                    sel.removeAllRanges();
                                    sel.addRange(newRange);
                                    window.lastCaretRange = newRange.cloneRange();
                                    window.savedRange = newRange.cloneRange();
                                    window.frozenSelection = null;
                                    window.lastSelectionRange = null;

                                    AndroidBridge.onTextChanged(editor.innerHTML);
                                    window.checkAndReportFormatState();
                                } else if ((e.key === ' ' || e.key === '　') && width > 0 && sel.isCollapsed) {
                                    // 物理キーボード等のスペースキー押下時
                                    var range = sel.getRangeAt(0);
                                    var isAtLineStart = isCaretAtLineStart(range.startContainer, range.startOffset);
                                    if (isAtLineStart) {
                                        e.preventDefault();
                                        var spaces = getIndentSpaces(width);
                                        document.execCommand('insertText', false, spaces);
                                        AndroidBridge.onTextChanged(editor.innerHTML);
                                    }
                                } else if (e.key === 'Tab') {
                                    e.preventDefault();
                                    var tabWidth = width > 0 ? width : 1;
                                    if (e.shiftKey) {
                                        // Shift + Tab: 字上げ（手前の全角スペースを最大tabWidth文字削除）
                                        for (var i = 0; i < tabWidth; i++) {
                                            document.execCommand('delete', false, null);
                                        }
                                    } else {
                                        // Tab: 字下げ（全角スペースをtabWidth文字挿入）
                                        var spaces = "　".repeat(tabWidth);
                                        document.execCommand('insertText', false, spaces);
                                    }
                                    AndroidBridge.onTextChanged(editor.innerHTML);
                                } else if (e.key === 'Backspace' && sel.isCollapsed && width > 0) {
                                    // Backspace: カーソル直前が全角スペースの場合、まとめてインデント幅分削除可能か判定
                                    var node = sel.anchorNode;
                                    var offset = sel.anchorOffset;
                                    if (node && node.nodeType === 3 && offset > 0) {
                                        var textBefore = node.nodeValue.substring(0, offset);
                                        // 直前の全角スペースの連続数をカウント
                                        var spaceCount = 0;
                                        for (var j = textBefore.length - 1; j >= 0; j--) {
                                            if (textBefore[j] === '　') {
                                                spaceCount++;
                                            } else {
                                                break;
                                            }
                                        }
                                        if (spaceCount >= width && width > 1) {
                                            e.preventDefault();
                                            for (var k = 0; k < width; k++) {
                                                document.execCommand('delete', false, null);
                                            }
                                            AndroidBridge.onTextChanged(editor.innerHTML);
                                        }
                                    }
                                } else if (e.key === 'Delete' && sel.isCollapsed && width > 0) {
                                    // Delete: カーソル直後が全角スペースの場合、まとめてインデント幅分削除可能か判定
                                    var node = sel.anchorNode;
                                    var offset = sel.anchorOffset;
                                    if (node && node.nodeType === 3 && offset < node.nodeValue.length) {
                                        var textAfter = node.nodeValue.substring(offset);
                                        var spaceCount = 0;
                                        for (var j = 0; j < textAfter.length; j++) {
                                            if (textAfter[j] === '　') {
                                                spaceCount++;
                                            } else {
                                                break;
                                            }
                                        }
                                        if (spaceCount >= width && width > 1) {
                                            e.preventDefault();
                                            for (var k = 0; k < width; k++) {
                                                document.execCommand('forwardDelete', false, null);
                                            }
                                            AndroidBridge.onTextChanged(editor.innerHTML);
                                        }
                                    }
                                }
                            });
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            // AndroidネイティブのWebView背景色を即座に更新
            webView.setBackgroundColor(surfaceColor.toArgb())

            // 余白・用紙・テーマ色・フォント設定・インデント幅のシグネチャを生成
            val themeSignature = "$surfaceColorCss-$textColorCss-${margins.top}-${margins.right}-${margins.bottom}-${margins.left}-$paperClass-$printPageSizeCss-$fontFamilyCss-$fontSize-$isRubyListActive-$indentWidth"
            if (themeSignature != lastThemeSignatureRef[0]) {
                lastThemeSignatureRef[0] = themeSignature

                val themeJs = """
                    window.indentWidth = $indentWidth;
                    var ed = document.getElementById('editor');
                    if (ed) {
                        ed.classList.remove('paper-fit', 'paper-a4', 'paper-b5');
                        ed.classList.add('$paperClass');
                        if (typeof setHeight === 'function') {
                            setHeight();
                        }
                        if ($isRubyListActive) {
                            ed.classList.add('highlight-all-rubies');
                        } else {
                            ed.classList.remove('highlight-all-rubies');
                        }
                    }
                    var dynamicStyleEl = document.getElementById('dynamic-editor-style');
                    if (dynamicStyleEl) {
                        var fontFam = "${fontFamilyCss.replace("'", "\\'")}";
                        dynamicStyleEl.textContent = 
                            '@media screen { ' +
                                'html, body { background-color: $surfaceColorCss; } ' +
                                '#editor { background-color: $surfaceColorCss; color: $textColorCss; padding: ${margins.top}px ${margins.right}px ${margins.bottom}px ${margins.left}px; } ' +
                                '#editor ruby rt { color: $textColorCss; } ' +
                                '#editor u { text-decoration-color: $textColorCss; } ' +
                                '#editor .bullet-line { padding-top: 2.5em; text-indent: -1em; } ' +
                            '} ' +
                            '@media print { ' +
                                '@page { size: $printPageSizeCss; margin: ${margins.top}px ${margins.right}px ${margins.bottom}px ${margins.left}px; } ' +
                                'html, body { display: block; writing-mode: vertical-rl; direction: ltr; margin: 0; padding: 0; width: 100%; height: 100%; background-color: #ffffff; overflow: visible; } ' +
                                '#editor { display: block; writing-mode: vertical-rl; direction: ltr; box-sizing: border-box; margin: 0; padding: 0; width: auto; height: 100%; background-color: #ffffff; color: #000000; font-family: ' + fontFam + '; font-size: ${fontSize}px; line-height: 2; outline: none; border: none; border-radius: 0; box-shadow: none; overflow: visible; word-break: break-all; white-space: pre-wrap; -webkit-print-color-adjust: exact; print-color-adjust: exact; } ' +
                                '.highlight-all-rubies ruby { background-color: transparent; box-shadow: none; } ' +
                                '#editor ruby rt { color: #000000; } ' +
                                '#editor u { text-decoration-color: #000000; } ' +
                                '#editor .bullet-line { padding-top: 2.5em; text-indent: -1em; } ' +
                            '}';
                    }
                """.trimIndent()
                webView.evaluateJavascript(themeJs, null)
            }

            // ボトムシートが開かれた瞬間に、最新のフォントスタイル・箇条書き・装飾状態を即時問い合わせて同期
            if (showFontSettingsBottomSheet) {
                if (!lastBottomSheetOpenRef[0]) {
                    lastBottomSheetOpenRef[0] = true
                    webView.evaluateJavascript("window.checkAndReportFormatState && window.checkAndReportFormatState(true);", null)
                }
            } else {
                lastBottomSheetOpenRef[0] = false
            }

            // ルビ一覧アイテムがタップされた時に、該当の <ruby> までスクロール移動する
            if (targetRubyIndex != null) {
                val jumpJs = """
                    var ed = document.getElementById('editor');
                    if (ed) {
                        var rubies = ed.querySelectorAll('ruby');
                        var idx = $targetRubyIndex;
                        if (rubies && rubies[idx]) {
                            rubies[idx].scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });
                        }
                    }
                """.trimIndent()
                webView.evaluateJavascript(jumpJs) {
                    onRubyJumpConsumed()
                }
            }
            
            if (fontApplyRequest != null && fontApplyRequest.id != lastHandledRequestIdRef[0]) {
                lastHandledRequestIdRef[0] = fontApplyRequest.id
                
                val targetFamilyCss = if ((fontApplyRequest.fontFamily ?: fontFamily) == EditorFontFamily.SERIF) "serif, 'Noto Serif JP'" else "sans-serif, 'Noto Sans JP'"
                val targetSize = fontApplyRequest.fontSize ?: fontSize
                val applyFamily = fontApplyRequest.fontFamily != null
                val applySize = fontApplyRequest.fontSize != null
                val targetColor = fontApplyRequest.textColor
                val applyColor = targetColor != null
                
                val js = """
                    (function() {
                        var curSel = window.getSelection();
                        var activeRange = (curSel && curSel.rangeCount > 0 && !curSel.isCollapsed) ? curSel.getRangeAt(0) : null;
                        var targetRange = activeRange || (window.frozenSelection && !window.frozenSelection.collapsed ? window.frozenSelection : null) || (window.savedRange && !window.savedRange.collapsed ? window.savedRange : null);

                        if (targetRange) {
                            var sel = window.getSelection();
                            sel.removeAllRanges();
                            sel.addRange(targetRange);
                            
                            ${if (applyFamily) """
                            document.execCommand('fontName', false, '__CUSTOM_FONT_FAMILY__');
                            var fonts = document.querySelectorAll('font[face="__CUSTOM_FONT_FAMILY__"]');
                            fonts.forEach(function(el) {
                                el.removeAttribute('face');
                                el.style.fontFamily = "$targetFamilyCss";
                            });
                            """ else ""}
                            
                            ${if (applySize) """
                            document.execCommand('fontSize', false, '7');
                            var fonts = document.querySelectorAll('font[size="7"], [style*="xxx-large"]');
                            fonts.forEach(function(el) {
                                el.removeAttribute('size');
                                el.style.fontSize = '${targetSize}px';
                            });
                            """ else ""}
                            
                            ${if (applyColor) """
                            if ('$targetColor'.length > 0) {
                                document.execCommand('foreColor', false, '$targetColor');
                                var colorFonts = document.querySelectorAll('font[color]');
                                colorFonts.forEach(function(el) {
                                    var col = el.getAttribute('color');
                                    el.removeAttribute('color');
                                    el.style.color = col;
                                });
                            } else {
                                // 文字色リセット: 選択範囲内の明示的な color 指定を除去
                                var colorSpans = document.querySelectorAll('#editor span[style*="color"], #editor font[color], #editor font[style*="color"]');
                                colorSpans.forEach(function(el) {
                                    if (sel.containsNode(el, true)) {
                                        el.style.color = '';
                                        el.removeAttribute('color');
                                        if (el.tagName.toLowerCase() === 'font' && !el.getAttribute('style') && !el.getAttribute('face') && !el.getAttribute('size')) {
                                            var p = el.parentNode;
                                            while (el.firstChild) { p.insertBefore(el.firstChild, el); }
                                            p.removeChild(el);
                                        }
                                    }
                                });
                            }
                            """ else ""}
                            
                            var rts = document.querySelectorAll('rt');
                            rts.forEach(function(rt) {
                                rt.style.fontSize = '';
                            });
                            
                            AndroidBridge.onTextChanged(document.getElementById('editor').innerHTML);
                            // 適用完了後、過去の古い選択記憶をリセットして他所への誤爆を防止
                            window.savedRange = null;
                            window.frozenSelection = null;
                            window.lastSelectionRange = null;
                            window.isFormatting = false;
                            window.ignoreSelectionChange = false;
                            window.checkAndReportFormatState && window.checkAndReportFormatState(true);
                        } else {
                            // 範囲選択がない場合（キャレットのみ、またはエディタが空の場合）
                            var ed = document.getElementById('editor');
                            if (ed) {
                                ed.focus();
                                ${if (applyColor) """
                                if ('$targetColor'.length > 0) {
                                    document.execCommand('foreColor', false, '$targetColor');
                                }
                                """ else ""}
                                ${if (applyFamily) """
                                document.execCommand('fontName', false, '__CUSTOM_FONT_FAMILY__');
                                var fList = ed.querySelectorAll('font[face="__CUSTOM_FONT_FAMILY__"]');
                                fList.forEach(function(el) {
                                    el.removeAttribute('face');
                                    el.style.fontFamily = "$targetFamilyCss";
                                });
                                """ else ""}
                                ${if (applySize) """
                                document.execCommand('fontSize', false, '7');
                                var sList = ed.querySelectorAll('font[size="7"], [style*="xxx-large"]');
                                sList.forEach(function(el) {
                                    el.removeAttribute('size');
                                    el.style.fontSize = '${targetSize}px';
                                });
                                """ else ""}
                                AndroidBridge.onTextChanged(ed.innerHTML);
                            }
                        }
                    })();
                """.trimIndent()
                webView.evaluateJavascript(js, null)
                onFontApplyConsumed()
            }
            
            if (rubyRequest != null && rubyRequest != lastHandledRubyRequestRef[0]) {
                lastHandledRubyRequestRef[0] = rubyRequest
                // ルビリクエストが来た場合、保存しておいた選択範囲を使ってルビを挿入する
                val js = """
                    (function() {
                        try {
                            if (window.savedRange && !window.savedRange.collapsed) {
                                var range = window.savedRange;
                                var baseText = range.toString();
                                if (baseText.length > 0) {
                                    // ルビタグの直後に確実な踏み台として \u200B を付与
                                    // 選択範囲が <u> 内にある場合、ルビの外側（または内側）にも <u> を維持する
                                    var startNode = range.startContainer;
                                    var startEl = (startNode.nodeType === 3 ? startNode.parentElement : startNode);
                                    var parentU = startEl ? startEl.closest('u') : null;
                                    
                                    var editorEl = document.getElementById('editor');

                                    // 選択テキストに適用されていた文字色を抽出して親文字に引き継ぐ（#editor枠は除外）
                                    var parentColor = startEl ? startEl.closest('[style*="color"], font[color]') : null;
                                    if (parentColor === editorEl) parentColor = null;
                                    var colorVal = parentColor ? (parentColor.style.color || parentColor.getAttribute('color')) : null;
                                    
                                    // 選択テキストに適用されていたフォント指定を抽出して親文字に引き継ぐ（#editor枠は除外）
                                    var parentFont = startEl ? startEl.closest('[style*="font-family"], font[face]') : null;
                                    if (parentFont === editorEl) parentFont = null;
                                    var fontVal = parentFont ? (parentFont.style.fontFamily || parentFont.getAttribute('face')) : null;

                                    var styledBase = baseText;
                                    var innerStyle = '';
                                    if (colorVal) innerStyle += 'color: ' + colorVal + ';';
                                    if (fontVal) innerStyle += 'font-family: ' + fontVal + ';';
                                    
                                    if (innerStyle.length > 0) {
                                        styledBase = '<span style="' + innerStyle + '">' + baseText + '</span>';
                                    }

                                    var rubyHtml = '<ruby>' + styledBase + '<rt>${rubyRequest}</rt></ruby>\u200B';
                                    var html = parentU ? "<u>" + rubyHtml + "</u>" : rubyHtml;
                                    
                                    window.ignoreSelectionChange = true;
                                    
                                    // 保存された範囲をブラウザのアクティブな選択状態として復元
                                    var sel = window.getSelection();
                                    sel.removeAllRanges();
                                    sel.addRange(range);

                                    // ブラウザの編集履歴（Undoスタック）に正しく記録させながらルビを挿入
                                    document.execCommand('insertHTML', false, html);
                                    
                                    // 挿入後、キャレットをルビの外側に確実に倒す
                                    var edEl = document.getElementById('editor');

                                    // 見出し要素（h1〜h4）の中で挿入された場合、ブラウザが勝手に付けたインラインの font-size をリセット
                                    var headingParent = range.startContainer.parentElement ? range.startContainer.parentElement.closest('h1, h2, h3, h4') : null;
                                    if (headingParent) {
                                        var rubies = headingParent.querySelectorAll('ruby');
                                        rubies.forEach(function(rb) {
                                            rb.style.removeProperty('font-size');
                                        });
                                    }

                                    if (sel.rangeCount > 0) {
                                        var curRange = sel.getRangeAt(0);
                                        curRange.collapse(false);
                                        sel.removeAllRanges();
                                        sel.addRange(curRange);
                                    }
                                    
                                    // 挿入後、変更されたHTMLからゼロ幅スペース（\u200B）を除去してAndroid側に送信
                                    // （エディタDOMの直接改変を避けてUndoスタックを保護しつつ、保存データと文字数カウントへの混入を完全防止）
                                    AndroidBridge.onTextChanged(edEl.innerHTML.replace(/\u200B/g, ''));
                                    
                                    // 範囲をリセットし、ロックを解除
                                    window.savedRange = null;
                                    setTimeout(function() { window.ignoreSelectionChange = false; }, 100);
                                }
                            }
                        } catch (e) {
                            console.error("rubyRequest error:", e);
                        }
                    })();
                """.trimIndent()
                
                webView.evaluateJavascript(js) {
                    onRubyApplied()
                }
            } else if (rubyRequest == null) {
                lastHandledRubyRequestRef[0] = null
            }
            
            if (formatCommand != null) {
                // コマンド名からタイムスタンプ部分を取り出し、純粋な命令名（"bold", "bulletList" 等）を取得
                val cmdName = formatCommand.substringBefore("_")
                // 文字装飾やUndo/Redoのリクエストが来た場合
                val js = """
                    (function() {
                        var targetEd = document.getElementById('editor');

                        // ブラウザが勝手に選択を解除しないよう、先に作業中フラグを立てる
                        window.isFormatting = true;
                        window.ignoreSelectionChange = true;

                        // 今まさに選択されている最新の選択範囲をチェック
                        var curSel = window.getSelection();
                        var hasActiveSelection = (curSel && curSel.rangeCount > 0 && !curSel.isCollapsed);

                        // 文字装飾（太字・斜体・傍線）用には、幅のある選択範囲（非collapsed）のみを厳選
                        var textRange = null;
                        if (hasActiveSelection) {
                            textRange = curSel.getRangeAt(0);
                        } else if (window.frozenSelection && !window.frozenSelection.collapsed) {
                            textRange = window.frozenSelection;
                        } else if (window.lastSelectionRange && !window.lastSelectionRange.collapsed) {
                            textRange = window.lastSelectionRange;
                        }

                        // 箇条書き用には、現在のアクティブなキャレット行を最優先、次に最新キャレット位置を使用（過去の範囲選択に引きずられない）
                        var activeCaret = (curSel && curSel.rangeCount > 0) ? curSel.getRangeAt(0) : null;
                        var caretRange = activeCaret || window.lastCaretRange || window.savedRange || textRange;

                        if (textRange && '$cmdName' !== 'bulletList' && '$cmdName' !== 'undo' && '$cmdName' !== 'redo') {
                            var sel = window.getSelection();
                            sel.removeAllRanges();
                            sel.addRange(textRange);
                            // 最新の選択範囲で記憶を更新
                            window.frozenSelection = textRange.cloneRange();
                            window.lastSelectionRange = textRange.cloneRange();
                            window.savedRange = textRange.cloneRange();
                        }

                        // 選択範囲をセットした後にフォーカスを戻す（選択解除を防ぐ）
                        if (targetEd) {
                            targetEd.focus();
                        }

                        var formatToggledOff = false;

                        if ('$cmdName' === 'undo') {
                            document.execCommand('undo', false, null);
                            window.frozenSelection = null;
                            window.lastSelectionRange = null;
                            window.savedRange = null;
                            AndroidBridge.onTextChanged(targetEd ? targetEd.innerHTML : '');
                        } else if ('$cmdName' === 'redo') {
                            document.execCommand('redo', false, null);
                            window.frozenSelection = null;
                            window.lastSelectionRange = null;
                            window.savedRange = null;
                            AndroidBridge.onTextChanged(targetEd ? targetEd.innerHTML : '');
                        } else if ('$cmdName' === 'bulletList') {
                            // 箇条書きは文字範囲選択ではなく、最新のキャレット行を最優先
                            if (caretRange) {
                                var sel = window.getSelection();
                                sel.removeAllRanges();
                                sel.addRange(caretRange);

                                var baseIndent = (window.indentWidth !== undefined) ? window.indentWidth : 0;
                                var bulletPrefix = '・';

                                var container = caretRange.startContainer;
                                var lineBlock = null;

                                // 1. エディタ自身がコンテナの場合（子要素インデックスから特定）
                                if (container === editor) {
                                    var childIdx = Math.min(caretRange.startOffset, editor.childNodes.length - 1);
                                    lineBlock = (childIdx >= 0) ? editor.childNodes[childIdx] : editor.lastChild;
                                } else {
                                    // 2. エディタ内部の要素・テキストノードの場合
                                    lineBlock = container;
                                    while (lineBlock && lineBlock.parentElement !== editor) {
                                        lineBlock = lineBlock.parentElement;
                                    }
                                }

                                // 3. 取得できなかった場合、またはエディタそのものの場合は新しいdivを作成して追加
                                if (!lineBlock || lineBlock === editor) {
                                    if (editor.childNodes.length === 0 || (editor.childNodes.length === 1 && editor.firstChild.nodeName === 'BR')) {
                                        editor.innerHTML = '';
                                        lineBlock = document.createElement('div');
                                        editor.appendChild(lineBlock);
                                    } else {
                                        lineBlock = editor.lastChild;
                                        if (!lineBlock || lineBlock.parentElement !== editor) {
                                            lineBlock = document.createElement('div');
                                            editor.appendChild(lineBlock);
                                        }
                                    }
                                }

                                if (lineBlock && lineBlock !== editor) {
                                    var rawText = (lineBlock.textContent || '').replace(/\u200B/g, '');
                                    var textWithoutIndent = rawText.replace(/^[ 　]*/, '');

                                    // 現在の行またはコンテナ内の文字色（color）を取得
                                    var existingColorEl = lineBlock.closest('[style*="color"], font[color]') || lineBlock.querySelector('[style*="color"], font[color]');
                                    var currentColor = existingColorEl ? (existingColorEl.style.color || existingColorEl.getAttribute('color')) : null;

                                    if (/^・/.test(textWithoutIndent)) {
                                        // 箇条書き解除: 基本インデント + 中点を除いたテキスト
                                        lineBlock.classList.remove('bullet-line');
                                        var newText = getIndentSpaces(baseIndent) + textWithoutIndent.replace(/^・/, '');
                                        if (currentColor) {
                                            lineBlock.innerHTML = '<span style="color: ' + currentColor + '">' + (newText.length > 0 ? newText : '<br>') + '</span>';
                                        } else {
                                            lineBlock.textContent = newText.length > 0 ? newText : '';
                                            if (lineBlock.textContent === '') {
                                                lineBlock.appendChild(document.createElement('br'));
                                            }
                                        }
                                    } else {
                                        // 箇条書き付与: 箇条書きインデント + 中点 + テキスト（中点も含めて文字色を適用）
                                        lineBlock.classList.add('bullet-line');
                                        var newText = bulletPrefix + textWithoutIndent;
                                        if (currentColor) {
                                            lineBlock.innerHTML = '<span style="color: ' + currentColor + '">' + newText + '</span>';
                                        } else {
                                            lineBlock.textContent = newText;
                                        }
                                    }

                                    // キャレットを行末（テキストの直後）に配置
                                    var newRange = document.createRange();
                                    newRange.selectNodeContents(lineBlock);
                                    newRange.collapse(false);
                                    sel.removeAllRanges();
                                    sel.addRange(newRange);
                                    window.lastCaretRange = newRange.cloneRange();
                                    window.frozenSelection = null;
                                    window.lastSelectionRange = null;
                                    window.savedRange = newRange.cloneRange();
                                }

                                AndroidBridge.onTextChanged(targetEd ? targetEd.innerHTML : '');
                                window.isFormatting = false;
                                window.ignoreSelectionChange = false;
                                window.checkAndReportFormatState && window.checkAndReportFormatState(true);
                            }
                        } else if (textRange) {
                            try {
                                var sel = window.getSelection();
                                sel.removeAllRanges();
                                sel.addRange(textRange);

                                var cmd = '$cmdName';
                                if (cmd === 'italic') {
                                    var startNode = textRange.startContainer;
                                    var startEl = (startNode.nodeType === 3) ? startNode.parentElement : startNode;
                                    var existingItalic = startEl ? startEl.closest('i, em') : null;

                                    if (existingItalic) {
                                        // すでに斜体がある場合は解体（解除）
                                        var parent = existingItalic.parentNode;
                                        while (existingItalic.firstChild) {
                                            parent.insertBefore(existingItalic.firstChild, existingItalic);
                                        }
                                        parent.removeChild(existingItalic);
                                        formatToggledOff = true;
                                    } else {
                                        document.execCommand('italic', false, null);
                                    }
                                } else if (cmd === 'bold') {
                                    var startNode = textRange.startContainer;
                                    var startEl = (startNode.nodeType === 3) ? startNode.parentElement : startNode;
                                    var existingBold = startEl ? startEl.closest('b, strong') : null;

                                    if (existingBold) {
                                        // すでに太字がある場合は解体（解除）
                                        var parent = existingBold.parentNode;
                                        while (existingBold.firstChild) {
                                            parent.insertBefore(existingBold.firstChild, existingBold);
                                        }
                                        parent.removeChild(existingBold);
                                        formatToggledOff = true;
                                    } else {
                                        document.execCommand('bold', false, null);
                                    }
                                } else if (cmd === 'underline') {
                                    var startNode = textRange.startContainer;
                                    var startEl = (startNode.nodeType === 3) ? startNode.parentElement : startNode;
                                    var existingUnderline = startEl ? startEl.closest('u') : null;

                                    if (existingUnderline) {
                                        // すでに下線がある場合は解体（解除）
                                        var parent = existingUnderline.parentNode;
                                        while (existingUnderline.firstChild) {
                                            parent.insertBefore(existingUnderline.firstChild, existingUnderline);
                                        }
                                        parent.removeChild(existingUnderline);
                                        formatToggledOff = true;
                                    } else {
                                        document.execCommand('underline', false, null);
                                    }
                                }

                                // 連続で装飾を適用できるよう、最新の選択範囲を取得・維持
                                var afterSel = window.getSelection();
                                if (afterSel && afterSel.rangeCount > 0 && !afterSel.isCollapsed) {
                                    var newR = afterSel.getRangeAt(0);
                                    window.frozenSelection = newR.cloneRange();
                                    window.lastSelectionRange = newR.cloneRange();
                                    window.lastCaretRange = newR.cloneRange();
                                    window.savedRange = newR.cloneRange();
                                } else {
                                    // 解除された場合は無理に過去の選択範囲を強制適用しない
                                    window.lastCaretRange = textRange.cloneRange();
                                    window.savedRange = textRange.cloneRange();
                                }
                            } catch (e) {
                                console.error('Format command error:', e);
                            }

                            if (window.ignoreSelectionTimer) {
                                clearTimeout(window.ignoreSelectionTimer);
                            }
                            window.ignoreSelectionTimer = setTimeout(function() {
                                window.isFormatting = false;
                                window.ignoreSelectionChange = false;
                            }, 20);
                        } else if ('$cmdName' === 'removeRuby') {
                            var rubies = document.getElementById('editor').querySelectorAll('ruby');
                            rubies.forEach(function(ruby) {
                                if (sel.containsNode(ruby, true) || ruby.contains(sel.anchorNode) || ruby.contains(sel.focusNode)) {
                                    var rts = ruby.querySelectorAll('rt');
                                    rts.forEach(function(rt) { rt.remove(); });
                                    var text = ruby.textContent;
                                    ruby.replaceWith(document.createTextNode(text));
                                }
                            });
                            window.lastSelectionRange = null;
                            window.frozenSelection = null;
                        }
                        
                        // 変更をAndroidに通知
                        AndroidBridge.onTextChanged(document.getElementById('editor').innerHTML);

                        // 最新のフォーマット状態を判定してAndroid側に通知（ボタンのアクティブ色反映）
                        var isB = false;
                        var isI = false;
                        var isU = false;
                        var isBul = false;

                        var checkSel = window.getSelection();
                        if (checkSel.rangeCount > 0) {
                            var checkNode = checkSel.getRangeAt(0).startContainer;
                            var checkEl = (checkNode.nodeType === 3) ? checkNode.parentElement : checkNode;
                            if (checkEl && editor.contains(checkEl)) {
                                isB = !!checkEl.closest('b, strong');
                                isI = !!checkEl.closest('i, em');
                                isU = !!checkEl.closest('u');

                                var line = (checkEl === editor) ? null : checkEl;
                                while (line && line.parentElement !== editor && line.parentElement) {
                                    line = line.parentElement;
                                }
                                var lTxt = line ? (line.textContent || '') : '';
                                isBul = /^[ 　]*・/.test(lTxt);

                                var sansEl = checkEl.closest('[style*="sans-serif"], [face*="sans-serif"]');
                                var family = sansEl ? 'sans-serif' : 'serif';
                                var comp = window.getComputedStyle(checkEl);
                                var size = parseInt(comp.fontSize, 10) || 20;
                                AndroidBridge.onSelectionStyle(family, size);

                                var colorEl = checkEl.closest('[style*="color"], font[color]');
                                var detectedHex = null;
                                if (colorEl && colorEl !== editor) {
                                    var rawCol = colorEl.style.color || (colorEl.tagName.toLowerCase() === 'font' ? colorEl.getAttribute('color') : null);
                                    if (rawCol) {
                                        var match = rawCol.match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/i);
                                        if (match) {
                                            var r = parseInt(match[1], 10).toString(16).padStart(2, '0');
                                            var g = parseInt(match[2], 10).toString(16).padStart(2, '0');
                                            var b = parseInt(match[3], 10).toString(16).padStart(2, '0');
                                            detectedHex = ('#' + r + g + b).toUpperCase();
                                        } else if (rawCol.startsWith('#')) {
                                            detectedHex = rawCol.toUpperCase();
                                        }
                                    }
                                }
                                AndroidBridge.onSelectionColor(detectedHex);
                            }
                        }

                        // 解除された直後のコマンドについては確実に OFF を保証
                        if (formatToggledOff) {
                            if ('$cmdName' === 'bold') isB = false;
                            if ('$cmdName' === 'italic') isI = false;
                            if ('$cmdName' === 'underline') isU = false;
                        }

                        AndroidBridge.onFormatState(isB, isI, isU, isBul);
                        })();
                """.trimIndent()
                
                webView.evaluateJavascript(js) {
                    onFormatApplied()
                }
            } else if (indentRequest != null) {
                // 字下げ・字上げリクエストの処理
                val js = """
                    (function() {
                        var sel = window.getSelection();
                        var range = (sel.rangeCount > 0 && !sel.isCollapsed) ? sel.getRangeAt(0) : window.savedRange;
                        var width = ${kotlin.math.abs(indentRequest)};
                        var isIndent = ${indentRequest > 0};
                        var spaces = "　".repeat(width);

                        if (isIndent) {
                            // 字下げ: カーソル位置または選択範囲に全角スペースを挿入
                            document.execCommand('insertText', false, spaces);
                        } else {
                            // 字上げ: カーソルの直前にある全角スペースを削除
                            for (var i = 0; i < width; i++) {
                                document.execCommand('delete', false, null);
                            }
                        }
                        AndroidBridge.onTextChanged(document.getElementById('editor').innerHTML);
                    })();
                """.trimIndent()
                webView.evaluateJavascript(js) {
                    onIndentConsumed()
                }
            } else if (paragraphStyleRequest != null) {
                // 段落スタイル適用リクエストの処理（標準DOM APIによる安全な置換）
                val targetTag = paragraphStyleRequest.tag
                val js = """
                    (function() {
                        var editor = document.getElementById('editor');
                        var targetTag = '$targetTag';
                        if (targetTag === 'p') targetTag = 'div';

                        var sel = window.getSelection();
                        var range = (sel.rangeCount > 0 && !sel.isCollapsed) ? sel.getRangeAt(0) : window.savedRange;
                        if (!range && window.lastCaretRange) range = window.lastCaretRange;
                        if (!range) return;

                        var container = range.startContainer;
                        var targetNode = container;
                        while (targetNode && targetNode.parentElement !== editor && targetNode !== editor) {
                            targetNode = targetNode.parentElement;
                        }

                        if (!targetNode || targetNode === editor) {
                            var childIdx = Math.min(range.startOffset, Math.max(0, editor.childNodes.length - 1));
                            targetNode = (childIdx >= 0 && editor.childNodes.length > 0) ? editor.childNodes[childIdx] : null;
                        }

                        var newTagName = (targetTag === 'p') ? 'div' : targetTag;
                        var newElement = document.createElement(newTagName);

                        if (targetNode && targetNode !== editor) {
                            if (targetNode.nodeType === 1) { // 既存の要素 (div, h1 等)
                                var currentTag = targetNode.tagName ? targetNode.tagName.toLowerCase() : 'div';
                                if (currentTag === targetTag || (targetTag === 'p' && currentTag === 'div')) {
                                    newElement = document.createElement('div');
                                }
                                if (targetNode.className) {
                                    newElement.className = targetNode.className;
                                }
                                while (targetNode.firstChild) {
                                    newElement.appendChild(targetNode.firstChild);
                                }
                                if (!newElement.firstChild) {
                                    newElement.appendChild(document.createElement('br'));
                                }
                                targetNode.replaceWith(newElement);
                            } else { // 裸のテキストノード
                                newElement.appendChild(targetNode.cloneNode(true));
                                targetNode.replaceWith(newElement);
                            }
                        } else {
                            newElement.appendChild(document.createElement('br'));
                            editor.appendChild(newElement);
                        }

                        var newRange = document.createRange();
                        newRange.selectNodeContents(newElement);
                        newRange.collapse(false);
                        sel.removeAllRanges();
                        sel.addRange(newRange);
                        window.lastCaretRange = newRange.cloneRange();
                        window.savedRange = newRange.cloneRange();
                        window.frozenSelection = null;
                        window.lastSelectionRange = null;

                        AndroidBridge.onTextChanged(editor.innerHTML);
                        if (window.checkAndReportFormatState) {
                            window.checkAndReportFormatState(true);
                        }
                    })();
                """.trimIndent()
                webView.evaluateJavascript(js) {
                    onParagraphStyleConsumed()
                }
            } else {
                // Compose側から（ファイル読み込み等で）全体のテキストが変更された場合のみ反映
                if (isInternalUpdateRef[0]) {
                    isInternalUpdateRef[0] = false
                } else if (value.text != lastKnownTextRef[0]) {
                    lastKnownTextRef[0] = value.text
                    val escapedHtml = HtmlTextConverter.convertToHtml(value.text)
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "\\n")
                    
                    val js = """
                        var el = document.getElementById('editor');
                        el.innerHTML = '$escapedHtml';
                        el.focus();
                        var sel = window.getSelection();
                        var range = document.createRange();
                        range.selectNodeContents(el);
                        range.collapse(false);
                        sel.removeAllRanges();
                        sel.addRange(range);
                    """.trimIndent()
                    
                    webView.evaluateJavascript(js, null)
                }
            }

            // PDFエクスポート・印刷リクエストの処理
            if (printRequest != null && printRequest != lastHandledPrintIdRef[0]) {
                lastHandledPrintIdRef[0] = printRequest
                com.tatextist.utils.PrintHelper.print(
                    context = webView.context,
                    webView = webView,
                    documentTitle = documentTitle,
                    paperSize = paperSize
                )
                onPrintConsumed()
            }
        }
    )
}
