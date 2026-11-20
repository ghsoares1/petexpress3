package com.petexpress.petexpress_site_backend.service;

import com.petexpress.petexpress_site_backend.dto.CheckoutRequest;
import com.petexpress.petexpress_site_backend.dto.PedidoResponse;
import com.petexpress.petexpress_site_backend.model.*;
import com.petexpress.petexpress_site_backend.repository.PedidoRepository;
import com.petexpress.petexpress_site_backend.repository.ProdutoRepository;
import com.petexpress.petexpress_site_backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PedidosLoja {
    public static final String RETIRADA = "Retirar na loja";
    private static final String ENTREGA_UBER = "Entrega via Uber";
    private static final Set<String> ENTREGAS = Set.of(RETIRADA, "Receber em casa", ENTREGA_UBER);
    private final PedidoRepository pedidos;
    private final ProdutoRepository produtos;
    private final UsuarioRepository usuarios;
    private final HistoricoEstoque historico;
    private final long minutosReserva;
    private final PromocoesService promocoes;
    private final EntregasService entregas;

    public PedidosLoja(PedidoRepository pedidos, ProdutoRepository produtos, UsuarioRepository usuarios,
                       HistoricoEstoque historico, PromocoesService promocoes, EntregasService entregas, @Value("${app.reserva-minutos:30}") long minutosReserva) {
        this.entregas = entregas;
        this.promocoes = promocoes;
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.usuarios = usuarios;
        this.historico = historico;
        this.minutosReserva = minutosReserva;
    }

    @Transactional
    public Checkout iniciar(CheckoutRequest request, Long usuarioId) {
        if (request.chave() == null || request.itens() == null || request.itens().isEmpty()
                || request.itens().size() > 100 || request.formaEntrega() == null
                || !ENTREGAS.contains(request.formaEntrega())) {
            throw erro(HttpStatus.BAD_REQUEST, "Confira os itens e a forma de entrega.");
        }
        String chave = request.chave().toString();
        var existente = pedidos.findByUsuarioIdAndChaveCheckout(usuarioId, chave);
        if (existente.isPresent()) {
            Pedido anterior = existente.get();
            if (anterior.getStatus() != PedidoStatus.PENDENTE || !anterior.getReservaAte().isAfter(Instant.now())) {
                throw erro(HttpStatus.GONE, "Esta tentativa terminou. Inicie o checkout novamente.");
            }
            if (anterior.getUrlPagamento() == null) {
                throw erro(HttpStatus.CONFLICT, "O checkout já está sendo preparado. Tente novamente em instantes.");
            }
            return checkout(anterior, false);
        }

        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Conta não encontrada."));
        Pedido pedido = new Pedido();
        pedido.setChaveCheckout(chave);
        pedido.setReferenciaPagamento(UUID.randomUUID().toString());
        pedido.setNumeroPedido("PE" + System.currentTimeMillis() + pedido.getReferenciaPagamento().substring(0, 8).toUpperCase());
        pedido.setUsuario(usuario);
        pedido.setCliente(campo(request.cliente(), usuario.getNome() + " " + usuario.getSobrenome(), 255));
        pedido.setFormaEntrega(request.formaEntrega());
        if (!RETIRADA.equals(request.formaEntrega())) {
            pedido.setEnderecoEntrega(campo(request.enderecoEntrega(), "", 600));
            if (pedido.getEnderecoEntrega().isBlank()) {
                throw erro(HttpStatus.BAD_REQUEST, "Informe o endereço de entrega.");
            }
        }
        pedido.setTaxaEntrega(0.0);
        if (ENTREGA_UBER.equals(request.formaEntrega())) {
            var cotacao = entregas.cotacaoParaCheckout(request.cotacaoId(), usuarioId);
            pedido.setEntregaCotacaoId(cotacao.id());
            pedido.setTaxaEntrega(cotacao.valor().doubleValue());
            pedido.setCliente(cotacao.destino().nome());
            pedido.setEnderecoEntrega(cotacao.destino().endereco().linha());
        }
        pedido.setStatus(PedidoStatus.PENDENTE);
        pedido.setReservaAte(Instant.now().plus(minutosReserva, ChronoUnit.MINUTES));
        pedido.setDataCriacao(LocalDateTime.now());
        pedido.setDataAtualizacao(pedido.getDataCriacao());

        BigDecimal total = BigDecimal.valueOf(pedido.getTaxaEntrega());
        for (CheckoutRequest.Item solicitado : request.itens()) {
            ItemPedido item = reservar(pedido, solicitado);
            pedido.addItem(item);
            total = total.add(BigDecimal.valueOf(item.getPrecoUnitario()).multiply(BigDecimal.valueOf(item.getQuantidade())));
        }
        BigDecimal subtotal = total.subtract(BigDecimal.valueOf(pedido.getTaxaEntrega()));
        var desconto = promocoes.desconto(request.cupom(), subtotal, true);
        pedido.setCupomId(desconto.id());
        pedido.setCupomCodigo(desconto.codigo());
        pedido.setDescontoCupom(desconto.valor().doubleValue());
        pedido.setTotal(total.subtract(desconto.valor()).doubleValue());
        Pedido salvo = pedidos.saveAndFlush(pedido);
        if (salvo.getEntregaCotacaoId() != null) entregas.vincular(salvo.getEntregaCotacaoId(), salvo.getId());
        return checkout(salvo, true);
    }

    private ItemPedido reservar(Pedido pedido, CheckoutRequest.Item solicitado) {
        if (solicitado == null || solicitado.quantidade() == null || solicitado.quantidade() <= 0
                || solicitado.quantidade() > 999 || solicitado.preco() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Quantidade ou preço inválido.");
        }
        Produto produto = localizar(solicitado);
        VariacaoProduto variacao = null;
        String nome = produto.getNome();
        Double preco = produto.getPreco();
        if (!produto.getVariacoes().isEmpty()) {
            variacao = variacao(produto, solicitado.variacaoCodigo());
            nome += " · " + variacao.getEmbalagem();
            preco = variacao.getPreco().doubleValue();
        } else if (solicitado.variacaoCodigo() != null) {
            throw erro(HttpStatus.CONFLICT, "A embalagem saiu do catálogo.");
        }

        Double original = preco;
        if (preco != null) {
            preco = promocoes.preco(produto.getId(), solicitado.variacaoCodigo(), BigDecimal.valueOf(preco)).doubleValue();
        }
        if (preco == null || preco <= 0 || BigDecimal.valueOf(preco).compareTo(solicitado.preco()) != 0) {
            throw erro(HttpStatus.CONFLICT, "O preço de " + nome + " mudou. Remova o item e adicione a embalagem novamente.");
        }
        int quantidade = solicitado.quantidade();
        if (variacao != null) {
            if (variacao.getEstoque() < quantidade) {
                throw erro(HttpStatus.CONFLICT, "Estoque insuficiente para " + nome + ".");
            }
            alterarEstoque(produto, variacao, -quantidade, quantidade, pedido, "RESERVA");
        }

        ItemPedido item = new ItemPedido();
        item.setProdutoId(produto.getId().toString());
        item.setVariacaoCodigo(variacao == null ? null : variacao.getCodigo());
        item.setNome(nome);
        item.setCategoria(produto.getCategoria());
        item.setPrecoOriginal(original);
        item.setPrecoUnitario(preco);
        item.setQuantidade(quantidade);
        item.setImagem(produto.getImagem());
        return item;
    }

    private Produto localizar(CheckoutRequest.Item item) {
        if (item.produtoId() != null) {
            return produtos.findById(item.produtoId())
                    .filter(produto -> produto.getSituacao() == Produto.Situacao.PUBLICADO)
                    .orElseThrow(() -> erro(HttpStatus.CONFLICT, "Um produto do carrinho saiu do catálogo."));
        }
        return produtos.findBySituacaoAndNomeIgnoreCaseOrderByIdAsc(Produto.Situacao.PUBLICADO, campo(item.nome(), "", 255))
                .stream().findFirst().orElseThrow(() -> erro(HttpStatus.CONFLICT, "Adicione novamente o item pelo catálogo."));
    }

    @Transactional
    public void vincular(String referencia, String preferencia, String url) {
        Pedido pedido = buscar(referencia);
        if (pedido.getStatus() != PedidoStatus.PENDENTE || !pedido.getReservaAte().isAfter(Instant.now())) {
            throw erro(HttpStatus.GONE, "A reserva expirou.");
        }
        pedido.setPreferenceId(preferencia);
        pedido.setUrlPagamento(url);
    }

    @Transactional
    public void cancelarSemPreferencia(String referencia) {
        Pedido pedido = buscar(referencia);
        if (pedido.getStatus() == PedidoStatus.PENDENTE && pedido.getPreferenceId() == null) {
            finalizarReserva(pedido, false);
            pedido.setStatus(PedidoStatus.CANCELADO);
        }
    }

    @Transactional(readOnly = true)
    public List<String> reservasVencidas() {
        return pedidos.findByStatusAndReservaAteBeforeOrderByReservaAteAsc(
                PedidoStatus.PENDENTE, Instant.now().minus(10, ChronoUnit.MINUTES))
                .stream().map(Pedido::getReferenciaPagamento).toList();
    }

    @Transactional
    public void expirar(String referencia, boolean houveRecusa) {
        Pedido pedido = buscar(referencia);
        if (pedido.getStatus() == PedidoStatus.PENDENTE && pedido.getReservaAte().isBefore(Instant.now())) {
            finalizarReserva(pedido, false);
            pedido.setStatus(houveRecusa ? PedidoStatus.PAGAMENTO_FALHOU : PedidoStatus.CANCELADO);
        }
    }

    @Transactional(readOnly = true)
    public String referenciaDoCliente(long id, Long usuarioId) {
        Pedido pedido = pedidos.findById(id).filter(atual -> atual.getUsuario().getId().equals(usuarioId))
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
        return pedido.getReferenciaPagamento();
    }

    @Transactional
    public void confirmarPagamento(PagamentoGateway.Pagamento pagamento) {
        Pedido pedido = pagamento.referencia() == null ? null
                : pedidos.findByReferenciaPagamento(pagamento.referencia()).orElse(null);
        if (pedido == null || pagamento.id() == null || pagamento.valor() == null || !"BRL".equals(pagamento.moeda())
                || BigDecimal.valueOf(pedido.getTotal()).compareTo(pagamento.valor()) != 0) {
            return;
        }
        if ("approved".equals(pagamento.status())) {
            if (pedido.getStatus() == PedidoStatus.PENDENTE) {
                finalizarReserva(pedido, true);
                pedido.setStatus(PedidoStatus.PAGAMENTO_APROVADO);
                pedido.setPagoEm(LocalDateTime.now(java.time.ZoneId.of("America/Sao_Paulo")));
                pedido.setFormaPagamento(pagamento.forma());
                pedido.setPaymentId(pagamento.id());
            } else if (pedido.getStatus() != PedidoStatus.PAGAMENTO_EM_REVISAO
                    && (pedido.getStatus() != PedidoStatus.PAGAMENTO_APROVADO
                    || !pagamento.id().equals(pedido.getPaymentId()))) {
                pedido.setStatus(PedidoStatus.PAGAMENTO_EM_REVISAO);
                pedido.setPaymentId(pagamento.id());
            }
        } else if (Set.of("refunded", "charged_back", "cancelled").contains(pagamento.status())
                && pagamento.id().equals(pedido.getPaymentId())
                && pedido.getStatus() == PedidoStatus.PAGAMENTO_APROVADO) {
            pedido.setStatus(PedidoStatus.PAGAMENTO_EM_REVISAO);
        }
    }

    private void finalizarReserva(Pedido pedido, boolean venda) {
        for (ItemPedido item : pedido.getItens()) {
            if (item.getVariacaoCodigo() == null) continue;
            Produto produto = produtos.findById(Long.valueOf(item.getProdutoId())).orElseThrow();
            VariacaoProduto variacao = variacao(produto, item.getVariacaoCodigo());
            int quantidade = item.getQuantidade();
            alterarEstoque(produto, variacao, venda ? 0 : quantidade, -quantidade, pedido, venda ? "VENDA" : "LIBERACAO");
        }
        pedido.setDataAtualizacao(LocalDateTime.now());
    }

    private VariacaoProduto variacao(Produto produto, String codigo) {
        return produto.getVariacoes().stream().filter(item -> item.getCodigo().equals(codigo)).findFirst()
                .orElseThrow(() -> erro(HttpStatus.CONFLICT, "Selecione novamente a embalagem de " + produto.getNome() + "."));
    }

    private void alterarEstoque(Produto produto, VariacaoProduto variacao, int delta, int reserva, Pedido pedido, String tipo) {
        int anterior = variacao.getEstoque();
        int atual = anterior + delta;
        int reservado = variacao.getReservado() + reserva;
        if (atual < 0 || reservado < 0) throw new IllegalStateException("Saldo da reserva inconsistente.");
        var alterada = new VariacaoProduto(variacao.getCodigo(), variacao.getEmbalagem(), variacao.getPreco(),
                atual, variacao.getEstoqueMinimo(), reservado);
        var variacoes = produto.getVariacoes();
        variacoes.set(variacoes.indexOf(variacao), alterada);
        produto.setPreco(ProdutoService.precoInicial(variacoes).orElseThrow());
        historico.registrar(produto, alterada, 0L, tipo, "Pedido " + pedido.getNumeroPedido(), anterior, atual);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar() {
        return pedidos.findAllByOrderByDataCriacaoDesc().stream().map(PedidoResponse::from).toList();
    }

    @Transactional
    public PedidoResponse expedir(long id, ExpedicaoStatus proximo, String rastreio) {
        Pedido pedido = pedidos.findById(id).orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
        if (pedido.getStatus() != PedidoStatus.PAGAMENTO_APROVADO) {
            throw erro(HttpStatus.CONFLICT, "A expedição exige pagamento aprovado.");
        }
        ExpedicaoStatus atual = pedido.getExpedicao();
        if (ENTREGA_UBER.equals(pedido.getFormaEntrega()) && proximo != ExpedicaoStatus.EM_SEPARACAO && proximo != atual) {
            throw erro(HttpStatus.CONFLICT, "O envio e a conclusão serão atualizados pelo acompanhamento Uber Direct.");
        }
        boolean retirada = RETIRADA.equals(pedido.getFormaEntrega());
        ExpedicaoStatus permitido = switch (atual) {
            case AGUARDANDO -> ExpedicaoStatus.EM_SEPARACAO;
            case EM_SEPARACAO -> retirada ? ExpedicaoStatus.PRONTO_PARA_RETIRADA : ExpedicaoStatus.ENVIADO;
            case ENVIADO, PRONTO_PARA_RETIRADA -> ExpedicaoStatus.CONCLUIDO;
            case CONCLUIDO -> ExpedicaoStatus.CONCLUIDO;
        };
        if (proximo == null || (proximo != atual && proximo != permitido)) {
            throw erro(HttpStatus.BAD_REQUEST, "Atualize a expedição na ordem indicada.");
        }
        String codigo = campo(rastreio, "", 100);
        if (proximo == ExpedicaoStatus.ENVIADO) pedido.setRastreio(codigo);
        pedido.setExpedicao(proximo);
        pedido.setDataAtualizacao(LocalDateTime.now());
        return PedidoResponse.from(pedido);
    }

    private Pedido buscar(String referencia) {
        return pedidos.findByReferenciaPagamento(referencia)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
    }

    private Checkout checkout(Pedido pedido, boolean novo) {
        List<Linha> linhas = new ArrayList<>(pedido.getItens().stream()
                .map(item -> new Linha(item.getNome(), item.getQuantidade(), item.getPrecoUnitario())).toList());
        if (pedido.getDescontoCupom() != null && pedido.getDescontoCupom() > 0) {
            return new Checkout(pedido.getId(), pedido.getReferenciaPagamento(), pedido.getReservaAte(),
                    pedido.getUrlPagamento(), novo, List.of(new Linha("Pedido " + pedido.getNumeroPedido(), 1, pedido.getTotal())));
        }
        if (pedido.getTaxaEntrega() > 0) linhas.add(new Linha("Taxa de entrega", 1, pedido.getTaxaEntrega()));
        return new Checkout(pedido.getId(), pedido.getReferenciaPagamento(), pedido.getReservaAte(),
                pedido.getUrlPagamento(), novo, linhas);
    }

    private String campo(String valor, String padrao, int limite) {
        String texto = valor == null || valor.isBlank() ? padrao : valor.strip();
        if (texto.length() > limite) throw erro(HttpStatus.BAD_REQUEST, "Um campo do pedido excede o tamanho permitido.");
        return texto;
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }

    public record Linha(String title, int quantity, double unit_price) {}
    public record Checkout(Long id, String referencia, Instant reservaAte, String url, boolean novo, List<Linha> linhas) {}
}
