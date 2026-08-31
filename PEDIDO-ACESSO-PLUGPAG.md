# Pedido de liberação — modelo SmartPOS (maquininha Smart)

Como sua maquininha é a **Smart**, o processo de liberação é diferente do modelo Bluetooth (Minizinha): não é só autenticar uma conta — é preciso um contato comercial no PagBank e passar pela revisão/publicação do app na própria maquininha. Segundo a documentação oficial: *"Todo parceiro desenvolvedor do PagBank precisa ter um contato comercial no PagBank e possuir uma conta avançada no PagBank."* e *"Somente os desenvolvedores das aplicações podem solicitar a liberação dos Apps nas máquinas."*

## Canal a usar

Procure, no site **developer.pagbank.com.br**, o formulário de contato com o **time de Parcerias** (mencionado na FAQ do SmartPOS) — é esse o canal certo para o modelo Smart, diferente do fórum de desenvolvedores usado no modelo Bluetooth. Se você já tem um gerente de conta/contato comercial no PagBank (pela sua conta Empresarial), esse contato direto costuma agilizar o processo.

---

**Assunto:** Solicitação de parceria/liberação de app para maquininha Smart (SmartPOS)

Olá, time de Parcerias PagBank,

Sou responsável técnico pela **[RAZÃO SOCIAL / NOME DA EMPRESA]** (CNPJ **[SEU CNPJ]**), conta **[Empresarial/Avançada]** PagBank. Estamos desenvolvendo uma solução de micromercado autônomo (geladeira liberada por reconhecimento facial, com pagamento no próprio local) e gostaria de solicitar o processo de parceria/liberação para publicar um aplicativo próprio em nossos terminais **Smart**, usando o SDK PlugPag (modelo SmartPOS/embarcado).

**Dados da integração:**
- Equipamento: Maquininha **Smart**
- Modelo de integração: PlugPag SmartPOS (app instalado embarcado no terminal)
- Plataforma: Android nativo (Kotlin), sem WebView — em conformidade com a exigência de não usar aplicações baseadas em WebView/WebApp nesse modelo
- Biblioteca: `br.com.uol.pagseguro.plugpagservice.wrapper:wrapper` (versão 1.30.51)
- Uso do terminal: catálogo de produtos + cobrança (crédito, débito e Pix), integrado a um banco de dados próprio (Supabase) para controle de estoque
- E-mail da conta PagBank: carvalhosilvafrancisco@gmail.com

**Estágio atual do projeto:** o aplicativo já está desenvolvido e compilando com sucesso.

**Peço, especificamente:**
1. O envio de um **Terminal de Desenvolvimento (DEBUG)** para que eu possa instalar e testar o aplicativo via ADB antes da homologação — sei que ele é diferente do terminal de produção (identificado por marca d'água na tela).
2. Orientação sobre o fluxo completo: teste no terminal DEBUG → abertura de chamado para Homologação de App (com APK em modo release, vídeo demonstrativo e guia do usuário) → após aprovação, vinculação do(s) número(s) de série do(s) meu(s) terminal(is) de produção.

Fico à disposição para enviar a documentação necessária.

Atenciosamente,
**[SEU NOME]**
carvalhosilvafrancisco@gmail.com

---

## O que muda em relação ao pedido anterior (modelo Bluetooth)

- **Canal diferente**: não é mais o fórum de desenvolvedores/autenticação self-service — é o formulário de **Parcerias**, porque o modelo Smart exige revisão e publicação do app pelo próprio PagBank na maquininha.
- **Sem menção a "Minizinha" ou "Bluetooth"** — agora o pedido é claramente sobre o modelo Smart/SmartPOS, que é o equipamento que você realmente tem.
- **Deixei explícito que o app não usa WebView** — isso é justamente o ponto que a revisão de segurança deles costuma checar nesse modelo, então adiantar essa informação ajuda a evitar idas e vindas.
- Pedido menciona que o app **já está pronto e compilando**, para mostrar que não é um pedido especulativo — o app está no repositório `android-maquininha/`, reescrito 100% nativo (sem WebView), como descrito no `README-PLUGPAG.md`.

## Se seu contato comercial pedir mais detalhes técnicos

Aponte para este projeto (`android-maquininha/`) e destaque:
- `app/src/main/java/com/levoconveniencia/maquininha/MainActivity.kt` — toda a interface é construída com Views Android nativas (sem WebView).
- `app/src/main/java/com/levoconveniencia/maquininha/SupabaseApi.kt` — a comunicação com o catálogo/estoque é feita por chamadas HTTP diretas (REST) ao banco de dados, não por um navegador embutido.
