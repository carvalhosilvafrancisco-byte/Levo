# Como subir os arquivos certos no GitHub e compilar o app

Este guia parte do zero: criar um repositório novo, colocar os arquivos certos nele, e rodar a compilação automática. Vou detalhar cada clique.

---

## Parte 1 — Quais arquivos você precisa ter

São **7 arquivos de código** (mais 3 arquivos de guia/documentação, que são opcionais — não entram na compilação, são só para consulta). A estrutura completa, na **raiz** do repositório (sem nenhuma pasta "android-maquininha" por fora — vamos simplificar e já criar o repositório com esses arquivos direto na raiz dele), é esta:

```
(raiz do repositório)
│
├── .github/
│   └── workflows/
│       └── build-apk.yml          ← manda o GitHub compilar sozinho
│
├── app/
│   ├── build.gradle                ← dependências do app (inclui o SDK PlugPag)
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml ← permissões e configuração do app
│           ├── java/
│           │   └── com/levoconveniencia/maquininha/
│           │       └── MainActivity.kt   ← todo o código do app
│           └── res/
│               └── values/
│                   └── themes.xml  ← tema visual (tela cheia)
│
├── build.gradle                    ← configuração geral do projeto
└── settings.gradle                 ← nome do projeto
```

**Esses 7 arquivos** (`build-apk.yml`, `app/build.gradle`, `AndroidManifest.xml`, `MainActivity.kt`, `themes.xml`, `build.gradle` da raiz, `settings.gradle`) são o mínimo necessário para o GitHub conseguir compilar. Todos já foram gerados nas mensagens anteriores — se você ainda não baixou, me avise que eu reenvio.

Para facilitar, também empacotei tudo (incluindo os guias) em um único arquivo `.zip`, anexado nesta mensagen — assim você não precisa caçar arquivo por arquivo.

---

## Parte 2 — Criar o repositório

1. Entre em **github.com** e faça login (a mesma conta que você usa para o Vercel, se for o caso).
2. Clique no **"+"** no canto superior direito → **"New repository"**.
3. Dê um nome, por exemplo `levo-maquininha-android`.
4. Deixe como **"Private"** (privado) — não precisa ser público.
5. **Não marque** nenhuma caixinha de "Add a README" ou ".gitignore" — deixe o repositório vazio mesmo.
6. Clique em **"Create repository"**.

Você vai cair numa tela vazia, com instruções técnicas de linha de comando — pode ignorar essa tela por enquanto, vamos usar outro caminho.

---

## Parte 3 — Colocar os arquivos dentro (dois métodos — escolha um)

### Método A (recomendado): GitHub Desktop + o arquivo .zip

Esse é o caminho mais confiável porque copia a estrutura de pastas certinha, sem o problema de o navegador "esconder" a pasta `.github`.

1. Baixe e instale o **GitHub Desktop** (desktop.github.com) — é um programa pequeno e leve, bem mais simples que o Android Studio. Instala em menos de 2 minutos.
2. Abra o GitHub Desktop e faça login com a mesma conta do GitHub.
3. Menu **File → Clone repository** → aba "GitHub.com" → selecione o repositório `levo-maquininha-android` que você acabou de criar → **"Clone"**. Ele vai perguntar onde salvar no seu computador — pode deixar o local padrão.
4. Abra a pasta onde ele foi clonado (o GitHub Desktop mostra o caminho, ou vá em **Repository → Show in Explorer/Finder**).
5. **Extraia o `.zip`** que eu te mandei — dentro dele já estão todas as pastas (`app/`, `.github/`, etc). Copie **o conteúdo de dentro do zip** (não a pasta zip inteira) para dentro da pasta que o GitHub Desktop clonou, de forma que o `build.gradle` fique direto na raiz dela (não dentro de uma subpasta extra).
6. Volte para o GitHub Desktop — ele vai detectar automaticamente todos os arquivos novos, listados à esquerda (incluindo os que estão dentro de `.github/`, sem esconder nada).
7. Embaixo, escreva uma mensagem tipo "Primeira versão do app" no campo de resumo, e clique no botão azul **"Commit to main"**.
8. Depois clique em **"Push origin"** (topo da tela) — isso envia tudo para o GitHub.

### Método B (sem instalar nada, só pelo navegador): criar arquivo por arquivo

Mais manual (você vai repetir o mesmo processo 7 vezes), mas funciona 100% pelo site, sem instalar programa nenhum — inclusive resolve sozinho o problema da pasta escondida, porque você digita o caminho completo do arquivo, e o GitHub cria as pastas automaticamente.

Para cada um dos 7 arquivos:

1. Na página do repositório, clique em **"Add file"** → **"Create new file"**.
2. No campo de nome do arquivo (que fica no topo, onde normalmente só se digitaria "arquivo.txt"), digite o **caminho completo**, por exemplo:
   ```
   .github/workflows/build-apk.yml
   ```
   Ao digitar a barra `/`, o GitHub cria a pasta sozinho — repare que ele mostra visualmente as "migalhas de pão" (breadcrumb) da pasta sendo formada.
3. Copie o conteúdo daquele arquivo (abra o arquivo que eu te enviei, selecione tudo, copie) e cole na caixa de texto grande abaixo.
4. Role até o final da página → clique em **"Commit changes"** (botão verde).
5. Repita para os outros 6 arquivos, usando estes caminhos exatos:
   - `.github/workflows/build-apk.yml`
   - `build.gradle`
   - `settings.gradle`
   - `app/build.gradle`
   - `app/src/main/AndroidManifest.xml`
   - `app/src/main/java/com/levoconveniencia/maquininha/MainActivity.kt`
   - `app/src/main/res/values/themes.xml`

Repare que dois arquivos se chamam `build.gradle` — um fica na raiz, outro dentro de `app/`. São arquivos diferentes, com conteúdos diferentes; preste atenção no caminho ao criar cada um.

---

## Parte 4 — Rodar a compilação e baixar o APK

1. Na página do repositório no GitHub, clique na aba **"Actions"** (no menu de cima, ao lado de "Code", "Issues", "Pull requests").
2. Se os arquivos foram enviados corretamente, você já deve ver uma execução em andamento (ou já concluída) chamada **"Compilar APK da Maquininha"**, com um círculo amarelo (rodando) ou ✅ verde (concluído). Se não tiver rodado sozinho, clique em **"Compilar APK da Maquininha"** na lista à esquerda → botão **"Run workflow"** (canto direito, dropdown) → **"Run workflow"** de novo para confirmar.
3. Clique em cima da execução (a linha com a data/hora) para abrir os detalhes.
4. Aguarde os passos rodarem — leva de 3 a 6 minutos na primeira vez. Cada linha (Baixar código, Configurar Java, Configurar Android SDK, etc.) vai ganhando um ✅ conforme termina.
5. Quando tudo estiver verde, **role até o final da página** — na seção **"Artifacts"**, vai aparecer **"levo-maquininha-apk"**. Clique para baixar.
6. Isso baixa um `.zip` pequeno — abra ele, e dentro está o `app-debug.apk`. Esse é o arquivo que você instala no tablet/terminal.

---

## Se algo der errado

- **Ficou vermelho (❌) em algum passo**: clique em cima do passo que falhou para ver a mensagem de erro em detalhes, e me envie o texto (ou um print) que eu leio e te digo o que ajustar.
- **Erro comum "Could not find br.com.uol.pagseguro.plugpagservice.wrapper:wrapper"**: normalmente significa que o `build.gradle` da raiz (o que tem o repositório extra do PlugPag) não foi enviado, ou foi enviado com conteúdo errado — confira se ele está exatamente na raiz do repositório, não dentro de `app/`.
- **A aba "Actions" não mostra nada**: confirme que o arquivo `build-apk.yml` está exatamente no caminho `.github/workflows/build-apk.yml` (maiúsculas/minúsculas importam) — esse é o arquivo que "avisa" o GitHub que existe uma automação para rodar.

Depois que tiver o `.apk` baixado, é só voltar para o `README-PLUGPAG.md` (Parte 6, "Configurar dentro do app já instalado") para os últimos passos: instalar no terminal e colar o código de ativação do PagBank.
