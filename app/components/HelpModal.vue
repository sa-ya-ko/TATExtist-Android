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
            <li><strong>ページのレイアウト</strong>: 右側のサイドバーから「レイアウト」を変更できます。「横にページを追加」にすると文字数に合わせて自動的にページが増え、「縦にページを追加」にするとWordのように縦にスクロールしながら手動でページを管理できます。</li>
            <li><strong>エディタの見た目</strong>: レイアウトが「横にページを追加」のとき、サイドバーの「見た目」から「分割ページ（原稿用紙のような見開き）」か「巻物（切れ目のない長い紙）」かを選ぶことができます。</li>
            <li><strong>ルビの振り方</strong>: ルビを振りたい文字を選択し、上のメニューから「ルビ付与」ボタンを押してください。</li>
            <li><strong>ルビの削除の仕方</strong>: すでにルビが振られている文字を選択し、上のメニューから「ルビ解除」ボタンを押すと、ルビだけが消えて元の文字に戻ります。</li>
            <li><strong>文字の装飾</strong>: 文字を選択して、上のメニューから太字・斜体・下線・文字色・フォントサイズ・書体（明朝体や手書き風など）を変更できます。</li>
            <li><strong>印刷</strong>: ファイルメニューの「印刷する」を選ぶと、ブラウザの印刷画面が開きます。※ブラウザの印刷設定によって、用紙の端にURLや日付などが印字される場合があります。不要な場合は印刷画面の設定（詳細設定など）からオフにして印刷してください。</li>
            <li><strong>PDFダウンロード</strong>: ファイルメニューの「PDFとしてダウンロード」を選ぶと、縦書きのままPDFファイルとして保存できます。</li>
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

        <!-- 利用規約 -->
        <div v-if="activeTab === 'terms'">
          <h3 class="font-bold text-lg mb-3">利用規約</h3>
          <div class="bg-gray-50 p-4 rounded text-gray-600">
            ここに利用規約の本文が入ります。後ほど正式な文章に差し替えてください。
            <br><br>
            第1条（適用）<br>
            本規約は、ユーザーと当社との間の本サービスの利用に関わる一切の関係に適用されるものとします。
            <br><br>
            第2条（利用登録）<br>
            ...
          </div>
        </div>

        <!-- プライバシーポリシー -->
        <div v-if="activeTab === 'privacy'">
          <h3 class="font-bold text-lg mb-3">プライバシーポリシー</h3>
          <div class="bg-gray-50 p-4 rounded text-gray-600">
            ここにプライバシーポリシーの本文が入ります。後ほど正式な文章に差し替えてください。
            <br><br>
            1. 個人情報の収集方法<br>
            当社は、ユーザーが利用登録をする際に氏名、生年月日、住所、電話番号、メールアドレスなどの個人情報をお尋ねすることがあります。
            <br><br>
            2. 個人情報を収集・利用する目的<br>
            ...
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
  { id: 'terms', label: '利用規約' },
  { id: 'privacy', label: 'プライバシーポリシー' }
]
</script>
