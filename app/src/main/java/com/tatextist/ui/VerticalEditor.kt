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
    modifier: Modifier = Modifier
) {

    val surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
    val onSurfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val surfaceColorCss = String.format("#%06X", 0xFFFFFF and surfaceColor.toArgb())
    val textColorCss = String.format("#%06X", 0xFFFFFF and onSurfaceColor.toArgb())
    val fontFamilyCss = if (fontFamily == EditorFontFamily.SERIF) "serif, 'Noto Serif JP'" else "sans-serif, 'Noto Sans JP'"

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
                            #editor {
                                writing-mode: vertical-rl;
                                width: 100vw;
                                box-sizing: border-box;
                                padding: 20px;
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
                        <div id="editor" contenteditable="true">${HtmlTextConverter.convertToHtml(value.text)}</div>
                        <script>
                            const editor = document.getElementById('editor');
                            
                            // 高さをブラウザの物理ピクセルに合わせて絶対固定する関数（Flexboxバグ回避用）
                            function setHeight() {
                                const h = window.innerHeight + 'px';
                                document.body.style.height = h;
                                editor.style.height = h;
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

                            // スマホのIME（ソフトウェアキーボード）でのスペース入力を行頭で捕捉
                            editor.addEventListener('beforeinput', function(e) {
                                var width = (window.indentWidth !== undefined) ? window.indentWidth : 0;
                                if (width > 0 && (e.data === ' ' || e.data === '　')) {
                                    var sel = window.getSelection();
                                    if (sel.rangeCount > 0 && sel.isCollapsed) {
                                        var range = sel.getRangeAt(0);
                                        var node = range.startContainer;
                                        var offset = range.startOffset;
                                        var isAtLineStart = false;

                                        if (node === editor && offset === 0) {
                                            isAtLineStart = true;
                                        } else if (node && node.nodeType === 3) {
                                            var textBefore = node.nodeValue.substring(0, offset);
                                            if (textBefore.trim().length === 0) {
                                                isAtLineStart = true;
                                            }
                                        }
                                        if (isAtLineStart) {
                                            e.preventDefault();
                                            var spaces = getIndentSpaces(width);
                                            document.execCommand('insertText', false, spaces);
                                            AndroidBridge.onTextChanged(editor.innerHTML);
                                        }
                                    }
                                }
                            });
                            
                            // ユーザーがエディタを手動タップした時に古い選択を安全にリセット
                            editor.addEventListener('pointerdown', function() {
                                var sel = window.getSelection();
                                if (!sel || sel.isCollapsed) {
                                    window.frozenSelection = null;
                                }
                            });

                            // Composeのボトムシートが開くとWebViewがフォーカスを失い、
                            // 選択範囲が消滅してしまうため、常に最新の選択範囲を保存しておく
                            window.savedRange = null;
                            window.lastSelectionRange = null;
                            window.ignoreSelectionChange = false;
                            document.addEventListener('selectionchange', function() {
                                if (window.ignoreSelectionChange) return;
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

                                    // 現在の行の直後に新しい段落を挿入
                                    if (curLine && curLine.parentNode && curLine !== editor) {
                                        curLine.parentNode.insertBefore(newDiv, curLine.nextSibling);
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
                                    var node = sel.anchorNode;
                                    var offset = sel.anchorOffset;
                                    var isAtLineStart = false;
                                    if (node) {
                                        if (node === editor && offset === 0) {
                                            isAtLineStart = true;
                                        } else if (node.nodeType === 3) {
                                            var textBefore = node.nodeValue.substring(0, offset);
                                            if (textBefore.trim().length === 0) {
                                                isAtLineStart = true;
                                            }
                                        }
                                    }
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
                // 文字装飾やUndo/Redoのリクエストが来た場合
                val js = """
                    (function() {
                        var targetEd = document.getElementById('editor');
                        if (targetEd) {
                            targetEd.focus();
                        }
                        if ('$formatCommand' === 'undo') {
                            document.execCommand('undo', false, null);
                            AndroidBridge.onTextChanged(targetEd ? targetEd.innerHTML : '');
                        } else if ('$formatCommand' === 'redo') {
                            document.execCommand('redo', false, null);
                            AndroidBridge.onTextChanged(targetEd ? targetEd.innerHTML : '');
                    } else if ('$formatCommand' === 'bulletList') {
                        // 箇条書きは文字範囲選択ではなく、最新のキャレット行を最優先
                        var range = window.lastCaretRange || window.savedRange || window.frozenSelection || ((window.getSelection().rangeCount > 0) ? window.getSelection().getRangeAt(0) : null);
                        if (range) {
                            var sel = window.getSelection();
                            sel.removeAllRanges();
                            sel.addRange(range);

                            var baseIndent = (window.indentWidth !== undefined) ? window.indentWidth : 0;
                            var bulletPrefix = getIndentSpaces(baseIndent + 1.5) + '・';

                            var container = range.startContainer;
                            var lineBlock = null;

                            // 1. エディタ自身がコンテナの場合（子要素インデックスから特定）
                            if (container === editor) {
                                var childIdx = Math.min(range.startOffset, editor.childNodes.length - 1);
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
                    } else if (window.frozenSelection || window.lastSelectionRange || window.lastCaretRange || window.savedRange) {
                        var range = window.frozenSelection || window.lastSelectionRange || window.lastCaretRange || window.savedRange;
                        var sel = window.getSelection();
                        sel.removeAllRanges();
                        sel.addRange(range);

                        window.ignoreSelectionChange = true;

                        var cmd = '$formatCommand';
                        var tagSelector = (cmd === 'bold') ? 'b, strong' : (cmd === 'italic' ? 'i, em' : 'u');
                        var tagName = (cmd === 'bold') ? 'b' : (cmd === 'italic' ? 'i' : 'u');
                        var execCmd = (cmd === 'bold') ? 'bold' : (cmd === 'italic' ? 'italic' : 'underline');

                        // 1. すでに適用されている装飾タグがあれば解除（アンラップ）
                        var existingTags = editor.querySelectorAll(tagSelector);
                        var unwrappedAny = false;
                        var currentRange = sel.getRangeAt(0).cloneRange();
                        existingTags.forEach(function(tag) {
                            if (sel.containsNode(tag, true) || tag.contains(sel.anchorNode) || tag.contains(sel.focusNode)) {
                                var frag = document.createDocumentFragment();
                                while(tag.firstChild) frag.appendChild(tag.firstChild);
                                tag.replaceWith(frag);
                                unwrappedAny = true;
                            }
                        });

                        if (!unwrappedAny) {
                            // 2. 選択範囲内に <ruby> が含まれているか検査
                            var rubies = [];
                            editor.querySelectorAll('ruby').forEach(function(rb) {
                                if (sel.containsNode(rb, true) || rb.contains(sel.anchorNode) || rb.contains(sel.focusNode)) {
                                    rubies.push(rb);
                                }
                            });

                            if (rubies.length > 0) {
                                rubies.forEach(function(rb) {
                                    var parent = rb.parentElement;
                                    if (parent && parent.tagName.toLowerCase() !== tagName) {
                                        var wrap = document.createElement(tagName);
                                        rb.parentNode.insertBefore(wrap, rb);
                                        wrap.appendChild(rb);
                                    }
                                });
                            } else {
                                // ルビを含まない通常テキストの場合は標準コマンドを実行
                                document.execCommand(execCmd, false, null);
                            }
                        }

                        // 3. 連続で装飾を適用できるよう、最新の選択範囲を取得・維持
                        var afterSel = window.getSelection();
                        var activeRange = (afterSel && afterSel.rangeCount > 0 && !afterSel.isCollapsed)
                            ? afterSel.getRangeAt(0)
                            : currentRange;

                        if (afterSel) {
                            afterSel.removeAllRanges();
                            afterSel.addRange(activeRange);
                        }

                        window.frozenSelection = activeRange.cloneRange();
                        window.lastSelectionRange = activeRange.cloneRange();
                        window.lastCaretRange = activeRange.cloneRange();
                        window.savedRange = activeRange.cloneRange();

                        setTimeout(function() {
                            window.ignoreSelectionChange = false;
                        }, 200);
                    } else if ('$formatCommand' === 'removeRuby') {
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
