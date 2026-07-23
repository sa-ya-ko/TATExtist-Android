export default defineEventHandler(async (event) => {
  const token = getCookie(event, 'google_access_token')

  if (!token) {
    throw createError({ statusCode: 401, statusMessage: 'Unauthorized. No access token found.' })
  }

  try {
    // Fetch file list from Google Drive API
    const response = await $fetch('https://www.googleapis.com/drive/v3/files', {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`
      },
      query: {
        q: "mimeType='application/json' and name contains '.tategaki'",
        spaces: 'drive',
        fields: "files(id, name, modifiedTime)"
      }
    })

    return (response as any).files || []
  } catch (err: any) {
    console.error('Google Drive List Error:', err.data || err)
    
    if (err.statusCode === 401) {
      throw createError({ statusCode: 401, statusMessage: 'Token expired or invalid' })
    }
    
    throw createError({ statusCode: 500, statusMessage: 'Failed to fetch files from Google Drive' })
  }
})
