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

## 6. 全角記号について
1. ()や!、?などの全角記号は半角に変換する(エディタの機能としては、全角と半角どちらも使えるようにする)

## 7. リッチテキスト編集とexecCommandの段階的移行方針
1. **非推奨APIからの脱却:**
   - `document.execCommand` はWeb標準において非推奨（Deprecated）となっているため、長期的な保守性と安定性を確保するため、可能な箇所から段階的に依存を解消する。
2. **段階的アプローチの適用:**
   - 一括での全面刷新ではなく、部分選択装飾（フォントサイズ・書体変更等）や細やかなUndo/Redo制御が求められる機能から優先して、標準のDOM API（`Selection`, `Range`）や自前の状態・履歴管理への移行を進める。
3. **新規実装時の指針:**
   - 新規のテキスト装飾・編集ロジックを設計する際は、可能な限り `execCommand` に依存せず、HTML構造を明示的かつクリーンに制御できるモダンな実装を採用する。

## 8. Chrome DevTools（開発者ツール）によるWebViewのデバッグ・検証手順
実機またはエミュレータ上の縦書きエディタ（WebView）のDOMツリーやJavaScriptの変数を、PCのChromeからリアルタイムに検証・デバッグする手順です。

1. **アプリの準備:**
   - 実機またはエミュレータでアプリを起動し、エディタ画面を開いておく（実機の場合は端末設定でUSBデバッグを有効化してPCと接続）。
2. **Chromeでインスペクタを開く:**
   - PCのGoogle Chromeのアドレスバーに `chrome://inspect/#devices` と入力してEnterを押す。
3. **対象の特定と起動:**
   - 「Remote Target」欄に起動中のアプリ（`com.tatextist`）が表示されるので、その下にある青い「inspect」リンクをクリックする。
4. **検証のポイント:**
   - **Elementsタブ:** 文字サイズや装飾変更時に生成される `<span>` や `<ruby>` のHTML構造を視覚的に確認。
   - **Consoleタブ:** `window.savedRange` を入力して選択範囲の状態を確認したり、JavaScriptコマンドを直接実行して動作検証。

## 9. 今後の必須タスクと知見記録ルール (Next Tasks & Knowledge Mandate)
1. **実施すべき必須タスク:**
   - **イベント境界の明示的な整理:**
     「ボトムシートを開いてフォーカスが外れた時」と「ユーザーがタップして選択を解除した時」のように、ブラウザのライフサイクル（フォーカス・タップ・入力）の状態遷移を明確に分けた判定を入れること。
   - **実環境（DevTools）との照合:**
     単にKotlin側のテストをパスさせるだけでなく、PCのChrome（`chrome://inspect`）でWebView内部のDOM状態（`window.savedRange` の中身など）が意図通りに遷移しているかを確認しながら、確実に一段ずつ詰めていくこと。
2. **作業結果の記録義務:**
   - 上記の作業を実施した結果、得られた知見や挙動の詳細を必ずプロジェクトルートの `knowledge.md` に記録・蓄積すること。