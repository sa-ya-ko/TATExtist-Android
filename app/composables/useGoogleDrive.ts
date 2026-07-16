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

export const useGoogleDrive = () => {
  const config = useRuntimeConfig()
  const CLIENT_ID = config.public.googleClientId as string
  const API_KEY = config.public.googleApiKey as string

  const isGoogleApiReady = ref(false)
  const isAuthenticated = ref(false)
  const tokenClient = ref<any>(null)

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
            throw tokenResponse
          }
          isAuthenticated.value = true
        },
      })
    }
  }

  // ログイン要求
  const handleAuthClick = () => {
    if (tokenClient.value) {
      if (window.gapi.client.getToken() === null) {
        tokenClient.value.requestAccessToken({prompt: 'consent'})
      } else {
        tokenClient.value.requestAccessToken({prompt: ''})
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
      alert("Google Driveに保存するには、先にGoogleアカウントで認証（ログイン）してください。")
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

  return {
    isGoogleApiReady,
    isAuthenticated,
    initGoogleDrive,
    handleAuthClick,
    handleSignoutClick,
    saveToDrive
  }
}
