# Bug History & Troubleshooting Log (バグ記録台帳)

本ドキュメントは、制作過程で発生した不具合、その技術的要因、および解決アプローチを記録する台帳である。

---

## Bug 001: 日本語入力（IME）の強制中断と span タグの無限多重ネスト
- **現象 (What):**
  - 1文字打つたびに勝手に確定され、漢字変換ができなくなる。
  - `<span style="font-size: 16px;"><span style="font-size: 16px;">...` のように文字数分 span タグが増殖する。
- **原因 (Why):**
  - `input` リスナー内で、未選択時用のスタイル予約（`pendingStyle`）を反映するために `range.selectNodeContents` および `sel.addRange` でカーソル（キャレット）を強制再配置していた。
  - ブラウザ（Chromium）は入力セッション中に外部からカーソルを移動させられると、IMEの漢字変換セッションを強制キャンセル・確定してしまう。
  - また `pendingStyle` がリセットされずに残留し、キーストロークごとに外側を `<span>` で包み続けていた。
- **対策 (How):**
  - `input` リスナー内の自作DOM操作を完全撤去。
  - 未選択時のスタイル適用は、入力中ではなく「ボタンを押した瞬間」にゼロ幅スペース（`\u200B`）付きプレースホルダー `<span>` を挿入してキャレットをあらかじめ中に配置する方式に変更。

---

## Bug 002: Undo（元に戻す）実行時のルビ重複と文字入れ替わり
- **現象 (What):**
  - Undo を押すと、ルビが付いていた文字が重複生成され、その後のテキストと順番が入れ替わって戻らなくなる。
- **原因 (Why):**
  - Undo 実行直後に `AndroidBridge.onTextChanged` 経由で Android 側の `value.text` が更新された際、Compose の `update` ブロックが「テキスト変更」と誤認して `el.innerHTML = '$escapedHtml'` を実行していた。
  - `innerHTML` への丸ごと代入により、ブラウザが保持していた内部 Undo 履歴スタックが消滅。さらに青空文庫形式への再パースのズレによってルビが二重化・テキスト破損を起こしていた。
- **対策 (How):**
  - `isInternalUpdateRef` フラグを導入し、WebView内部発の更新（文字入力、Undo、装飾）である間は `innerHTML` の外部上書きを完全に遮断。ファイル読み込み時のみ上書きを許可するようにガードした。

---

## Bug 003: 複数装飾時の Undo で行頭に文字が分裂・コピーされる
- **現象 (What):**
  - 複数箇所にルビやフォントスタイルが存在する状態で Undo を2回押すと、別の場所の装飾文字が行頭にコピーされる（例: `<span ...>必要</span>今日は雨なので...`）。
- **原因 (Why):**
  - `execCommand` で付与した `<font>` マーカーをモダンな `<span>` に変換する際、`el.parentNode.replaceChild(span, el)` で手動DOM切断を行っていた。
  - Chromium の Undo スタック内部において、手動切断されたノードの親子関係ポインタが破損し、Undo 時に親要素（行頭）に吐き出されてしまっていた。
- **対策 (How):**
  - `replaceChild` を撤去し、`el.removeAttribute('size'); el.style.fontSize = ...` のようにブラウザ生成ノードの属性とスタイルを直接変更する形にして、DOMツリーの親子関係を一切切断しないようにした。

---

## Bug 004: 選択解除後にフォントサイズを変更すると過去の装飾箇所が誤爆する
- **現象 (What):**
  - テキスト選択を解除してカーソルのみにした状態でサイズを変更すると、直前に装飾した別の文字が勝手に選択されてサイズ変更される。
- **原因 (Why):**
  - ボトムシートを開いた瞬間のフォーカス喪失で選択範囲が消えるのを防ぐため `savedRange` を保持していたが、ユーザー自身がエディタ内をタップして選択を解除した際にも `savedRange` がクリアされずに残り続けていた。
- **対策 (How):**
  - `document.activeElement === editor` かつ `isCollapsed === true` の時（＝ユーザー自身がエディタ内をタップして選択解除した瞬間）のみ `window.savedRange = null` にリセットする境界判定を導入した。

---

## Bug 005: ライトモード切り替え時にタイトルバーとWebViewエディタが黒いまま残留する
- **現象 (What):**
  - ダークモードからライトモードに切り替えた際、画面上部のタイトル部（TopAppBar）および縦書き本文エディタ（WebView）の背景が黒いまま残り、ライトモード色（淡いミント白 `#F7FAF7`）に切り替わらない。
- **原因 (Why):**
  1. WebViewが `Color.Transparent` の透過モードになっていたが、Android ネイティブの WebView 背景描画が初期化時の色を保持し、再描画時に更新されていなかった。また HTML 内の `document.body` のみのスタイル更新にとどまり、`documentElement`（htmlタグ）や `#editor` への背景色指定が欠落していた。
  2. Compose側の `Scaffold` および `TopAppBar` において、`containerColor` が未指定だったため、テーマ変更時に明示的に `surface` カラーへ追従していなかった。
- **対策 (How):**
  1. `VerticalEditor.kt` の `AndroidView.update` ブロック内で、ネイティブの `webView.setBackgroundColor(surfaceColor.toArgb())` を直接実行。さらに JavaScript で `documentElement`、`body`、`#editor` の背景色を即座に同一の `surfaceColorCss` へ同期。
  2. `EditorScreen.kt` の `Scaffold` に `containerColor = MaterialTheme.colorScheme.surface`、`TopAppBar` に `TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)` を明示的に指定した。

---

## Bug 006: 画面最上部にAndroidネイティブの黒いActionBar「TATExtist」が残留する
- **現象 (What):**
  - ComposeのTopAppBar（タイトル「無題のドキュメント」など）の上に、アプリ名「TATExtist」と書かれた黒い帯（バー）が常時表示されてしまう。
- **原因 (Why):**
  - `AndroidManifest.xml` の `<application>` に `android:theme` が指定されておらず、Androidシステムがデフォルトの暗色ActionBarテーマ（黒背景に白文字のネイティブActionBar）を自動適用していたため。
- **対策 (How):**
  - `AndroidManifest.xml` の `<application>` に `android:theme="@android:style/Theme.Material.NoActionBar"` を明示し、ネイティブの不要な黒いActionBarを非表示にした。

---

## Bug 007: エディタ内容を全削除した際に文字数（スペースあり）が「1」と誤判定される
- **現象 (What):**
  - エディタに文字を入力したあと BackSpace 等で全文字を削除して空にした際、文字数カウント（スペースあり）が `0` ではなく `1` とカウントされる。
- **原因 (Why):**
  - Chromium の contenteditable 仕様により、中身が完全に消去された際、キャレット保持のために `<br>` または `<div><br></div>` が自動挿入される。
  - `HtmlTextConverter.convertFromHtml()` がこの `<br>` を改行コード `\n`（長さ1文字）へと変換していたため、`text.length` が 1、かつ `\n` は空白文字であるため `charCountWithoutSpaces`（スペース無視）のみが 0 となっていた。
- **対策 (How):**
  - `HtmlTextConverter.convertFromHtml()` において、テキストが純粋なタグ（`<br>`, `<div>`, `<p>`）および空白のみで構成されており、本文の文字や `<ruby>` が存在しない場合は、結果を空文字 `""` にサニタイズするように改修。ルビやスタイルのある本文には影響しないよう境界判定を保護した。


