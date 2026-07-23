export default defineEventHandler(async (event) => {
  const token = getCookie(event, 'google_access_token')
  if (!token) {
    throw createError({ statusCode: 401, statusMessage: 'Unauthorized. No access token found.' })
  }

  const body = await readBody(event)
  
  // バリデーション (Validation): 想定外のデータ（巨大な文字列や不正な形式）をブロック
  if (!body || !body.pages || !Array.isArray(body.pages)) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid file data format. "pages" array is required.' })
  }

  const fileName = (body.documentTitle || '無題のドキュメント') + '.tategaki'
  const fileContent = JSON.stringify(body)

  // マルチパートアップロード用のデータ組み立て
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
    const response = await $fetch('https://www.googleapis.com/upload/drive/v3/files', {
      method: 'POST',
      query: { uploadType: 'multipart' },
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': `multipart/related; boundary="${boundary}"`
      },
      body: multipartRequestBody
    })
    
    return response
  } catch (err: any) {
    console.error('Google Drive Save Error:', err.data || err)
    if (err.statusCode === 401) {
      throw createError({ statusCode: 401, statusMessage: 'Token expired or invalid' })
    }
    throw createError({ statusCode: 500, statusMessage: 'Failed to save file to Google Drive' })
  }
})
