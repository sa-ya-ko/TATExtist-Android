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
    isRubyListActive: Boolean = false,
    targetRubyIndex: Int? = null,
    onRubyJumpConsumed: () -> Unit = {},
    indentRequest: Int? = null,
    onIndentConsumed: () -> Unit = {},
    indentWidth: Float = 0.0f,
    paperSize: PaperSize = PaperSize.FIT,
    margins: EditorMargins = EditorMargins(),
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

    // WebViewとComposeのテキスト状態を同期するための変数（ループ防止用）
    // mutableStateOfを使うと不要な再描画が走り、IMEの漢字変換（コンポジション）が強制終了されるため、配列で状態を持たせます
    val lastKnownTextRef = remember { arrayOf(value.text) }
    val lastHandledRequestIdRef = remember { arrayOf<Long?>(null) }
    val isInternalUpdateRef = remember { arrayOf(false) }

    // 3. ComposeとWebViewの通信（ブリッジ）
    val jsBridge = remember(onValueChange, onStyleDetected, onFormatStateDetected) {
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
                            html, body {
                                margin: 0;
                                padding: 0;
                                width: 100vw;
                                background-color: $surfaceColorCss;
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
                                background-color: $surfaceColorCss;
                                color: $textColorCss;
                                padding: ${margins.top}px ${margins.right}px ${margins.bottom}px ${margins.left}px;
                                transition: all 0.25s ease-out;
                            }

                            /* 用紙サイズ: 画面フィット（通常モード） */
                            #editor.paper-fit {
                                width: 100vw;
                                height: 100vh;
                                box-shadow: none;
                                border: none;
                                border-radius: 0;
                            }

                            /* 用紙サイズ: A4（横長・縦書き原稿 297mm × 210mm 比率: 1.414 : 1） */
                            #editor.paper-a4 {
                                height: calc(100vh - 32px);
                                aspect-ratio: 297 / 210;
                                border-radius: 6px;
                                border: 1px solid rgba(128, 128, 128, 0.25);
                                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
                            }

                            /* 用紙サイズ: B5（横長・縦書き原稿 257mm × 182mm 比率: 1.412 : 1） */
                            #editor.paper-b5 {
                                height: calc(100vh - 32px);
                                aspect-ratio: 257 / 182;
                                border-radius: 6px;
                                border: 1px solid rgba(128, 128, 128, 0.25);
                                box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
                            }

                            /* 
                              親要素（太字や斜体のタグ）から遺伝した装飾をルビ（フリガナ）部分でリセットする。
                              これにより、段落ごと太字にしてもフリガナは読みやすい標準フォントを維持する。
                              優先度の関係上、!important なしでも直接指定（要素セレクタ）が遺伝に勝つ。
                              また、user-select: none を追加することで、選択時にフリガナが邪魔にならずスムーズに選択できるようになる。
                            */
                            rt {
                                font-weight: normal;
                                font-style: normal;
                                text-decoration: none;
                                -webkit-user-select: none;
                                user-select: none;
                                pointer-events: none;
                            }


                            /* ルビ一覧タブ表示時、本文中の全ルビを薄い黄色でハイライト */
                            .highlight-all-rubies ruby {
                                background-color: rgba(255, 235, 59, 0.45);
                                border-radius: 4px;
                                box-shadow: 0 0 0 2px rgba(255, 235, 59, 0.45);
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
                            document.addEventListener('selectionchange', function() {
                                if (window.ignoreSelectionChange || window.isFormatting) return;
                                var sel = window.getSelection();
                                if (sel.rangeCount > 0) {
                                    var range = sel.getRangeAt(0);
                                    var node = range.startContainer;
                                    var el = (node.nodeType === 3) ? node.parentElement : node;

                                    // 装飾状態（太字、斜体、傍線、箇条書き）を判定
                                    var isBold = false;
                                    var isItalic = false;
                                    var isUnderline = false;
                                    var isBullet = false;
                                    if (el && editor.contains(el)) {
                                        isBold = !!el.closest('b, strong');
                                        isItalic = !!el.closest('i, em');
                                        isUnderline = !!el.closest('u');

                                        var curLine = (el === editor) ? null : el;
                                        while (curLine && curLine.parentElement !== editor && curLine.parentElement) {
                                            curLine = curLine.parentElement;
                                        }
                                        var lineText = curLine ? (curLine.textContent || '') : (editor.textContent || '');
                                        isBullet = /^[ 　]*・/.test(lineText);
                                    }
                                    AndroidBridge.onFormatState(isBold, isItalic, isUnderline, isBullet);

                                    window.lastCaretRange = range.cloneRange();
                                    if (!sel.isCollapsed) {
                                        window.lastSelectionRange = range.cloneRange();
                                        window.frozenSelection = range.cloneRange();
                                    }
                                    window.savedRange = range.cloneRange();
                                    if (!sel.isCollapsed && el) {
                                        var comp = window.getComputedStyle(el);
                                        var family = comp.fontFamily || 'serif';
                                        var size = parseInt(comp.fontSize, 10) || 20;
                                        AndroidBridge.onSelectionStyle(family, size);
                                    }
                                }
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
                                            curLine.textContent = '';
                                            curLine.appendChild(document.createElement('br'));
                                        } else {
                                            document.execCommand('delete', false, null);
                                        }
                                        AndroidBridge.onTextChanged(editor.innerHTML);
                                        return;
                                    }

                                    // 新しい段落DIV要素を作成
                                    var newDiv = document.createElement('div');
                                    var width = (window.indentWidth !== undefined) ? window.indentWidth : 0;

                                    if (isCurrentBullet) {
                                        var totalBulletIndent = width + 1.5;
                                        newDiv.textContent = getIndentSpaces(totalBulletIndent) + '・';
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

                                    AndroidBridge.onTextChanged(editor.innerHTML);
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

            // テーマ（ダーク/ライト）が切り替わった時に、背景色と文字色をリアルタイムに更新する
            val themeJs = """
                document.documentElement.style.backgroundColor = '$surfaceColorCss';
                document.body.style.backgroundColor = '$surfaceColorCss';
                window.indentWidth = $indentWidth;
                var ed = document.getElementById('editor');
                if (ed) {
                    ed.style.backgroundColor = '$surfaceColorCss';
                    ed.style.color = '$textColorCss';
                    ed.style.padding = '${margins.top}px ${margins.right}px ${margins.bottom}px ${margins.left}px';
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
            """.trimIndent()
            webView.evaluateJavascript(themeJs, null)

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
                
                val js = """
                    if (window.savedRange && !window.savedRange.collapsed) {
                        var sel = window.getSelection();
                        sel.removeAllRanges();
                        sel.addRange(window.savedRange);
                        
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
                        
                        var rts = document.querySelectorAll('rt');
                        rts.forEach(function(rt) {
                            rt.style.fontSize = '';
                        });
                        
                        AndroidBridge.onTextChanged(document.getElementById('editor').innerHTML);
                        window.savedRange = null;
                    } else {
                        var sel = window.getSelection();
                        var range = (sel.rangeCount > 0) ? sel.getRangeAt(0) : window.savedRange;
                        if (range) {
                            var span = document.createElement('span');
                            ${if (applyFamily) "span.style.fontFamily = \"$targetFamilyCss\";" else ""}
                            ${if (applySize) "span.style.fontSize = '${targetSize}px';" else ""}
                            var textNode = document.createTextNode('\u200B');
                            span.appendChild(textNode);
                            range.insertNode(span);
                            
                            var newRange = document.createRange();
                            newRange.setStart(textNode, 1);
                            newRange.collapse(true);
                            sel.removeAllRanges();
                            sel.addRange(newRange);
                            window.savedRange = newRange.cloneRange();
                            AndroidBridge.onTextChanged(document.getElementById('editor').innerHTML);
                        }
                    }
                """.trimIndent()
                webView.evaluateJavascript(js, null)
                onFontApplyConsumed()
            }
            
            if (rubyRequest != null) {
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
                                    var parentU = (range.startContainer.nodeType === 3 ? range.startContainer.parentElement : range.startContainer).closest('u');
                                    var rubyHtml = "<ruby>" + baseText + "<rt>${rubyRequest}</rt></ruby>";
                                    var html = (parentU ? "<u>" + rubyHtml + "</u>" : rubyHtml) + "\u200B";
                                    
                                    window.ignoreSelectionChange = true;
                                    
                                    // 保存された範囲をブラウザのアクティブな選択状態として復元
                                    var sel = window.getSelection();
                                    sel.removeAllRanges();
                                    sel.addRange(range);
                                    
                                    // ブラウザの編集履歴（Undoスタック）に正しく記録させながらルビを挿入
                                    document.execCommand('insertHTML', false, html);
                                    
                                    // 挿入後、キャレットを \u200B の直後（ルビの外側）に確実に倒す
                                    if (sel.rangeCount > 0) {
                                        var curRange = sel.getRangeAt(0);
                                        curRange.collapse(false);
                                        sel.removeAllRanges();
                                        sel.addRange(curRange);
                                    }
                                    
                                    // 挿入後、変更されたHTMLをAndroid側に送信
                                    AndroidBridge.onTextChanged(document.getElementById('editor').innerHTML);
                                    
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
            } else if (formatCommand != null) {
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

                        // 箇条書き用には、文字選択または最新のキャレット位置を使用
                        var caretRange = textRange || window.lastCaretRange || window.savedRange || ((curSel && curSel.rangeCount > 0) ? curSel.getRangeAt(0) : null);

                        if (textRange) {
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

                        if ('$cmdName' === 'undo') {
                            document.execCommand('undo', false, null);
                            AndroidBridge.onTextChanged(targetEd ? targetEd.innerHTML : '');
                        } else if ('$cmdName' === 'redo') {
                            document.execCommand('redo', false, null);
                            AndroidBridge.onTextChanged(targetEd ? targetEd.innerHTML : '');
                        } else if ('$cmdName' === 'bulletList') {
                        // 箇条書きは文字範囲選択ではなく、最新のキャレット行を最優先
                        if (caretRange) {
                            var sel = window.getSelection();
                            sel.removeAllRanges();
                            sel.addRange(caretRange);

                            var baseIndent = (window.indentWidth !== undefined) ? window.indentWidth : 0;
                            var bulletPrefix = getIndentSpaces(baseIndent + 1.5) + '・';

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

                            // 3. 取得できなかった場合のフォールバック（末尾要素を優先）
                            if (!lineBlock || lineBlock === editor) {
                                lineBlock = editor.lastChild || editor;
                            }

                            if (lineBlock) {
                                var rawText = (lineBlock.textContent || '').replace(/\u200B/g, '');
                                // 既存の行頭スペース（字下げ）を一旦取り除く
                                var textWithoutIndent = rawText.replace(/^[ 　]*/, '');

                                if (/^・/.test(textWithoutIndent)) {
                                    // 箇条書き解除: 基本インデント + 中点を除いたテキスト
                                    var newText = getIndentSpaces(baseIndent) + textWithoutIndent.replace(/^・/, '');
                                    if (lineBlock.nodeType === 3) {
                                        lineBlock.nodeValue = newText;
                                    } else {
                                        lineBlock.textContent = newText;
                                    }
                                } else {
                                    // 箇条書き付与: 箇条書きインデント + 中点 + テキスト
                                    var newText = bulletPrefix + textWithoutIndent.trim();
                                    if (lineBlock.nodeType === 3) {
                                        lineBlock.nodeValue = newText;
                                    } else {
                                        lineBlock.innerHTML = '';
                                        lineBlock.textContent = newText;
                                    }
                                }

                                // キャレットを行末（中黒の直後）に配置
                                var newRange = document.createRange();
                                newRange.selectNodeContents(lineBlock);
                                newRange.collapse(false);
                                sel.removeAllRanges();
                                sel.addRange(newRange);
                                window.lastCaretRange = newRange.cloneRange();
                                window.frozenSelection = null;
                                window.savedRange = newRange.cloneRange();
                            }

                            AndroidBridge.onTextChanged(targetEd ? targetEd.innerHTML : '');
                        }
                    } else if (textRange) {
                        try {
                            var sel = window.getSelection();
                            sel.removeAllRanges();
                            sel.addRange(textRange);

                            var cmd = '$cmdName';
                            if (cmd === 'italic') {
                                // 選択範囲の中や親要素に <i> や <em> または font-style: italic があるか探す
                                var startNode = textRange.startContainer;
                                var startEl = (startNode.nodeType === 3) ? startNode.parentElement : startNode;
                                var existingItalic = startEl ? startEl.closest('i, em') : null;

                                if (existingItalic) {
                                    // すでに斜体がある場合は、外側の <b> 等を壊さずに <i> タグのみを解体（解除）
                                    var parent = existingItalic.parentNode;
                                    while (existingItalic.firstChild) {
                                        parent.insertBefore(existingItalic.firstChild, existingItalic);
                                    }
                                    parent.removeChild(existingItalic);
                                } else {
                                    // 斜体がない場合は付与
                                    document.execCommand('italic', false, null);
                                }
                            } else if (cmd === 'bold') {
                                var startNode = textRange.startContainer;
                                var startEl = (startNode.nodeType === 3) ? startNode.parentElement : startNode;
                                var existingBold = startEl ? startEl.closest('b, strong') : null;

                                if (existingBold) {
                                    // すでに太字がある場合は、内側の <i> 等を壊さずに <b> タグのみを解体（解除）
                                    var parent = existingBold.parentNode;
                                    while (existingBold.firstChild) {
                                        parent.insertBefore(existingBold.firstChild, existingBold);
                                    }
                                    parent.removeChild(existingBold);
                                } else {
                                    document.execCommand('bold', false, null);
                                }
                            } else if (cmd === 'underline') {
                                var startNode = textRange.startContainer;
                                var startEl = (startNode.nodeType === 3) ? startNode.parentElement : startNode;
                                var existingUnderline = startEl ? startEl.closest('u') : null;

                                if (existingUnderline) {
                                    // すでに下線がある場合は、<u> タグのみを解体（解除）
                                    var parent = existingUnderline.parentNode;
                                    while (existingUnderline.firstChild) {
                                        parent.insertBefore(existingUnderline.firstChild, existingUnderline);
                                    }
                                    parent.removeChild(existingUnderline);
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
                                // ブラウザが選択を外してしまった場合、直前の有効なtextRangeを強制再設定
                                window.frozenSelection = textRange.cloneRange();
                                window.lastSelectionRange = textRange.cloneRange();
                                window.lastCaretRange = textRange.cloneRange();
                                window.savedRange = textRange.cloneRange();
                                if (afterSel) {
                                    afterSel.removeAllRanges();
                                    afterSel.addRange(textRange);
                                }
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
                    var isB = document.queryCommandState('bold');
                    var isI = document.queryCommandState('italic');
                    var isU = document.queryCommandState('underline');
                    var isBul = false;

                    var checkSel = window.getSelection();
                    if (checkSel.rangeCount > 0) {
                        var checkNode = checkSel.getRangeAt(0).startContainer;
                        var checkEl = (checkNode.nodeType === 3) ? checkNode.parentElement : checkNode;
                        if (checkEl && editor.contains(checkEl)) {
                            if (!isB) isB = !!checkEl.closest('b, strong');
                            if (!isI) isI = !!checkEl.closest('i, em');
                            if (!isU) isU = !!checkEl.closest('u');

                            var line = (checkEl === editor) ? null : checkEl;
                            while (line && line.parentElement !== editor && line.parentElement) {
                                line = line.parentElement;
                            }
                            var lTxt = line ? (line.textContent || '') : (editor.textContent || '');
                            isBul = /^[ 　]*・/.test(lTxt);
                        }
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
        }
    )
}
