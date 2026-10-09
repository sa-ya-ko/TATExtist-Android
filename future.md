# 将来検討する機能・改善 (Future Considerations)

## 1. UI / アクセシビリティ

### Android 14+ (API 34+) OS標準コントラスト設定との自動連動
- **概要:**
  - Android 14以降のOS設定（`設定` → `ユーザー補助` → `色と動き` → `コントラスト` [標準 / 中 / 高]）にアプリのテーマを自動連動させる。
- **実装方針:**
  - `Theme.kt` 内の `TATExtistTheme` にて `UiModeManager.getContrast()` を取得。
  - OS設定値に応じて `Color.kt` / `Theme.kt` に定義済みの `mediumContrastLightColorScheme` / `mediumContrastDarkColorScheme`、および `highContrastLightColorScheme` / `highContrastDarkColorScheme` を動的に切り替える。
  - アプリ内に手動の切り替えボタンを新設する必要はなく、OS全体のアクセシビリティ設定を自動反映できる。

## 2. エディタ執筆UX (Editor Experience)

### ルビのインライン・ライブ編集（青空文庫形式展開）機能
- **概要:**
  - 通常時は `<ruby>漢字<rt>かんじ</rt></ruby>` として縦書きの美しいルビ付きテキストとしてレンダリングする。
  - カーソルがルビ箇所に当たった（またはフォーカスした）瞬間、自動的に青空文庫形式 `|漢字《かんじ》` のテキストノードにインライン展開する。
  - ユーザーはボトムシートを開かなくても、キーボードから直接親文字やフリガナを推敲・修正できる。
  - カーソルがその単語から外れた瞬間に、再び自動で `<ruby>` タグに復元してレンダリングする（Notion や Obsidian の Live Preview / WYSIWYG と同様のUX）。
- **実装方針:**
  - WebView 側の `selectionchange` リスナーにてキャレット位置の親ノードを判定。
  - 対象ルビノードを `|漢字《かんじ》` に置換し、フォーカス外れ時に正規表現で `<ruby>` に再パースする。

### ルビ付きテキストのクリップボード（コピー＆ペースト）完全保持機能
- **概要:**
  - 通常のブラウザ/OS標準のクリップボードでは、`<ruby>` 要素をコピーした際にHTML構造が剥ぎ取られ、プレーンテキストとして親文字とふりがなが連結（例: 「ああいうえお」）されて崩れてしまう。
  - エディタ内でルビ付き文字をコピー＆ペーストした際にも、ルビ構造を破壊せずそのまま完全に再現できるようにする。
- **実装方針:**
  - WebView 側の `copy` イベントをインターセプト（横取り）し、選択範囲内のルビを青空文庫形式（`|親文字《ルビ》`）および完全なリッチHTMLとしてクリップボードの `text/plain` / `text/html` に格納。
  - `paste` イベント時に、クリップボードから渡された青空文庫形式やHTMLを `HtmlTextConverter.convertToHtml()` を経由して安全に復元・挿入する。

## 3. エディタ・アーキテクチャ刷新 (Editor Architecture Modernization)

### 自前Undo/Redoスタックの構築と非推奨 `document.execCommand` からの完全脱却
- **概要:**
  - 現在、ブラウザ（Chromium）の内部Undoスタックを壊さないために非推奨APIである `document.execCommand`（`insertHTML`, `bold`, `italic`, `undo`, `redo`等）を維持している。
  - アプリの全基本機能が完成した後の刷新タスクとして、Google DocsやNotionのように、エディタの状態変更（青空文庫形式テキストや装飾履歴）をKotlin/Compose側で自前の一元Undo/Redoスタックとして管理するアーキテクチャへ段階的に移行する。
- **実装方針:**
  - `EditorViewModel` または専用の `EditorHistoryManager` に Undo/Redo の履歴スナップショット（またはCommandパターン）を構築。
  - ブラウザ側のブラックボックスな Undo 機構に依存せず、常にKotlin側からクリーンなHTMLをWebViewへ描画するシングルソース・オブ・トゥルース（Single Source of Truth）を確立する。
  - これにより、`document.execCommand` を完全廃止し、W3C標準のモダンなDOM APIのみで安定動作する強固なエディタ基盤を実現する。

## 4. インデント
- 行頭インデント
- ぶら下がりインデント
- 