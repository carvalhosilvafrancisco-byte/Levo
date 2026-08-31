# Como testar o app sem a maquininha física

## O que dá para testar em qualquer Android comum (celular, tablet ou emulador)

O `.apk` deste projeto é um app Android normal — diferente da instalação na maquininha Smart (que é travada), ele instala tranquilamente em **qualquer celular Android** através do processo comum ("fontes desconhecidas" no Android, igual qualquer app fora da Play Store) ou num **emulador do Android Studio**, sem precisar de nenhum equipamento físico do PagBank.

Nele, dá pra testar:
- A tela de espera ("Toque aqui para iniciar")
- O catálogo carregando os produtos de verdade do Supabase
- A seleção de quantidade
- A escolha da forma de pagamento (crédito/débito/Pix)
- **O pagamento em si, de forma simulada** (novidade — veja abaixo)
- A baixa de estoque e o registro da venda no Supabase (aparecendo no Dashboard/Histórico do admin)
- O retorno automático à tela de espera

## Modo de teste (novo) — pagamentos simulados

Adicionei um **modo de teste** no app: quando ele tenta cobrar de verdade e não encontra um terminal PagBank real conectado (o que é esperado em qualquer aparelho que não seja a maquininha Smart), ele agora **pergunta** se você quer simular a aprovação daquela venda, em vez de só mostrar um erro.

Você tem duas opções nessa hora:
- **"Simular esta venda"** — aprova só essa vez, e pergunta de novo na próxima.
- **"Sempre simular neste aparelho"** — liga o modo de teste permanentemente nesse aparelho específico (útil se for usar só para testes por um tempo). Quando ativado, aparece uma faixa laranja no topo da tela do catálogo, escrito "⚠️ MODO TESTE", para nunca ter dúvida de que aquele pagamento não é de verdade.

Você também pode ligar/desligar isso manualmente a qualquer momento: toque e segure a tela de espera por ~3 segundos → marque ou desmarque a caixinha "Modo de teste (simular pagamentos, sem terminal real)".

**Importante:** quando o app estiver rodando na maquininha Smart de verdade (com o SDK PlugPag realmente conectado ao terminal), esse modo de teste não deveria ser acionado — ele só entra em ação quando a chamada ao terminal falha, o que só deve acontecer fora da maquininha real. Ainda assim, é uma boa prática **desligar o modo de teste manualmente** antes de ir para produção de verdade, para garantir que nenhuma venda seja simulada por engano.

## O que NÃO dá para testar fora da maquininha

- A comunicação real com o terminal PagBank (óbvio — sem o SDK PlugPag conectado a um terminal de verdade, isso só é simulado).
- Nenhuma característica de hardware específica da linha Smart (impressora térmica embutida, se você vier a usar, etc.).

## Resumo dos caminhos possíveis para testar

1. **Instalar num celular Android qualquer** — baixe o `.apk` (gerado pelo GitHub Actions, como sempre) e instale normalmente. Mais rápido, usa um aparelho que você já tem.
2. **Emulador do Android Studio** — se você tiver o Android Studio instalado, pode criar um "dispositivo virtual" e rodar o app nele sem precisar de nenhum aparelho físico.
3. **Terminal de Desenvolvimento (DEBUG) do PagBank** — é o único jeito de testar o pagamento de verdade (mesmo que simulado pelo próprio PagBank), mas depende do processo de parceria já em andamento.

Use as opções 1 ou 2 agora, para validar todo o resto do app enquanto aguarda o retorno do PagBank sobre o terminal DEBUG.
