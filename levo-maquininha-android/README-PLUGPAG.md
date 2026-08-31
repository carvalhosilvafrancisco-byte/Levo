# Levo Maquininha — App Android para o terminal PagBank (PlugPag)

Este projeto é a "casca" nativa Android que conecta o app web da Levo Conveniência (o mesmo `index.html` que já está publicado) a um terminal físico PagBank de verdade, via SDK **PlugPag**.

**Como funciona, em uma frase:** o app abre a tela da maquininha do site dentro de um WebView em tela cheia; quando o cliente toca em "Pagar", em vez de simular a aprovação (como no protótipo no navegador), ele aciona o terminal PagBank de verdade e só dá baixa no estoque quando o pagamento é aprovado.

---

## 0. Antes de tudo: isso não roda "no navegador"

O SDK PlugPag só existe para apps nativos (Android, iOS, Windows, Linux) — não existe versão para JavaScript rodando num site comum. Por isso este projeto é um **app Android separado**, que existe só para abrir o mesmo site dentro dele e fazer a ponte com o terminal. Você (ou um desenvolvedor Android) vai precisar compilar isso com o **Android Studio** — não dá para simplesmente "abrir" estes arquivos num navegador.

Se você não tem um desenvolvedor Android à disposição, a Levo pode terceirizar essa etapa específica (é um projeto pequeno, poucas telas) com um freelancer Android — o código já está pronto e comentado neste projeto para servir de base.

---

## 1. Pré-requisitos

1. **Uma conta de desenvolvedor no PagBank**: crie em developer.pagbank.com.br.
2. **Solicitar acesso ao SDK PlugPag**: diferente das APIs REST, o PlugPag exige que você entre em contato com o time de integração do PagBank para liberar o uso em um terminal de produção (eles vinculam o app a um código de ativação por terminal). Procure "Fale com o PagBank" ou "Solicitar integração PlugPag" na documentação.
3. **Um terminal PagBank compatível** — os modelos mais usados para esse tipo de integração são as linhas **Moderninha Smart** (o app roda embarcado dentro do próprio terminal, que já tem tela touch, ideal para o nosso caso) ou terminais que aceitem conexão Bluetooth com um tablet separado.
4. **Android Studio** instalado (gratuito, da Google) — é onde este projeto é aberto, compilado e enviado para o terminal.
5. O **código de ativação** do seu terminal específico, fornecido pelo PagBank quando você contrata a integração.

---

## 2. Como este projeto está organizado

```
android-maquininha/
├── build.gradle              (configuração raiz)
├── settings.gradle
└── app/
    ├── build.gradle           (dependências, incluindo o SDK PlugPag)
    └── src/main/
        ├── AndroidManifest.xml
        └── java/com/levoconveniencia/maquininha/
            └── MainActivity.kt   (todo o código-fonte está aqui)
```

## 3. O que o `MainActivity.kt` faz, passo a passo

1. Abre um `WebView` em tela cheia carregando a URL do seu app publicado (`https://seuapp.vercel.app/?maquininha=1`).
2. Expõe um objeto chamado `AndroidPag` para o JavaScript da página — é assim que o site "conversa" com o terminal.
3. No `index.html` que já construímos, a função `pagarNaMaquininha()` detecta esse objeto automaticamente (`window.AndroidPag`) e, quando ele existe, chama `window.AndroidPag.cobrar(produtoId, quantidade, valorEmCentavos)` em vez de simular a aprovação.
4. O Kotlin recebe essa chamada, mostra um diálogo simples para o cliente escolher crédito/débito/Pix, e aciona `plugPag.doPayment(...)` — que é o momento em que o terminal físico pede o cartão/aproximação de verdade.
5. Quando o terminal responde, o app chama de volta uma função JavaScript na página (`confirmarPagamentoNativoSucesso` ou `confirmarPagamentoNativoFalha`, já implementadas no `index.html`), que dá baixa no estoque, registra a venda no Supabase e mostra a tela de sucesso — exatamente como no fluxo simulado, só que agora com um pagamento de verdade por trás.

## 4. Como configurar antes de compilar

Abra `MainActivity.kt` e troque esta linha pela URL real do seu app publicado:

```kotlin
private val URL_PADRAO_APP = "https://SEU-APP.vercel.app/?maquininha=1"
```

O **código de ativação do terminal** não precisa ir no código — ele é digitado depois, direto no app já instalado: toque e segure a tela por uns 3 segundos para abrir a tela de configurações (URL do app + código de ativação), que ficam salvos no aparelho.

## 5. Como compilar e instalar

1. Abra a pasta `android-maquininha/` no Android Studio ("Open" → selecione a pasta).
2. Deixe o Android Studio baixar as dependências (primeira vez demora alguns minutos).
3. Conecte o terminal/tablet ao computador via USB (com a "Depuração USB" ativada nas opções de desenvolvedor do aparelho), ou gere um APK (`Build → Build APK(s)`) para instalar manualmente depois.
4. Rode o app (▶) — ele vai abrir em tela cheia já mostrando a tela de espera da maquininha.
5. Toque e segure a tela, cole a URL do seu app e o código de ativação do terminal, salve.

## 6. Testando com segurança

O PagBank disponibiliza um **ambiente de testes (sandbox)** e terminais de demonstração para validar a integração sem mexer com dinheiro de verdade — pergunte ao time de integração sobre isso antes de testar em produção. Só troque para um terminal de produção depois de validar todo o fluxo (escolha da forma de pagamento, aprovação, e a baixa de estoque aparecendo certinha no painel admin) no ambiente de testes.

## 7. Pontos de atenção

- **Versão do SDK**: os nomes de classes e métodos do PlugPag podem mudar entre versões. O código aqui foi escrito com base na documentação pública mais recente — se o PagBank te entregar uma versão diferente do SDK, pode ser necessário ajustar pequenos detalhes (nomes de métodos, parâmetros).
- **Este app não substitui o app web**: ele só empresta uma "janela" nativa para o mesmo site rodar dentro, mantendo tudo que já construímos (catálogo, estoque, Supabase, histórico) funcionando exatamente igual.
- **Atualizações do app web continuam automáticas**: como o WebView carrega a URL ao vivo, qualquer atualização que você publicar no `index.html` (Vercel) aparece automaticamente no terminal, sem precisar recompilar o app Android — só recompile se mudar algo neste projeto Android em si.
