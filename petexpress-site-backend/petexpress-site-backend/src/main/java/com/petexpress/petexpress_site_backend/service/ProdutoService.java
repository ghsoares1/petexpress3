package com.petexpress.petexpress_site_backend.service;

import com.petexpress.petexpress_site_backend.model.PagamentoCartItem;
import com.petexpress.petexpress_site_backend.model.Produto;
import com.petexpress.petexpress_site_backend.model.Produto.Situacao;
import com.petexpress.petexpress_site_backend.model.VariacaoProduto;
import com.petexpress.petexpress_site_backend.repository.ProdutoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ProdutoService {
    private static final Set<String> ANIMAIS = Set.of("CACHORRO", "GATO", "PASSARO", "PEIXE", "OUTRO");
    private final ProdutoRepository produtos;

    private final HistoricoEstoque historico;

    public ProdutoService(ProdutoRepository produtos, HistoricoEstoque historico) {
        this.historico = historico;
        this.produtos = produtos;
    }

    public List<Produto> listarTodos() {
        return produtos.findBySituacao(Situacao.PUBLICADO);
    }

    public List<Produto> buscarPorTipoAnimal(String tipoAnimal) {
        return produtos.findBySituacaoAndTipoAnimalIgnoreCase(Situacao.PUBLICADO, tipoAnimal);
    }

    public Optional<Produto> buscarPorId(Long id) {
        return produtos.findById(id).filter(produto -> produto.getSituacao() == Situacao.PUBLICADO);
    }

    public List<Produto> listarParaVendedor() {
        return produtos.findAllByOrderByIdDesc();
    }

    @Transactional
    public Produto salvar(Long id, Anuncio anuncio, Long vendedorId) {
        Produto produto = id == null ? new Produto() : produtos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
        String nome = campo(anuncio.nome(), 255);
        String marca = campo(anuncio.tipoProduto(), 255);
        String animal = campo(anuncio.tipoAnimal(), 20);
        String categoria = campo(anuncio.categoria(), 100);
        Situacao situacao = anuncio.situacao();
        exigir(!nome.isEmpty() && situacao != null, "Informe o nome e a situação do produto.");
        exigir(animal.isEmpty() || ANIMAIS.contains(animal), "Selecione um tipo de pet válido.");

        List<String> fotos = anuncio.fotos() == null ? List.of() : anuncio.fotos().stream()
                .map(foto -> campo(foto, 255)).filter(foto -> !foto.isEmpty()).distinct().toList();
        exigir(fotos.size() <= 6 && fotos.stream().allMatch(this::fotoValida),
                "Informe até seis URLs HTTP(S) ou caminhos de imagem em img/.");

        List<Embalagem> embalagens = anuncio.variacoes() == null ? List.of() : anuncio.variacoes();
        exigir(embalagens.size() <= 30, "Cadastre até 30 embalagens por produto.");
        Map<String, VariacaoProduto> existentes = new HashMap<>();
        produto.getVariacoes().forEach(variacao -> existentes.put(variacao.getCodigo(), variacao));
        Set<String> nomes = new HashSet<>();
        Set<String> novas = new HashSet<>();
        List<VariacaoProduto> variacoes = embalagens.stream().map(embalagem -> {
            exigir(embalagem != null, "Embalagem inválida.");
            String rotulo = campo(embalagem.embalagem(), 100);
            exigir(!rotulo.isEmpty() && nomes.add(rotulo.toLowerCase(Locale.ROOT)),
                    "Informe um nome diferente para cada embalagem.");
            BigDecimal preco = embalagem.preco();
            exigir(preco != null && preco.signum() > 0 && preco.stripTrailingZeros().scale() <= 2
                    && preco.compareTo(new BigDecimal("999999.99")) <= 0, "Informe um preço válido.");
            String codigo = campo(embalagem.codigo(), 36);
            if (codigo.isEmpty()) {
                exigir(embalagem.estoque() != null && embalagem.estoque() >= 0, "Informe um estoque inicial válido.");
                String novoCodigo = UUID.randomUUID().toString();
                novas.add(novoCodigo);
                return new VariacaoProduto(novoCodigo, rotulo, preco, embalagem.estoque(), 0, 0);
            }
            VariacaoProduto atual = existentes.remove(codigo);
            exigir(atual != null, "Embalagem inválida para este produto.");
            return new VariacaoProduto(codigo, rotulo, preco, atual.getEstoque(), atual.getEstoqueMinimo(), atual.getReservado());
        }).toList();
        exigir(existentes.values().stream().noneMatch(variacao -> variacao.getEstoque() > 0 || variacao.getReservado() > 0),
                "Só é possível remover embalagens com saldo e reservas zerados.");
        boolean legado = id != null && produto.getVariacoes().isEmpty() && produto.getPreco() != null;
        Double precoAvulso = legado ? anuncio.precoAvulso() : null;
        if (legado) {
            exigir(precoAvulso != null && Double.isFinite(precoAvulso)
                    && precoAvulso > 0 && precoAvulso <= 999999.99, "Informe um preço válido.");
        }
        if (situacao == Situacao.PUBLICADO) {
            exigir(!marca.isEmpty() && !animal.isEmpty() && !categoria.isEmpty()
                    && !fotos.isEmpty() && (!variacoes.isEmpty() || legado),
                    "Para publicar, informe marca, tipo de pet, categoria, foto e embalagem.");
        }

        produto.setNome(nome);
        produto.setTipoProduto(marca);
        produto.setTipoAnimal(animal);
        produto.setCategoria(categoria);
        produto.setDescricao(campo(anuncio.descricao(), 255));
        produto.setFotos(fotos);
        produto.setImagem(fotos.isEmpty() ? null : fotos.get(0));
        produto.setSituacao(situacao);
        produto.getVariacoes().clear();
        produto.getVariacoes().addAll(variacoes);
        produto.setPreco(ProdutoService.precoInicial(variacoes).orElse(precoAvulso));
        Produto salvo = produtos.saveAndFlush(produto);
        for (VariacaoProduto variacao : variacoes) {
            if (novas.contains(variacao.getCodigo())) {
                historico.registrar(salvo, variacao, vendedorId, "SALDO_INICIAL",
                        "Cadastro da embalagem", 0, variacao.getEstoque());
            }
        }
        return salvo;
    }

    static Optional<Double> precoInicial(List<VariacaoProduto> variacoes) {
        return variacoes.stream().filter(variacao -> variacao.getEstoque() > 0)
                .map(VariacaoProduto::getPreco).min(BigDecimal::compareTo)
                .or(() -> variacoes.stream().map(VariacaoProduto::getPreco).min(BigDecimal::compareTo))
                .map(BigDecimal::doubleValue);
    }

    public void validarCompra(Long id, String codigo, long quantidade, PagamentoCartItem item) {
        Produto produto = buscarPorId(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.CONFLICT, "Um produto do carrinho saiu do catálogo."));
        double preco = produto.getPreco();
        String nome = produto.getNome();
        if (!produto.getVariacoes().isEmpty()) {
            VariacaoProduto variacao = produto.getVariacoes().stream()
                    .filter(opcao -> opcao.getCodigo().equals(codigo)).findFirst().orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.CONFLICT, "Selecione novamente a embalagem de " + produto.getNome() + "."));
            if (variacao.getEstoque() < quantidade) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Estoque insuficiente para " + produto.getNome() + " · " + variacao.getEmbalagem() + ".");
            }
            preco = variacao.getPreco().doubleValue();
            nome += " · " + variacao.getEmbalagem();
        } else if (codigo != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A embalagem selecionada saiu do catálogo.");
        }
        if (item.getPrice() == null || Double.compare(preco, item.getPrice()) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O preço de " + nome + " mudou. Adicione o produto novamente pelo catálogo.");
        }
        item.setTitle(nome);
        item.setPrice(preco);
    }

    private String campo(String valor, int limite) {
        String texto = valor == null ? "" : valor.strip();
        exigir(texto.length() <= limite, "Um dos campos excede o limite de " + limite + " caracteres.");
        return texto;
    }

    private boolean fotoValida(String url) {
        if (url.matches("img/[a-zA-Z0-9_./-]+") && !url.contains("..")) {
            return true;
        }
        try {
            URI uri = URI.create(url);
            return Set.of("http", "https").contains(String.valueOf(uri.getScheme()).toLowerCase(Locale.ROOT))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
        }
    }

    public record Anuncio(String nome, String tipoProduto, String tipoAnimal, String categoria, String descricao,
                          List<String> fotos, Situacao situacao, Double precoAvulso, List<Embalagem> variacoes) {}

    public record Embalagem(String codigo, String embalagem, BigDecimal preco, Integer estoque) {}
}
