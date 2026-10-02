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

---

## Bug 008: 操作イベント（装飾ボタン等）が1文字カウントされる不具合と、全消去時のルビ残骸による文字数加算
- **現象 (What):**
  - 文字未選択時に太字・斜体やフォント等のボタンを押すと、文字を入力していないのに文字数カウントが1増える。
  - 文字を全消去した際、親文字が消えてもルビ（`<rt>`）だけがエディタ上に残留する。
  - 一度全消去して0文字にした後、再入力すると前回の文字数が加算されてしまう。
- **原因 (Why):**
  1. 未選択時のスタイル予約プレースホルダーとして挿入されたゼロ幅スペース（`\u200B`）が、`EditorViewModel` 内の `text.length` で1文字としてカウントされていた。
  2. Chromium の contenteditable では、親文字がBackspace等で消えても `<ruby>` や `user-select: none` の付いた `<rt>` がDOMに残存する。
  3. 残骸の `<ruby>` があると、`HtmlTextConverter` が空判定できず内部テキストとして維持され、次回入力時に前回の残骸が引き継がれて加算されていた。
- **対策 (How):**
  1. `EditorViewModel` の文字数計算時に `\u200B` を完全に除外してカウントする。
  2. `HtmlTextConverter.convertFromHtml()` において、親文字が空の `<ruby>`（例: `<ruby><rt>...</rt></ruby>`）を正規表現で自動パージする。
  3. WebView の `input` イベント時およびキー消去時に、空の `<ruby>` ノードをDOMから自動削除（`ruby.remove()`）する安全なクリーンアップを追加。

---

## Bug 009: フォントやスタイル装飾を付与した際に文字数が膨大（80字等）にカウントされる
- **現象 (What):**
  - 「今度はどうかな？」（8文字）にゴシック体やフォントサイズなどの装飾を適用すると、文字数カウントが「80字」などタグの長さに応じて跳ね上がる。
- **原因 (Why):**
  - `HtmlTextConverter.convertFromHtml()` において、フォントやスタイルのHTMLタグ（`<font style="...">`, `<span style="...">`, `<b>`, `<i>` 等）が保護リストに入っていたため、プレーンテキスト文字列の中に生のHTMLタグがそのまま残存していた。
  - `EditorViewModel` がその文字列の `length` をそのまま本文の文字数としてカウントしたため、タグの文字列（60〜70文字分）が文字数に加算されていた。
- **対策 (How):**
  - Google のベストプラクティスに基づき、文字数カウントや行数カウント用の「純粋な可視プレーンテキスト抽出（`extractPlainText`）」を導入。
  - ルビ（`|漢字《かんじ》`）からフリガナ記法を除去して親文字（漢字）のみを抽出し、すべてのHTMLタグ（`<font>`, `<span>`, `<b>` 等）および `\u200B` を完全除去した純粋な可視文字列に対して `charCount` を計算するように分離・改修。

---

## Bug 010: PC/デスクトップ（1920×1080）におけるツールバー間延びおよびボトムシート下部見切れ
- **現象 (What):**
  1. Medium Desktop（1920×1080）などの大画面環境で、フローティングツールバーが画面横いっぱいに伸びてしまい、各ボタンの間隔が不自然に開いて間延びしていた。
  2. 書式設定やツール設定のボトムシート（`ModalBottomSheet`）において、横幅が全画面に広がるとともに、縦方向の高さ制限で見切れ（Clipping）が発生し、下部の項目（太字・斜体や下部ボタン）が画面外に隠れて操作不能になっていた。
- **原因 (Why):**
  1. ツールバーの `Surface` に最大幅制限がなく、`fillMaxWidth()` に従い画面端まで延伸していた。
  2. ボトムシートの内部コンテンツである `Column` に垂直スクロール（`Modifier.verticalScroll`）およびシステム余白（`navigationBarsPadding`）が適用されておらず、画面の高さがコンテンツの高さを下回った場合に溢れた要素が表示できなかった。
- **対策 (How):**
  1. ツールバーの `Surface` を `Box(contentAlignment = Alignment.BottomCenter)` で包み、`.widthIn(max = 520.dp)` を設定して画面下部中央に適切なカプセルサイズで収まるように制限。
  2. 書式設定、ルビ設定、ツール設定の各ボトムシート `Column` に `.widthIn(max = 560.dp)`、`.align(Alignment.CenterHorizontally)`、`.verticalScroll(rememberScrollState())`、および `.navigationBarsPadding()` を適用し、中央揃えかつ全項目を確実にスクロール・操作可能に改善。

---

## Bug 011: 連続した単語へのルビ付与時に直前のルビ文字が親文字に巻き込まれる不具合
- **現象 (What):**
  - 「検索機能」という単語に対して、「検索」に「けんさく」とルビを振った後、続けて「機能」に「きのう」とルビを振ると、「けんさく」の最後の文字「く」が脱落し、「く機能」として親文字に巻き込まれてしまう（`<ruby>検索<rt>けんさ</rt></ruby><ruby>く機能<rt>きのう</rt></ruby>`）。
- **原因 (Why):**
  - Chromium のコンテンツエディタにおいて、縦書き環境下で `<ruby>` タグの直後にある文字（「機能」）を選択した際、選択範囲（Range）の始点が直前の `<rt>` タグ（ふりがな部分）の末尾テキストノードに迷い込んでしまい、フリガナの「く」が選択範囲の先頭テキストとして拾われていた。
- **対策 (How):**
  1. CSS において `rt { pointer-events: none; }` を指定し、ふりがな部分のタッチ・ドラッグの当たり判定を無効化して本文テキストのみが選択されるようにした。
  2. JavaScript において、ルビ挿入直前に選択範囲の始点（`startContainer`）が `<rt>` の内部にあるかを検査し、もし `<rt>` 内にあれば直近の `<ruby>` の直後（`nextSibling`）へ選択開始位置を自動補正するサニタイズ処理を追加。

---

## Bug 012: カタカナ等のルビ付与時にふりがな（<rt>）が本文に漏れ出し、後続文字がルビに吸い込まれる不具合
- **現象 (What):**
  - 「ルビジャンプ」という文字列の「ルビ」に「るび」とルビを振ると、外側の `<ruby>` タグが欠落して本文中に「ルビるびジャンプ」とふりがなが露出した。
  - 続けて「ジャンプ」にルビを振ると、本文の「ジャンプ」が直前の `<rt>` の中に吸い込まれて `<ruby>ルビ<rt>るびジャンプじゃんぷ</rt></ruby>` と壊れてしまった。
- **原因 (Why):**
  - Bug 011 で導入した `range.setStart(parentRuby.nextSibling, 0)` という JavaScript の自動補正ロジックが、通常の文字選択に対しても誤作動。選択範囲の開始位置を勝手に後ろへワープさせて選択範囲を破壊してしまい、`insertHTML` が正常な `<ruby>` で囲めずに `<rt>` だけを本文に埋め込んでしまっていた。
- **対策 (How):**
  - 誤爆を引き起こしていた JavaScript の `range.setStart` ロジックを完全に撤去。
  - フリガナの誤選択防止は CSS の `rt { user-select: none; pointer-events: none; }` のみに委ねる安全な設計に戻し、ユーザーの選択テキストが 100% 正確に取得されるように復元。
  - なお、`document.execCommand` からの完全脱却および自前Undoスタックの構築は、アプリ完成後のアーキテクチャ刷新フェーズ（`future.md`）として計画的に実施する方針を明確化。

---

## Bug 013: ルビ付与直後のキャレット吸着と後続文字の親文字取り込み不具合
- **現象 (What):**
  - 「あ」に「あいうえお」とルビを振った直後に続けて文字（例: 「あ」）を入力すると、新しく打った文字が `<ruby>` の親文字テキストノードの末尾に吸着・取り込まれ、`<ruby>ああ<rt>あいうえお</rt></ruby>` になってしまう。
  - 手動でキャレットを動かして本文に入力しようとしても、末尾に本文テキストノードが存在しないため、ブラウザの当たり判定によって `<ruby>` 内部が選択されて親文字にルビがかかってしまう。
- **原因 (Why):**
  - Chromium（contenteditable）のインライン要素境界におけるオートマージ仕様によるもの。エディタ末尾のインライン要素（`<ruby>`）の直後に本文テキストノードが存在しない場合、ブラウザは標準の `Range.setStartAfter` や親コンテナ基準のキャレット指定を行っても、直前のインライン要素（親文字）の末尾にカーソルを強制再吸着させ、入力された文字を結合してしまう。
- **対策 (How):**
  1. `VerticalEditor.kt` のルビ挿入処理において、`<ruby>` の直後に一時的な踏み台としてゼロ幅スペース（`\u200B`）を1文字同時に配置し、キャレットを `\u200B` の直後（ルビの外側）へ確実に倒す。
  2. ゼロ幅スペースによる過去の弊害（文字数カウントの狂い・青空文庫形式への不可視文字混入・削除時の空振り）を完全に防ぐ二重防御として、Kotlin 側の `HtmlTextConverter.kt` にて、DOMから受け取った文字列内のすべての `\u200B` を無条件で完全消去（サニタイズ）する処理を追加。
  3. `HtmlTextConverterTest.kt` にユニットテストを追加し、ルビ直後に `\u200B` が存在しても青空文庫形式（`|あ《あいうえお》`）および文字数カウントが 100% 正確に保たれることを保証。
