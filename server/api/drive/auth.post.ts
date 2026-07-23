export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  const code = body.code

  if (!code) {
    throw createError({ statusCode: 400, statusMessage: 'No authorization code provided' })
  }

  const config = useRuntimeConfig()
  
  if (!config.public.googleClientId || !config.googleClientSecret) {
    throw createError({ statusCode: 500, statusMessage: 'Google Client ID or Secret is not configured' })
  }

  try {
    // Exchange the authorization code for an access token
    const response = await $fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: new URLSearchParams({
        client_id: config.public.googleClientId,
        client_secret: config.googleClientSecret,
        code: code,
        grant_type: 'authorization_code',
        redirect_uri: 'postmessage' // Required when using GIS popup mode
      })
    })

    const { access_token, expires_in, refresh_token } = response as any

    // Store the access token in a secure, HTTP-only cookie
    setCookie(event, 'google_access_token', access_token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      maxAge: expires_in,
      path: '/'
    })

    // If a refresh token is provided (usually only on first consent), store it as well
    if (refresh_token) {
      setCookie(event, 'google_refresh_token', refresh_token, {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        maxAge: 60 * 60 * 24 * 30 * 6, // roughly 6 months
        path: '/'
      })
    }

    return { success: true }
  } catch (err: any) {
    console.error('Google Auth Error:', err.data || err)
    throw createError({ statusCode: 401, statusMessage: 'Failed to authenticate with Google' })
  }
})
