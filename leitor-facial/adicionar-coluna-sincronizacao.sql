-- Rode isto no SQL Editor do Supabase (mesmo lugar onde você rodou o script
-- original de criação das tabelas). Isso adiciona uma coluna nova na tabela
-- de usuários, usada pela ponte (bridge_leitor_facial.py) para saber quais
-- clientes ainda precisam ter o rosto enviado ao leitor facial.

alter table usuarios add column if not exists sincronizado_leitor boolean default false;

-- Novos cadastros feitos a partir de agora já entram com sincronizado_leitor = false
-- automaticamente (valor padrão da coluna), então a ponte vai detectá-los sozinha.
