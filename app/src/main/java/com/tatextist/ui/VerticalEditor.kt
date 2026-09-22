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
    modifier: Modifier = Modifier
) {
    // ダークモード判定：端末の設定に合わせてテキスト色を切り替えます
    val isDarkTheme = isSystemInDarkTheme()
    val textColorCss = if (isDarkTheme) "#FFFFFF" else "#000000"
    val fontFamilyCss = if (fontFamily == EditorFontFamily.SERIF) "serif, 'Noto Serif JP'" else "sans-serif, 'Noto Sans JP'"

    // WebViewとComposeのテキスト状態を同期するための変数（ループ防止用）
    // mutableStateOfを使うと不要な再描画が走り、IMEの漢字変換（コンポジション）が強制終了されるため、配列で状態を持たせます
    val lastKnownTextRef = remember { arrayOf(value.text) }

    // 3. ComposeとWebViewの通信（ブリッジ）
    val jsBridge = remember(onValueChange) {
        object {
            // メインスレッド（UI操作担当）に処理を委譲するためのハンドラー
            private val handler = android.os.Handler(android.os.Looper.getMainLooper())
            
            @JavascriptInterface
            fun onTextChanged(newHtml: String) {
                // WebViewの裏方スレッドから呼ばれるため、handlerを使ってメインスレッドで実行させる
                handler.post {
                    val plainText = HtmlTextConverter.convertFromHtml(newHtml)
                    lastKnownTextRef[0] = plainText // WebViewからの更新を記録（再描画はトリガーしない）
                    onValueChange(TextFieldValue(text = plainText))
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
                
                // 背景を透明（Transparent）にする
                setBackgroundColor(Color.Transparent.toArgb())
                
                settings.apply {
                    javaScriptEnabled = true
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                
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
                                background-color: transparent;
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
                                -webkit-user-select: none;
                                user-select: none;
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

                            editor.addEventListener('input', function() {
                                AndroidBridge.onTextChanged(editor.innerHTML);
                            });
                            
                            // Composeのボトムシートが開くとWebViewがフォーカスを失い、
                            // 選択範囲が消滅してしまうため、常に最新の選択範囲を保存しておく
                            window.savedRange = null;
                            window.ignoreSelectionChange = false;
                            document.addEventListener('selectionchange', function() {
                                if (window.ignoreSelectionChange) return;
                                var sel = window.getSelection();
                                if (sel.rangeCount > 0) {
                                    window.savedRange = sel.getRangeAt(0);
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
            // テーマ（ダーク/ライト）が切り替わった時に、文字色をリアルタイムに更新する
            webView.evaluateJavascript("document.getElementById('editor').style.color = '$textColorCss';", null)
            // フォントサイズと書体のリアルタイム更新
            webView.evaluateJavascript(
                "var ed = document.getElementById('editor'); if (ed) { ed.style.fontSize = '${fontSize}px'; ed.style.fontFamily = \"$fontFamilyCss\"; }",
                null
            )
            
            if (rubyRequest != null) {
                // ルビリクエストが来た場合、保存しておいた選択範囲を使ってルビを挿入する
                val js = """
                    if (window.savedRange && !window.savedRange.collapsed) {
                        var range = window.savedRange;
                        var baseText = range.toString();
                        if (baseText.length > 0) {
                            var html = "<ruby>" + baseText + "<rt>${rubyRequest}</rt></ruby>";
                            
                            window.ignoreSelectionChange = true;
                            
                            // 保存された範囲をブラウザのアクティブな選択状態として復元
                            var sel = window.getSelection();
                            sel.removeAllRanges();
                            sel.addRange(range);
                            
                            // ブラウザの編集履歴（Undoスタック）に正しく記録させながらルビを挿入
                            document.execCommand('insertHTML', false, html);
                            
                            // 挿入後、選択状態を完全に解除する（二重実行バグ防止）
                            sel.removeAllRanges();
                            
                            // 挿入後、変更されたHTMLをAndroid側に送信
                            AndroidBridge.onTextChanged(document.getElementById('editor').innerHTML);
                            
                            // 範囲をリセットし、ロックを解除
                            window.savedRange = null;
                            setTimeout(function() { window.ignoreSelectionChange = false; }, 100);
                        }
                    }
                """.trimIndent()
                
                webView.evaluateJavascript(js) {
                    onRubyApplied()
                }
            } else if (formatCommand != null) {
                // 文字装飾やUndo/Redoのリクエストが来た場合
                val js = """
                    if ('$formatCommand' === 'undo') {
                        document.execCommand('undo', false, null);
                        AndroidBridge.onTextChanged(document.getElementById('editor').innerHTML);
                    } else if ('$formatCommand' === 'redo') {
                        document.execCommand('redo', false, null);
                        AndroidBridge.onTextChanged(document.getElementById('editor').innerHTML);
                    } else if (window.savedRange && !window.savedRange.collapsed) {
                        var range = window.savedRange;
                        var sel = window.getSelection();
                        sel.removeAllRanges();
                        sel.addRange(range);
                        
                        if ('$formatCommand' === 'bold') {
                            var tags = document.getElementById('editor').querySelectorAll('b, strong');
                            var unwrappedAny = false;
                            var currentRange = sel.getRangeAt(0).cloneRange();
                            tags.forEach(function(tag) {
                                if (sel.containsNode(tag, true) || tag.contains(sel.anchorNode) || tag.contains(sel.focusNode)) {
                                    var frag = document.createDocumentFragment();
                                    while(tag.firstChild) frag.appendChild(tag.firstChild);
                                    tag.replaceWith(frag);
                                    unwrappedAny = true;
                                }
                            });
                            if (!unwrappedAny) {
                                document.execCommand('bold', false, null);
                            } else {
                                sel.removeAllRanges();
                                sel.addRange(currentRange);
                            }
                        } else if ('$formatCommand' === 'italic') {
                            var tags = document.getElementById('editor').querySelectorAll('i, em');
                            var unwrappedAny = false;
                            var currentRange = sel.getRangeAt(0).cloneRange();
                            tags.forEach(function(tag) {
                                if (sel.containsNode(tag, true) || tag.contains(sel.anchorNode) || tag.contains(sel.focusNode)) {
                                    var frag = document.createDocumentFragment();
                                    while(tag.firstChild) frag.appendChild(tag.firstChild);
                                    tag.replaceWith(frag);
                                    unwrappedAny = true;
                                }
                            });
                            if (!unwrappedAny) {
                                document.execCommand('italic', false, null);
                            } else {
                                sel.removeAllRanges();
                                sel.addRange(currentRange);
                            }
                        } else if ('$formatCommand' === 'removeRuby') {
                            var rubies = document.getElementById('editor').querySelectorAll('ruby');
                            rubies.forEach(function(ruby) {
                                // 選択範囲とルビが重なっているか判定
                                if (sel.containsNode(ruby, true) || ruby.contains(sel.anchorNode) || ruby.contains(sel.focusNode)) {
                                    // ルビのフリガナ（rt）を削除
                                    var rts = ruby.querySelectorAll('rt');
                                    rts.forEach(function(rt) { rt.remove(); });
                                    // rubyタグを外して中のテキストだけ残す
                                    var text = ruby.textContent;
                                    ruby.replaceWith(document.createTextNode(text));
                                }
                            });
                        }
                        
                        // 変更をAndroidに通知
                        AndroidBridge.onTextChanged(document.getElementById('editor').innerHTML);
                        window.savedRange = null;
                    }
                """.trimIndent()
                
                webView.evaluateJavascript(js) {
                    onFormatApplied()
                }
            } else {
                // Compose側から（ファイル読み込み等で）全体のテキストが変更された場合のみ反映
                if (value.text != lastKnownTextRef[0]) {
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
