package com.levoconveniencia.maquininha

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** Um produto do catálogo, espelhando a tabela `produtos` do Supabase. */
data class Produto(
    val id: String,
    val nome: String,
    val categoria: String,
    val preco: Double,
    val quantidade: Int,
    val emoji: String
)

/**
 * Fala diretamente com a API REST do Supabase (sem SDK JavaScript, sem WebView —
 * proibido no modelo SmartPOS). Usa a mesma URL e chave "anon" já configuradas
 * no app web (Painel Admin → Integrações), e as mesmas tabelas/colunas.
 */
class SupabaseApi(private val baseUrl: String, private val anonKey: String) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun headers(builder: Request.Builder): Request.Builder {
        // Só o header "apikey" é necessário (é assim que o teste direto no
        // navegador funcionou). O header "Authorization: Bearer" espera um
        // JWT tradicional — a chave nova "sb_publishable_..." não é um JWT,
        // e mandar ela nesse header pode fazer o Supabase tratar a requisição
        // com um papel/role diferente do esperado, retornando uma lista vazia
        // em vez de erro (por causa das regras de segurança das tabelas).
        return builder.addHeader("apikey", anonKey)
    }

    /** Busca todos os produtos, ordenados por nome. */
    fun listarProdutos(callback: (lista: List<Produto>?, erro: String?) -> Unit) {
        val url = "$baseUrl/rest/v1/produtos?select=*&order=nome.asc"
        val req = headers(Request.Builder().url(url)).get().build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, "${e.javaClass.simpleName}: ${e.message} (URL: $url)")
            }
            override fun onResponse(call: Call, response: Response) {
                response.use { r ->
                    if (!r.isSuccessful) {
                        val corpo = try { r.body?.string()?.take(200) } catch (e: Exception) { null }
                        callback(null, "Erro HTTP ${r.code} em $url — resposta: $corpo")
                        return
                    }
                    try {
                        val arr = JSONArray(r.body?.string() ?: "[]")
                        val lista = mutableListOf<Produto>()
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            lista.add(
                                Produto(
                                    id = o.getString("id"),
                                    nome = o.getString("nome"),
                                    categoria = o.optString("categoria", ""),
                                    preco = o.getDouble("preco"),
                                    quantidade = o.getInt("quantidade"),
                                    emoji = o.optString("emoji", "🛒")
                                )
                            )
                        }
                        callback(lista, null)
                    } catch (e: Exception) {
                        callback(null, "Resposta inesperada do Supabase: ${e.message}")
                    }
                }
            }
        })
    }

    /** Atualiza a quantidade em estoque de um produto (depois de uma venda). */
    fun atualizarEstoque(produtoId: String, novaQuantidade: Int, callback: (ok: Boolean, erro: String?) -> Unit) {
        val corpo = JSONObject().put("quantidade", novaQuantidade).toString().toRequestBody(jsonMediaType)
        val req = headers(
            Request.Builder().url("$baseUrl/rest/v1/produtos?id=eq.$produtoId")
        ).addHeader("Prefer", "return=minimal").patch(corpo).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false, e.message) }
            override fun onResponse(call: Call, response: Response) {
                response.use { r -> callback(r.isSuccessful, if (r.isSuccessful) null else "Erro HTTP ${r.code}") }
            }
        })
    }

    /** Registra a venda na tabela `consumos`, para aparecer no Histórico/Dashboard do admin. */
    fun registrarVenda(
        produtoId: String, produtoNome: String, quantidade: Int, preco: Double, valorTotal: Double,
        callback: (ok: Boolean, erro: String?) -> Unit
    ) {
        val consumo = JSONObject().apply {
            put("id", "mq-" + System.currentTimeMillis().toString(36) + (0..999).random())
            put("usuario_id", JSONObject.NULL)
            put("usuario_nome", "Cliente (maquininha)")
            put("produto_id", produtoId)
            put("produto_nome", produtoNome)
            put("quantidade", quantidade)
            put("preco", preco)
            put("valor_total", valorTotal)
            put("pago", true)
            put("origem", "maquininha")
        }
        val corpo = consumo.toString().toRequestBody(jsonMediaType)
        val req = headers(
            Request.Builder().url("$baseUrl/rest/v1/consumos")
        ).addHeader("Prefer", "return=minimal").post(corpo).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false, e.message) }
            override fun onResponse(call: Call, response: Response) {
                response.use { r -> callback(r.isSuccessful, if (r.isSuccessful) null else "Erro HTTP ${r.code}") }
            }
        })
    }
}
