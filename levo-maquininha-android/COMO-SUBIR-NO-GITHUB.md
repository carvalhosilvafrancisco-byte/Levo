# Como subir os arquivos certos no GitHub e compilar o app

Este guia parte do zero absoluto: criar conta, criar repositório, colocar os arquivos certos, rodar a compilação, baixar e instalar o app no tablet. Vou explicar cada termo técnico e cada clique, sem pular etapas.

---

## Parte 0 — Glossário rápido (para entender os próximos passos)

Antes de começar, alguns termos que vão aparecer bastante:

- **Repositório**: é só uma "pasta de projeto" dentro do GitHub, na nuvem. É onde os arquivos do app vão morar.
- **Commit**: é como salvar um "ponto de controle" das suas alterações, com uma mensagem descrevendo o que mudou (ex: "Primeira versão do app"). Pense nisso como salvar um arquivo do Word, mas guardando o histórico de cada salvamento.
- **Push**: depois de fazer um ou mais "commits" no seu computador, o "push" é o ato de **enviar** esses commits para o GitHub, na nuvem. Antes do push, as mudanças ficam só na sua máquina.
- **Clone**: é "baixar uma cópia" de um repositório que já existe no GitHub para o seu computador, para você poder editar os arquivos localmente.
- **GitHub Actions**: é o "robô" do GitHub que executa tarefas automáticas — no nosso caso, ele vai ler os arquivos do projeto Android e compilar o `.apk` sozinho, em um computador na nuvem deles (não no seu).
- **Artifact** ("artefato"): é o nome que o GitHub dá para um arquivo gerado por uma dessas automações — no nosso caso, o `.apk` compilado.
- **APK**: é o formato de arquivo instalável de um app Android — equivalente a um `.exe` no Windows.

---

## Parte 1 — Quais arquivos você precisa ter

São **7 arquivos de código**, que juntos formam o projeto inteiro. Cada um tem uma função:

| Arquivo | Para que serve |
|---|---|
| `.github/workflows/build-apk.yml` | A "receita" que diz ao robô do GitHub como compilar o app |
| `build.gradle` (raiz) | Configuração geral do projeto — diz onde buscar as bibliotecas (inclusive a do PagBank) |
| `settings.gradle` | Só declara o nome do projeto |
| `app/build.gradle` | Lista as bibliotecas específicas que o app usa (a do PlugPag entra aqui) |
| `app/src/main/AndroidManifest.xml` | Permissões do app (internet, Bluetooth) e outras configurações |
| `app/src/main/java/com/levoconveniencia/maquininha/MainActivity.kt` | Todo o código do app — é o "cérebro" dele |
| `app/src/main/res/values/themes.xml` | O visual/tema (deixa o app em tela cheia) |

A estrutura completa, na **raiz** do repositório (ou seja: quando você olhar a pasta principal do projeto, esses são os primeiros itens que devem aparecer, sem nenhuma pasta "android-maquininha" por fora envolvendo tudo):

```
(raiz do repositório)
│
├── .github/
│   └── workflows/
│       └── build-apk.yml
│
├── app/
│   ├── build.gradle
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/
│           │   └── com/levoconveniencia/maquininha/
│           │       └── MainActivity.kt
│           └── res/
│               └── values/
│                   └── themes.xml
│
├── build.gradle
└── settings.gradle
```

Todos esses 7 arquivos já estão dentro do `.zip` (`levo-maquininha-android.zip`) que te enviei — não precisa criar nenhum do zero, só posicioná-los certinho, o que os próximos passos explicam.

---

## Parte 2 — Criar conta e repositório no GitHub

**Se você já tem conta no GitHub** (provavelmente sim, já que usa para o Vercel), pule direto para o passo 4.

1. Acesse **github.com** → botão **"Sign up"** (canto superior direito).
2. Preencha e-mail, senha e um nome de usuário. Confirme o e-mail que ele te envia.
3. Pronto, conta criada — pode ser gratuita, não precisa pagar nada para o que vamos fazer.
4. Já logado, clique no ícone **"+"** no canto superior direito da tela → no menu que abre, clique em **"New repository"**.
5. Na tela de criação:
   - **Repository name**: digite `levo-maquininha-android` (ou o nome que preferir, sem espaços).
   - **Description**: pode deixar em branco, é opcional.
   - Deixe marcado **"Private"** (assim só você tem acesso).
   - **Não marque nenhuma das caixinhas** "Add a README file", "Add .gitignore" ou "Choose a license" — queremos o repositório completamente vazio.
6. Clique no botão verde **"Create repository"**, no final da página.

Você vai cair numa página com um título parecido com "Quick setup" e um monte de comandos de linha de comando (`git init`, `git remote add`...). **Pode ignorar essa tela** — vamos usar um caminho mais visual, sem digitar comandos.

---

## Parte 3 — Colocar os arquivos dentro (dois métodos — escolha um)

### Método A (recomendado): GitHub Desktop + o arquivo .zip

Esse é o caminho mais confiável porque copia a estrutura de pastas certinha, sem o problema de o navegador "esconder" a pasta `.github` durante um arrastar-e-soltar.

**3A.1 — Instalar o GitHub Desktop**

1. Acesse **desktop.github.com** → clique em **"Download for Windows"** (ou macOS, dependendo do seu sistema).
2. Abra o instalador baixado. Ele instala sozinho, sem perguntas complicadas — é bem mais leve e rápido que o Android Studio (menos de 2 minutos).
3. Ao abrir pela primeira vez, ele pede para fazer login — clique em **"Sign in to GitHub.com"** e entre com a mesma conta que você criou/já tinha.
4. Ele pode pedir seu nome e e-mail para "identificar seus commits" — preencha com seus dados reais ou um apelido, não tem problema.

**3A.2 — Clonar (baixar) o repositório vazio para o seu computador**

1. No GitHub Desktop, vá no menu **File → Clone repository...** (ou clique no botão "Clone a repository from the Internet" se for a primeira vez usando).
2. Na janela que abre, clique na aba **"GitHub.com"** — deve aparecer uma lista com os repositórios da sua conta, incluindo o `levo-maquininha-android` que você acabou de criar. Clique nele para selecionar.
3. Embaixo, tem um campo **"Local path"** mostrando onde ele vai salvar no seu computador (por exemplo, uma pasta `Documentos/GitHub/levo-maquininha-android`). Pode deixar como está.
4. Clique no botão azul **"Clone"**.
5. Em poucos segundos, ele termina — você já tem uma pasta vazia no seu computador, conectada ao repositório do GitHub.

**3A.3 — Copiar os arquivos do .zip para dentro dessa pasta**

1. No GitHub Desktop, vá no menu **Repository → Show in Explorer** (Windows) ou **Show in Finder** (Mac). Isso abre a pasta que foi clonada, no seu gerenciador de arquivos normal.
2. Agora, em outra janela, localize o arquivo `levo-maquininha-android.zip` que baixou do Claude, e **extraia/descompacte** ele (no Windows: botão direito → "Extrair tudo"; no Mac: clique duas vezes).
3. Depois de extraído, você vai ver uma pasta com os arquivos dentro (`app`, `.github`, `build.gradle`, etc — pode ser que fique dentro de uma pasta chamada `levo-maquininha-android` criada pela extração).
4. **Selecione todo o conteúdo de dentro dessa pasta extraída** (todos os arquivos e pastas: `app`, `.github`, `build.gradle`, `settings.gradle`, e os `.md` de guia) e **copie**.
5. **Cole dentro da pasta que o GitHub Desktop clonou** (a do passo 3A.2). O resultado final deve ser: ao abrir a pasta clonada, você já vê `app`, `.github`, `build.gradle` etc **direto ali dentro**, sem mais uma pasta por cima.

> **Dica para conferir se ficou certo**: dentro da pasta clonada, deve existir um arquivo chamado `build.gradle` visível imediatamente (não dentro de outra subpasta). Se você só está vendo uma única pasta chamada `levo-maquininha-android` (ou similar) e nada mais, significa que colou um nível "a mais" — entre nela e mova o conteúdo um nível para cima.

**3A.4 — Enviar (commit + push) para o GitHub**

1. Volte para o programa **GitHub Desktop**. Ele detecta sozinho todos os arquivos novos e mostra uma lista à esquerda, com caixinhas marcadas — isso inclui os arquivos dentro de `.github/`, sem esconder nada (diferente do navegador).
2. No canto inferior esquerdo, tem dois campos de texto: um menor ("Summary") e um maior ("Description", opcional). No campo "Summary", escreva algo como `Primeira versão do app`.
3. Clique no botão azul **"Commit to main"**. Isso salva o "ponto de controle" no seu computador (ainda não foi para o GitHub).
4. Agora, no topo da janela, vai aparecer um botão **"Push origin"** (às vezes com uma setinha para cima). Clique nele.
5. Aguarde a barra de progresso — isso está enviando os arquivos de verdade para o GitHub, na nuvem.
6. Para confirmar que funcionou: abra **github.com**, entre no repositório `levo-maquininha-android`, e você deve ver todos os arquivos e pastas listados ali.

### Método B (sem instalar nada, só pelo navegador): criar arquivo por arquivo

Mais manual (você repete o processo 7 vezes), mas funciona 100% pelo site, sem instalar programa nenhum.

Para cada um dos 7 arquivos, dentro da página do repositório no GitHub:

1. Clique no botão **"Add file"** (perto do topo da página, ao lado de um botão verde "Code") → no menu que abre, clique em **"Create new file"**.
2. Vai abrir uma tela com um campo de texto no topo, onde normalmente se digitaria só "arquivo.txt". Ali, digite o **caminho completo**, por exemplo:
   ```
   .github/workflows/build-apk.yml
   ```
   Conforme você digita a barra `/`, o GitHub cria a pasta sozinho — repare que ele vai mostrando visualmente o "caminho" (breadcrumb) se formando acima do campo.
3. Abra o arquivo correspondente (o que baixou do Claude ou de dentro do `.zip`) em qualquer editor de texto simples (Bloco de Notas, TextEdit), selecione todo o conteúdo (Ctrl+A / Cmd+A), copie (Ctrl+C / Cmd+C).
4. Volte na aba do navegador, clique dentro da caixa de texto grande (onde ficaria o conteúdo do arquivo) e cole (Ctrl+V / Cmd+V).
5. Role até o final da página → clique no botão verde **"Commit changes..."** → na janela que abre, pode deixar a mensagem padrão e clicar em **"Commit changes"** de novo para confirmar.
6. Repita esse processo (passos 1 a 5) para os outros 6 arquivos, usando estes caminhos exatos, um de cada vez:
   - `.github/workflows/build-apk.yml`
   - `build.gradle`
   - `settings.gradle`
   - `app/build.gradle`
   - `app/src/main/AndroidManifest.xml`
   - `app/src/main/java/com/levoconveniencia/maquininha/MainActivity.kt`
   - `app/src/main/res/values/themes.xml`

**Atenção**: dois arquivos se chamam `build.gradle` — um fica na raiz, outro dentro de `app/`. São arquivos diferentes, com conteúdos diferentes; preste bastante atenção no caminho digitado em cada um, para não colar o conteúdo errado no lugar errado.

---

## Parte 4 — Rodar a compilação e baixar o APK

1. Na página do repositório no GitHub, clique na aba **"Actions"** (fica no menu horizontal do topo, ao lado de "Code", "Issues", "Pull requests" — pode ser preciso rolar esse menu ou ele aparece direto, dependendo da largura da tela).
2. Se os arquivos foram enviados corretamente, uma execução já deve ter começado sozinha (o envio dos arquivos "avisa" o robô para rodar). Você vai ver uma linha chamada **"Compilar APK da Maquininha"**, com um ícone:
   - 🟡 círculo amarelo girando = ainda rodando.
   - ✅ verde = terminou com sucesso.
   - ❌ vermelho = deu algum erro.
3. Se não apareceu nada rodando, clique em **"Compilar APK da Maquininha"** na lista à esquerda → um botão **"Run workflow"** aparece à direita (é um menu suspenso) → clique nele, depois clique no botão verde **"Run workflow"** que aparece dentro do menu, para confirmar.
4. Clique em cima da execução (a linha com a data/hora, ex: "há 2 minutos") para abrir os detalhes.
5. Você vai ver uma lista de passos, um embaixo do outro: "Baixar o código do repositório", "Configurar Java", "Configurar o Android SDK", "Configurar o Gradle", "Compilar o APK", "Disponibilizar o APK para download". Cada um ganha um ✅ conforme termina. O processo inteiro leva de **3 a 6 minutos** na primeira vez (as próximas costumam ser mais rápidas).
6. Quando **todos** os passos estiverem com ✅ verde, **role a página até o final** — vai ter uma seção chamada **"Artifacts"**, com um item **"levo-maquininha-apk"**. Clique nele para baixar.
7. Isso baixa um arquivo `.zip` pequeno para o seu computador. Abra/extraia ele — dentro está o `app-debug.apk`. **Esse é o arquivo final**, pronto para instalar no tablet/terminal.

---

## Parte 5 — Instalar o APK no tablet/terminal

1. Transfira o arquivo `app-debug.apk` para o tablet — pelo jeito que for mais fácil pra você: enviar por e-mail para você mesmo e abrir no tablet, subir no Google Drive e baixar de lá pelo tablet, ou por cabo USB.
2. No tablet, toque no arquivo `.apk` baixado (geralmente aparece na pasta "Downloads").
3. O Android vai mostrar um aviso do tipo **"Por segurança, seu telefone está bloqueado de instalar apps desconhecidos desta fonte"**. Isso é normal — toque em **"Configurações"** (ou "Settings") nesse próprio aviso.
4. Ele te leva a uma tela para **permitir "Instalar apps desconhecidos"** para o app que você usou para abrir o arquivo (ex: "Arquivos", "Chrome", ou o app de e-mail). Ative essa permissão.
5. Volte (botão "voltar" do celular) — a instalação deve continuar de onde parou, ou toque no `.apk` de novo.
6. Toque em **"Instalar"**. Em poucos segundos, aparece **"App instalado"** → toque em **"Abrir"**.

O app deve abrir direto em tela cheia, na tela de espera da maquininha ("Toque aqui para iniciar").

---

## Se algo der errado

- **Ficou vermelho (❌) em algum passo do Actions**: clique em cima do passo que falhou para ver a mensagem de erro detalhada (em texto, geralmente em vermelho). Copie esse texto (ou tire um print) e me envie — eu leio e digo exatamente o que ajustar.
- **Erro do tipo "Could not find br.com.uol.pagseguro.plugpagservice.wrapper:wrapper"**: normalmente significa que o `build.gradle` **da raiz** (não o de dentro de `app/`) não foi enviado, ou foi enviado no lugar errado. Confira se ele está exatamente na raiz do repositório.
- **A aba "Actions" não mostra nenhuma execução**: confirme que o arquivo está exatamente no caminho `.github/workflows/build-apk.yml` — maiúsculas/minúsculas importam, e o nome da pasta `.github` precisa começar com ponto.
- **No Método A, o GitHub Desktop não mostra nenhum arquivo novo para "commitar"**: normalmente significa que os arquivos foram colados no lugar errado (fora da pasta clonada). Confira o caminho no topo da janela do Explorer/Finder.
- **"Instalar apps desconhecidos" não aparece como opção no tablet**: em alguns Androids mais restritos (principalmente em versões corporativas/MDM), essa permissão pode estar bloqueada pelo fabricante — nesse caso, seria necessário instalar por outro meio (ex: via ADB/USB com um computador), me avise que explico esse caminho alternativo se for o seu caso.

Depois que tiver o app instalado, é só voltar para o `README-PLUGPAG.md` (Parte 6, "Configurar dentro do app já instalado") para o último passo: colar o código de ativação do terminal PagBank direto dentro do app (toque e segure a tela por 3 segundos para abrir essa configuração).

