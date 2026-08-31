# Levo Conveniência — Guia de Hospedagem e Próximos Passos

Este documento explica **onde hospedar o app**, **como ele funciona hoje** e **como evoluir** para produção (backend real + reconhecimento facial de verdade + pagamento).

---

## 0. O fluxo completo do cliente — e o status de cada etapa

```
1. Abertura da porta via leitor facial (Intelbras/iDFace)   → 🟡 documentado, falta integrar (seção 4)
2. Cliente pega o produto na geladeira                       → (ação física, sem software)
3. Cliente seleciona o produto na tela da maquininha Smart    → ✅ pronto (app nativo, projeto android-maquininha/)
4. Cliente paga (crédito/débito/Pix) direto na maquininha     → 🟡 código pronto, aguardando liberação do PagBank
5. Estoque e histórico atualizam sozinhos no painel admin     → ✅ pronto (Supabase, tempo real)
```

- **Etapa 1 (porta/facial)**: ainda é a única parte sem integração de código — hoje é só reconhecimento facial simulado no cadastro do app do cliente (usado para "ensinar" o rosto ao leitor físico, no futuro). O leitor físico (Intelbras ou Control iD/iDFace) ainda precisa ser comprado e integrado — veja a seção 4.
- **Etapas 3 e 4 (maquininha)**: o projeto inteiro está pronto em `android-maquininha/` — um app 100% nativo (sem WebView, por exigência do PagBank para o modelo da sua maquininha Smart), que mostra o catálogo, deixa escolher a forma de pagamento e aciona o terminal de verdade via SDK PlugPag. Falta só a liberação/homologação do PagBank para rodar em produção — veja `android-maquininha/COMO-INSTALAR-NA-MAQUININHA-SMART.md` para o processo completo.
- **Etapa 5 (estoque/histórico)**: já funciona de ponta a ponta. Assim que uma venda é aprovada na maquininha, o app grava direto no Supabase — o painel admin (Dashboard, Estoque, Histórico) atualiza sozinho, sem nenhuma ação manual.

O app do celular do cliente (catálogo pra conferir o que tem, histórico de compras, dados da conta) roda em paralelo a esse fluxo, mas não faz parte do caminho de abrir/pagar — ele é só consulta.

---

## 1. O que você tem agora

Um arquivo único, `index.html`, com todo o app dentro (HTML + CSS + JavaScript). Ele já funciona como:

- **Fluxo alinhado ao uso real**: o app do celular é uma vitrine + conta do cliente — não é mais o "portão" de entrada. O reconhecimento facial acontece uma única vez, no cadastro (a foto capturada ali é o que seria enviado ao leitor físico da porta); no dia a dia, o cliente nunca mais escaneia o rosto pelo celular — isso acontece direto no leitor da geladeira (ver seção 4).
- Cadastro de usuário com nome, telefone, e-mail, CPF e foto do rosto (uma única vez). Ao concluir, o cliente já fica "logado" no app neste aparelho — não precisa se identificar de novo nas próximas vezes que abrir o app.
- **Consentimento LGPD obrigatório no cadastro**, com Política de Privacidade completa acessível a qualquer momento (splash ou aba Perfil).
- App do cliente com três abas: **Produtos** (catálogo com foto, preço e quantidade disponível — só visualização, sem botão de comprar/pagar), **Histórico** (todas as compras já pagas: o quê, quando, quanto) e **Perfil** (dados pessoais, resumo e botão para sair da conta).
- **Tela da maquininha (kiosk, sem login, sem saída)**: pensada para rodar 24h num tablet fixo ao lado da geladeira. Abre numa **tela de espera** com a logo e "Toque aqui para iniciar" — ao tocar, mostra os produtos; o cliente escolhe a quantidade e toca em pagar. No momento em que o pagamento é confirmado, **o estoque é debitado automaticamente** e a tela volta sozinha para a espera, pronta para o próximo cliente. Se ninguém tocar em nada por 45 segundos, também volta sozinha para a tela de espera. Não existe botão de sair nessa tela — é para ficar sempre ligada. Essa tela é aberta pelo link `?maquininha=1` (também acessível pelo painel admin, aba Integrações) e pelo link "Este aparelho é a maquininha" na tela inicial.
- Toda tela tem um botão de voltar/sair claro — nas do cliente e nas do administrador — para facilitar o manuseio.
- Painel administrativo (senha padrão `admin123`) com cinco abas:
  - **Dashboard** — faturamento do dia, do mês, ticket médio, gráfico de faturamento diário e ranking dos produtos mais vendidos, todos atualizados automaticamente conforme as vendas acontecem na maquininha.
  - **Estoque** — cadastrar produtos com foto real, ajustar quantidade, editar/excluir, e um botão de **venda manual** só para casos fora da maquininha (ex: dinheiro).
  - **Usuários** — todos os dados de cada cliente, bloqueio de acesso e lista negra.
  - **Histórico** — todos os pagamentos de todos os clientes, com uma etiqueta indicando se a venda veio da maquininha (automática) ou foi lançada manualmente.
  - **Integrações** — onde fica o link da tela da maquininha, e os campos para conectar o leitor facial físico e o gateway de pagamento real (ver seções 4 e 6).

### Até onde a automação vai

O app debita o estoque **na hora** assim que alguém paga na maquininha — isso é automático, sem você confirmar nada. A questão é se essa atualização chega **instantaneamente aos outros aparelhos** (o celular do cliente, o seu celular de admin):

- **Sem o Supabase conectado** (seção 3): cada aparelho guarda os dados só nele mesmo (localStorage). A sincronização automática só acontece entre abas do mesmo navegador — bom para testar tudo sozinho, mas não resolve a operação real com aparelhos físicos diferentes.
- **Com o Supabase conectado** (seção 3 — já implementado, é só configurar): qualquer venda na maquininha aparece instantaneamente em todos os aparelhos conectados, seja o celular do cliente, o tablet da maquininha ou o seu painel admin.

Além disso, como a maquininha não pede login (por decisão sua, para agilizar a compra), hoje ela registra a venda como "Cliente (maquininha)", sem vincular a um cliente específico — a vinculação automática por pessoa só fica 100% precisa quando a integração com o leitor facial (seção 4) estiver pronta, cruzando "quem abriu a porta" com "quem pagou".

Sem o Supabase conectado, os dados (usuários, produtos, consumos) ficam salvos apenas no **localStorage do navegador** de cada aparelho — funcionam offline, mas isolados. Configurando o Supabase (seção 3), tudo passa a ficar centralizado e sincronizado.

---

## 2. Onde hospedar (passo a passo)

Como é um app **estático** (só HTML/CSS/JS, sem servidor próprio ainda), qualquer uma destas opções funciona e todas têm plano gratuito:

### Opção recomendada para começar: **Vercel**
1. Crie uma conta em vercel.com (pode entrar com GitHub).
2. Crie um repositório no GitHub e suba o arquivo `index.html`.
3. No Vercel, clique em "Add New Project", conecte o repositório e clique em "Deploy".
4. Em poucos segundos você recebe uma URL pública (ex: `pontofrio24.vercel.app`) já com HTTPS — **obrigatório**, pois o acesso à câmera só funciona em conexões seguras (HTTPS) ou em `localhost`.

### Alternativas igualmente boas:
- **Netlify** (netlify.com) — arraste a pasta do projeto direto no painel ("Deploy manually"), sem precisar de GitHub.
- **GitHub Pages** — gratuito, direto do seu repositório GitHub, ideal se você já usa GitHub.
- **Cloudflare Pages** — rápido e com boa distribuição no Brasil.

### Domínio próprio
Depois de publicado em qualquer uma dessas plataformas, você pode apontar um domínio próprio (ex: `pontofrio24.com.br`), registrado no Registro.br, para a URL gerada.

### App de celular "de verdade" (PWA) — já está implementado

**Atualização: esta etapa já está pronta.** Agora, além do `index.html`, o projeto tem mais arquivos que precisam ser publicados **na mesma pasta** dele:

```
index.html
manifest.json
service-worker.js
icons/
  icon-192.png
  icon-512.png
  icon-512-maskable.png
  apple-touch-icon.png
  favicon-32.png
```

**No GitHub/Vercel:** suba todos esses arquivos mantendo essa mesma estrutura de pastas (a pasta `icons` precisa continuar se chamando `icons`, no mesmo nível do `index.html`). Se você já tem um repositório, é só adicionar os arquivos novos ao lado do `index.html` existente e subir — não precisa mudar nada na configuração do Vercel.

**O que isso habilita:**
- No Android (Chrome), depois de visitar o site algumas vezes, aparece um botão **"Instalar app no celular"** na aba Perfil do cliente — um toque e o app vai para a tela inicial do celular, com ícone próprio, abrindo em tela cheia (sem a barra de endereço do navegador), como um app nativo.
- No iPhone (Safari não tem esse botão automático, é uma limitação da Apple), a aba Perfil mostra a instrução: toque no ícone de compartilhar do Safari → "Adicionar à Tela de Início".
- O app carrega mais rápido em visitas seguintes e funciona com internet instável, porque o "esqueleto" dele (telas, estilos) fica guardado no aparelho — os dados (estoque, histórico) continuam vindo do Supabase normalmente quando há conexão.

**Ao publicar uma atualização do app no futuro:** abra o arquivo `service-worker.js` e troque `levo-conveniencia-v1` para `v2` (ou o próximo número) na primeira linha de código. Isso avisa o navegador de quem já instalou o app para baixar a versão nova, em vez de continuar preso na versão antiga em cache.

### Travando de verdade o tablet da maquininha (nível do aparelho)
Dentro do app, a tela da maquininha já não tem nenhum botão de sair, e volta sozinha para a tela de espera após pagar ou após 45 segundos parada. Isso impede a **navegação dentro do app**. Mas para impedir que a pessoa saia do navegador (feche a aba, abra outro app, mude de site), é preciso travar isso no **próprio aparelho** — isso é feito fora do código, na configuração do tablet:
- **Tablet Android**: ative o "Modo de fixação de tela" (Screen Pinning) em Configurações → Segurança, ou instale o app **Fully Kiosk Browser** (gratuito), que abre um site em tela cheia e bloqueia a saída — é a opção mais usada para totens comerciais.
- **iPad**: ative o **Acesso Guiado** (Guided Access) em Ajustes → Acessibilidade, que trava o iPad num único app/site.
- **Chrome/Windows**: rode o navegador com a flag `--kiosk` (ex: `chrome --kiosk https://seuapp.vercel.app?maquininha=1`), que abre em tela cheia sem barra de endereço.

---

## 3. Backend: já está implementado (Supabase)

**Atualização: esta etapa já está pronta no app.** O app agora sabe se conectar ao **Supabase** — um banco de dados na nuvem com sincronização em tempo real — para que:

- O estoque seja o **mesmo para todo mundo**, em qualquer aparelho.
- Uma venda na maquininha apareça **instantaneamente** no catálogo do cliente e no seu painel admin, mesmo sendo três aparelhos físicos diferentes.
- Os dados de cadastro fiquem centralizados, não apenas no navegador de cada pessoa.

### Passo a passo para conectar (leva uns 5 minutos)

1. Crie uma conta grátis em **supabase.com** e clique em "New Project". Escolha um nome e uma senha de banco (guarde essa senha, mas ela não será usada no app).
2. Dentro do projeto, vá no menu **SQL Editor** (ícone de banco de dados na lateral) e clique em "New query".
3. Abra o app, vá em **Painel Admin → Integrações**, e copie o SQL que já está pronto ali (campo "SQL para colar no SQL Editor do Supabase"). Cole no SQL Editor do Supabase e clique em "Run". Isso cria as três tabelas (`usuarios`, `produtos`, `consumos`) e liga a sincronização em tempo real.
4. No Supabase, vá em **Project Settings → API** (em projetos mais novos pode aparecer como **Settings → API Keys**). Copie o campo **Project URL** e a chave pública — o nome dela varia conforme quando seu projeto foi criado:
   - Projetos mais antigos: chave chamada **`anon`** ou **`anon public`** (aba "Legacy API Keys").
   - Projetos mais novos: chave chamada **`publishable key`**, que começa com `sb_publishable_...` (aba "API Keys").
   
   Qualquer uma das duas funciona no campo do app. **Nunca copie a chave `service_role` ou `secret key`** (`sb_secret_...`) — essa é a chave mestra do servidor, com acesso total ao banco, e não pode ir para um app que roda no navegador do cliente.
5. Volte no app, cole os dois valores nos campos correspondentes e clique em **"Conectar e sincronizar"**.

Pronto — a partir daí, todo aparelho que abrir o app com essa mesma URL/chave configuradas (celular do cliente, tablet da maquininha, seu celular de admin) vai ler e escrever no mesmo banco, em tempo real.

### Como isso é configurado em cada aparelho

A conexão (URL + chave) fica salva no navegador de cada aparelho, então você precisa repetir o passo 5 uma vez em cada um: no celular de cadastro dos clientes (ou publicar isso já embutido, veja abaixo), no tablet da maquininha e no seu celular de admin.

**Dica para não repetir isso em cada celular de cliente:** como a chave *anon public* do Supabase é feita para ser pública (protegida pelas regras de segurança da tabela, não por sigilo), você pode deixá-la já preenchida "de fábrica" no arquivo `index.html`, direto no código, para que todo novo cliente já abra o app conectado, sem precisar configurar nada. Se quiser isso, me avise com a URL e a chave do seu projeto que eu ajusto o arquivo.

### Um ponto de segurança a saber

O SQL que o app gera cria as tabelas com uma política de acesso **aberta** (qualquer pessoa com a chave anon consegue ler e escrever), para o protótipo funcionar sem complicação. Isso é aceitável para testar, mas antes de operar com dinheiro de verdade, vale restringir isso — por exemplo, permitindo que só o painel admin (com uma segunda camada de autenticação) possa alterar usuários e preços, enquanto o app do cliente e a maquininha só têm permissão de leitura e de criar vendas. Posso te ajudar a apertar essas regras (chamadas de "Row Level Security") quando você estiver perto de lançar de verdade.

Se preferir não usar Supabase, as alternativas continuam válidas: **Firebase**, ou um backend próprio em **Node.js + PostgreSQL**.

---

## 4. Integração com leitor facial físico (Intelbras / Control iD-iDFace)

Diferente de um app comum, aqui **o hardware é quem trava e destrava a fechadura da geladeira** — o app não substitui o leitor facial físico, ele passa a *conversar* com ele. É assim que normalmente essa integração é montada nos dois fabricantes que você mencionou:

### Como funciona na prática
1. **O leitor facial fica na porta da geladeira** (ex: linha Intelbras SS FACE ou Control iD iDFace), conectado à rede local (Wi-Fi/Ethernet) e à fechadura elétrica/eletroímã.
2. Os **rostos cadastrados no seu app** precisam ser sincronizados com o dispositivo — ou seja, quando alguém se cadastra no Levo Conveniência, a foto e os dados precisam ser enviados automaticamente para o leitor (para que ele reconheça aquele rosto ao vivo).
3. Quando o leitor reconhece um rosto e libera a fechadura, ele registra esse evento — o app precisa "escutar" esse evento para saber quem entrou e liberar a tela de compras correspondente.
4. Se você bloquear ou colocar alguém em lista negra no app (já implementado no painel admin), essa informação também precisa ser enviada ao leitor, para que ele pare de liberar o acesso daquela pessoa fisicamente — não apenas na tela do app.

### O que isso exige tecnicamente
- Ambos os fabricantes vendem controladores que funcionam como equipamento standalone (cadastro pela própria tela touch do aparelho) **e** oferecem integração via rede local para sistemas de gestão — é assim que empresas de controle de acesso (ex: parceiros Intelbras e Control iD) sincronizam listas de usuários e recebem eventos de acesso em tempo real.
- Essa sincronização é feita por um **backend** (servidor seu, não o navegador do cliente) que se comunica com o IP do dispositivo na rede local, e também recebe notificações/eventos do equipamento.
- Como os protocolos e SDKs de cada fabricante mudam com frequência e variam por modelo (ex: SS 3530 FACE vs SS 7530 FACE, iDFace Lite vs iDFace Pro), o caminho mais seguro é:
  1. Definir qual modelo específico você vai comprar.
  2. Entrar em contato com o time de integração da Intelbras ou revenda autorizada Control iD/iDFace e solicitar a documentação de integração (SDK/API) daquele modelo — eles fornecem isso para desenvolvedores.
  3. Eu te ajudo a implementar a ponte entre o backend do Levo Conveniência e essa API assim que você tiver o modelo e a documentação em mãos.

No app, já deixei pronta a aba **Integrações → Leitor facial** no painel admin, onde você poderá salvar a marca e o IP do equipamento assim que essa etapa avançar.

### Alternativa mais simples para validar o negócio primeiro
Enquanto a integração física não está pronta, muitos micromercados autônomos usam o próprio leitor facial do fabricante de forma standalone (cadastro direto no aparelho) só para liberar a porta, e usam o app **separadamente** para mostrar catálogo/estoque e registrar consumo por autodeclaração do cliente. É uma forma de operar e começar a vender enquanto a integração completa é desenvolvida.

---

### Como o app "sabe" que uma compra aconteceu, hoje e no futuro

Hoje, no protótipo, quem confirma que o pagamento aconteceu é a própria **tela da maquininha** (a que roda sem login, pensada para um tablet fixo ao lado da geladeira) — a pessoa seleciona o item, toca em pagar, e o estoque já é debitado automaticamente naquele instante, sem você precisar confirmar nada. Isso já resolve a automação do estoque.

O que ainda depende das integrações das seções 4 e 6 é a parte de **saber com precisão quem comprou** (hoje a venda entra como "Cliente (maquininha)", sem nome) e de **cobrar de verdade em vez de simular a aprovação**. Numa operação real: o leitor facial informa ao backend quem abriu a porta, e o gateway de pagamento (PagBank/Stone) informa ao backend quando aquele pagamento foi de fato aprovado — o backend cruza as duas informações para vincular a compra ao cliente certo automaticamente.

## 5. Integração com a maquininha PagBank (PlugPag) — já implementada

**Atualização: esta etapa já foi construída.** Ela vive em um projeto separado, dentro da pasta `android-maquininha/`, porque a comunicação com uma maquininha física por Bluetooth só é possível a partir de um **app Android nativo** — nenhum navegador (nem o nosso PWA) tem acesso a isso. Veja o arquivo `android-maquininha/README-PLUGPAG.md` para o passo a passo completo. Resumo:

- É um app Android pequeno que abre o mesmo site da maquininha (`?maquininha=1`) dentro de uma janela em tela cheia.
- Quando o cliente toca em "Pagar", em vez de simular a aprovação, ele aciona de verdade o SDK **PlugPag** do PagBank, que fala com o terminal físico por Bluetooth (ou roda embarcado, se você usar um terminal Moderninha Smart, que já é um Android completo).
- Quando o terminal aprova o pagamento, o app nativo avisa o site, que dá baixa no estoque e registra a venda no Supabase — exatamente como já funciona hoje na simulação, só que com dinheiro de verdade por trás.
- **Você (ou um desenvolvedor Android) precisa compilar esse projeto no Android Studio** — eu não consigo compilar/testar um app Android neste ambiente, mas todo o código-fonte já está pronto e comentado.

**O que falta para colocar em produção:**
1. Solicitar ao PagBank o acesso ao SDK PlugPag para o seu CNPJ (não é liberado por padrão, precisa pedir pelo canal de integração deles).
2. Ter um terminal PagBank compatível (a linha Moderninha Smart é a mais indicada, porque o app roda direto na tela dele, sem precisar de um tablet separado).
3. Pegar o código de ativação daquele terminal específico com o PagBank.
4. Compilar o app e configurar a URL do seu site + o código de ativação (isso é feito direto no app, sem precisar recompilar).

### Alternativa mais simples, se quiser evitar o app Android
Se em algum momento preferir não depender de compilar um app nativo, existe um caminho **só em nuvem**: gerar uma cobrança Pix por QR Code através da API do PagBank (sem precisar de terminal físico nem app Android), mostrando o QR Code na própria tela do site. Não é o que você pediu agora, mas fica registrado aqui caso queira simplificar depois — é só avisar.

### Sobre a Stone (opção alternativa de gateway)
Se no futuro trocar de gateway, a Stone tem um caminho parecido chamado **Stone Connect**, voltado para terminais integrados a sistemas de terceiros — inclusive com um case público de parceria com a VMtecnologia especificamente para micromercados autônomos. A estrutura seria semelhante (app nativo se comunicando com o terminal), mas com o SDK da Stone no lugar do PlugPag.

---

## 6. Conformidade com a LGPD

O app já inclui, na tela de cadastro:
- Um resumo claro de **para que os dados servem** (liberar acesso facial, registrar consumo, e permitir que você entre em contato — inclusive para cobranças/pendências).
- Uma **caixa de consentimento obrigatória**, que precisa ser marcada antes de concluir o cadastro.
- Uma **Política de Privacidade completa**, acessível a qualquer momento pela tela inicial, com finalidade, base legal, direitos do titular e prazo de retenção.
- No painel admin, cada usuário mostra a data em que aceitou o consentimento.

**O que fica por sua conta, antes de operar de verdade:**
1. Preencher, no texto da Política de Privacidade (dentro do `index.html`, seção `modalPrivacidade`), o CNPJ/razão social do seu negócio como controlador dos dados, e um contato (e-mail ou telefone) para o encarregado de dados (DPO) — pode ser você mesmo em uma operação pequena.
2. Definir por quanto tempo os dados ficam guardados após alguém parar de usar o serviço, e um processo simples para excluir dados quando solicitado (hoje isso pode ser feito manualmente pelo admin, apagando o cadastro).
3. Ao integrar o leitor facial físico (seção 4), lembrar que a **imagem facial é um dado sensível** pela LGPD — o consentimento específico para isso já está coberto no texto atual, mas reforce com o fabricante do leitor onde e por quanto tempo aquele dispositivo guarda as imagens.
4. Se for cobrar ou registrar CPF, considere também os termos de uso do gateway de pagamento escolhido (PagBank/Stone), que têm suas próprias exigências de dados do titular do cartão.

Isso não substitui uma orientação jurídica formal — para operar com segurança, vale uma revisão rápida com um advogado especializado em LGPD antes do lançamento oficial, principalmente por lidar com biometria facial (dado sensível).

---

## 7. Resumo do que fazer agora vs. depois

**Agora (o que já está pronto):**
- [x] Cadastro com dados + CPF + foto do rosto (uma única vez) + consentimento LGPD obrigatório
- [x] App do cliente sem login facial repetido — sessão persistente no aparelho
- [x] Catálogo (visualização), Histórico e Perfil para o cliente
- [x] **Tela da maquininha (kiosk sem login) com débito automático de estoque no momento do pagamento**
- [x] **Sincronização em tempo real entre aparelhos via Supabase (é só conectar — passo a passo na seção 3)**
- [x] **App instalável no celular (PWA) — o cliente pode adicionar à tela inicial e usar como um app nativo**
- [x] Painel admin com abas: Dashboard, Estoque, Usuários (bloqueio e lista negra), Histórico e Integrações
- [x] Política de Privacidade completa dentro do app

**Próximo passo sugerido:** publicar o app (Vercel/Netlify) incluindo os arquivos novos do PWA (`manifest.json`, `service-worker.js`, pasta `icons/`), conectar o Supabase seguindo a seção 3, preencher o CNPJ e contato do DPO na Política de Privacidade, e testar o fluxo completo com um tablet fazendo o papel da maquininha e o celular fazendo o papel do app do cliente — a venda deve aparecer instantaneamente nos dois.

**Depois:**
1. Apertar as regras de segurança (Row Level Security) do Supabase antes de operar com dinheiro de verdade — hoje elas estão abertas para simplificar o teste.
2. Integração real com o leitor facial (Intelbras ou Control iD/iDFace) — é o que vai permitir saber automaticamente qual cliente pagou.
3. **Compilar o app Android da maquininha** (pasta `android-maquininha/`, veja o README dentro dela) e solicitar ao PagBank o acesso ao SDK PlugPag para o seu terminal.

---

*Este projeto está salvo no seu histórico de conversa — é só voltar aqui para pedir ajustes, novas telas ou avançar para qualquer uma das etapas acima.*
