<template>
  <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" @click.self="closeHelpModal">
    <div class="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col overflow-hidden">
      <!-- Header -->
      <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 shrink-0">
        <h2 class="text-xl font-bold text-gray-800 flex items-center gap-2">
          <span class="material-symbols-outlined">help</span>
          ヘルプ
        </h2>
        <button @click="closeHelpModal" class="text-gray-500 hover:bg-gray-100 p-2 rounded-full transition-colors">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <!-- Tabs -->
      <div class="flex border-b border-gray-200 px-6 shrink-0 pt-2">
        <button 
          v-for="tab in tabs" 
          :key="tab.id"
          @click="activeTab = tab.id"
          class="px-4 py-2 text-sm font-medium border-b-2 transition-colors"
          :class="activeTab === tab.id ? 'border-primary text-primary' : 'border-transparent text-gray-500 hover:text-gray-700'"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Content -->
      <div class="p-6 overflow-y-auto flex-1 text-gray-700 text-sm leading-relaxed">
        <!-- 使い方とショートカット -->
        <div v-if="activeTab === 'usage'">
          <h3 class="font-bold text-lg mb-3 border-b pb-2">基本的な使い方</h3>
          <ul class="list-disc pl-5 space-y-2 mb-6">
            <li>
              <strong>ページのレイアウト</strong>: 右側のサイドバーから｢レイアウト｣を変更できます。
              <ul class="list-[circle] pl-5 mt-2 space-y-1">
                <li><strong>｢横にページを追加｣</strong>: 向かって左側に自動でページが追加され、常に現在のページが右側に表示されます。</li>
                <li><strong>｢縦にページを追加｣</strong>: ページ追加機能は手動となります。｢新しいページを追加｣ボタンで現在のページの下に次のページを追加できます。ページを削除したい場合は、｢ページを削除｣ボタンをクリックすることで、1ページずつ削除できます。</li>
              </ul>
            </li>
            <li><strong>エディタの見た目</strong>: レイアウトが｢横にページを追加｣を選択しているとき、サイドバーの「見た目」から｢巻物(切れ目のない長い紙)｣か｢分割ページ(ページの区切り線が表示される見た目)｣かを選ぶことができます。</li>
            <li><strong>ルビの振り方</strong>: ルビを振りたい文字を選択し、上のメニューから「ルビ付与」ボタンを押してください。</li>
            <li><strong>ルビの削除の仕方</strong>: すでにルビが振られている文字を選択し、上のメニューから｢ルビ解除｣ボタンを押すと、ルビだけが消えて元の文字に戻ります。</li>
            <li><strong>文字の装飾</strong>: 文字を選択して、上のメニューから太字・斜体・下線・文字色・フォントサイズ・書体（明朝体や手書き風など）を変更できます。</li>
            <li>
              <strong>字下げ（インデント）</strong>:
              <ul class="list-[circle] pl-5 mt-2 space-y-1">
                <li><strong>1字下げ</strong>: 文字を選択せずに <code>Tab</code> キーを押すと、カーソル位置に全角スペースが挿入され、1文字分の字下げができます。</li>
                <li><strong>ブロックインデント（段落全体の字下げ）</strong>: 文字を選択した状態で <code>Tab</code> キーを押すか、メニューバーの「字下げ」ボタンを押すと、段落全体をインデントできます。長文の引用などに便利です。</li>
              </ul>
            </li>
            <li><strong>印刷</strong>: ファイルメニューの｢印刷する｣を選ぶと、ブラウザの印刷画面が開きます。※ブラウザの印刷設定によって、用紙の端にURLや日付などが印字される場合があります。不要な場合は印刷画面の設定(詳細設定など)からオフにして印刷してください。</li>
            <li><strong>PDFダウンロード</strong>: ファイルメニューの｢PDFとしてダウンロード｣を選ぶと、縦書きのままPDFファイルとして保存できます。</li>
            <li>
              <strong>PCとモバイルでの表示について</strong>: ｢縦にページを追加｣を選択している場合、PCとモバイルでは1ページに表示できる量が異なります。PCで表示されていた文章の一部が、モバイルではページの表示範囲を超えて見えなくなることがあります。文章自体が削除されたわけではありません。その場合は、PCで確認するか、モバイルで｢新しいページを追加｣を使ってページを分けてください。
            </li>
          </ul>

          <h3 class="font-bold text-lg mb-3 border-b pb-2">ショートカットキー</h3>
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-gray-50">
                <th class="border p-2 w-1/3">操作</th>
                <th class="border p-2">Windows</th>
                <th class="border p-2">Mac</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="border p-2">元に戻す</td>
                <td class="border p-2">Ctrl + Z</td>
                <td class="border p-2">Cmd + Z</td>
              </tr>
              <tr>
                <td class="border p-2">やり直す</td>
                <td class="border p-2">Ctrl + Y</td>
                <td class="border p-2">Cmd + Shift + Z</td>
              </tr>
              <tr>
                <td class="border p-2">太字</td>
                <td class="border p-2">Ctrl + B</td>
                <td class="border p-2">Cmd + B</td>
              </tr>
              <tr>
                <td class="border p-2">斜体</td>
                <td class="border p-2">Ctrl + I</td>
                <td class="border p-2">Cmd + I</td>
              </tr>
              <tr>
                <td class="border p-2">下線</td>
                <td class="border p-2">Ctrl + U</td>
                <td class="border p-2">Cmd + U</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- フィードバック -->
        <div v-if="activeTab === 'feedback'">
          <h3 class="font-bold text-lg mb-3">フィードバック</h3>
          <p class="mb-4 text-gray-700">｢たてもじ｣へのご質問やご要望は下記お問い合わせフォームよりお願いいたします</p>
          <div class="bg-gray-50 rounded text-gray-600 flex justify-center w-full">
            <iframe src="https://docs.google.com/forms/d/e/1FAIpQLScLrtHzwt_F52GWVe0Eq1938utwTvToNwPJQkg5iNo8AHIL3w/viewform?embedded=true" width="100%" height="1033" frameborder="0" marginheight="0" marginwidth="0">読み込んでいます…</iframe>
          </div>
        </div>

        <!-- 利用規約 -->
        <div v-if="activeTab === 'terms'">
          <h3 class="font-bold text-lg mb-3">利用規約</h3>
          <div class="bg-gray-50 p-4 rounded text-gray-600 space-y-4 max-h-[60vh] overflow-y-auto">
            <p>本利用規約（以下「本規約」といいます。）は、開発者（以下「当方」といいます。）が提供する縦書きエディタ（以下「本サービス」といいます。）の利用条件を定めるものです。ユーザーの皆さまには、本規約に従って本サービスをご利用いただきます。</p>

            <h4 class="font-bold text-gray-800">第1条（適用およびプライバシー）</h4>
            <ul class="list-disc pl-5 space-y-1">
              <li>本規約は、ユーザーと本サービスの利用に関わる一切の関係に適用されるものとします。本サービスを利用することにより、ユーザーは本規約に同意したものとみなされます。</li>
              <li>本サービスにおける個人情報等の取り扱いについては、別途定めるプライバシーポリシーに従うものとします。</li>
            </ul>

            <h4 class="font-bold text-gray-800">第2条（権利の帰属とデータ処理）</h4>
            <ul class="list-disc pl-5 space-y-1">
              <li>ユーザーが本サービスを利用して作成した文章その他のコンテンツ（以下「ユーザーコンテンツ」といいます。）に関する著作権その他一切の権利は、ユーザーまたは正当な権利者に帰属します。</li>
              <li>当方は、ユーザーコンテンツについて所有権その他の権利を取得しません。</li>
              <li>本サービスでは、ユーザーコンテンツは、ユーザーがクラウド保存機能等を利用する場合を除き、ユーザーの端末内に保存されます。クラウド保存機能を利用した場合は、ユーザーが指定したクラウドサービス上に保存されます。当方は、本サービスを提供するために必要な期間および必要な範囲に限り、ユーザーコンテンツを保存、複製、送信その他必要な処理を行うことができます。</li>
              <li>当方は、ユーザーコンテンツを、ユーザーの明示的な同意なく、AIモデルの学習その他本サービスの提供に必要な範囲を超える目的で利用しません。</li>
              <li>本サービス自体（プログラム、デザイン、ロゴ等）に関する著作権および知的財産権は、すべて当方または正当な権利者に帰属します。</li>
            </ul>

            <h4 class="font-bold text-gray-800">第3条（アカウント管理）</h4>
            <p>本サービスにおいてGoogleアカウント等の外部サービス連携を利用する場合、ユーザーは自己の認証情報を適切に管理するものとします。ユーザーの認証情報の管理不十分、使用上の過誤または第三者による不正使用によって生じた損害について、当方の故意または重過失による場合を除き、当方は責任を負いません。</p>

            <h4 class="font-bold text-gray-800">第4条（禁止事項）</h4>
            <p>ユーザーは、本サービスの利用にあたり、以下の行為をしてはなりません。</p>
            <ul class="list-disc pl-5 space-y-1">
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

            <h4 class="font-bold text-gray-800">第5条（本サービスの利用制限および提供の停止・終了等）</h4>
            <ul class="list-disc pl-5 space-y-1">
              <li>当方は、ユーザーが本規約に違反した場合、事前の通知なく本サービスの利用を制限または停止できるものとします。</li>
              <li>当方は、システムの保守、障害対応、不可抗力、その他必要と判断した場合、事前に通知することなく本サービスの提供を一時的に停止または中断できるものとします。</li>
              <li>当方は、本サービスの内容または仕様を変更することがあります。ユーザーに重大な影響を及ぼす変更については、可能な限り事前に通知します。また、当方は、合理的な期間をもって事前に通知した上で、本サービスの全部または一部の提供を終了することがあります。</li>
            </ul>

            <h4 class="font-bold text-gray-800">第6条（損害賠償および紛争の解決）</h4>
            <ul class="list-disc pl-5 space-y-1">
              <li>ユーザーが本規約に違反したこと、または本サービスの利用に関連して当方に損害を生じさせた場合、当該ユーザーは当方に対しその損害を賠償するものとします。</li>
              <li>ユーザーコンテンツに起因して第三者との間で著作権侵害等の紛争が生じた場合は、当方の故意または重過失による場合を除き、ユーザー自身の責任と費用において解決するものとします。</li>
            </ul>

            <h4 class="font-bold text-gray-800">第7条（免責事項）</h4>
            <ul class="list-disc pl-5 space-y-1">
              <li>当方は、本サービスに事実上または法律上の瑕疵（バグ、エラー、特定の目的への適合性、セキュリティの欠陥等）がないことを明示的にも黙示的にも保証しておりません。</li>
              <li>本サービスがGoogle ドライブ等の外部サービスと連携する機能について、当該外部サービスの障害、仕様変更または提供終了により当該機能が利用できなくなった場合、当方は責任を負いません。また、外部サービスの利用にあたっては、当該サービス提供者が定める利用規約等にも従うものとします。</li>
              <li>当方の故意または重過失による場合を除き、本サービスの利用または利用不能により生じた損害について、当方は責任を負いません。なお、当方に過失（重過失を除く）がある場合であっても、当方が負う損害賠償責任は、ユーザーに現実に生じた直接かつ通常の損害に限られ、その上限は当該ユーザーが過去12か月間に当方へ支払った利用料金の額（無料サービスの場合は0円）とします。</li>
              <li>本条の免責規定は、消費者契約法その他の法令により無効または制限される場合には、その適用される範囲で効力を有するものとします。</li>
            </ul>

            <h4 class="font-bold text-gray-800">第8条（権利義務の譲渡の禁止）</h4>
            <p>ユーザーは、当方の事前の承諾なく、本規約に基づく権利もしくは義務を第三者に譲渡し、または担保に供することはできません。</p>

            <h4 class="font-bold text-gray-800">第9条（利用規約の変更）</h4>
            <p>当方は、必要と判断した場合には、あらかじめ本サービス上での掲載その他当方が適当と判断する方法により変更内容および効力発生時期を周知した上で、本規約を変更することができるものとします。変更後に本サービスを利用した場合は、ユーザーは変更後の規約に同意したものとみなします。</p>

            <h4 class="font-bold text-gray-800">第10条（分離可能性）</h4>
            <p>本規約のいずれかの条項またはその一部が、消費者契約法その他の法令等により無効または執行不能と判断された場合であっても、本規約の残りの規定は、継続して完全に効力を有するものとします。</p>

            <h4 class="font-bold text-gray-800">第11条（準拠法・裁判管轄）</h4>
            <p>本規約の解釈にあたっては、日本法を準拠法とします。本サービスに関して紛争が生じた場合には、静岡地方裁判所または静岡簡易裁判所を第一審の専属的合意管轄裁判所とします。</p>

            <div class="mt-6 pt-4 border-t border-gray-200">
              <p class="text-xs text-gray-500 mb-2">
                制定日：2026年7月19日<br>
                最終改定日：2026年7月19日
              </p>
              <h4 class="font-bold text-gray-800 mb-1">お問い合わせ先</h4>
              <p>
                本規約に関するお問い合わせは、
                <a href="https://forms.gle/w37AjmJYV64k5HmN6" target="_blank" rel="noopener noreferrer" class="text-primary hover:underline font-bold">お問い合わせフォーム</a>
                よりご連絡ください。
              </p>
            </div>
          </div>
        </div>

        <!-- プライバシーポリシー -->
        <div v-if="activeTab === 'privacy'">
          <h3 class="font-bold text-lg mb-3">プライバシーポリシー</h3>
          <div class="bg-gray-50 p-4 rounded text-gray-600 space-y-4 max-h-[60vh] overflow-y-auto">
            <p>開発者（以下「当方」といいます。）は、当方が提供する縦書きエディタ（以下「本サービス」といいます。）における、ユーザーの個人情報およびデータの取り扱いについて、以下のとおりプライバシーポリシー（以下「本ポリシー」といいます。）を定めます。</p>

            <h4 class="font-bold text-gray-800">1. 取り扱う情報</h4>
            <p>当方は、本サービスの提供にあたり、必要な範囲で以下の情報を取得し、または機能提供に必要な範囲で一時的に処理する場合があります。</p>
            <ul class="list-disc pl-5 space-y-1">
              <li><strong>利用環境情報：</strong>ブラウザの種類、オペレーティングシステム、アクセス日時、閲覧ページ、参照元その他利用状況に関する情報。</li>
              <li><strong>コンテンツデータ：</strong>ユーザーコンテンツおよび設定データ。本サービスでは原則としてユーザーの端末内に保存されますが、PDF生成やクラウド保存機能を利用する場合には、当該機能の提供に必要な範囲で一時的にサーバー上で処理されることがあります。</li>
              <li><strong>認証情報：</strong>クラウド連携機能を利用する場合には、当該機能の提供に必要な認証情報（アクセストークン等）を一時的に取り扱うことがあります。認証情報は必要な範囲でのみ利用し、恒久的に保存しません。</li>
              <li><strong>お問い合わせ情報：</strong>フィードバックやお問い合わせフォームを通じてご提供いただいた氏名（任意）、メールアドレスその他お問い合わせ対応に必要な情報。</li>
            </ul>

            <h4 class="font-bold text-gray-800">2. 情報の利用目的</h4>
            <p>当方は、取り扱う情報を以下の目的で利用します。</p>
            <ul class="list-disc pl-5 space-y-1">
              <li>本サービスの提供、運営、および機能（PDF生成、クラウド保存等）の実行のため</li>
              <li>システムの保守、不具合の調査、セキュリティ向上のため</li>
              <li>本サービスの利用状況の分析およびサービス改善のため</li>
              <li>ユーザーからのお問い合わせへの対応のため</li>
            </ul>
            <div class="mt-3 p-3 bg-blue-50 border border-blue-100 rounded text-sm text-gray-700">
              <strong>【データ保存とAI学習に関する方針】</strong><br>
              本サービスでは、機能提供のためにユーザーコンテンツをサーバー上で一時的に処理する場合がありますが、処理完了後は速やかに破棄し、当方のデータベースへ恒久的に保存することはありません。また、当方は、ユーザーコンテンツを、ユーザーの明示的な同意なくAIモデルの学習その他本サービスの提供に必要な範囲を超える目的で利用しません。
            </div>

            <h4 class="font-bold text-gray-800">3. 第三者への提供と共有</h4>
            <p>当方は、以下の場合を除き、ユーザーの個人情報およびユーザーコンテンツを第三者に提供・共有することはありません。</p>
            <ul class="list-disc pl-5 space-y-1">
              <li>ユーザー本人の同意がある場合（ユーザーが明示的に指定したクラウドサービスへの送信を含みます）</li>
              <li>法令に基づき開示することが必要である場合</li>
              <li>人の生命、身体または財産の保護のために必要があり、本人の同意を得ることが困難である場合</li>
              <li>当方が本サービスの提供に必要な範囲内において、業務委託先（ホスティング環境等のクラウドプロバイダ）に一時的な処理を委託する場合</li>
            </ul>

            <h4 class="font-bold text-gray-800">4. Cookieおよびブラウザストレージ</h4>
            <p>本サービスでは、利用状況の把握や操作性の向上のために、Cookie（クッキー）またはローカルストレージ（LocalStorage）などの技術を使用しています。ユーザーはブラウザの設定によりCookie等を無効化することができます。ただし、その場合、本サービスの一部機能（設定の自動保存など）が正常に利用できないことがあります。</p>
            <p class="mt-2">本サービスでは、サービスの利用状況の分析および改善を目的としてGoogle Analytics 4（以下「Google Analytics」といいます。）を利用しています。</p>
            <p class="mt-2">Google AnalyticsはCookie等を利用して利用状況に関する情報を収集します。収集された情報はGoogleに送信され、Googleのプライバシーポリシーに基づいて取り扱われます。Google Analyticsの詳細については<a href="https://marketingplatform.google.com/about/analytics/terms/jp/" target="_blank" rel="noopener noreferrer" class="text-primary hover:underline">Google Analytics利用規約</a>および<a href="https://policies.google.com/privacy?hl=ja" target="_blank" rel="noopener noreferrer" class="text-primary hover:underline">Googleのプライバシーポリシー</a>をご確認ください。</p>

            <h4 class="font-bold text-gray-800">5. 外部サービスとの連携</h4>
            <p>本サービスにおいて、Google ドライブその他の外部サービスとの連携機能を利用する場合、当方は当該機能の提供に必要な範囲のデータのみを当該外部サービスに送信します。当方は、外部サービスとの連携に必要な範囲を超えてユーザーのデータへアクセスすることはありません。当方は、Google Drive APIを通じてアクセスするユーザーデータを、本サービスの機能提供以外の目的では利用しません。外部サービス上でのデータの取り扱いについては、各サービス提供者のプライバシーポリシーが適用されます。</p>

            <h4 class="font-bold text-gray-800">6. プライバシーポリシーの変更</h4>
            <p>当方は、法令の改正や本サービスの仕様変更に伴い、必要に応じて本ポリシーを変更することがあります。変更を行った場合は、本サービス上での掲示その他適当な方法によりユーザーに通知します。変更後に本サービスを利用した場合、ユーザーは変更後の本ポリシーに同意したものとみなされます。</p>

            <h4 class="font-bold text-gray-800">7. お問い合わせ</h4>
            <p>
              ユーザーは、本ポリシーまたは個人情報の取り扱いに関するお問い合わせを、
              <a href="https://forms.gle/w37AjmJYV64k5HmN6" target="_blank" rel="noopener noreferrer" class="text-primary hover:underline font-bold">お問い合わせフォーム</a>
              を通じて行うことができます。
            </p>
            
            <div class="mt-6 pt-4 border-t border-gray-200">
              <p class="text-xs text-gray-500">
                制定日：2026年7月19日<br>
                最終改定日：2026年7月19日
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useEditorState } from '../composables/useEditorState'

const { closeHelpModal } = useEditorState()

const activeTab = ref('usage')
const tabs = [
  { id: 'usage', label: '使い方・ショートカット' },
  { id: 'feedback', label: 'フィードバック' },
  { id: 'terms', label: '利用規約' },
  { id: 'privacy', label: 'プライバシーポリシー' }
]
</script>
