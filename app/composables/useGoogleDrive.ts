import { ref } from 'vue'
import { useEditorState } from './useEditorState'

declare global {
  interface Window {
    gapi: any;
    google: any;
  }
}

const DISCOVERY_DOC = 'https://www.googleapis.com/discovery/v1/apis/drive/v3/rest'
const SCOPES = 'https://www.googleapis.com/auth/drive.file'

// 状態変数を関数の外に置き、アプリ全体で共有されるようにします
const isGoogleApiReady = ref(false)
const isAuthenticated = ref(false)
const tokenClient = ref<any>(null)
const isDriveModalOpen = ref(false)
const driveFiles = ref<any[]>([])
const isLoadingFiles = ref(false)
let pendingAction: (() => void) | null = null // ログイン後に実行する処理を記憶する変数

export const useGoogleDrive = () => {
  const config = useRuntimeConfig()
  const CLIENT_ID = config.public.googleClientId as string
  const API_KEY = config.public.googleApiKey as string

  const { documentTitle, pages, paperSize, margins } = useEditorState()

  // Google APIの初期化
  const initGoogleDrive = () => {
    if (typeof window === 'undefined') return

    // gapi (Google API Client) の初期化
    if (window.gapi) {
      window.gapi.load('client', async () => {
        try {
          await window.gapi.client.init({
            apiKey: API_KEY,
            discoveryDocs: [DISCOVERY_DOC],
          })
          isGoogleApiReady.value = true
        } catch (err) {
          console.error('Google API Initialization Error:', err)
        }
      })
    }

    // Google Identity Services (GIS) の初期化
    if (window.google?.accounts?.oauth2) {
      tokenClient.value = window.google.accounts.oauth2.initTokenClient({
        client_id: CLIENT_ID,
        scope: SCOPES,
        callback: (tokenResponse: any) => {
          if (tokenResponse.error !== undefined) {
            console.error('Auth Error:', tokenResponse.error)
            pendingAction = null // エラー時はキャンセル
            throw tokenResponse
          }
          isAuthenticated.value = true
          
          // ログイン成功時、待たせていた処理（保存や開く）があれば自動で実行する
          if (pendingAction) {
            pendingAction()
            pendingAction = null
          }
        },
      })
    }
  }

  // ログイン要求
  const handleAuthClick = () => {
    if (tokenClient.value) {
      if (window.gapi.client.getToken() === null) {
        tokenClient.value.requestAccessToken({ prompt: 'consent' })
      } else {
        tokenClient.value.requestAccessToken({ prompt: '' })
      }
    } else {
      alert("Google APIの準備が完了していません。時間をおいて再度お試しください。")
    }
  }

  // サインアウト
  const handleSignoutClick = () => {
    const token = window.gapi.client.getToken()
    if (token !== null) {
      window.google.accounts.oauth2.revoke(token.access_token, () => {
        window.gapi.client.setToken('')
        isAuthenticated.value = false
      })
    }
  }

  // 現在のエディタの内容を保存
  const saveToDrive = async () => {
    if (!isAuthenticated.value) {
      // 未ログインの場合は、保存処理を予約してからログイン画面を出す
      pendingAction = saveToDrive
      handleAuthClick()
      return
    }

    // 保存するデータ（tategakiフォーマット）
    const data = {
      documentTitle: documentTitle.value,
      pages: pages.value,
      paperSize: paperSize.value,
      margins: margins.value,
      version: '1.0'
    }

    const fileContent = JSON.stringify(data, null, 2)
    const fileName = (documentTitle.value || '無題のドキュメント') + '.tategaki'

    // Google Drive APIでマルチパートアップロードするためのデータ作成
    const boundary = '-------314159265358979323846'
    const delimiter = "\r\n--" + boundary + "\r\n"
    const close_delim = "\r\n--" + boundary + "--"

    const metadata = {
      name: fileName,
      mimeType: 'application/json'
    }

    const multipartRequestBody =
      delimiter +
      'Content-Type: application/json\r\n\r\n' +
      JSON.stringify(metadata) +
      delimiter +
      'Content-Type: application/json\r\n\r\n' +
      fileContent +
      close_delim

    try {
      const request = window.gapi.client.request({
        path: '/upload/drive/v3/files',
        method: 'POST',
        params: { uploadType: 'multipart' },
        headers: {
          'Content-Type': 'multipart/related; boundary="' + boundary + '"'
        },
        body: multipartRequestBody
      })

      request.execute((file: any) => {
        if (file && file.id) {
          alert(`Google Driveに保存しました！（ファイルID: ${file.id}）`)
        } else {
          alert("保存に失敗しました。")
        }
      })
    } catch (err) {
      console.error("Upload Error:", err)
      alert("保存中にエラーが発生しました。")
    }
  }

  const loadFileListFromDrive = async () => {
    if (!isAuthenticated.value) {
      // 未ログインの場合は、読み込み処理を予約してからログイン画面を出す
      pendingAction = loadFileListFromDrive
      handleAuthClick()
      return
    }
    isLoadingFiles.value = true
    isDriveModalOpen.value = true
    try {
      const response = await window.gapi.client.drive.files.list({
        q: "mimeType='application/json' and name contains '.tategaki'",
        spaces: 'drive',
        fields: "files(id, name, modifiedTime)"
      })
      driveFiles.value = response.result.files || []
    } catch (err) {
      console.error('File list error:', err)
      alert("ファイル一覧の取得に失敗しました。")
      isDriveModalOpen.value = false
    } finally {
      isLoadingFiles.value = false
    }
  }

  const loadFromDrive = async (fileId: string) => {
    try {
      const response = await window.gapi.client.drive.files.get({
        fileId: fileId,
        alt: 'media'
      })

      let data;
      if (typeof response.result === 'string') {
        data = JSON.parse(response.result)
      } else if (response.body) {
        data = JSON.parse(response.body)
      } else {
        data = response.result
      }

      if (data && data.pages) {
        if (data.documentTitle) documentTitle.value = data.documentTitle
        pages.value = data.pages.map((p: any) => ({ ...p, id: Date.now() + Math.random() }))
        if (data.paperSize) paperSize.value = data.paperSize
        if (data.margins) margins.value = data.margins

        localStorage.setItem('vertical-editor-pages', JSON.stringify(pages.value))

        alert("ファイルを読み込みました。")
        isDriveModalOpen.value = false
      } else {
        throw new Error("ファイルの内容が正しい形式ではありません。")
      }
    } catch (err: any) {
      console.error('File load error:', err)
      const errorMessage = err?.result?.error?.message || err?.message || String(err)
      alert("ファイルの読み込みに失敗しました。\nエラー詳細: " + errorMessage)
    }
  }

  return {
    isGoogleApiReady,
    isAuthenticated,
    isDriveModalOpen,
    driveFiles,
    isLoadingFiles,
    initGoogleDrive,
    handleAuthClick,
    handleSignoutClick,
    saveToDrive,
    loadFileListFromDrive,
    loadFromDrive
  }
}
