# Passo a passo: compilando o app da maquininha no Android Studio

Este guia é só sobre a parte "mecânica" — instalar o programa, abrir o projeto, compilar e colocar no aparelho. Para as partes específicas do PagBank/PlugPag (código de ativação, tipos de terminal), veja o `README-PLUGPAG.md` na mesma pasta.

Não é preciso saber programar para seguir isso — é um passo a passo de cliques. Onde for preciso mexer em código, eu aviso exatamente qual linha trocar.

---

## Parte 1 — Instalar o Android Studio

1. Acesse **developer.android.com/studio** e baixe a versão para o seu sistema (Windows ou Mac).
2. Abra o instalador e siga o assistente com as opções padrão ("Standard installation"). Ele já baixa o **Android SDK** junto — isso pode levar uns 15-30 minutos na primeira vez, dependendo da internet.
3. Ao abrir pela primeira vez, o Android Studio pode pedir para instalar componentes adicionais (SDK Platform, SDK Build-Tools) — aceite tudo.

## Parte 2 — Abrir o projeto

1. Baixe/copie a pasta **`android-maquininha`** (a que eu gerei) para o computador onde vai instalar o Android Studio.
2. Abra o Android Studio → tela inicial → **"Open"** (ou "Open an Existing Project").
3. Selecione a pasta `android-maquininha` inteira (não uma subpasta) e confirme.
4. O Android Studio vai começar a sincronizar o projeto automaticamente (aparece uma barra de progresso embaixo, "Gradle Sync"). Na primeira vez, isso baixa todas as bibliotecas do projeto (inclusive o SDK PlugPag) — pode levar alguns minutos.

**Se der erro no Gradle Sync:**
- Erro relacionado a "JDK" ou "Java version": vá em `File → Settings → Build Tools → Gradle` e confirme que está usando o JDK que veio embutido no Android Studio ("Embedded JDK").
- Erro para baixar `br.com.uol.pagseguro.plugpagservice.wrapper`: confirme que o repositório extra está no `build.gradle` raiz (ele já está incluído neste projeto) e que você tem conexão com a internet livre (alguns firewalls corporativos bloqueiam repositórios do GitHub).

## Parte 3 — Ajustar a única linha que você precisa mudar

1. No painel à esquerda, navegue até: `app → src → main → java → com.levoconveniencia.maquininha → MainActivity.kt`.
2. Logo no topo do arquivo, localize esta linha:
   ```kotlin
   private val URL_PADRAO_APP = "https://SEU-APP.vercel.app/?maquininha=1"
   ```
3. Troque `https://SEU-APP.vercel.app` pela URL de verdade onde seu app está publicado (a mesma que você usa no Vercel), mantendo o `/?maquininha=1` no final.
4. Salve o arquivo (Ctrl+S ou Cmd+S).

Você **não precisa** editar mais nada no código — o código de ativação do terminal PagBank é digitado depois, direto dentro do app já instalado (explico no passo 7).

## Parte 4 — Testar em um aparelho conectado por cabo (recomendado antes de gerar o APK final)

1. No tablet/terminal Android, ative o **"Modo desenvolvedor"**: vá em Configurações → Sobre o aparelho → toque 7 vezes seguidas em "Número da versão" (ou "Build number"). Vai aparecer uma mensagem "Você agora é um desenvolvedor!".
2. Volte em Configurações → agora vai existir um menu novo, **"Opções do desenvolvedor"** → ative a **"Depuração USB"**.
3. Conecte o aparelho ao computador com um cabo USB. Pode aparecer uma janela no aparelho perguntando "Permitir depuração USB deste computador?" → toque em "Permitir".
4. No Android Studio, no topo, vai aparecer o nome do seu aparelho num menu suspenso (ao lado do botão verde ▶ "Run"). Selecione ele.
5. Clique no botão verde ▶ (ou `Shift+F10`). O Android Studio compila e instala o app direto no aparelho, e ele abre sozinho.

## Parte 5 — Gerar o APK para instalar sem precisar do computador depois

Depois de testar e confirmar que está tudo certo, gere um arquivo instalável (`.apk`) para guardar e instalar em outros terminais sem precisar do Android Studio toda vez:

1. Menu superior: `Build → Build Bundle(s) / APK(s) → Build APK(s)`.
2. Aguarde a barra de progresso terminar. Vai aparecer uma notificação no canto inferior direito: **"APK(s) generated successfully"** → clique em **"locate"** para abrir a pasta onde ele foi salvo.
3. O arquivo vai estar em algo como: `android-maquininha/app/build/outputs/apk/debug/app-debug.apk`.
4. Copie esse arquivo `.apk` para o tablet/terminal (por cabo USB, e-mail, Google Drive, o que for mais fácil).
5. No aparelho, abra o arquivo `.apk` copiado. Se aparecer um aviso de "Instalar apps de fontes desconhecidas", toque em "Configurações" → ative a permissão para aquele app (ex: Gerenciador de Arquivos, ou o app que você usou para abrir o arquivo) → volte e toque em "Instalar".

> **Nota sobre o tipo de APK:** o passo acima gera um APK de **debug** (bom para testar). Para uma versão "de produção" mais otimizada, o processo é parecido, mas usando `Build → Generate Signed App Bundle / APK` e criando uma chave de assinatura — isso só é necessário se quiser publicar formalmente ou distribuir para muitos terminais; para 1-2 terminais próprios, o APK de debug funciona tranquilamente.

## Parte 6 — Configurar dentro do app já instalado

1. Abra o app no terminal/tablet — ele já abre em tela cheia, na tela de espera da maquininha.
2. **Toque e segure a tela por uns 3 segundos** — abre uma caixa de configuração com dois campos:
   - **URL do app**: confirme que está certa (você já colocou no código, mas pode ajustar aqui sem recompilar).
   - **Código de ativação do terminal**: cole o código que o PagBank te passou para aquele terminal específico.
3. Toque em **"Salvar e reiniciar"**.
4. Pronto — o app deve ativar o terminal e ficar pronto para cobrar de verdade.

## Resumo visual do caminho completo

1. Instalar Android Studio →
2. Abrir a pasta `android-maquininha` →
3. Trocar a URL no `MainActivity.kt` →
4. Testar conectado por cabo (▶ Run) →
5. Gerar o `.apk` (Build → Build APK(s)) →
6. Instalar o `.apk` no terminal →
7. Configurar (toque e segure → URL + código de ativação) →
8. Testar uma venda de verdade no ambiente de testes do PagBank antes de ir para produção.

Qualquer erro específico que aparecer no meio do caminho, me manda a mensagem de erro exata (print ou texto) que eu te ajudo a resolver.
