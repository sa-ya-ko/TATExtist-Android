// https://nuxt.com/docs/api/configuration/nuxt-config
import tsconfigPaths from 'vite-tsconfig-paths'
import tailwindcss from '@tailwindcss/vite'

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  runtimeConfig: {
    googleClientSecret: '', // .env の NUXT_GOOGLE_CLIENT_SECRET で上書きされます
    public: {
      googleClientId: '', // .env の NUXT_PUBLIC_GOOGLE_CLIENT_ID で上書きされます
      googleApiKey: ''    // .env の NUXT_PUBLIC_GOOGLE_API_KEY で上書きされます
    }
  },
  site: {
    url: process.env.NUXT_PUBLIC_SITE_URL || 'http://localhost:3000',
    name: 'たてもじ',
    description: '美しい縦書きで文章を執筆・PDF出力できる無料のWebアプリ。'
  },
  app: {
    head: {
      titleTemplate: '%s | たてもじ',
      title: '縦書きエディタ',
      meta: [
        { name: 'description', content: '美しい縦書きで文章を執筆・PDF出力できる無料のWebアプリ。原稿用紙のような美しいレイアウトで、ブラウザ上で快適に小説やエッセイを書くことができます。' },
        { property: 'og:title', content: 'たてもじ - 縦書きエディタ' },
        { property: 'og:description', content: '美しい縦書きで文章を執筆・PDF出力できる無料のWebアプリ。' },
        { property: 'og:type', content: 'website' },
        { name: 'twitter:card', content: 'summary_large_image' }
      ],
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
    '@vite-pwa/nuxt',
    '@nuxtjs/sitemap',
    'nuxt-schema-org'
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
