export default defineEventHandler(async (event) => {
  const token = getCookie(event, 'google_access_token')
  if (!token) {
    throw createError({ statusCode: 401, statusMessage: 'Unauthorized. No access token found.' })
  }

  const query = getQuery(event)
  const fileId = query.fileId as string

  if (!fileId) {
    throw createError({ statusCode: 400, statusMessage: 'File ID is required' })
  }

  try {
    const response = await $fetch(`https://www.googleapis.com/drive/v3/files/${fileId}`, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`
      },
      query: {
        alt: 'media'
      }
    })
    
    return response
  } catch (err: any) {
    console.error('Google Drive Load Error:', err.data || err)
    if (err.statusCode === 401) {
      throw createError({ statusCode: 401, statusMessage: 'Token expired or invalid' })
    }
    throw createError({ statusCode: 500, statusMessage: 'Failed to load file from Google Drive' })
  }
})
