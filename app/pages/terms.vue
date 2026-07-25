<template>
  <div class="h-screen overflow-y-auto bg-surface-container-low dark:bg-gray-900 text-on-surface dark:text-gray-200 py-12 px-4 sm:px-6 lg:px-8 font-sans selection:bg-primary/20">
    <div class="max-w-3xl mx-auto bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6 sm:p-10 border border-outline-variant dark:border-gray-700">
      <!-- パンくずリスト (UI) -->
      <nav class="text-sm mb-6 flex items-center gap-2 text-on-surface-variant dark:text-gray-400">
        <NuxtLink to="/" class="hover:text-primary dark:hover:text-blue-400 transition-colors flex items-center gap-1">
          <span class="material-symbols-outlined text-[16px]">home</span>
          トップ
        </NuxtLink>
        <span class="material-symbols-outlined text-[16px]">chevron_right</span>
        <span class="text-on-surface dark:text-gray-200 font-medium">利用規約</span>
      </nav>

      <div class="mb-8 border-b border-outline-variant dark:border-gray-700 pb-4">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">利用規約</h1>
      </div>

      <div class="space-y-6 text-sm leading-relaxed text-gray-700 dark:text-gray-300">
        <p>本利用規約（以下「本規約」といいます。）は、開発者（以下「当方」といいます。）が提供する縦書きエディタ（以下「本サービス」といいます。）の利用条件を定めるものです。ユーザーの皆さまには、本規約に従って本サービスをご利用いただきます。</p>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">第1条（適用およびプライバシー）</h2>
          <ul class="list-disc pl-5 space-y-2">
            <li>本規約は、ユーザーと本サービスの利用に関わる一切の関係に適用されるものとします。本サービスを利用することにより、ユーザーは本規約に同意したものとみなされます。</li>
            <li>本サービスにおける個人情報等の取り扱いについては、別途定めるプライバシーポリシーに従うものとします。</li>
          </ul>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">第2条（権利の帰属とデータ処理）</h2>
          <ul class="list-disc pl-5 space-y-2">
            <li>ユーザーが本サービスを利用して作成した文章その他のコンテンツ（以下「ユーザーコンテンツ」といいます。）に関する著作権その他一切の権利は、ユーザーまたは正当な権利者に帰属します。</li>
            <li>当方は、ユーザーコンテンツについて所有権その他の権利を取得しません。</li>
            <li>本サービスでは、ユーザーコンテンツは、ユーザーがクラウド保存機能等を利用する場合を除き、ユーザーの端末内に保存されます。クラウド保存機能を利用した場合は、ユーザーが指定したクラウドサービス上に保存されます。当方は、本サービスを提供するために必要な期間および必要な範囲に限り、ユーザーコンテンツを保存、複製、送信その他必要な処理を行うことができます。</li>
            <li>当方は、ユーザーコンテンツを、ユーザーの明示的な同意なく、AIモデルの学習その他本サービスの提供に必要な範囲を超える目的で利用しません。</li>
            <li>本サービス自体（プログラム、デザイン、ロゴ等）に関する著作権および知的財産権は、すべて当方または正当な権利者に帰属します。</li>
          </ul>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">第3条（アカウント管理）</h2>
          <p>本サービスにおいてGoogleアカウント等の外部サービス連携を利用する場合、ユーザーは自己の認証情報を適切に管理するものとします。ユーザーの認証情報の管理不十分、使用上の過誤または第三者による不正使用によって生じた損害について、当方の故意または重過失による場合を除き、当方は責任を負いません。</p>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">第4条（禁止事項）</h2>
          <p class="mb-2">ユーザーは、本サービスの利用にあたり、以下の行為をしてはなりません。</p>
          <ul class="list-disc pl-5 space-y-2">
            <li>法令または公序良俗に違反する行為、および犯罪行為に関連する行為</li>
            <li>本サービスまたは第三者の知的財産権等の権利を侵害する行為</li>
            <li>本サービスのリバースエンジニアリング、プログラムの解析、または無断での複製・改変・再配布</li>
            <li>本サービスのソースコード、APIその他の技術情報を不正に取得しようとする行為、およびセキュリティ機能を回避する行為</li>
            <li>本サービスのサーバーやネットワークに過度な負荷をかける行為（BOT等による自動アクセスを含みます）</li>
            <li>不正アクセス、またはこれを試みる行為</li>
            <li>本サービスの運営を妨害し、またはそのおそれのある行為</li>
            <li>他のユーザーに関する個人情報等を収集する行為、および他人になりすます行為</li>
            <li>その他、当方が不適切と判断する行為</li>
          </ul>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">第5条（本サービスの利用制限および提供の停止・終了等）</h2>
          <ul class="list-disc pl-5 space-y-2">
            <li>当方は、ユーザーが本規約に違反した場合、事前の通知なく本サービスの利用を制限または停止できるものとします。</li>
            <li>当方は、システムの保守、障害対応、不可抗力、その他必要と判断した場合、事前に通知することなく本サービスの提供を一時的に停止または中断できるものとします。</li>
            <li>当方は、本サービスの内容または仕様を変更することがあります。ユーザーに重大な影響を及ぼす変更については、可能な限り事前に通知します。また、当方は、合理的な期間をもって事前に通知した上で、本サービスの全部または一部の提供を終了することがあります。</li>
          </ul>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">第6条（損害賠償および紛争の解決）</h2>
          <ul class="list-disc pl-5 space-y-2">
            <li>ユーザーが本規約に違反したこと、または本サービスの利用に関連して当方に損害を生じさせた場合、当該ユーザーは当方に対しその損害を賠償するものとします。</li>
            <li>ユーザーコンテンツに起因して第三者との間で著作権侵害等の紛争が生じた場合は、当方の故意または重過失による場合を除き、ユーザー自身の責任と費用において解決するものとします。</li>
          </ul>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">第7条（免責事項）</h2>
          <ul class="list-disc pl-5 space-y-2">
            <li>当方は、本サービスに事実上または法律上の瑕疵（バグ、エラー、特定の目的への適合性、セキュリティの欠陥等）がないことを明示的にも黙示的にも保証しておりません。</li>
            <li>本サービスがGoogle ドライブ等の外部サービスと連携する機能について、当該外部サービスの障害、仕様変更または提供終了により当該機能が利用できなくなった場合、当方は責任を負いません。また、外部サービスの利用にあたっては、当該サービス提供者が定める利用規約等にも従うものとします。</li>
            <li>当方の故意または重過失による場合を除き、本サービスの利用または利用不能により生じた損害について、当方は責任を負いません。なお、当方に過失（重過失を除く）がある場合であっても、当方が負う損害賠償責任は、ユーザーに現実に生じた直接かつ通常の損害に限られ、その上限は当該ユーザーが過去12か月間に当方へ支払った利用料金の額（無料サービスの場合は0円）とします。</li>
            <li>本条の免責規定は、消費者契約法その他の法令により無効または制限される場合には、その適用される範囲で効力を有するものとします。</li>
          </ul>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">第8条（権利義務の譲渡の禁止）</h2>
          <p>ユーザーは、当方の事前の承諾なく、本規約に基づく権利もしくは義務を第三者に譲渡し、または担保に供することはできません。</p>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">第9条（利用規約の変更）</h2>
          <p>当方は、必要と判断した場合には、あらかじめ本サービス上での掲載その他当方が適当と判断する方法により変更内容および効力発生時期を周知した上で、本規約を変更することができるものとします。変更後に本サービスを利用した場合は、ユーザーは変更後の規約に同意したものとみなします。</p>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">第10条（分離可能性）</h2>
          <p>本規約のいずれかの条項またはその一部が、消費者契約法その他の法令等により無効または執行不能と判断された場合であっても、本規約の残りの規定は、継続して完全に効力を有するものとします。</p>
        </section>

        <section>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3 border-l-4 border-primary pl-3">第11条（準拠法・裁判管轄）</h2>
          <p>本規約の解釈にあたっては、日本法を準拠法とします。本サービスに関して紛争が生じた場合には、静岡地方裁判所または静岡簡易裁判所を第一審の専属的合意管轄裁判所とします。</p>
        </section>

        <div class="mt-12 pt-6 border-t border-outline-variant dark:border-gray-700">
          <p class="text-xs text-on-surface-variant dark:text-gray-400 mb-4">
            制定日：2026年7月19日<br>
            最終改定日：2026年7月19日
          </p>
          <h4 class="font-bold text-gray-900 dark:text-gray-100 mb-2">お問い合わせ先</h4>
          <p>
            本規約に関するお問い合わせは、
            <a href="https://forms.gle/w37AjmJYV64k5HmN6" target="_blank" rel="noopener noreferrer" class="text-primary dark:text-blue-400 hover:underline font-bold transition-colors">お問い合わせフォーム</a>
            よりご連絡ください。
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
useHead({
  title: '利用規約',
  meta: [
    { name: 'description', content: 'TATExtist(縦書き専用エディタ)の利用規約について記載しています。' }
  ]
})

useSchemaOrg([
  defineBreadcrumb({
    itemListElement: [
      { name: 'トップ', item: '/' },
      { name: '利用規約', item: '/terms' }
    ]
  })
])
</script>
