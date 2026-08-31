# Levo Maquininha — App Android nativo para a maquininha Smart (SmartPOS)

Este projeto é o app que roda **instalado dentro da própria maquininha** (modelo Smart/SmartPOS, ex: Moderninha Smart), mostrando o catálogo de produtos e cobrando o cliente de verdade via SDK **PlugPag**.

**Importante — isso NÃO é o mesmo app do celular do cliente.** O `index.html` (catálogo, histórico, perfil do cliente) continua existindo e funcionando exatamente igual, publicado no Vercel. Este projeto aqui é 100% separado, e é o único que roda dentro da maquininha física.

---

## 0. Por que este app é 100% nativo (sem WebView)

A documentação oficial do PagBank proíbe explicitamente aplicações baseadas em WebView/WebApp no modelo SmartPOS — a maquininha Smart entra exatamente nessa categoria. Por isso, diferente de uma primeira versão deste projeto (que carregava o site dentro de uma janela), **este app não usa navegador nenhum por trás** — todas as telas (espera, catálogo, pagamento) são construídas em Kotlin puro, e a comunicação com o catálogo/estoque é feita direto pela **API REST do Supabase**, sem depender do site do cliente.

Isso significa que a página `?maquininha=1` do `index.html` (que existia para simular esse fluxo num navegador comum) **não é mais o que roda na maquininha de verdade** — ela continua no ar só como uma simulação de demonstração, útil pra testar a lógica num navegador qualquer, mas o terminal físico usa este projeto Android aqui.

---

## 1. Pré-requisitos

1. **Conta de desenvolvedor no PagBank**: developer.pagbank.com.br.
2. **Liberação da conta/app para o modelo SmartPOS**: diferente de uma simples autenticação, o modelo SmartPOS exige que o PagBank **revise e aprove o app** antes de disponibilizá-lo na maquininha — incluindo uma revisão de segurança de código feita pelo time deles. É necessário ter um contato comercial no PagBank e uma conta Empresarial/Avançada. Veja `PEDIDO-ACESSO-PLUGPAG.md` para o texto do pedido.
3. Um terminal **Smart** (o mesmo que você já tem).
4. Um projeto **Supabase** configurado (o mesmo que já está conectado ao app web) — a URL e a chave "anon public"/"publishable" dele.
5. **Android Studio** para compilar (ou o caminho sem instalação nada, pelo GitHub Actions — veja `COMO-SUBIR-NO-GITHUB.md` e `ALTERNATIVAS-SEM-ANDROID-STUDIO.md`).
6. Um **Terminal de Desenvolvimento (DEBUG)**, fornecido pelo PagBank — é nele que você testa antes da homologação. Veja `COMO-INSTALAR-NA-MAQUININHA-SMART.md`.

---

## 2. Como este projeto está organizado

```
android-maquininha/
├── build.gradle
├── settings.gradle
├── gradle.properties
└── app/
    ├── build.gradle
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/mipmap-*/ic_launcher*.png   (ícone do app)
        └── java/com/levoconveniencia/maquininha/
            ├── MainActivity.kt   ← todas as telas (espera, catálogo, pagamento)
            └── SupabaseApi.kt    ← chamadas HTTP ao Supabase (ler produtos, dar baixa, registrar venda)
```

## 3. Como o app funciona, passo a passo

1. **Tela de espera**: logo + "Toque aqui para iniciar", ocupando a tela toda.
2. Ao tocar, busca os produtos direto do Supabase (`SupabaseApi.listarProdutos`) e mostra o catálogo — nome, preço, estoque disponível, com um seletor de quantidade e um botão "Pagar" por item.
3. Ao tocar em "Pagar", mostra um menu simples (Crédito / Débito / Pix) e aciona o terminal de verdade via `plugPag.doPayment(...)`.
4. Se aprovado: dá baixa no estoque (`SupabaseApi.atualizarEstoque`) e registra a venda (`SupabaseApi.registrarVenda`) nas mesmas tabelas que o app web já usa — a venda aparece automaticamente no Histórico e no Dashboard do painel admin, exatamente como as vendas feitas pela simulação no navegador.
5. Mostra uma tela de sucesso por alguns segundos e volta sozinho para a tela de espera — pronta para o próximo cliente. O mesmo acontece se ninguém tocar em nada por 45 segundos.

## 4. Como configurar antes de compilar

Não é necessário editar nenhuma URL no código desta vez — a configuração é feita **dentro do próprio app**, depois de instalado: toque e segure a tela de espera por ~3 segundos para abrir a tela de configurações, com três campos:

- **URL do projeto Supabase** (ex: `https://xxxxxxxx.supabase.co`)
- **Chave anon public/publishable do Supabase** (a mesma usada no painel admin do app web)
- **Código de ativação do terminal** (fornecido pelo PagBank para essa maquininha específica)

## 5. Como compilar

Compilar o `.apk` é o mesmo processo já usado até aqui — veja `COMO-SUBIR-NO-GITHUB.md` (GitHub Actions, sem precisar instalar Android Studio) ou `PASSO-A-PASSO-ANDROID-STUDIO.md` (compilação local).

**Atenção**: instalar esse `.apk` numa maquininha Smart **não é igual a instalar num tablet comum**. Terminais de produção não aceitam apps instalados livremente — existe um processo específico do PagBank (terminal de testes próprio, homologação, depois vinculação do terminal de produção). O passo a passo completo desse processo está em `COMO-INSTALAR-NA-MAQUININHA-SMART.md` — leia antes de tentar instalar.

## 6. Testando com segurança

O teste "com segurança" aqui não é só sobre sandbox — é sobre usar o **Terminal de Desenvolvimento (DEBUG)** fornecido pelo PagBank (diferente do seu terminal de produção), como detalhado em `COMO-INSTALAR-NA-MAQUININHA-SMART.md`. Nele, as transações são simuladas e não mexem em dinheiro de verdade. Só depois de validar tudo por lá — e passar pela homologação — o app chega no seu terminal de produção de fato.

## 7. Pontos de atenção

- **A parte de pagamento (`doPayment`) é a que tem menor confirmação direta** da documentação pública para essa combinação específica de SDK. As demais assinaturas (`PlugPag(context)`, `initializeAndActivatePinpad(...)` com `.result`/`.message`) já foram confirmadas numa compilação real bem-sucedida anteriormente com essa mesma versão do wrapper (`1.30.51`). Se o build falhar em algum ponto, envie a mensagem de erro completa do Kotlin — ela indica exatamente qual assinatura ajustar, e corrigimos com precisão (como já fizemos outras vezes neste projeto).
- **Segurança das chaves do Supabase dentro do app**: como a chave usada é a "anon public"/"publishable", ela é feita para ser exposta em clientes (protegida pelas regras de acesso da tabela, não por sigilo) — mesmo assim, vale revisar as políticas de acesso (Row Level Security) do Supabase antes de operar com dinheiro de verdade, como já indicado no guia principal do projeto web.
- **Revisão de segurança do PagBank**: diferente do modelo Bluetooth (mais self-service), o modelo SmartPOS passa por uma revisão de segurança de código feita pelo time do PagBank antes de liberar o app na maquininha — pode levar mais tempo. Vale já enviar o pedido (`PEDIDO-ACESSO-PLUGPAG.md`) com antecedência.

## 8. Histórico de decisões (para referência futura)

Este projeto passou por duas mudanças de arquitetura, registradas aqui para não se perder:

1. **Primeira versão**: WebView carregando o site do app web (`?maquininha=1`), com uma ponte JavaScript para o PlugPag. Funcionava, mas usava por engano a biblioteca do modelo SmartPOS (`plugpagservice.wrapper`) — que proíbe justamente esse tipo de app (WebView).
2. **Segunda versão**: trocada para a biblioteca "clássica" Bluetooth (`br.com.uol.pagseguro:plugpag:3.0.0`), pensando em um terminal externo tipo Minizinha conectado a um tablet separado.
3. **Versão atual (esta)**: como o terminal real é uma **maquininha Smart** (SmartPOS), voltamos para a biblioteca `plugpagservice.wrapper` (a correta para esse modelo) — mas desta vez **reescrevendo a tela de catálogo/pagamento como código nativo**, sem WebView nenhum, para respeitar a restrição da documentação oficial.
