# Integração com o leitor facial — Control iD / iDFace

## Por que Control iD, e não Intelbras

Pesquisei os dois antes de decidir:

- **Intelbras**: tem API/SDK para os equipamentos da linha facial (Bio-T), mas a documentação **não é pública** — é preciso entrar em contato com o suporte (WhatsApp) e pedir acesso, caso a caso, para cada empresa. Isso torna o início mais lento e imprevisível.
- **Control iD (iDFace)**: tem uma **API REST totalmente documentada publicamente** (controlid.com.br/docs/access-api-pt/), com exemplos de código prontos no GitHub deles e uma coleção de exemplos no Postman. Não precisa pedir permissão a ninguém para começar a estudar/testar.

Por isso a integração abaixo foi construída para o **iDFace**. Se você preferir comprar um Intelbras mesmo assim, me avise — o princípio geral (ponte local sincronizando com a nuvem) continua o mesmo, só mudam os endpoints específicos.

## Como a integração funciona (arquitetura)

```
[App do cliente] --cadastro c/ foto--> [Supabase]
                                            |
                                            |  (o script abaixo verifica
                                            |   periodicamente por novos
                                            |   cadastros)
                                            v
                          [Ponte local — bridge_leitor_facial.py]
                          (roda num PC/Raspberry Pi na loja,
                           na mesma rede do leitor)
                                            |
                                            |  envia o rosto via API local
                                            v
                                   [Leitor facial iDFace]
                                            |
                                    reconhece e libera a porta
```

**Por que existe essa "ponte" no meio, em vez do Supabase falar direto com o leitor?** Porque o leitor facial só aceita conexões dentro da rede local da loja (pelo IP dele, tipo `192.168.1.50`) — a internet não alcança esse endereço de fora. Por isso é necessário ter algo rodando fisicamente na loja, na mesma rede, fazendo essa ponte. É um script simples em Python, feito para ficar ligado o tempo todo num computador pequeno (tipo um Raspberry Pi) ou até no próprio computador que já for usado para outras coisas na loja.

## O que está pronto nesta pasta

- **`bridge_leitor_facial.py`** — o script da ponte. Verifica o Supabase a cada 15 segundos por clientes novos, e cadastra o rosto deles no leitor automaticamente. Também bloqueia/desativa clientes que forem marcados como bloqueados ou lista negra no painel admin.
- **`adicionar-coluna-sincronizacao.sql`** — um comando SQL curto que você roda uma vez no Supabase, para criar a coluna que o script usa para controlar quem já foi sincronizado.

## Passo a passo para colocar em funcionamento

### 1. Rodar o SQL no Supabase
Copie o conteúdo de `adicionar-coluna-sincronizacao.sql` e cole no **SQL Editor** do seu projeto Supabase (o mesmo lugar onde você rodou o script de criação das tabelas originalmente). Clique em "Run".

### 2. Configurar o leitor facial na rede
1. Ligue o iDFace na energia e conecte na rede (Wi-Fi ou cabo) da loja.
2. No próprio display touch do equipamento, vá em Configurações → Rede, e anote o **endereço IP** dele (algo como `192.168.1.50`).
3. Ainda nas configurações do equipamento, defina uma **senha de administrador** (por padrão costuma vir `admin`/`admin` — troque essa senha).

### 3. Preparar o computador/Raspberry Pi da ponte
Escolha um computador pequeno para ficar sempre ligado na loja, na mesma rede do leitor (pode ser um Raspberry Pi, um mini PC, ou até um computador já existente que fique ligado o dia todo).

1. Instale o **Python 3** nele (python.org — gratuito).
2. Abra um terminal/prompt de comando e instale a única biblioteca externa necessária:
   ```
   pip install requests
   ```
3. Copie o arquivo `bridge_leitor_facial.py` para esse computador.
4. Abra o arquivo num editor de texto simples e preencha a seção **CONFIGURAÇÃO** no topo:
   - `SUPABASE_URL` e `SUPABASE_ANON_KEY` — os mesmos já usados no painel admin do app (aba Integrações).
   - `LEITOR_IP` — o IP que você anotou no passo 2.
   - `LEITOR_USUARIO` e `LEITOR_SENHA` — as credenciais de admin do leitor.
5. Rode o script:
   ```
   python bridge_leitor_facial.py
   ```
   Deve aparecer uma mensagem "Ponte Levo Conveniência <-> Leitor Facial iniciada." — deixe essa janela aberta.

### 4. Testar
Faça um cadastro novo no app do cliente (com foto do rosto). Em até 15 segundos, a janela do script deve mostrar `✅ Rosto de '[nome]' cadastrado no leitor com sucesso.` — depois disso, teste no próprio leitor se o rosto foi reconhecido.

### 5. Deixar rodando sempre (produção)
Rodar manualmente (`python bridge_leitor_facial.py`) é ótimo para testar, mas para produção o ideal é configurar esse script para iniciar sozinho com o computador e reiniciar automaticamente se cair. Isso é feito de forma diferente em cada sistema operacional (ex: "Serviço do Windows" no Windows, ou `systemd` no Linux/Raspberry Pi) — me avise qual sistema operacional você vai usar nesse computador que preparo o passo a passo específico.

## Limitações desta primeira versão

- **Só envia cadastros para o leitor** (nome + rosto) e bloqueia usuários banidos. Ainda **não captura os eventos de acesso** (quem abriu a porta e quando) de volta para o Supabase — isso é o que permitiria, no futuro, saber automaticamente "quem" pagou na maquininha sem precisar do campo genérico "Cliente (maquininha)". Dá para adicionar isso depois, usando o mecanismo "Monitor" da Control iD (o leitor avisa um endereço nosso toda vez que libera uma porta) — me avise quando quiser evoluir para essa parte.
- **Não testei este script contra um leitor real** (não tenho o equipamento aqui para validar). Os endpoints (`user_set.fcgi`, `face_create.fcgi`) e o formato dos dados foram escritos com base na documentação pública oficial da Control iD, mas pode ser necessário um pequeno ajuste na prática — qualquer erro que aparecer no terminal do script, me envie a mensagem completa que eu ajusto com precisão, como já fizemos com a integração do PagBank.
