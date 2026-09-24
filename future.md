# 将来検討する機能・改善 (Future Considerations)

## 1. UI / アクセシビリティ

### Android 14+ (API 34+) OS標準コントラスト設定との自動連動
- **概要:**
  - Android 14以降のOS設定（`設定` → `ユーザー補助` → `色と動き` → `コントラスト` [標準 / 中 / 高]）にアプリのテーマを自動連動させる。
- **実装方針:**
  - `Theme.kt` 内の `TATExtistTheme` にて `UiModeManager.getContrast()` を取得。
  - OS設定値に応じて `Color.kt` / `Theme.kt` に定義済みの `mediumContrastLightColorScheme` / `mediumContrastDarkColorScheme`、および `highContrastLightColorScheme` / `highContrastDarkColorScheme` を動的に切り替える。
  - アプリ内に手動の切り替えボタンを新設する必要はなく、OS全体のアクセシビリティ設定を自動反映できる。
