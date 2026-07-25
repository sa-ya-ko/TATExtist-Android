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
            <li><strong>保存データ（.tategaki）について</strong>: パソコンに保存されるファイルの拡張子は <code>.tategaki</code> となりますが、中身は一般的な「JSONデータ」です。拡張子を <code>.json</code> に変更することで、他のテキストエディタでも中身を直接編集することが可能です。</li>
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
          <p class="mb-4 text-gray-700">｢TATExtist｣へのご質問やご要望は下記お問い合わせフォームよりお願いいたします</p>
          <div class="bg-gray-50 rounded text-gray-600 flex justify-center w-full">
            <iframe src="https://docs.google.com/forms/d/e/1FAIpQLScLrtHzwt_F52GWVe0Eq1938utwTvToNwPJQkg5iNo8AHIL3w/viewform?embedded=true" width="100%" height="1033" frameborder="0" marginheight="0" marginwidth="0">読み込んでいます…</iframe>
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
  { id: 'feedback', label: 'フィードバック' }
]
</script>
