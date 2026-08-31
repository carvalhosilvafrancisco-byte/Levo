# Como o app roda de verdade na maquininha Smart

Diferente de um tablet comum, a maquininha Smart **não deixa instalar qualquer `.apk` livremente**. Existe um processo específico do PagBank, em 3 fases. Este guia explica cada uma.

---

## Fase 1 — Testar no Terminal de Desenvolvimento (DEBUG)

Você **não testa direto no seu terminal de produção**. O PagBank fornece um terminal separado, só para testes — o **Terminal de Desenvolvimento (DEBUG)** — com as mesmas especificações do terminal real, mas as transações são simuladas (não mexem em dinheiro de verdade) e a tela tem uma **marca d'água** para diferenciar visualmente de um terminal de produção.

**Como conseguir um:** é pedido diretamente ao seu contato comercial no PagBank, dentro do processo de parceria (veja `PEDIDO-ACESSO-PLUGPAG.md` — já ajustei o texto para solicitar isso explicitamente).

**Como instalar o app nele** (só funciona no terminal DEBUG, não no de produção):

1. No terminal DEBUG, ative o modo desenvolvedor: normalmente em Configurações → Sobre o aparelho → tocar 7 vezes em "Número da versão" (igual em qualquer Android). Depois, em Opções do desenvolvedor, ative a **Depuração USB**.
2. Instale o **ADB** (Android Debug Bridge) no seu computador — vem junto com o Android Studio, ou pode ser baixado sozinho como "SDK Platform Tools" em developer.android.com/tools/releases/platform-tools (bem mais leve que o Android Studio inteiro).
3. Conecte o terminal DEBUG ao computador por cabo USB. Autorize a depuração quando o terminal perguntar.
4. Gere o `.apk` do projeto (mesmo processo já usado — GitHub Actions ou Android Studio local).
5. No terminal/prompt de comando do computador, dentro da pasta onde está o `.apk` baixado, rode:
   ```
   adb devices
   ```
   Isso deve listar o terminal DEBUG conectado, confirmando que está tudo certo.
6. Instale o app:
   ```
   adb install app-debug.apk
   ```
7. O app aparece instalado no terminal DEBUG — abra e teste o fluxo completo (catálogo, escolha da forma de pagamento, transação simulada).

## Fase 2 — Homologação

Depois de validar tudo no terminal DEBUG, você abre um chamado no canal de suporte do PagBank, escolhendo a opção **"Homologação de App"**, enviando:

- O **`.apk` em modo release** (não debug — esse é gerado com `Build → Generate Signed App Bundle / APK` no Android Studio, ou o equivalente no GitHub Actions, usando uma chave de assinatura).
- Um **vídeo demonstrativo** mostrando o app funcionando, incluindo as chamadas de pagamento de verdade (mesmo que simuladas no terminal DEBUG).
- Um **guia do usuário** simples da aplicação (pode ser um documento curto explicando as telas: espera → catálogo → pagamento).

O PagBank faz um teste funcional e uma revisão de segurança (política de boas práticas deles) e devolve um relatório — se estiver tudo certo, o app fica apto para produção; se não, é preciso ajustar e reenviar.

**Prazo esperado:** cerca de 7 dias úteis, segundo o SLA divulgado por eles.

## Fase 3 — Produção: vincular seu(s) terminal(is)

Depois de homologado, o app é publicado na **loja de aplicativos interna** do PagBank. Mas ele só aparece no(s) seu(s) terminal(is) de produção depois de você **vincular o número de série** deles:

1. Abra um chamado no canal de suporte, opção **"Vinculação de terminais"**.
2. Envie um arquivo `.txt` simples, com os números de série das máquinas onde o app deve rodar (o número de série normalmente aparece numa etiqueta física no terminal, ou em Configurações → Sobre o aparelho).

**Prazo esperado:** cerca de 24 horas úteis.

**Importante:** segundo o próprio PagBank, **só quem desenvolveu o app pode pedir essa liberação** — ou seja, precisa ser você (ou alguém com acesso à mesma conta de parceria) abrindo esse chamado, não pode ser terceirizado para outra pessoa sem acesso à conta.

---

## Resumo do caminho completo

```
Pedido de parceria (PEDIDO-ACESSO-PLUGPAG.md)
        ↓
Recebe o Terminal de Desenvolvimento (DEBUG)
        ↓
Testa o app via ADB no terminal DEBUG
        ↓
Abre chamado "Homologação de App" (.apk release + vídeo + guia do usuário)
        ↓
PagBank revisa (~7 dias úteis) → aprova ou pede ajustes
        ↓
App publicado na loja interna do PagBank
        ↓
Abre chamado "Vinculação de terminais" com o número de série da sua maquininha
        ↓
App aparece e roda na sua maquininha de produção
```

Cada chamado de suporte mencionado aqui é aberto pelo mesmo canal usado no pedido inicial de parceria — vale perguntar ao seu contato comercial exatamente onde abrir cada tipo de chamado, já que a interface do site pode mudar com o tempo.
