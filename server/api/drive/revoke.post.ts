export default defineEventHandler(async (event) => {
  const token = getCookie(event, 'google_access_token')
  
  // 1. サーバー側でCookie（金庫）を破壊する
  deleteCookie(event, 'google_access_token', { path: '/' })
  deleteCookie(event, 'google_refresh_token', { path: '/' })

  if (token) {
    try {
      // 2. Google側にも「このトークンはもう無効化して」とお願いする
      await $fetch('https://oauth2.googleapis.com/revoke', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams({ token: token })
      })
    } catch (err: any) {
      console.error('Google Token Revoke Error:', err.data || err)
      // Cookieはすでに消しているので、Google側のエラーは無視して正常終了とする
    }
  }

  return { success: true }
})
