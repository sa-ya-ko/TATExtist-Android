# Project Rules & Reference Context

まずは、移植元アプリをアンドロイドアプリにすること。

## 1. Primary References (参照対象Webページ)
- **移植元Webアプリ:** https://tat-extist-gamma.vercel.app/
- **Android Architecture Best Practices:** https://developer.android.com/topic/architecture
- **Material 3 Expressive:** https://m3.material.io/
- **Android Jetpack Compose UI 指針:** https://developer.android.com/develop/ui/compose
- **WebViewのベストプラクティス:** https://developer.android.com/develop/ui/views/layout/webapps/best-practices
-**AndroidWebViewのベストプラクティス:**https://developer.android.com/develop/ui/views/layout/webapps/webview
-**Androidセキュリティのベストプラクティス:** https://developer.android.com/privacy-and-security/security-tips?hl=ja
-**OAuth 2.0ネイティブアプリ用:** https://developers.google.com/identity/protocols/oauth2/native-app?hl=ja

## 2. Test-Driven Development (TDD) Methodology 【厳守ルール】
コード実装を行う際は、必ず以下の **TDDサイクル（Red-Green-Refactor）** に従って対話・コード生成を行ってください。

1. **Red (テスト作成):**
   - 新機能や画面ロジックを実装する前に、必ず先に失敗するユニットテスト（またはUIテスト）を作成・提示してください。
   - 例: ViewModel、Repository、Use Casesのロジックテスト（JUnit5, MockK, Turbine等を使用）。
2. **Green (最小実装):**
   - テストをパスさせるための「必要最小限の実装コード」のみを書いてください。過度な作り込みは禁止します。
3. **Refactor (リファクタリング):**
   - テストが通る状態を維持したまま、コードの可読性やパフォーマンス向上、Material 3 Expressiveに沿ったリファクタリングを行ってください。

※AIは一気にプロダクションコードとテストを同時に出力せず、「まずテストを書く」段階からステップを踏んで提示してください。

## 3. Technical Stack & Development Guidelines
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3 Expressive)
- **Architecture:** MVVM / Unidirectional Data Flow (UDF)
- **Testing Tools:** JUnit5, MockK, Turbine (StateFlow検証用), Compose Test Framework

## 4. Instructions for AI Assistant (AIへの指示事項)
1. **TDDの強制:** ユーザーからの新機能要求に対しては、まず「どのようなテストコードを書くか」を提案してください。
2. **公式プラクティスの遵守:** Google推奨のアーキテクチャおよびMaterial 3 Expressiveガイドラインに従ってください。
3. **Webサイトの要素解析:** `https://tat-extist-gamma.vercel.app/` の仕様を読み解き、画面ロジックとUIコンポーネントを分解してテスト設計に落とし込んでください。

## 5. CSSについて
1. !importantは極力使わない
