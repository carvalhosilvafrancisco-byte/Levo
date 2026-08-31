/* Service Worker — Levo Conveniência
   Guarda o "esqueleto" do app em cache para abrir instantaneamente e
   funcionar mesmo com internet instável. Os dados (produtos, estoque,
   histórico) continuam vindo do Supabase/localStorage normalmente —
   este arquivo só cuida dos arquivos do próprio app (HTML, ícones, manifest).

   IMPORTANTE: sempre que publicar uma atualização do app, troque o número
   da linha abaixo (ex: 'v1' -> 'v2'). Isso avisa o navegador para buscar
   a versão nova em vez de continuar usando a antiga em cache. */
const CACHE_NAME = 'levo-conveniencia-v1';

const ARQUIVOS_DO_APP = [
  './',
  './index.html',
  './manifest.json',
  './icons/icon-192.png',
  './icons/icon-512.png',
  './icons/icon-512-maskable.png',
  './icons/apple-touch-icon.png',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(ARQUIVOS_DO_APP))
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((chaves) =>
      Promise.all(
        chaves.filter((chave) => chave !== CACHE_NAME).map((chave) => caches.delete(chave))
      )
    )
  );
  self.clients.claim();
});

// Estratégia "stale-while-revalidate": responde na hora com o que já está em
// cache (app abre rápido, funciona offline) e, em paralelo, busca a versão
// mais nova na rede para atualizar o cache para a próxima vez.
self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;
  if (!event.request.url.startsWith(self.location.origin)) return; // não intercepta Supabase, fontes, etc.

  event.respondWith(
    caches.match(event.request).then((respostaEmCache) => {
      const buscaNaRede = fetch(event.request)
        .then((respostaDaRede) => {
          if (respostaDaRede && respostaDaRede.status === 200) {
            const copia = respostaDaRede.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copia));
          }
          return respostaDaRede;
        })
        .catch(() => respostaEmCache);
      return respostaEmCache || buscaNaRede;
    })
  );
});
