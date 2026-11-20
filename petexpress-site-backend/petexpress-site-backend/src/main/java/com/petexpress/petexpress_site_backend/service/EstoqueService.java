package com.petexpress.petexpress_site_backend.service;

import com.petexpress.petexpress_site_backend.model.Produto;
import com.petexpress.petexpress_site_backend.model.VariacaoProduto;
import com.petexpress.petexpress_site_backend.repository.ProdutoRepository;
import com.petexpress.petexpress_site_backend.service.HistoricoEstoque.Movimentacao;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EstoqueService {
    private static final int TAMANHO_PAGINA = 25;
    private final ProdutoRepository produtos;
    private final HistoricoEstoque historico;

    public EstoqueService(ProdutoRepository produtos, HistoricoEstoque historico) {
        this.produtos = produtos;
        this.historico = historico;
    }

    public List<Saldo> listarSaldos() {
        return produtos.findAllByOrderByIdDesc().stream()
                .flatMap(produto -> produto.getVariacoes().stream().map(variacao -> saldo(produto, variacao)))
                .toList();
    }

    public PaginaHistorico listarHistorico(String codigo, Long antesDe) {
        List<Movimentacao> linhas = historico.listar(codigo,
                antesDe == null ? Long.MAX_VALUE : antesDe, TAMANHO_PAGINA + 1);
        if (linhas.size() <= TAMANHO_PAGINA) {
            return new PaginaHistorico(linhas, null);
        }
        return new PaginaHistorico(linhas.subList(0, TAMANHO_PAGINA), linhas.get(TAMANHO_PAGINA - 1).id());
    }

    @Transactional
    public Saldo movimentar(long produtoId, String codigo, Lancamento lancamento, Long vendedorId) {
        Produto produto = buscarProduto(produtoId);
        VariacaoProduto atual = buscarVariacao(produto, codigo);
        int anterior = atual.getEstoque();
        if (lancamento.saldoEsperado() == null || lancamento.saldoEsperado() != anterior) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O saldo mudou. Confira o estoque antes de tentar novamente.");
        }

        String motivo = lancamento.motivo() == null ? "" : lancamento.motivo().strip();
        Integer quantidade = lancamento.quantidade();
        Tipo tipo = lancamento.tipo();
        if (tipo == null || quantidade == null || quantidade < 0
                || (tipo != Tipo.AJUSTE && quantidade == 0) || motivo.isEmpty() || motivo.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um tipo, uma quantidade válida e um motivo de até 255 caracteres.");
        }

        long novoSaldo = switch (tipo) {
            case ENTRADA -> anterior + (long) quantidade;
            case SAIDA -> anterior - (long) quantidade;
            case AJUSTE -> quantidade;
        };
        if (novoSaldo < 0 || novoSaldo + atual.getReservado() > Integer.MAX_VALUE || novoSaldo == anterior) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma alteração que resulte em um novo saldo válido, sem deixar o estoque negativo.");
        }

        VariacaoProduto alterada = atualizar(produto, atual, (int) novoSaldo, atual.getEstoqueMinimo());
        produto.setPreco(ProdutoService.precoInicial(produto.getVariacoes()).orElseThrow());
        produtos.flush();
        historico.registrar(produto, alterada, vendedorId, tipo.name(), motivo, anterior, (int) novoSaldo);
        return saldo(produto, alterada);
    }

    @Transactional
    public Saldo definirMinimo(long produtoId, String codigo, Minimo minimo) {
        if (minimo.quantidade() == null || minimo.quantidade() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um estoque mínimo válido.");
        }
        Produto produto = buscarProduto(produtoId);
        VariacaoProduto atual = buscarVariacao(produto, codigo);
        return saldo(produto, atualizar(produto, atual, atual.getEstoque(), minimo.quantidade()));
    }

    private Produto buscarProduto(long id) {
        return produtos.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
    }

    private VariacaoProduto buscarVariacao(Produto produto, String codigo) {
        return produto.getVariacoes().stream().filter(variacao -> variacao.getCodigo().equals(codigo))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Embalagem não encontrada."));
    }

    private VariacaoProduto atualizar(Produto produto, VariacaoProduto atual, int estoque, int minimo) {
        VariacaoProduto alterada = new VariacaoProduto(
                atual.getCodigo(), atual.getEmbalagem(), atual.getPreco(), estoque, minimo, atual.getReservado());
        produto.getVariacoes().set(produto.getVariacoes().indexOf(atual), alterada);
        return alterada;
    }

    private Saldo saldo(Produto produto, VariacaoProduto variacao) {
        return new Saldo(produto.getId(), produto.getNome(), produto.getSituacao(), variacao.getCodigo(),
                variacao.getEmbalagem(), variacao.getEstoque(), variacao.getEstoqueMinimo(), variacao.getReservado());
    }

    public enum Tipo { ENTRADA, SAIDA, AJUSTE }

    public record Lancamento(Tipo tipo, Integer quantidade, String motivo, Integer saldoEsperado) {}
    public record Minimo(Integer quantidade) {}
    public record Saldo(long produtoId, String produto, Produto.Situacao situacao, String codigo,
                        String embalagem, int estoque, int estoqueMinimo, int reservado) {}
    public record PaginaHistorico(List<Movimentacao> movimentacoes, Long proximaPagina) {}
}
