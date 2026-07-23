<template>
  <div class="h-screen overflow-y-auto bg-surface-container-low dark:bg-gray-900 text-on-surface dark:text-gray-200 py-12 px-4 sm:px-6 lg:px-8 font-sans selection:bg-primary/20">
    <div class="max-w-3xl mx-auto bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6 sm:p-10 border border-outline-variant dark:border-gray-700">
      <div class="mb-8 flex justify-between items-center border-b border-outline-variant dark:border-gray-700 pb-4">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">プライバシーポリシー</h1>
        <NuxtLink to="/" class="text-primary dark:text-blue-400 hover:underline text-sm font-medium flex items-center gap-1 transition-colors">
          <span class="material-symbols-outlined text-[18px]">arrow_back</span>
          エディタに戻る
        </NuxtLink>
      </div>

      <div class="space-y-6 text-sm leading-relaxed text-gray-700 dark:text-gray-300">
        <p>開発者（以下「当方」といいます。）は、当方が提供する縦書きエディタ（以下「本サービス」といいます。）における、ユーザーの個人情報およびデータの取り扱いについて、以下のとおりプライバシーポリシー（以下「本ポリシー」といいます。）を定めます。</p>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">1. 取り扱う情報</h2>
          <p class="mb-2">当方は、本サービスの提供にあたり、必要な範囲で以下の情報を取得し、または機能提供に必要な範囲で一時的に処理する場合があります。</p>
          <ul class="list-disc pl-5 space-y-2">
            <li><strong>利用環境情報：</strong>ブラウザの種類、オペレーティングシステム、アクセス日時、閲覧ページ、参照元その他利用状況に関する情報。</li>
            <li><strong>コンテンツデータ：</strong>ユーザーコンテンツおよび設定データ。本サービスでは原則としてユーザーの端末内に保存されますが、PDF生成やクラウド保存機能を利用する場合には、当該機能の提供に必要な範囲で一時的にサーバー上で処理されることがあります。</li>
            <li><strong>認証情報：</strong>クラウド連携機能を利用する場合には、当該機能の提供に必要な認証情報（アクセストークン等）を一時的に取り扱うことがあります。認証情報は必要な範囲でのみ利用し、恒久的に保存しません。</li>
            <li><strong>お問い合わせ情報：</strong>フィードバックやお問い合わせフォームを通じてご提供いただいた氏名（任意）、メールアドレスその他お問い合わせ対応に必要な情報。</li>
          </ul>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">2. 情報の利用目的</h2>
          <p class="mb-2">当方は、取り扱う情報を以下の目的で利用します。</p>
          <ul class="list-disc pl-5 space-y-2">
            <li>本サービスの提供、運営、および機能（PDF生成、クラウド保存等）の実行のため</li>
            <li>システムの保守、不具合の調査、セキュリティ向上のため</li>
            <li>本サービスの利用状況の分析およびサービス改善のため</li>
            <li>ユーザーからのお問い合わせへの対応のため</li>
          </ul>
          <div class="mt-4 p-4 bg-primary/5 dark:bg-gray-700 border border-primary/20 dark:border-gray-600 rounded-lg text-sm">
            <strong class="text-primary dark:text-blue-300 block mb-1">【データ保存とAI学習に関する方針】</strong>
            本サービスでは、機能提供のためにユーザーコンテンツをサーバー上で一時的に処理する場合がありますが、処理完了後は速やかに破棄し、当方のデータベースへ恒久的に保存することはありません。また、当方は、ユーザーコンテンツを、ユーザーの明示的な同意なくAIモデルの学習その他本サービスの提供に必要な範囲を超える目的で利用しません。
          </div>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">3. 第三者への提供と共有</h2>
          <p class="mb-2">当方は、以下の場合を除き、ユーザーの個人情報およびユーザーコンテンツを第三者に提供・共有することはありません。</p>
          <ul class="list-disc pl-5 space-y-2">
            <li>ユーザー本人の同意がある場合（ユーザーが明示的に指定したクラウドサービスへの送信を含みます）</li>
            <li>法令に基づき開示することが必要である場合</li>
            <li>人の生命、身体または財産の保護のために必要があり、本人の同意を得ることが困難である場合</li>
            <li>当方が本サービスの提供に必要な範囲内において、業務委託先（ホスティング環境等のクラウドプロバイダ）に一時的な処理を委託する場合</li>
          </ul>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">4. Cookieおよびブラウザストレージ</h2>
          <div class="space-y-3">
            <p>本サービスでは、利用状況の把握や操作性の向上のために、Cookie（クッキー）またはローカルストレージ（LocalStorage）などの技術を使用しています。ユーザーはブラウザの設定によりCookie等を無効化することができます。ただし、その場合、本サービスの一部機能（設定の自動保存など）が正常に利用できないことがあります。</p>
            <p>本サービスでは、サービスの利用状況の分析および改善を目的としてGoogle Analytics 4（以下「Google Analytics」といいます。）を利用しています。</p>
            <p>Google AnalyticsはCookie等を利用して利用状況に関する情報を収集します。収集された情報はGoogleに送信され、Googleのプライバシーポリシーに基づいて取り扱われます。Google Analyticsの詳細については<a href="https://marketingplatform.google.com/about/analytics/terms/jp/" target="_blank" rel="noopener noreferrer" class="text-primary dark:text-blue-400 hover:underline transition-colors">Google Analytics利用規約</a>および<a href="https://policies.google.com/privacy?hl=ja" target="_blank" rel="noopener noreferrer" class="text-primary dark:text-blue-400 hover:underline transition-colors">Googleのプライバシーポリシー</a>をご確認ください。</p>
          </div>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">5. 外部サービスとの連携</h2>
          <p>本サービスにおいて、Google ドライブその他の外部サービスとの連携機能を利用する場合、当方は当該機能の提供に必要な範囲のデータのみを当該外部サービスに送信します。当方は、外部サービスとの連携に必要な範囲を超えてユーザーのデータへアクセスすることはありません。当方は、Google Drive APIを通じてアクセスするユーザーデータを、本サービスの機能提供以外の目的では利用しません。外部サービス上でのデータの取り扱いについては、各サービス提供者のプライバシーポリシーが適用されます。</p>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">6. プライバシーポリシーの変更</h2>
          <p>当方は、法令の改正や本サービスの仕様変更に伴い、必要に応じて本ポリシーを変更することがあります。変更を行った場合は、本サービス上での掲示その他適当な方法によりユーザーに通知します。変更後に本サービスを利用した場合、ユーザーは変更後の本ポリシーに同意したものとみなされます。</p>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">7. お問い合わせ</h2>
          <p>
            ユーザーは、本ポリシーまたは個人情報の取り扱いに関するお問い合わせを、
            <a href="https://forms.gle/w37AjmJYV64k5HmN6" target="_blank" rel="noopener noreferrer" class="text-primary dark:text-blue-400 hover:underline font-bold transition-colors">お問い合わせフォーム</a>
            を通じて行うことができます。
          </p>
        </section>
        
        <div class="mt-12 pt-6 border-t border-outline-variant dark:border-gray-700">
          <p class="text-xs text-on-surface-variant dark:text-gray-400">
            制定日：2026年7月19日<br>
            最終改定日：2026年7月19日
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
useHead({
  title: 'プライバシーポリシー',
  meta: [
    { name: 'description', content: 'たてもじ（縦書きエディタ）のプライバシーポリシーについて記載しています。' }
  ]
})
</script>
