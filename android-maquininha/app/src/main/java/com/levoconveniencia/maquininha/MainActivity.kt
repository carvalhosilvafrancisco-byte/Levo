package com.levoconveniencia.maquininha

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagActivationData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData
import java.text.NumberFormat
import java.util.Locale

/**
 * App nativo da maquininha da Levo Conveniência — modelo SmartPOS (app instalado
 * embarcado dentro da própria maquininha, ex: Moderninha Smart).
 *
 * IMPORTANTE: este app NÃO usa WebView. A documentação oficial do PagBank proíbe
 * explicitamente aplicações baseadas em WebView/WebApp para o modelo SmartPOS —
 * por isso todas as telas aqui são Views Android nativas, construídas em Kotlin.
 * A comunicação com o catálogo/estoque é feita direto pela API REST do Supabase
 * (arquivo SupabaseApi.kt), sem depender de carregar o site do app do cliente.
 *
 * As assinaturas do SDK PlugPag usadas aqui (`PlugPag(context)`,
 * `initializeAndActivatePinpad(...)`, `doPayment(...)` retornando um objeto com
 * `.result`/`.message`) já foram confirmadas numa compilação real bem-sucedida
 * anteriormente — se voltar a dar erro de compilação, o próprio erro do Kotlin
 * indica a assinatura exata a corrigir.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var plugPag: PlugPag
    private var supabaseApi: SupabaseApi? = null

    private lateinit var root: FrameLayout
    private lateinit var telaEspera: LinearLayout
    private lateinit var telaCatalogo: LinearLayout
    private lateinit var listaProdutos: LinearLayout

    private val qtdSelecionada = mutableMapOf<String, Int>()
    private var produtosCache: List<Produto> = emptyList()

    private val handlerInatividade = Handler(Looper.getMainLooper())
    private val TEMPO_INATIVIDADE_MS = 45_000L
    private val voltarParaEsperaRunnable = Runnable { mostrarTelaEspera() }

    // Credenciais do Supabase do projeto Levo Conveniência, já embutidas no app.
    // Isso é seguro porque a chave abaixo é do tipo "anon public"/"publishable" —
    // feita para ser exposta em clientes, protegida pelas regras de acesso das
    // tabelas (Row Level Security), não por sigilo. Assim, quem instalar o app
    // pela loja do PagBank não precisa configurar nada — já funciona sozinho.
    private val SUPABASE_URL_EMBUTIDO = "https://eeqkhlxplkswqpwvqpji.supabase.co"
    private val SUPABASE_KEY_EMBUTIDA = "sb_publishable_nrgrl4nLkMoOX97th4QMiw_HsLAEqjA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("levo_maquininha_prefs", Context.MODE_PRIVATE)
        plugPag = PlugPag(this)

        root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#EAF4F6"))
        setContentView(root)

        construirTelaEspera()
        construirTelaCatalogo()
        mostrarTelaEspera()

        ativarTerminalSeNecessario()
        configurarSupabase()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Qualquer toque na tela reinicia o contador de inatividade (só importa
        // enquanto a tela de catálogo estiver visível).
        reiniciarTimerInatividade()
        return super.dispatchTouchEvent(ev)
    }

    private fun reiniciarTimerInatividade() {
        handlerInatividade.removeCallbacks(voltarParaEsperaRunnable)
        if (telaCatalogo.visibility == View.VISIBLE) {
            handlerInatividade.postDelayed(voltarParaEsperaRunnable, TEMPO_INATIVIDADE_MS)
        }
    }

    // ==================== CONFIGURAÇÃO (Supabase + PagBank) ====================

    private fun configurarSupabase() {
        // Sempre conecta com as credenciais embutidas — nenhuma configuração
        // manual é necessária. O campo abaixo existe só para o caso raro de
        // precisar apontar para outro projeto Supabase durante testes internos.
        val urlPersonalizada = prefs.getString("supabase_url_override", null)
        val keyPersonalizada = prefs.getString("supabase_key_override", null)
        val url = if (!urlPersonalizada.isNullOrBlank()) urlPersonalizada else SUPABASE_URL_EMBUTIDO
        val key = if (!keyPersonalizada.isNullOrBlank()) keyPersonalizada else SUPABASE_KEY_EMBUTIDA
        supabaseApi = SupabaseApi(url.trimEnd('/'), key)
    }

    private fun ativarTerminalSeNecessario() {
        val codigoAtivacao = prefs.getString("codigo_ativacao", null)
        if (codigoAtivacao.isNullOrBlank()) return
        Thread {
            try {
                val resultado = plugPag.initializeAndActivatePinpad(PlugPagActivationData(codigoAtivacao))
                if (resultado.result != PlugPag.RET_OK) {
                    runOnUiThread {
                        Toast.makeText(this, "Não foi possível ativar o terminal (código ${resultado.result}).", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Erro ao ativar o terminal: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /** Toque e segure a tela de espera por ~3s para configurar Supabase e ativação. */
    private fun abrirConfiguracoes() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 30, 50, 10)

        fun campo(hintTexto: String, valorAtual: String?): EditText {
            val e = EditText(this)
            e.hint = hintTexto
            e.setText(valorAtual ?: "")
            layout.addView(e)
            return e
        }

        val campoAtivacao = campo("Código de ativação do terminal (PagBank)", prefs.getString("codigo_ativacao", ""))

        val checkModoTeste = CheckBox(this).apply {
            text = "Modo de teste (simular pagamentos, sem terminal real)"
            isChecked = prefs.getBoolean("modo_teste", false)
            setPadding(0, 20, 0, 0)
        }
        layout.addView(checkModoTeste)

        AlertDialog.Builder(this)
            .setTitle("Configurações da maquininha")
            .setView(layout)
            .setPositiveButton("Salvar e reiniciar") { _, _ ->
                prefs.edit()
                    .putString("codigo_ativacao", campoAtivacao.text.toString().trim())
                    .putBoolean("modo_teste", checkModoTeste.isChecked)
                    .apply()
                recreate()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ==================== TELA DE ESPERA ====================

    private fun construirTelaEspera() {
        telaEspera = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0B1E2D"))
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val cartaoLogo = LinearLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            setPadding(60, 50, 60, 50)
        }
        val imgLogo = ImageView(this).apply {
            setImageResource(R.drawable.logo_levo)
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(600, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        cartaoLogo.addView(imgLogo)
        telaEspera.addView(cartaoLogo)

        val txtToque = TextView(this).apply {
            text = "Toque aqui para iniciar"
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(0, 60, 0, 0)
        }
        telaEspera.addView(txtToque)

        telaEspera.setOnClickListener { mostrarTelaCatalogo() }
        telaEspera.setOnLongClickListener { abrirConfiguracoes(); true }

        root.addView(telaEspera)
    }

    private fun mostrarTelaEspera() {
        handlerInatividade.removeCallbacks(voltarParaEsperaRunnable)
        telaEspera.visibility = View.VISIBLE
        telaCatalogo.visibility = View.GONE
    }

    // ==================== TELA DE CATÁLOGO / PAGAMENTO ====================

    private fun construirTelaCatalogo() {
        telaCatalogo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            visibility = View.GONE
        }

        if (prefs.getBoolean("modo_teste", false)) {
            val faixaTeste = TextView(this).apply {
                text = "⚠️ MODO TESTE — pagamentos são simulados, sem terminal real"
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#E08A00"))
                gravity = Gravity.CENTER
                setPadding(0, 12, 0, 12)
            }
            telaCatalogo.addView(faixaTeste)
        }

        val cabecalho = TextView(this).apply {
            text = "Selecione o que você retirou e toque em pagar"
            textSize = 15f
            setPadding(40, 50, 40, 20)
            setTextColor(Color.parseColor("#5C7A89"))
        }
        telaCatalogo.addView(cabecalho)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        listaProdutos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 0, 30, 30)
        }
        scroll.addView(listaProdutos)
        telaCatalogo.addView(scroll)

        root.addView(telaCatalogo)
    }

    private fun mostrarTelaCatalogo() {
        telaEspera.visibility = View.GONE
        telaCatalogo.visibility = View.VISIBLE
        reiniciarTimerInatividade()
        carregarProdutos()
    }

    private fun carregarProdutos() {
        val api = supabaseApi
        if (api == null) {
            mostrarMensagemCatalogo("Erro interno de configuração. Tente reiniciar o app.")
            return
        }
        mostrarMensagemCatalogo("Carregando produtos…")
        api.listarProdutos { lista, erro ->
            runOnUiThread {
                if (lista == null) {
                    mostrarMensagemCatalogo("Não foi possível carregar os produtos: $erro")
                    return@runOnUiThread
                }
                produtosCache = lista
                renderizarProdutos(lista)
            }
        }
    }

    private fun mostrarMensagemCatalogo(msg: String) {
        listaProdutos.removeAllViews()
        val txt = TextView(this).apply {
            text = msg
            textSize = 14f
            setPadding(20, 40, 20, 40)
            setTextColor(Color.parseColor("#5C7A89"))
        }
        listaProdutos.addView(txt)
    }

    private fun formatoMoeda(valor: Double): String {
        val nf = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return nf.format(valor)
    }

    private fun renderizarProdutos(lista: List<Produto>) {
        listaProdutos.removeAllViews()
        if (lista.isEmpty()) {
            mostrarMensagemCatalogo("Nenhum produto cadastrado ainda.")
            return
        }
        for (p in lista) {
            listaProdutos.addView(criarLinhaProduto(p))
        }
    }

    private fun criarLinhaProduto(p: Produto): View {
        if (qtdSelecionada[p.id] == null || qtdSelecionada[p.id]!! > p.quantidade) {
            qtdSelecionada[p.id] = if (p.quantidade > 0) 1 else 0
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(30, 25, 30, 25)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, 16)
            layoutParams = lp
        }

        val linhaTopo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val nomeTxt = TextView(this).apply {
            text = "${p.emoji}  ${p.nome}"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val precoTxt = TextView(this).apply {
            text = formatoMoeda(p.preco)
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#1D8296"))
        }
        linhaTopo.addView(nomeTxt)
        linhaTopo.addView(precoTxt)
        card.addView(linhaTopo)

        val estoqueTxt = TextView(this).apply {
            text = if (p.quantidade <= 0) "Esgotado" else "${p.quantidade} disponíveis"
            textSize = 12.5f
            setTextColor(if (p.quantidade <= 0) Color.parseColor("#8FA6B0") else Color.parseColor("#3FA66E"))
            setPadding(0, 4, 0, 16)
        }
        card.addView(estoqueTxt)

        if (p.quantidade > 0) {
            val linhaAcao = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val btnMenos = Button(this).apply { text = "−" }
            val qtdTxt = TextView(this).apply {
                text = qtdSelecionada[p.id].toString()
                textSize = 16f
                setPadding(30, 0, 30, 0)
                gravity = Gravity.CENTER
            }
            val btnMais = Button(this).apply { text = "+" }

            btnMenos.setOnClickListener {
                val atual = qtdSelecionada[p.id] ?: 1
                qtdSelecionada[p.id] = (atual - 1).coerceAtLeast(1)
                qtdTxt.text = qtdSelecionada[p.id].toString()
            }
            btnMais.setOnClickListener {
                val atual = qtdSelecionada[p.id] ?: 1
                qtdSelecionada[p.id] = (atual + 1).coerceAtMost(p.quantidade)
                qtdTxt.text = qtdSelecionada[p.id].toString()
            }

            val btnPagar = Button(this).apply {
                text = "Pagar"
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 20
                }
                setOnClickListener { iniciarCobranca(p) }
            }

            linhaAcao.addView(btnMenos)
            linhaAcao.addView(qtdTxt)
            linhaAcao.addView(btnMais)
            linhaAcao.addView(btnPagar)
            card.addView(linhaAcao)
        }

        return card
    }

    // ==================== PAGAMENTO ====================

    private fun iniciarCobranca(produto: Produto) {
        val qtd = qtdSelecionada[produto.id] ?: 1
        val total = produto.preco * qtd

        val opcoes = arrayOf("Cartão de crédito", "Cartão de débito", "Pix")
        AlertDialog.Builder(this)
            .setTitle("Forma de pagamento — ${formatoMoeda(total)}")
            .setItems(opcoes) { _, index ->
                val tipo = when (index) {
                    0 -> PlugPag.TYPE_CREDITO
                    1 -> PlugPag.TYPE_DEBITO
                    else -> PlugPag.TYPE_PIX
                }
                executarPagamento(produto, qtd, total, tipo)
            }
            .show()
    }

    private fun executarPagamento(produto: Produto, qtd: Int, total: Double, tipo: Int) {
        val modoTesteFixo = prefs.getBoolean("modo_teste", false)
        if (modoTesteFixo) {
            simularPagamento(produto, qtd, total)
            return
        }

        Toast.makeText(this, "Siga as instruções no terminal…", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val codigoVenda = "PED${System.currentTimeMillis()}"
                val valorCentavos = Math.round(total * 100).toInt()
                val paymentData = PlugPagPaymentData(
                    tipo, valorCentavos, PlugPag.INSTALLMENT_TYPE_A_VISTA, 1, codigoVenda
                )
                val resultado = plugPag.doPayment(paymentData)

                runOnUiThread {
                    if (resultado.result == PlugPag.RET_OK) {
                        finalizarVendaAprovada(produto, qtd, total)
                    } else {
                        val motivo = resultado.message ?: "Transação não aprovada"
                        Toast.makeText(this, "Pagamento não aprovado: $motivo", Toast.LENGTH_LONG).show()
                        reiniciarTimerInatividade()
                    }
                }
            } catch (e: Exception) {
                // Isso normalmente acontece quando o app roda fora de um terminal PagBank
                // de verdade (ex: testando num celular comum ou emulador) — o serviço do
                // PlugPag não existe nesses aparelhos. Em vez de só mostrar erro, oferece
                // simular a aprovação, para dar pra testar o resto do app (catálogo,
                // Supabase, telas) sem precisar do terminal físico.
                runOnUiThread {
                    oferecerModoTeste(produto, qtd, total, e.message ?: "desconhecido")
                }
            }
        }.start()
    }

    /** Chamado quando o SDK PlugPag falha — provavelmente por não haver terminal real conectado. */
    private fun oferecerModoTeste(produto: Produto, qtd: Int, total: Double, motivoErro: String) {
        AlertDialog.Builder(this)
            .setTitle("Terminal PagBank não encontrado")
            .setMessage("Não foi possível falar com um terminal real (erro: $motivoErro).\n\nIsso é esperado se você estiver testando fora da maquininha Smart. Quer simular a aprovação deste pagamento, só para testar o app?")
            .setPositiveButton("Simular esta venda") { _, _ ->
                simularPagamento(produto, qtd, total)
            }
            .setNeutralButton("Sempre simular neste aparelho") { _, _ ->
                prefs.edit().putBoolean("modo_teste", true).apply()
                simularPagamento(produto, qtd, total)
            }
            .setNegativeButton("Cancelar") { _, _ -> reiniciarTimerInatividade() }
            .show()
    }

    /** Simula uma aprovação instantânea, sem falar com o terminal — só para testes. */
    private fun simularPagamento(produto: Produto, qtd: Int, total: Double) {
        Toast.makeText(this, "⚠️ MODO TESTE — pagamento simulado, não é dinheiro de verdade", Toast.LENGTH_LONG).show()
        finalizarVendaAprovada(produto, qtd, total)
    }

    /** Pagamento aprovado: dá baixa no estoque e registra a venda no Supabase. */
    private fun finalizarVendaAprovada(produto: Produto, qtd: Int, total: Double) {
        val api = supabaseApi
        val novaQtd = (produto.quantidade - qtd).coerceAtLeast(0)

        mostrarTelaSucesso(produto, qtd, total)

        if (api == null) {
            Toast.makeText(this, "Venda aprovada, mas houve um erro interno — estoque não foi atualizado.", Toast.LENGTH_LONG).show()
            return
        }
        api.atualizarEstoque(produto.id, novaQtd) { ok, erro ->
            if (!ok) runOnUiThread { Toast.makeText(this, "Falha ao atualizar estoque: $erro", Toast.LENGTH_LONG).show() }
        }
        api.registrarVenda(produto.id, produto.nome, qtd, produto.preco, total) { ok, erro ->
            if (!ok) runOnUiThread { Toast.makeText(this, "Falha ao registrar venda: $erro", Toast.LENGTH_LONG).show() }
        }
    }

    private fun mostrarTelaSucesso(produto: Produto, qtd: Int, total: Double) {
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#3FA66E"))
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        overlay.addView(TextView(this).apply {
            text = "Pagamento aprovado"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        })
        overlay.addView(TextView(this).apply {
            text = "${qtd}x ${produto.nome} · ${formatoMoeda(total)}"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 0)
        })

        root.addView(overlay)
        handlerInatividade.postDelayed({
            root.removeView(overlay)
            mostrarTelaEspera()
        }, 2600)
    }
}
