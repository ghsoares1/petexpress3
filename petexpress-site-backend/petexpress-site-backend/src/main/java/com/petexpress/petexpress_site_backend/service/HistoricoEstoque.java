package com.petexpress.petexpress_site_backend.service;

import com.petexpress.petexpress_site_backend.model.Produto;
import com.petexpress.petexpress_site_backend.model.VariacaoProduto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class HistoricoEstoque {
    private final JdbcTemplate jdbc;

    public HistoricoEstoque(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void registrar(Produto produto, VariacaoProduto variacao, Long vendedorId,
                          String tipo, String motivo, int anterior, int atual) {
        int inseridos = jdbc.update("""
                INSERT INTO estoque_movimentacao
                    (produto_id, variacao_codigo, produto_nome, embalagem, vendedor_id, vendedor_nome,
                     tipo, motivo, saldo_anterior, saldo_atual, registrado_em)
                VALUES (?, ?, ?, ?, ?, coalesce((SELECT trim(coalesce(nome, '') || ' ' || coalesce(sobrenome, ''))
                       FROM usuario WHERE id = ?), 'Sistema'), ?, ?, ?, ?, ?)
                """, produto.getId(), variacao.getCodigo(), produto.getNome(), variacao.getEmbalagem(),
                vendedorId, vendedorId, tipo, motivo, anterior, atual, Instant.now().toString());
        if (inseridos != 1) {
            throw new IllegalStateException("Não foi possível identificar o responsável pela movimentação.");
        }
    }

    public List<Movimentacao> listar(String codigo, long antesDe, int limite) {
        return jdbc.query("""
                SELECT id, produto_nome, embalagem, vendedor_nome, tipo, motivo,
                       saldo_anterior, saldo_atual, registrado_em
                FROM estoque_movimentacao
                WHERE id < ? AND (? IS NULL OR variacao_codigo = ?)
                ORDER BY id DESC LIMIT ?
                """, (rs, row) -> new Movimentacao(
                rs.getLong("id"), rs.getString("produto_nome"), rs.getString("embalagem"),
                rs.getString("vendedor_nome"), rs.getString("tipo"), rs.getString("motivo"),
                rs.getInt("saldo_anterior"), rs.getInt("saldo_atual"), rs.getString("registrado_em")),
                antesDe, codigo, codigo, limite);
    }

    public record Movimentacao(long id, String produto, String embalagem, String responsavel, String tipo,
                               String motivo, int saldoAnterior, int saldoAtual, String registradoEm) {}
}
