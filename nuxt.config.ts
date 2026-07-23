// https://nuxt.com/docs/api/configuration/nuxt-config
import tsconfigPaths from 'vite-tsconfig-paths'
import tailwindcss from '@tailwindcss/vite'

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  runtimeConfig: {
    public: {
      googleClientId: '', // .env の NUXT_PUBLIC_GOOGLE_CLIENT_ID で上書きされます
      googleApiKey: ''    // .env の NUXT_PUBLIC_GOOGLE_API_KEY で上書きされます
    }
  },
  app: {
    head: {
      link: [
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Noto+Serif+JP:wght@400;600&display=swap' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Noto+Sans+JP:wght@400;500;600&display=swap' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Shippori+Mincho:wght@400;600&display=swap' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Klee+One:wght@400;600&display=swap' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Zen+Kaku+Gothic+New:wght@400;500;600&display=swap' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap' },
        { rel: 'icon', type: 'image/webp', href: '/favicon.webp' }
      ],
      script: [
        { src: 'https://apis.google.com/js/api.js', async: true, defer: true },
        { src: 'https://accounts.google.com/gsi/client', async: true, defer: true }
      ]
    }
  },
  modules: [
    '@vite-pwa/nuxt'
  ],
  pwa: {
    registerType: 'autoUpdate',
    manifest: {
      name: '縦書きエディタ',
      short_name: 'Tategaki',
      description: '美しい縦書きで文章を執筆・PDF出力できるWebアプリ',
      theme_color: '#f7f9ff',
      background_color: '#ffffff',
      icons: [
        {
          src: '/icon-192x192.png',
          sizes: '192x192',
          type: 'image/png'
        },
        {
          src: '/icon-512x512.png',
          sizes: '512x512',
          type: 'image/png'
        }
      ]
    },
    workbox: {
      navigateFallback: '/',
      globPatterns: ['**/*.{js,css,html,png,svg,ico}']
    }
  },
  css: ["~/assets/css/tailwind.css"],
  vite: {
    plugins: [
      tailwindcss(),
      tsconfigPaths()
    ]
  }
})
