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
    description: '縦書きで文章を執筆･PDF出力できるWebサイト。基本的なツールから、ルビを振る機能、Google Driveと連携して保存、読み込む機能まで使えて、縦書きのままPDF保存もできる縦書き専用エディタです。'
  },
  app: {
    head: {
      titleTemplate: '%s | たてもじ',
      title: '縦書き専用エディタ',
      meta: [
        { name: 'description', content: '縦書きで文章を執筆･PDF出力できるWebサイト。基本的なツールから、ルビを振る機能、Google Driveと連携して保存、読み込む機能まで使えて、縦書きのままPDF保存もできる縦書き専用エディタです。' },
        { property: 'og:title', content: 'たてもじ - 縦書き専用エディタ' },
        { property: 'og:description', content: '縦書きで文章を執筆･PDF出力できるWebサイト。基本的なツールから、ルビを振る機能、Google Driveと連携して保存、読み込む機能まで使えて、縦書きのままPDF保存もできる縦書き専用エディタです。' },
        { property: 'og:type', content: 'website' },
        { property: 'og:image', content: (process.env.NUXT_PUBLIC_SITE_URL || 'http://localhost:3000') + '/OGP.webp' },
        { name: 'twitter:card', content: 'summary_large_image' },
        { name: 'twitter:image', content: (process.env.NUXT_PUBLIC_SITE_URL || 'http://localhost:3000') + '/OGP.webp' }
      ],
      link: [
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Noto+Serif+JP:wght@400;600&display=swap' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Noto+Sans+JP:wght@400;500;600&display=swap' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Shippori+Mincho:wght@400;600&display=swap' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Klee+One:wght@400;600&display=swap' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Zen+Kaku+Gothic+New:wght@400;500;600&display=swap' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap' },
        { rel: 'icon', type: 'image/webp', href: '/favicon2.webp' }
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
    'nuxt-schema-org',
    'nuxt-security'
  ],
  security: {
    corsHandler: {
      origin: process.env.NUXT_PUBLIC_SITE_URL || 'http://localhost:3000',
      methods: ['GET', 'POST', 'OPTIONS']
    },
    headers: {
      crossOriginEmbedderPolicy: 'unsafe-none',
      contentSecurityPolicy: {
        'default-src': ["'self'"],
        'script-src': ["'self'", "'unsafe-inline'", "'unsafe-eval'", 'https://apis.google.com', 'https://accounts.google.com'],
        'connect-src': ["'self'", 'https://oauth2.googleapis.com', 'https://www.googleapis.com'],
        'frame-src': ["'self'", 'https://accounts.google.com'],
        'img-src': ["'self'", 'data:', 'blob:', 'https:'],
        'style-src': ["'self'", "'unsafe-inline'", 'https://fonts.googleapis.com'],
        'font-src': ["'self'", 'https://fonts.gstatic.com']
      }
    }
  },
  pwa: {
    registerType: 'autoUpdate',
    manifest: {
      name: 'たてもじ',
      short_name: 'TateMoji',
      description: '縦書きで文章を執筆･PDF出力できるWebサイト。基本的なツールから、ルビを振る機能、Google Driveと連携して保存、読み込む機能まで使えて、縦書きのままPDF保存もできる縦書き専用エディタです。',
      theme_color: '#f7f9ff',
      background_color: '#ffffff',
      icons: [
        {
          src: '/favicon1.webp',
          sizes: '192x192',
          type: 'image/webp'
        },
        {
          src: '/favicon2.webp',
          sizes: '512x512',
          type: 'image/webp'
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
