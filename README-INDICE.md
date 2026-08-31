# App Levo — Projeto Consolidado

Este é o projeto completo da Levo Conveniência, tudo num único lugar. Antes deste pacote, os arquivos estavam espalhados em conversas e downloads separados — agora está tudo organizado assim:

```
App-Levo/
├── web/                          → o app do celular do cliente + painel admin (publicado no Vercel)
├── android-maquininha/           → o app nativo que roda na maquininha Smart (compilado via GitHub Actions)
├── leitor-facial/                → a "ponte" que sincroniza cadastros com o leitor facial (Control iD/iDFace)
├── .github/workflows/            → a automação que compila o app da maquininha sozinha
├── GUIA-HOSPEDAGEM-E-PROXIMOS-PASSOS.md   → guia geral do projeto inteiro (comece por aqui se estiver perdido)
└── fluxograma-sistema.mermaid    → o diagrama do funcionamento completo do sistema
```

## Como subir este projeto para o GitHub

Use o **GitHub Desktop** (o mesmo processo que já usamos até aqui):

1. Se o repositório "App Levo" que você criou ainda está vazio, clone ele com o GitHub Desktop.
2. Copie **todo o conteúdo desta pasta** (`App-Levo/`) para dentro da pasta que o GitHub Desktop clonou — de forma que `web/`, `android-maquininha/`, `.github/` e os arquivos `.md` fiquem direto na raiz do repositório, sem mais uma pasta por cima.
3. No GitHub Desktop: escreva um resumo (ex: "Consolida projeto Levo — web, maquininha e leitor facial"), **Commit to main**, depois **Push origin**.

**Atenção com a pasta `.github`** (começa com ponto, pode ficar escondida em alguns exploradores de arquivo) — confira que ela foi copiada junto. Ative "Mostrar arquivos ocultos" no seu explorador de arquivos se precisar.

## Como publicar cada parte

### 1. App web (pasta `web/`) → Vercel
Como agora o `index.html` não está mais na raiz do repositório (está dentro de `web/`), você precisa avisar o Vercel onde procurar:
1. No painel do Vercel, abra o projeto já conectado a este repositório.
2. Vá em **Settings → General → Root Directory**.
3. Defina como `web`.
4. Salve — o próximo deploy já vai puxar os arquivos de dentro dessa pasta.

Se você criar o projeto do zero no Vercel agora (em vez de reaproveitar um já existente), essa mesma opção "Root Directory" aparece já na tela de importação do projeto — configure lá antes de finalizar.

### 2. App da maquininha (pasta `android-maquininha/`) → GitHub Actions
Isso já está pronto para funcionar automaticamente. O workflow (`.github/workflows/build-apk.yml`) foi ajustado para observar mudanças dentro de `android-maquininha/` especificamente, e compilar o `.apk` sozinho, do mesmo jeito que já testamos antes — só que agora dentro deste repositório consolidado. Depois do primeiro push, vá na aba **Actions** do GitHub e acompanhe como sempre.

**Nota sobre os guias dentro de `android-maquininha/`**: os arquivos `COMO-SUBIR-NO-GITHUB.md`, `PASSO-A-PASSO-ANDROID-STUDIO.md` etc. foram escritos originalmente pensando nesse projeto como um repositório **separado** — a parte de "quais arquivos colocar onde" não se aplica mais exatamente (agora já vem tudo pronto, dentro da subpasta certa). O restante desses guias (como rodar o Actions, como resolver erros de compilação, como instalar na maquininha) continua igualmente válido.

### 3. Ponte do leitor facial (pasta `leitor-facial/`)
Essa parte não "sobe" para nenhum serviço — ela é um script Python que roda localmente, num computador dentro da rede da loja. Siga o `leitor-facial/INTEGRACAO-LEITOR-FACIAL.md` para configurar (ele já está pronto para uso, só falta o equipamento físico chegar).

## Documentos de referência geral

- **`GUIA-HOSPEDAGEM-E-PROXIMOS-PASSOS.md`** — visão geral de tudo: onde hospedar, o que já está pronto, o que falta, LGPD, backend (Supabase).
- **`fluxograma-sistema.mermaid`** — o diagrama visual completo do fluxo (abertura da porta → retirada do produto → pagamento → atualização do estoque). Abra em qualquer visualizador de Mermaid (o próprio GitHub renderiza esse arquivo automaticamente se você abrir ele lá).

## O que já está pronto vs. pendente (resumo rápido)

| Parte | Status |
|---|---|
| App web (catálogo, histórico, perfil, admin) | ✅ Pronto e publicado |
| Sincronização em tempo real (Supabase) | ✅ Pronto |
| App da maquininha (nativo, sem WebView) | ✅ Código pronto, compilando com sucesso |
| Credenciais do Supabase embutidas no app da maquininha | ✅ Pronto |
| Liberação do PagBank (Terminal DEBUG + homologação) | 🟡 Pedido enviado, aguardando retorno |
| Leitor facial (Control iD/iDFace) | 🟡 Código da ponte pronto, aguardando equipamento físico |

Qualquer dúvida sobre onde encontrar alguma coisa específica dentro deste pacote, é só perguntar.
