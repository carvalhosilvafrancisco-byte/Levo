package com.levoconveniencia.maquininha

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagAppIdentification
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagActivationData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData

/**
 * App "casca" da maquininha da Levo Conveniência.
 *
 * O que ele faz:
 * 1. Abre, em tela cheia, o mesmo app web (PWA) que já construímos, na tela da
 *    maquininha (?maquininha=1).
 * 2. Quando o cliente toca em "Pagar" dentro da página, o JavaScript chama
 *    window.AndroidPag.cobrar(...) — esse arquivo escuta essa chamada aqui.
 * 3. Aciona o terminal PagBank de verdade via SDK PlugPag.
 * 4. Quando o terminal responde (aprovado ou recusado), chama de volta uma
 *    função JavaScript na página para dar baixa no estoque (ou avisar erro).
 *
 * IMPORTANTE: os nomes exatos de classes/métodos do PlugPag podem mudar entre
 * versões do SDK. Este código foi escrito com base na documentação pública
 * mais recente disponível — confira com o suporte de integração do PagBank
 * (developer.pagbank.com.br) antes de compilar para produção, e ajuste se a
 * versão do SDK que você recebeu tiver assinaturas diferentes.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var prefs: SharedPreferences
    private lateinit var plugPag: PlugPag

    // TROQUE AQUI pela URL publicada do seu app (a mesma que você usa no
    // celular/tablet), sempre terminando em ?maquininha=1
    private val URL_PADRAO_APP = "https://SEU-APP.vercel.app/?maquininha=1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("levo_maquininha_prefs", Context.MODE_PRIVATE)

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(PagBridge(this), "AndroidPag")

        // Toque e segure por ~3s em qualquer lugar da tela para abrir as
        // configurações (URL do app + código de ativação do terminal).
        webView.setOnLongClickListener {
            abrirConfiguracoes()
            true
        }

        plugPag = PlugPag(this, PlugPagAppIdentification("LevoConveniencia", "1.0"))

        val urlSalva = prefs.getString("url_app", null) ?: URL_PADRAO_APP
        webView.loadUrl(urlSalva)

        ativarTerminalSeNecessario()
    }

    /** Ativa o terminal com o código fornecido pelo PagBank (feito uma vez por terminal). */
    private fun ativarTerminalSeNecessario() {
        val codigoAtivacao = prefs.getString("codigo_ativacao", null)
        if (codigoAtivacao.isNullOrBlank()) {
            Toast.makeText(this, "Terminal ainda não configurado. Toque e segure a tela para configurar.", Toast.LENGTH_LONG).show()
            return
        }
        Thread {
            try {
                val resultado = plugPag.initializeAndActivatePinpad(PlugPagActivationData(codigoAtivacao))
                if (resultado != PlugPag.RET_OK) {
                    runOnUiThread {
                        Toast.makeText(this, "Não foi possível ativar o terminal (código $resultado).", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Erro ao ativar o terminal: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    /** Executa a cobrança de verdade no terminal PagBank e avisa o app web do resultado. */
    fun iniciarCobranca(produtoId: String, quantidade: Int, valorCentavos: Int) {
        // Deixa o cliente escolher a forma de pagamento antes de acionar o terminal.
        runOnUiThread {
            val opcoes = arrayOf("Cartão de crédito", "Cartão de débito", "Pix (QR no terminal)")
            AlertDialog.Builder(this)
                .setTitle("Forma de pagamento")
                .setItems(opcoes) { _, index ->
                    val tipo = when (index) {
                        0 -> PlugPag.TYPE_CREDITO
                        1 -> PlugPag.TYPE_DEBITO
                        else -> PlugPag.TYPE_PIX
                    }
                    executarPagamento(produtoId, quantidade, valorCentavos, tipo)
                }
                .setOnCancelListener {
                    webView.post {
                        webView.evaluateJavascript(
                            "window.confirmarPagamentoNativoFalha('${jsEscape(produtoId)}', 'Cancelado pelo cliente')", null
                        )
                    }
                }
                .show()
        }
    }

    private fun executarPagamento(produtoId: String, quantidade: Int, valorCentavos: Int, tipo: Int) {
        Thread {
            try {
                val codigoVenda = "PED${System.currentTimeMillis()}"
                val paymentData = PlugPagPaymentData(
                    tipo,
                    valorCentavos,
                    PlugPag.INSTALLMENT_TYPE_A_VISTA,
                    1,
                    codigoVenda
                )
                val resultado = plugPag.doPayment(paymentData)

                runOnUiThread {
                    if (resultado.result == PlugPag.RET_OK) {
                        webView.evaluateJavascript(
                            "window.confirmarPagamentoNativoSucesso('${jsEscape(produtoId)}', $quantidade)", null
                        )
                    } else {
                        val motivo = resultado.errorMessage ?: resultado.message ?: "Transação não aprovada"
                        webView.evaluateJavascript(
                            "window.confirmarPagamentoNativoFalha('${jsEscape(produtoId)}', '${jsEscape(motivo)}')", null
                        )
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    webView.evaluateJavascript(
                        "window.confirmarPagamentoNativoFalha('${jsEscape(produtoId)}', '${jsEscape(e.message ?: "Erro no terminal")}')", null
                    )
                }
            }
        }.start()
    }

    private fun jsEscape(s: String): String = s.replace("\\", "\\\\").replace("'", "\\'")

    /** Tela simples (toque e segure) para configurar a URL do app e o código de ativação do terminal. */
    private fun abrirConfiguracoes() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 30, 40, 10)

        val campoUrl = EditText(this)
        campoUrl.hint = "URL do app (com ?maquininha=1)"
        campoUrl.setText(prefs.getString("url_app", URL_PADRAO_APP))
        layout.addView(campoUrl)

        val campoAtivacao = EditText(this)
        campoAtivacao.hint = "Código de ativação do terminal (PagBank)"
        campoAtivacao.setText(prefs.getString("codigo_ativacao", ""))
        layout.addView(campoAtivacao)

        AlertDialog.Builder(this)
            .setTitle("Configurações da maquininha")
            .setView(layout)
            .setPositiveButton("Salvar e reiniciar") { _, _ ->
                prefs.edit()
                    .putString("url_app", campoUrl.text.toString().trim())
                    .putString("codigo_ativacao", campoAtivacao.text.toString().trim())
                    .apply()
                recreate()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Objeto exposto ao JavaScript da página como `window.AndroidPag`. */
    inner class PagBridge(private val activity: MainActivity) {
        @JavascriptInterface
        fun cobrar(produtoId: String, quantidade: Int, valorCentavos: Int) {
            activity.iniciarCobranca(produtoId, quantidade, valorCentavos)
        }
    }
}
