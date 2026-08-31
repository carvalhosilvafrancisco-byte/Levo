"""
Ponte Levo Conveniência <-> Leitor Facial (Control iD / iDFace)
================================================================

O QUE ESTE SCRIPT FAZ
----------------------
Roda em um computador (ou Raspberry Pi) ligado na MESMA REDE LOCAL do
leitor facial, o tempo todo, em segundo plano. A cada alguns segundos:

1. Pergunta ao Supabase (nosso banco de dados na nuvem) se existe algum
   cliente cadastrado no app que ainda não foi enviado ao leitor facial.
2. Se existir, conecta no leitor facial pelo IP local dele e envia o
   rosto (foto) daquele cliente, cadastrando-o como usuário autorizado.
3. Marca esse cliente como "sincronizado" no Supabase, para não reenviar.
4. Também verifica clientes bloqueados/lista negra e os remove do leitor.

POR QUE ISSO PRECISA RODAR LOCALMENTE (E NÃO NA NUVEM)
--------------------------------------------------------
O leitor facial só aceita conexão pelo endereço IP dele DENTRO da rede
da loja (ex: 192.168.1.50) — a internet não alcança esse endereço de
fora. Por isso, este script não pode rodar no Supabase ou em qualquer
servidor na nuvem; ele precisa estar ligado, fisicamente, na mesma rede
Wi-Fi/cabo do leitor.

O QUE VOCÊ PRECISA CONFIGURAR ABAIXO
--------------------------------------
Preencha as variáveis na seção "CONFIGURAÇÃO" logo abaixo antes de rodar.

COMO RODAR
-----------
1. Instale o Python 3 (python.org) no computador/Raspberry Pi que vai
   ficar ligado na loja.
2. Instale a única biblioteca externa necessária:
       pip install requests
3. Preencha a seção CONFIGURAÇÃO abaixo.
4. Rode:
       python bridge_leitor_facial.py
   Deixe essa janela aberta (ou configure para rodar como serviço/na
   inicialização do sistema — veja o guia INTEGRACAO-LEITOR-FACIAL.md).
"""

import time
import base64
import requests

# ============================================================
# CONFIGURAÇÃO — preencha com os dados reais antes de rodar
# ============================================================

# Mesmos dados usados no painel admin do app (Integrações → Supabase)
SUPABASE_URL = "https://SEU-PROJETO.supabase.co"
SUPABASE_ANON_KEY = "SUA_CHAVE_ANON_AQUI"

# Endereço IP do leitor facial dentro da rede local da loja
# (veja esse IP no próprio display do leitor, em Configurações de Rede)
LEITOR_IP = "192.168.1.50"
LEITOR_USUARIO = "admin"      # usuário padrão de fábrica do Control iD
LEITOR_SENHA = "admin"        # troque para a senha real configurada no leitor

# A cada quantos segundos verificar se há novos cadastros para sincronizar
INTERVALO_SEGUNDOS = 15

# ============================================================
# Não é necessário editar nada abaixo desta linha
# ============================================================

SUPABASE_HEADERS = {
    "apikey": SUPABASE_ANON_KEY,
    "Authorization": f"Bearer {SUPABASE_ANON_KEY}",
    "Content-Type": "application/json",
}


def log(msg):
    print(f"[{time.strftime('%d/%m %H:%M:%S')}] {msg}")


def buscar_usuarios_pendentes():
    """Busca no Supabase clientes que ainda não foram enviados ao leitor facial."""
    url = (
        f"{SUPABASE_URL}/rest/v1/usuarios"
        "?select=id,nome,foto,bloqueado,lista_negra,sincronizado_leitor"
        "&sincronizado_leitor=eq.false"
    )
    try:
        r = requests.get(url, headers=SUPABASE_HEADERS, timeout=10)
        r.raise_for_status()
        return r.json()
    except Exception as e:
        log(f"Erro ao buscar usuários pendentes no Supabase: {e}")
        return []


def marcar_como_sincronizado(usuario_id):
    """Marca no Supabase que este usuário já foi enviado ao leitor."""
    url = f"{SUPABASE_URL}/rest/v1/usuarios?id=eq.{usuario_id}"
    try:
        requests.patch(
            url,
            headers={**SUPABASE_HEADERS, "Prefer": "return=minimal"},
            json={"sincronizado_leitor": True},
            timeout=10,
        )
    except Exception as e:
        log(f"Erro ao marcar usuário {usuario_id} como sincronizado: {e}")


def login_leitor():
    """Faz login no leitor facial e retorna o token de sessão."""
    url = f"http://{LEITOR_IP}/login.fcgi"
    try:
        r = requests.post(
            url,
            json={"login": LEITOR_USUARIO, "password": LEITOR_SENHA},
            timeout=10,
        )
        r.raise_for_status()
        return r.json().get("session")
    except Exception as e:
        log(f"Erro ao conectar no leitor facial ({LEITOR_IP}): {e}")
        return None


def cadastrar_ou_atualizar_usuario(session, usuario):
    """
    Cria (ou garante que existe) o usuário no leitor, e associa o rosto dele.
    Usuários bloqueados/lista negra não têm o rosto liberado.
    """
    if usuario.get("bloqueado") or usuario.get("lista_negra"):
        log(f"Usuário {usuario['nome']} está bloqueado — não será liberado no leitor.")
        marcar_como_sincronizado(usuario["id"])
        return

    foto_b64 = usuario.get("foto")
    if not foto_b64:
        log(f"Usuário {usuario['nome']} não tem foto cadastrada — pulando.")
        return

    # A foto no nosso banco vem como data URL (ex: "data:image/jpeg;base64,...").
    # O leitor espera só a parte em base64, sem esse prefixo.
    if "," in foto_b64:
        foto_b64 = foto_b64.split(",", 1)[1]

    # 1) Garante que o usuário existe no leitor (cria se necessário).
    #    Usamos o próprio ID do Supabase como "registration" para conseguir
    #    identificar esse usuário depois, em eventos de acesso.
    url_user = f"http://{LEITOR_IP}/user_set.fcgi?session={session}"
    payload_user = {
        "user": {
            "name": usuario["nome"],
            "registration": usuario["id"],
        }
    }
    try:
        r = requests.post(url_user, json=payload_user, timeout=10)
        r.raise_for_status()
        user_id_leitor = r.json().get("id")
    except Exception as e:
        log(f"Erro ao criar/atualizar usuário '{usuario['nome']}' no leitor: {e}")
        return

    # 2) Envia o rosto (foto) associado a esse usuário.
    url_face = f"http://{LEITOR_IP}/face_create.fcgi?session={session}"
    payload_face = {
        "user_id": user_id_leitor,
        "image": foto_b64,
        "save": True,
    }
    try:
        r = requests.post(url_face, json=payload_face, timeout=15)
        r.raise_for_status()
        log(f"✅ Rosto de '{usuario['nome']}' cadastrado no leitor com sucesso.")
        marcar_como_sincronizado(usuario["id"])
    except Exception as e:
        log(f"Erro ao enviar o rosto de '{usuario['nome']}' ao leitor: {e}")


def ciclo():
    pendentes = buscar_usuarios_pendentes()
    if not pendentes:
        return
    log(f"{len(pendentes)} usuário(s) pendente(s) de sincronizar com o leitor facial.")

    session = login_leitor()
    if not session:
        return

    for usuario in pendentes:
        cadastrar_ou_atualizar_usuario(session, usuario)


def main():
    log("Ponte Levo Conveniência <-> Leitor Facial iniciada.")
    log(f"Leitor facial: {LEITOR_IP}  |  Verificando a cada {INTERVALO_SEGUNDOS}s.")
    while True:
        ciclo()
        time.sleep(INTERVALO_SEGUNDOS)


if __name__ == "__main__":
    main()
