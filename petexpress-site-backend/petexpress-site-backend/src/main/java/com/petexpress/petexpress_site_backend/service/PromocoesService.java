package com.petexpress.petexpress_site_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petexpress.petexpress_site_backend.dto.CheckoutRequest;
import com.petexpress.petexpress_site_backend.model.Produto;
import com.petexpress.petexpress_site_backend.repository.ProdutoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class PromocoesService {
    private final JdbcTemplate jdbc;
    private final ProdutoRepository produtos;
    private final ObjectMapper mapper;

    public PromocoesService(JdbcTemplate jdbc, ProdutoRepository produtos, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.produtos = produtos;
        this.mapper = mapper;
    }

    public List<Oferta> ofertas() {
        return jdbc.query("SELECT * FROM oferta ORDER BY id DESC", (rs, n) -> new Oferta(
                rs.getLong("id"), rs.getString("nome"), rs.getLong("produto_id"), rs.getString("variacao_codigo"),
                rs.getBigDecimal("percentual"), rs.getString("inicio"), rs.getString("fim"), rs.getBoolean("ativo")));
    }

    public List<Cupom> cupons() {
        return jdbc.query("""
                SELECT c.*, (SELECT count(*) FROM pedido p WHERE p.cupom_id = c.id
                    AND p.status IN ('PENDENTE','PAGAMENTO_APROVADO','PAGAMENTO_EM_REVISAO')) AS usos
                FROM cupom c ORDER BY c.id DESC
                """, (rs, n) -> new Cupom(rs.getLong("id"), rs.getString("codigo"), rs.getString("tipo"),
                rs.getBigDecimal("valor"), rs.getBigDecimal("minimo"), rs.getInt("limite"),
                rs.getString("inicio"), rs.getString("fim"), rs.getBoolean("ativo"), rs.getInt("usos")));
    }

    @Transactional
    public void salvarOferta(Long id, Oferta dados) {
        validarDatas(dados.inicio(), dados.fim());
        String nome = texto(dados.nome(), 100);
        if (nome.isEmpty() || dados.produtoId() == null || !positivo(dados.percentual())
                || dados.percentual().compareTo(new BigDecimal("100")) >= 0) {
            throw invalido("Informe o produto e um desconto entre 0 e 100%.");
        }
        Produto produto = produtos.findById(dados.produtoId()).orElseThrow(() -> invalido("Produto não encontrado."));
        String variacao = dados.variacaoCodigo() == null || dados.variacaoCodigo().isBlank() ? null : dados.variacaoCodigo();
        if (variacao != null && produto.getVariacoes().stream().noneMatch(v -> v.getCodigo().equals(variacao))) {
            throw invalido("Selecione um tamanho do produto.");
        }
        if (id == null) {
            jdbc.update("INSERT INTO oferta(nome,produto_id,variacao_codigo,percentual,inicio,fim,ativo) VALUES(?,?,?,?,?,?,?)",
                    nome, produto.getId(), variacao, dados.percentual(), data(dados.inicio()), data(dados.fim()), dados.ativo());
        } else {
            exigirAtualizado(jdbc.update("UPDATE oferta SET nome=?,produto_id=?,variacao_codigo=?,percentual=?,inicio=?,fim=?,ativo=? WHERE id=?",
                    nome, produto.getId(), variacao, dados.percentual(), data(dados.inicio()), data(dados.fim()), dados.ativo(), id));
        }
    }

    @Transactional
    public void salvarCupom(Long id, Cupom dados) {
        validarDatas(dados.inicio(), dados.fim());
        String codigo = texto(dados.codigo(), 30).toUpperCase(Locale.ROOT);
        if (!codigo.matches("[A-Z0-9_-]{3,30}") || dados.tipo() == null
                || !Set.of("PERCENTUAL", "VALOR").contains(dados.tipo()) || !positivo(dados.valor())
                || ("PERCENTUAL".equals(dados.tipo()) && dados.valor().compareTo(new BigDecimal("100")) >= 0)
                || dados.minimo() == null || dados.minimo().signum() < 0 || dados.minimo().scale() > 2
                || dados.minimo().compareTo(new BigDecimal("999999.99")) > 0 || dados.limite() < 1) {
            throw invalido("Confira o código, desconto, compra mínima e limite do cupom.");
        }
        Integer duplicados = jdbc.queryForObject("SELECT count(*) FROM cupom WHERE codigo=? AND id<>?", Integer.class, codigo, id == null ? 0 : id);
        if (duplicados != null && duplicados > 0) throw invalido("Já existe um cupom com esse código.");
        if (id == null) {
            jdbc.update("INSERT INTO cupom(codigo,tipo,valor,minimo,limite,inicio,fim,ativo) VALUES(?,?,?,?,?,?,?,?)",
                    codigo, dados.tipo(), dados.valor(), dados.minimo(), dados.limite(), data(dados.inicio()), data(dados.fim()), dados.ativo());
        } else {
            exigirAtualizado(jdbc.update("UPDATE cupom SET codigo=?,tipo=?,valor=?,minimo=?,limite=?,inicio=?,fim=?,ativo=?,versao=versao+1 WHERE id=?",
                    codigo, dados.tipo(), dados.valor(), dados.minimo(), dados.limite(), data(dados.inicio()), data(dados.fim()), dados.ativo(), id));
        }
    }

    public BigDecimal preco(Long produto, String variacao, BigDecimal original) {
        if (original == null) return null;
        Instant agora = Instant.now();
        BigDecimal percentual = ofertas().stream().filter(oferta -> oferta.ativo() && oferta.produtoId().equals(produto)
                        && (oferta.variacaoCodigo() == null || oferta.variacaoCodigo().equals(variacao))
                        && vigente(oferta.inicio(), oferta.fim(), agora))
                .map(Oferta::percentual).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        return original.multiply(BigDecimal.ONE.subtract(percentual.movePointLeft(2)))
                .setScale(2, RoundingMode.HALF_UP).max(new BigDecimal("0.01"));
    }

    public Map<String, Object> catalogo(Produto produto) {
        Map<String, Object> resultado = mapper.convertValue(produto, new TypeReference<>() {});
        List<Map<String, Object>> variacoes = new ArrayList<>();
        for (var variacao : produto.getVariacoes()) {
            Map<String, Object> opcao = mapper.convertValue(variacao, new TypeReference<>() {});
            opcao.put("precoOriginal", variacao.getPreco());
            opcao.put("preco", preco(produto.getId(), variacao.getCodigo(), variacao.getPreco()));
            variacoes.add(opcao);
        }
        resultado.put("variacoes", variacoes);
        if (variacoes.isEmpty()) {
            resultado.put("precoOriginal", produto.getPreco());
            resultado.put("preco", preco(produto.getId(), null, produto.getPreco() == null ? null : BigDecimal.valueOf(produto.getPreco())));
        } else {
            Map<String, Object> menor = variacoes.stream().filter(v -> ((Number) v.get("estoque")).intValue() > 0)
                    .min(Comparator.comparing(v -> (BigDecimal) v.get("preco")))
                    .orElseGet(() -> variacoes.stream().min(Comparator.comparing(v -> (BigDecimal) v.get("preco"))).orElseThrow());
            resultado.put("preco", menor.get("preco"));
            resultado.put("precoOriginal", menor.get("precoOriginal"));
        }
        return resultado;
    }

    public Desconto desconto(String codigo, BigDecimal subtotal, boolean reservarUso) {
        if (codigo == null || codigo.isBlank()) return new Desconto(null, null, BigDecimal.ZERO);
        String normalizado = texto(codigo, 30).toUpperCase(Locale.ROOT);
        Cupom cupom = cupons().stream().filter(c -> c.codigo().equals(normalizado)).findFirst()
                .orElseThrow(() -> invalido("Cupom não encontrado."));
        if (!cupom.ativo() || !vigente(cupom.inicio(), cupom.fim(), Instant.now())) throw invalido("O cupom está fora da validade.");
        if (subtotal.compareTo(cupom.minimo()) < 0) throw invalido("O carrinho não atingiu a compra mínima do cupom.");
        if (cupom.usos() >= cupom.limite()) throw invalido("O limite de uso deste cupom foi atingido.");
        if (reservarUso) {
            // Serializa a reserva do uso com a criação do pedido na mesma transação.
            jdbc.update("UPDATE cupom SET versao=versao+1 WHERE id=?", cupom.id());
        }
        BigDecimal valor = "PERCENTUAL".equals(cupom.tipo())
                ? subtotal.multiply(cupom.valor().movePointLeft(2)).setScale(2, RoundingMode.HALF_UP) : cupom.valor();
        valor = valor.min(subtotal.subtract(new BigDecimal("0.01"))).max(BigDecimal.ZERO);
        return new Desconto(cupom.id(), cupom.codigo(), valor);
    }

    @Transactional(readOnly = true)
    public Orcamento orcar(OrcamentoRequest request) {
        if (request.itens() == null || request.itens().isEmpty() || request.itens().size() > 100) throw invalido("Carrinho vazio.");
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : request.itens()) {
            if (item == null || item.quantidade() == null || item.quantidade() < 1 || item.quantidade() > 999) throw invalido("Quantidade inválida.");
            Produto produto = localizar(item);
            BigDecimal original = produto.getPreco() == null ? null : BigDecimal.valueOf(produto.getPreco());
            if (!produto.getVariacoes().isEmpty()) {
                var variacao = produto.getVariacoes().stream().filter(v -> v.getCodigo().equals(item.variacaoCodigo()))
                        .findFirst().orElseThrow(() -> invalido("Selecione novamente o tamanho do pacote."));
                original = variacao.getPreco();
            }
            BigDecimal preco = preco(produto.getId(), item.variacaoCodigo(), original);
            if (preco == null || item.preco() == null || preco.compareTo(item.preco()) != 0) throw invalido("O preço mudou. Atualize o item pelo catálogo.");
            subtotal = subtotal.add(preco.multiply(BigDecimal.valueOf(item.quantidade())));
        }
        Desconto desconto = desconto(request.cupom(), subtotal, false);
        return new Orcamento(subtotal, desconto.valor(), subtotal.subtract(desconto.valor()), desconto.codigo());
    }

    private Produto localizar(CheckoutRequest.Item item) {
        if (item.produtoId() != null) return produtos.findById(item.produtoId())
                .filter(p -> p.getSituacao() == Produto.Situacao.PUBLICADO).orElseThrow(() -> invalido("Produto indisponível."));
        return produtos.findBySituacaoAndNomeIgnoreCaseOrderByIdAsc(Produto.Situacao.PUBLICADO, texto(item.nome(), 255))
                .stream().findFirst().orElseThrow(() -> invalido("Atualize o item pelo catálogo."));
    }

    private boolean vigente(String inicio, String fim, Instant agora) {
        return !agora.isBefore(Instant.parse(inicio)) && agora.isBefore(Instant.parse(fim));
    }
    private void validarDatas(String inicio, String fim) {
        try {
            if (!Instant.parse(fim).isAfter(Instant.parse(inicio))) throw invalido("O término deve ser posterior ao início.");
        } catch (java.time.format.DateTimeParseException | NullPointerException ex) {
            throw invalido("Informe início e término válidos.");
        }
    }
    private String data(String valor) { return Instant.parse(valor).toString(); }
    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0 && valor.scale() <= 2 && valor.compareTo(new BigDecimal("999999.99")) <= 0;
    }
    private String texto(String valor, int tamanho) {
        if (valor == null || valor.length() > tamanho) throw invalido("Confira o tamanho dos campos.");
        return valor.strip();
    }
    private void exigirAtualizado(int quantidade) {
        if (quantidade == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado.");
    }
    private ResponseStatusException invalido(String mensagem) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem); }

    public record Oferta(Long id, String nome, Long produtoId, String variacaoCodigo, BigDecimal percentual, String inicio, String fim, boolean ativo) {}
    public record Cupom(Long id, String codigo, String tipo, BigDecimal valor, BigDecimal minimo, int limite, String inicio, String fim, boolean ativo, int usos) {}
    public record Desconto(Long id, String codigo, BigDecimal valor) {}
    public record OrcamentoRequest(List<CheckoutRequest.Item> itens, String cupom) {}
    public record Orcamento(BigDecimal subtotal, BigDecimal desconto, BigDecimal totalProdutos, String cupom) {}
}
