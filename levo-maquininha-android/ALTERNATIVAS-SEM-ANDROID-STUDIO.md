# Alternativas para compilar o app sem o Android Studio no seu PC

Se o Android Studio não instala ou seu computador não aguenta (ele pede bastante espaço em disco e memória), aqui estão os caminhos possíveis, do mais recomendado ao mais trabalhoso.

---

## Opção 1 (recomendada): deixar o GitHub compilar pra você, de graça

Você já sobe arquivos no GitHub para publicar no Vercel — esse caminho usa exatamente a mesma conta, sem instalar nada pesado no seu computador. Eu já deixei o arquivo de configuração pronto: `.github/workflows/build-apk.yml`. Ele manda o **GitHub compilar o app inteiro na nuvem** e te entrega o `.apk` pronto para baixar.

**O passo a passo completo e detalhado — incluindo exatamente quais arquivos colocar e onde — está no arquivo `COMO-SUBIR-NO-GITHUB.md`, nesta mesma pasta.** Comece por ele.

---

## Opção 2: pedir para alguém compilar uma vez só

Se mexer com GitHub também não for o seu forte, esse é o caminho mais simples de todos para você: peça para um freelancer Android (ou alguém que já tenha Android Studio instalado) compilar o projeto uma única vez e te devolver o arquivo `.apk` pronto. É um trabalho de poucos minutos para quem já tem o ambiente configurado — os arquivos que eu já preparei (`android-maquininha/`) são tudo que essa pessoa precisa. Depois de ter o `.apk` em mãos, instalar em outros terminais é só copiar o arquivo, sem precisar repetir a compilação.

---

## Opção 3: um "Android Studio" rodando no navegador (Gitpod)

O **Gitpod** (gitpod.io) abre um ambiente de desenvolvimento completo dentro do navegador, sem instalar nada no seu PC — como se fosse um computador na nuvem só para programar. Tem plano gratuito com algumas horas por mês.

1. Suba a pasta `android-maquininha` para um repositório no GitHub (mesmo passo do início da Opção 1).
2. Acesse `gitpod.io/#` seguido do link do seu repositório (ex: `gitpod.io/#https://github.com/seu-usuario/seu-repo`).
3. Ele abre um editor de código completo no navegador, com um terminal Linux.
4. No terminal, dentro da pasta `android-maquininha`, rode: `gradle assembleDebug`.
5. O `.apk` gerado aparece em `app/build/outputs/apk/debug/` — baixe pelo próprio navegador (clique com o botão direito no arquivo → Download).

Esse caminho é mais técnico que a Opção 1 (exige digitar comandos), mas dá mais liberdade caso queira também editar o código Kotlin por lá.

---

## Opção 4: instalar só as ferramentas de linha de comando (sem o Android Studio "pesado")

Se o problema foi especificamente o instalador do Android Studio (programa grande, com interface gráfica pesada), existe uma versão **só de linha de comando**, bem mais leve, sem interface visual:

1. Instale um **JDK 17** (ex: Eclipse Temurin, gratuito, arquivo pequeno).
2. Baixe as **"Command line tools"** do Android, na parte de baixo da página developer.android.com/studio (é só um `.zip` pequeno, não o instalador completo).
3. Use o `sdkmanager` (vem dentro desse zip) para instalar apenas a "Platform" e as "Build-Tools" necessárias — bem mais leve que o Android Studio inteiro.
4. Instale o **Gradle** separadamente (gradle.org/install) ou deixe o próprio projeto cuidar disso.
5. No terminal, dentro da pasta `android-maquininha`, rode `gradle assembleDebug`.

Esse caminho ainda usa o seu computador, então só ajuda se o problema foi especificamente o Android Studio (interface gráfica), não a falta de espaço/memória em geral.

---

## Qual eu recomendo pra você

Como você já mexe com GitHub para o Vercel, a **Opção 1** é a que menos atrito vai te dar — você já sabe subir arquivos lá, e o resto é clicar em "Run workflow" e depois baixar o resultado. Comece por ela; se travar em algum ponto específico (por exemplo, a pasta `.github` sumir no upload), me avise que ajudo a resolver esse passo puntual.
