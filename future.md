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

