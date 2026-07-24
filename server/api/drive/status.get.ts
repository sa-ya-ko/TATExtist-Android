export default defineEventHandler((event) => {
  const token = getCookie(event, 'google_access_token')
  return { authenticated: !!token }
})
