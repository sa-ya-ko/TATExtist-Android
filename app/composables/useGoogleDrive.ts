import { ref } from 'vue'
import { useEditorState, sanitizeHTML } from './useEditorState'

declare global {
  interface Window {
    google: any;
  }
}

// 状態変数を関数の外に置き、アプリ全体で共有されるようにします
const isGoogleApiReady = ref(false)
const isAuthenticated = ref(false)
const codeClient = ref<any>(null)
const isDriveModalOpen = ref(false)
const driveFiles = ref<any[]>([])
const isLoadingFiles = ref(false)
let pendingAction: (() => void) | null = null // ログイン後に実行する処理を記憶する変数

export const useGoogleDrive = () => {
  const config = useRuntimeConfig()
  const CLIENT_ID = config.public.googleClientId as string

  const { documentTitle, pages, paperSize, margins } = useEditorState()

  // Google APIの初期化
  const initGoogleDrive = () => {
    if (typeof window === 'undefined') return

    // Google Identity Services (GIS) の初期化（gapiは廃止）
    if (window.google?.accounts?.oauth2) {
      codeClient.value = window.google.accounts.oauth2.initCodeClient({
        client_id: CLIENT_ID,
        scope: 'https://www.googleapis.com/auth/drive.file',
        ux_mode: 'popup',
        callback: async (response: any) => {
          if (response.error !== undefined) {
            console.error('Auth Error:', response.error)
            pendingAction = null // エラー時はキャンセル
            return
          }
          
          try {
            // サーバーサイドAPIにコードを送信してトークンに交換し、Cookieに保存してもらう
            await $fetch('/api/drive/auth', {
              method: 'POST',
              body: { code: response.code }
            })
            
            isAuthenticated.value = true
            
            // ログイン成功時、待たせていた処理（保存や開く）があれば自動で実行する
            if (pendingAction) {
              pendingAction()
              pendingAction = null
            }
          } catch (error) {
            console.error('Failed to exchange code:', error)
            alert('ログインに失敗しました。')
          }
        },
      })
      isGoogleApiReady.value = true
    }
  }

  // ログイン要求
  const handleAuthClick = () => {
    if (codeClient.value) {
      codeClient.value.requestCode()
    } else {
      alert("Google APIの準備が完了していません。時間をおいて再度お試しください。")
    }
  }

  // サインアウト（自社API経由で解除）
  const handleSignoutClick = async () => {
    try {
      await $fetch('/api/drive/revoke', { method: 'POST' })
    } catch (e) {
      console.error(e)
    } finally {
      isAuthenticated.value = false
      alert('Google Driveとの連携を解除しました。')
    }
  }

  // 現在のエディタの内容を保存（自社API経由）
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

    try {
      const response = await $fetch('/api/drive/save', {
        method: 'POST',
        body: data
      })
      
      const fileId = (response as any).id
      if (fileId) {
        alert(`Google Driveに保存しました！`)
      } else {
        alert("保存に失敗しました。")
      }
    } catch (err: any) {
      console.error("Upload Error:", err)
      if (err.response?.status === 401) {
        isAuthenticated.value = false
        alert("認証の有効期限が切れました。再度保存をお試しください。")
        pendingAction = saveToDrive
        handleAuthClick()
      } else {
        alert("保存中にエラーが発生しました。")
      }
    }
  }

  // ファイル一覧の取得（自社API経由）
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
      const files = await $fetch('/api/drive/list')
      driveFiles.value = files as any[]
    } catch (err: any) {
      console.error('File list error:', err)
      if (err.response?.status === 401) {
        isAuthenticated.value = false
        isDriveModalOpen.value = false
        alert("認証の有効期限が切れました。再度お試しください。")
        pendingAction = loadFileListFromDrive
        handleAuthClick()
      } else {
        alert("ファイル一覧の取得に失敗しました。")
        isDriveModalOpen.value = false
      }
    } finally {
      isLoadingFiles.value = false
    }
  }

  // ファイルの読み込み（自社API経由）
  const loadFromDrive = async (fileId: string) => {
    try {
      const data = await $fetch(`/api/drive/load?fileId=${fileId}`) as any

      if (data && data.pages) {
        if (data.documentTitle) documentTitle.value = data.documentTitle
        pages.value = data.pages.map((p: any) => ({ ...p, content: sanitizeHTML(p.content), id: Date.now() + Math.random() }))
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
      if (err.response?.status === 401) {
        isAuthenticated.value = false
        alert("認証の有効期限が切れました。再度読み込みをお試しください。")
      } else {
        const errorMessage = err?.message || String(err)
        alert("ファイルの読み込みに失敗しました。\nエラー詳細: " + errorMessage)
      }
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
